package com.example.safebridge

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.safebridge.adb.AdbArgumentValidator
import com.example.safebridge.adb.AdbCommand
import com.example.safebridge.adb.AdbDevice
import com.example.safebridge.adb.AdbRiskLevel
import com.example.safebridge.adb.SafeAdbExecutor
import com.example.safebridge.data.SafeBridgeDatabase
import com.example.safebridge.data.model.ClientPermissionEntity
import com.example.safebridge.permission.AccessCheckResult
import com.example.safebridge.permission.PermissionManager
import com.example.safebridge.sandbox.SandboxManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SafeBridgeSecurityTest {

    private lateinit var context: Context
    private lateinit var database: SafeBridgeDatabase
    private lateinit var sandboxManager: SandboxManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var adbExecutor: SafeAdbExecutor

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SafeBridgeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sandboxManager = SandboxManager(context)
        permissionManager = PermissionManager(context)
        adbExecutor = SafeAdbExecutor(context, sandboxManager)
    }

    @After
    fun teardown() {
        database.close()
    }

    // --- Permissions Tests ---

    @Test
    fun testGrantPermission() = runBlocking {
        val testPkg = context.packageName
        val myUid = android.os.Process.myUid()

        permissionManager.grantAccess(testPkg, durationMinutes = 60, oneTime = false)
        val result = permissionManager.checkAccess(myUid, testPkg)

        assertTrue("Expected Granted result", result is AccessCheckResult.Granted)
        val granted = (result as AccessCheckResult.Granted).entity
        assertTrue(granted.granted)
        assertEquals(1, granted.usedCount)
    }

    @Test
    fun testDenyPermission() = runBlocking {
        val testPkg = context.packageName
        val myUid = android.os.Process.myUid()

        // Grant access first, then revoke
        permissionManager.grantAccess(testPkg, durationMinutes = 60, oneTime = false)
        permissionManager.revokeAccess(testPkg)
        val result = permissionManager.checkAccess(myUid, testPkg)

        assertTrue("Expected Denied result", result is AccessCheckResult.Denied)
    }

    @Test
    fun testPermissionExpiration() = runBlocking {
        val testPkg = context.packageName
        val myUid = android.os.Process.myUid()

        // Expired in the past
        val expiredEntity = ClientPermissionEntity(
            packageName = testPkg,
            uid = myUid,
            granted = true,
            grantedAt = System.currentTimeMillis() - 100000,
            expiresAt = System.currentTimeMillis() - 50000,
            oneTime = false
        )
        val dao = database.clientPermissionDao()
        dao.insertOrUpdate(expiredEntity)

        assertTrue(expiredEntity.isExpired)
        assertFalse(expiredEntity.isValid)
    }

    @Test
    fun testOneTimePermissionExhaustion() = runBlocking {
        val testPkg = context.packageName
        val myUid = android.os.Process.myUid()

        permissionManager.grantAccess(testPkg, durationMinutes = null, oneTime = true)

        // First invocation succeeds
        val check1 = permissionManager.checkAccess(myUid, testPkg)
        assertTrue(check1 is AccessCheckResult.Granted)

        // Second invocation must be rejected as exhausted
        val check2 = permissionManager.checkAccess(myUid, testPkg)
        assertTrue(check2 is AccessCheckResult.OneTimeExhausted)
    }

    @Test
    fun testSpoofedUidRejection() = runBlocking {
        val claimedPkg = "com.google.android.fakeapp"
        val fakeUid = 99999

        val ownership = permissionManager.verifyUidOwnership(fakeUid, claimedPkg)
        assertFalse("UID ownership check must reject spoofed UID", ownership)

        val result = permissionManager.checkAccess(fakeUid, claimedPkg)
        assertTrue(result is AccessCheckResult.SpoofedUid)
    }

    // --- Sandbox Security Tests ---

    @Test
    fun testSandboxValidFileCreationAndRead() {
        val filename = "test_doc.txt"
        val content = "Safe content inside sandbox"

        val createRes = sandboxManager.createFile(filename, content)
        assertTrue(createRes.isSuccess)

        val readRes = sandboxManager.readFile(filename)
        assertTrue(readRes.isSuccess)
        assertEquals(content, readRes.getOrNull())
    }

    @Test
    fun testSandboxPathTraversalBlocked() {
        val evilPaths = listOf(
            "../etc/passwd",
            "..\\windows\\system32",
            "dir/../../escape.txt",
            "....//test",
            "normal/../../../data/system"
        )

        for (path in evilPaths) {
            try {
                sandboxManager.validateAndResolvePath(path)
                fail("Path '$path' should have thrown SecurityException for path traversal!")
            } catch (e: SecurityException) {
                // Expected
                assertTrue(e.message?.contains("traversal", ignoreCase = true) == true ||
                        e.message?.contains("forbidden", ignoreCase = true) == true ||
                        e.message?.contains("boundary", ignoreCase = true) == true)
            }
        }
    }

    @Test
    fun testSandboxAbsolutePathBlocked() {
        val absolutePaths = listOf(
            "/system/bin/sh",
            "/data/data/com.example/files",
            "/sdcard/DCIM",
            "C:\\secret"
        )

        for (path in absolutePaths) {
            try {
                sandboxManager.validateAndResolvePath(path)
                fail("Path '$path' should have thrown SecurityException for absolute path!")
            } catch (e: SecurityException) {
                // Expected
                assertTrue(e.message?.contains("forbidden", ignoreCase = true) == true ||
                        e.message?.contains("Absolute", ignoreCase = true) == true)
            }
        }
    }

    @Test
    fun testSandboxAllowlistedTasks() {
        val echoRes = sandboxManager.executeTask("echo", "Hello Android")
        assertTrue(echoRes.isSuccess)
        assertEquals("ECHO: Hello Android", echoRes.getOrNull())

        val statusRes = sandboxManager.executeTask("get_status", "")
        assertTrue(statusRes.isSuccess)
        assertTrue(statusRes.getOrNull()?.contains("Sandbox Status") == true)
    }

    @Test
    fun testSandboxDisallowedTaskBlocked() {
        val evilTasks = listOf("rm -rf /", "cat", "sh", "su", "chmod", "reboot", "unknown_task")
        for (task in evilTasks) {
            val res = sandboxManager.executeTask(task, "")
            assertFalse("Disallowed task '$task' must be rejected", res.isSuccess)
        }
    }

    // --- ADB Argument & Policy Tests ---

    @Test
    fun testAdbArgumentValidatorShellInjectionBlocked() {
        val dangerousInputs = listOf(
            "pkg; rm -rf /",
            "test | cat",
            "arg && whoami",
            "`id`",
            "test\ncat",
            "\$USER",
            "file>out.txt",
            "test<in.txt"
        )

        for (input in dangerousInputs) {
            val res = AdbArgumentValidator.validateArgument(input)
            assertFalse("Injection token in '$input' must be rejected", res.isSuccess)
        }
    }

    @Test
    fun testAdbArgumentValidatorPackageName() {
        assertTrue(AdbArgumentValidator.validatePackageName("com.example.app").isSuccess)
        assertTrue(AdbArgumentValidator.validatePackageName("com.demo_app.pkg123").isSuccess)

        assertFalse(AdbArgumentValidator.validatePackageName("invalid package;").isSuccess)
        assertFalse(AdbArgumentValidator.validatePackageName("../bad").isSuccess)
        assertFalse(AdbArgumentValidator.validatePackageName("").isSuccess)
    }

    @Test
    fun testAdbRiskLevelPolicyHighRiskRejected() = runBlocking {
        val device = AdbDevice("dev1", "Test Dev", "127.0.0.1", 5555, true)
        val highRiskCommand = AdbCommand.ForbiddenAction("Reboot")

        val result = adbExecutor.executeCommand(device, highRiskCommand, userExplicitlyApproved = true)
        assertFalse("High risk command must be blocked unconditionally", result.success)
        assertTrue(result.output.contains("SECURITY POLICY REJECTION"))
    }

    @Test
    fun testAdbRiskLevelMediumRequiresApproval() = runBlocking {
        val device = AdbDevice("dev1", "Test Dev", "127.0.0.1", 5555, true)
        val mediumCommand = AdbCommand.DumpsysActivities

        // Attempt without user approval
        val result = adbExecutor.executeCommand(device, mediumCommand, userExplicitlyApproved = false)
        assertFalse("Medium risk command must require user approval", result.success)
        assertTrue(result.output.contains("User approval required"))
    }
}
