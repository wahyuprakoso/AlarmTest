package com.alarmtest

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationHelper {
    
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // Not applicable for older versions
        }
    }
    
    fun requestDisableBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(context)) {
                try {
                    // First try the direct request
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Log.d("BatteryOptimization", "Requesting battery optimization exemption")
                } catch (e: Exception) {
                    Log.e("BatteryOptimization", "Error requesting battery optimization exemption", e)
                    // Fallback to general battery optimization settings
                    openBatteryOptimizationSettings(context)
                }
            } else {
                Log.d("BatteryOptimization", "Already ignoring battery optimizations")
            }
        }
        
        // Always request auto-start permission regardless of battery optimization status
        requestAutoStartPermission(context)
    }
    
    private fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("BatteryOptimization", "Opening battery optimization settings")
        } catch (e: Exception) {
            Log.e("BatteryOptimization", "Error opening battery optimization settings", e)
        }
    }
    
    // OEM-specific auto-start management
    fun requestAutoStartPermission(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        Log.d("BatteryOptimization", "Device manufacturer: $manufacturer")
        
        when {
            manufacturer.contains("xiaomi") -> openXiaomiAutoStart(context)
            manufacturer.contains("huawei") -> openHuaweiProtectedApps(context)
            manufacturer.contains("oppo") -> openOppoAutoStart(context)
            manufacturer.contains("vivo") -> openVivoAutoStart(context)
            manufacturer.contains("oneplus") -> openOnePlusBatteryOptimization(context)
            manufacturer.contains("samsung") -> openSamsungAutoStart(context)
            else -> {
                Log.d("BatteryOptimization", "No specific auto-start settings for $manufacturer")
                openGeneralSettings(context)
            }
        }
    }
    
    private fun openXiaomiAutoStart(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName("com.miui.securitycenter", 
                    "com.miui.permcenter.autostart.AutoStartManagementActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("BatteryOptimization", "Opening Xiaomi auto-start settings")
        } catch (e: Exception) {
            Log.e("BatteryOptimization", "Error opening Xiaomi settings", e)
            openGeneralSettings(context)
        }
    }
    
    private fun openHuaweiProtectedApps(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("BatteryOptimization", "Opening Huawei protected apps")
        } catch (e: Exception) {
            Log.e("BatteryOptimization", "Error opening Huawei settings", e)
            openGeneralSettings(context)
        }
    }
    
    private fun openOppoAutoStart(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName("com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("BatteryOptimization", "Opening Oppo auto-start settings")
        } catch (e: Exception) {
            Log.e("BatteryOptimization", "Error opening Oppo settings", e)
            openGeneralSettings(context)
        }
    }
    
    private fun openVivoAutoStart(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName("com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("BatteryOptimization", "Opening Vivo auto-start settings")
        } catch (e: Exception) {
            Log.e("BatteryOptimization", "Error opening Vivo settings", e)
            openGeneralSettings(context)
        }
    }
    
    private fun openOnePlusBatteryOptimization(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName("com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("BatteryOptimization", "Opening OnePlus battery optimization")
        } catch (e: Exception) {
            Log.e("BatteryOptimization", "Error opening OnePlus settings", e)
            openGeneralSettings(context)
        }
    }
    
    private fun openSamsungAutoStart(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName("com.samsung.android.lool",
                    "com.samsung.android.sm.battery.ui.BatteryActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("BatteryOptimization", "Opening Samsung battery settings")
        } catch (e: Exception) {
            Log.e("BatteryOptimization", "Error opening Samsung settings", e)
            openGeneralSettings(context)
        }
    }
    
    private fun openGeneralSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("BatteryOptimization", "Opening general app settings")
        } catch (e: Exception) {
            Log.e("BatteryOptimization", "Error opening general settings", e)
        }
    }
    
    // Notification settings
    fun openNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent().apply {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            Log.d("BatteryOptimization", "Opening notification settings")
        } catch (e: Exception) {
            Log.e("BatteryOptimization", "Error opening notification settings", e)
        }
    }
    
    // Check all critical permissions
    fun checkAllOptimizations(context: Context): OptimizationStatus {
        return OptimizationStatus(
            batteryOptimized = !isIgnoringBatteryOptimizations(context),
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.SDK_INT
        )
    }
    
    data class OptimizationStatus(
        val batteryOptimized: Boolean,
        val manufacturer: String,
        val model: String,
        val androidVersion: Int
    )
}