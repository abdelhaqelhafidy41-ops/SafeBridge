# SafeBridge 🛡️

[English](#english) | [العربية](#العربية)

---

## English

**SafeBridge** is an educational, standalone Android application demonstrating secure inter-process communication (IPC) via Android Binder, an isolated local filesystem Sandbox, and a restricted, policy-enforced Safe ADB Controller.

> **Important Boundary & Disclaimer:** SafeBridge is an independent educational architecture. It is **not** a clone or derivative of Shizuku or Rish, does not use Rikka packages or private keys, never requires root access, and strictly obeys standard Android security policies without using hidden or undocumented system APIs.

### Key Features

1. **Manager Application:**
   - Real-time service state dashboard (Running, Stopped, Error)
   - Interactive lifecycle control (Start, Stop, Restart)
   - Client authorization console: grant temporary access (15m, 1h, 24h), permanent, or one-time access
   - Live Binder telemetry: transaction counts, active DeathRecipient listeners

2. **Android Binder Service (`SafeBridgeService`):**
   - Implements standard AIDL (`ISafeBridgeService`, `ISafeBridgeCallback`)
   - Runs exclusively in the unprivileged app process (no root, no ADB server daemon)
   - Performs strict UID verification against `PackageManager.getPackagesForUid()` to prevent Intent and Binder spoofing
   - Uses `IBinder.DeathRecipient` on both client and service sides to handle unexpected process crashes

3. **Isolated Sandbox (`filesDir/sandbox/`):**
   - Confined strictly inside `context.filesDir/sandbox/`
   - Canonical path validation prevents path traversal (`../`, `..\`) and directory escaping
   - Strict task allowlist (`echo`, `get_status`, `list_sandbox`, `sha256`, `calc`)
   - Arbitrary shell commands (`sh`, `su`, `cat`, `rm -rf`) are strictly prohibited

4. **Interactive Demo Client (`SafeBridgeDemoClient`):**
   - Connects and binds to `SafeBridgeService` using explicit Intents
   - Demonstrates authorization requests, ping testing, and sandbox task execution
   - Tests callback delivery and automatic reconnection upon binder death

5. **Safe ADB Controller (`SafeAdbExecutor`):**
   - Strictly parameterized, non-arbitrary command allowlist (LOW and MEDIUM risk only)
   - HIGH risk commands (reboot, install/uninstall, root escalation, system deletion) are unconditionally rejected
   - Mandatory user confirmation dialog for MEDIUM risk operations (dumpsys, logcat, directory inspection)
   - Comprehensive audit logging with masked device IDs and redaction of sensitive credentials

6. **Automated GitHub Actions CI/CD (`.github/workflows/`):**
   - Automated builds on push/PR for Debug APK, Release APK, and App Bundle (`.aab`)
   - Automated testing and artifact uploads for instant mobile installation
   - See [GITHUB_WORKFLOW_GUIDE.md](GITHUB_WORKFLOW_GUIDE.md) for complete details.

---

## العربية

**SafeBridge** هو تطبيق تعليمي مستقل لنظام Android يشرح المفاهيم المتقدمة للتواصل الآمن بين العمليات (IPC) عبر Android Binder، مع بيئة عزل ملفات محلية (Sandbox)، وطبقة تحكم آمنة ومقيدة لأوامر ADB.

> **تنبيه وإخلاء مسؤولية أمني:** SafeBridge مشروع مستقل ومبني من الصفر لأغراض تعليمية. التطبيق **ليس** نسخة من Shizuku أو Rish، ولا يستخدم أي كود أو مفاتيح توقيع من Rikka، ولا يتطلب أو ينشئ أي خدمة Root، ويلتزم التزامًا تامًا بالحدود الأمنية الرسمية لنظام Android.

### المكونات الرئيسية

1. **تطبيق المدير (Manager UI):**
   - لوحة تحكم حية لعرض حالة الخدمة (متوقفة، قيد التشغيل، أخطاء).
   - أزرار التحكم بدورة الحياة: تشغيل، إيقاف، وإعادة تشغيل الخدمة.
   - إدارة طلبات العملاء: إمكانية منح صلاحية مؤقتة (15 دقيقة، ساعة، 24 ساعة)، صلاحية دائمة، أو صلاحية لمرة واحدة فقط.
   - إحصائيات Binder الحية وعدد المستمعين النشطين.

2. **خدمة Binder الآمنة (`SafeBridgeService`):**
   - تعرّف واجهات AIDL قياسية ونظيفة.
   - تعمل داخل عملية التطبيق العادية وغير المميزة (Unprivileged process).
   - فحص صارم لـ UID واسم الحزمة عبر `PackageManager` لمنع انتحال الهوية (Spoofing).
   - معالجة موت العمليات باستخدام `IBinder.DeathRecipient`.

3. **بيئة العزل (Sandbox):**
   - تقع حصريًا في `context.filesDir/sandbox/`.
   - منع ثغرات Path Traversal (`../`) وفحص المسار المعياري الحقيقي (Canonical Path).
   - تنفيذ مهام وهمية آمنة ومدرجة في Allowlist مسبقة (`echo`، `get_status`، `list_sandbox`، `sha256`، `calc`).
   - منع تنفيذ أوامر Shell حقيقية.

4. **العميل التجريبي (Demo Client):**
   - يحاكي تطبيق عميل يتصل بالخدمة، يطلب الموافقة، وينفذ المهام ويستقبل Callbacks غير المتزامنة.

5. **منفذ ADB الآمن والمقيد (`SafeAdbExecutor`):**
   - أوامر معلنة مسبقًا ومقسمة حسب درجة الخطورة (منخفضة ومتوسطة فقط).
   - الرفض التام لأي أمر مرتفع الخطورة (HIGH) مثل الروت أو إعادة تشغيل الجهاز.
   - نافذة موافقة صريحة للعمليات المتوسطة (MEDIUM).
   - سجل تدقيق كامل مع إخفاء المعرفات الحساسة.
