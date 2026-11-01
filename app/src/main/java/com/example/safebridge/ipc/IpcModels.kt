package com.example.safebridge.ipc

import android.os.Parcel
import android.os.Parcelable

/**
 * Parcelable request payload for tasks dispatched over Binder IPC.
 */
data class TaskRequest(
    val taskId: String,
    val callingPackage: String,
    val command: String,
    val argument: String,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(taskId)
        parcel.writeString(callingPackage)
        parcel.writeString(command)
        parcel.writeString(argument)
        parcel.writeLong(timestamp)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TaskRequest> {
        override fun createFromParcel(parcel: Parcel): TaskRequest = TaskRequest(parcel)
        override fun newArray(size: Int): Array<TaskRequest?> = arrayOfNulls(size)
    }
}

/**
 * Parcelable execution outcome returned from Binder service.
 */
data class TaskResult(
    val taskId: String,
    val success: Boolean,
    val message: String,
    val output: String,
    val executionTimeMs: Long
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(taskId)
        parcel.writeByte(if (success) 1 else 0)
        parcel.writeString(message)
        parcel.writeString(output)
        parcel.writeLong(executionTimeMs)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TaskResult> {
        override fun createFromParcel(parcel: Parcel): TaskResult = TaskResult(parcel)
        override fun newArray(size: Int): Array<TaskResult?> = arrayOfNulls(size)
    }
}

/**
 * Service health status data class.
 */
data class ServiceStatus(
    val isRunning: Boolean,
    val version: Int,
    val uptimeMillis: Long,
    val totalRequestsServed: Long,
    val activeRegisteredCallbacks: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readInt(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (isRunning) 1 else 0)
        parcel.writeInt(version)
        parcel.writeLong(uptimeMillis)
        parcel.writeLong(totalRequestsServed)
        parcel.writeInt(activeRegisteredCallbacks)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ServiceStatus> {
        override fun createFromParcel(parcel: Parcel): ServiceStatus = ServiceStatus(parcel)
        override fun newArray(size: Int): Array<ServiceStatus?> = arrayOfNulls(size)
    }
}
