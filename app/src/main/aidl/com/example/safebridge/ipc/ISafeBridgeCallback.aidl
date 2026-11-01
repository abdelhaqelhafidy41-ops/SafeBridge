// ISafeBridgeCallback.aidl
package com.example.safebridge.ipc;

interface ISafeBridgeCallback {
    void onTaskCompleted(String taskId, boolean success, String message, String output);
    void onAccessRevoked(String reason);
    void onServiceStatusChanged(String statusMessage);
}
