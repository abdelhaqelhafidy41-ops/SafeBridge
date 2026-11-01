// ISafeBridgeService.aidl
package com.example.safebridge.ipc;

import com.example.safebridge.ipc.ISafeBridgeCallback;

interface ISafeBridgeService {
    int getServiceVersion();
    String ping();
    boolean requestClientAccess(String packageName, String reason, boolean oneTime);
    boolean revokeClientAccess(String packageName);
    String getClientStatus(String packageName);
    String executeSandboxTask(String packageName, String command, String argument);
    boolean registerCallback(String packageName, ISafeBridgeCallback callback);
    boolean unregisterCallback(String packageName, ISafeBridgeCallback callback);
}
