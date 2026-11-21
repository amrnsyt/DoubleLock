package com.amrnsyt.doublelock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

object DeviceAdminManager {

    fun component(context: Context): ComponentName =
        ComponentName(context, DoubleLockDeviceAdminReceiver::class.java)

    fun isActive(context: Context): Boolean {
        val manager = context.getSystemService(DevicePolicyManager::class.java)
        return manager.isAdminActive(component(context))
    }

    fun lockNow(context: Context): Boolean {
        val manager = context.getSystemService(DevicePolicyManager::class.java)

        if (!manager.isAdminActive(component(context))) return false

        return try {
            manager.lockNow()
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
