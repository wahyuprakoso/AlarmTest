package com.alarmtest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Boot received: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d("BootReceiver", "🔄 Device boot/app update detected")
                
                // Start foreground service to maintain app presence
                startPersistenceService(context)
                
                // Schedule alarm rescheduling
                scheduleAlarmRescheduling(context)
                
                // Check and request optimizations after boot
                checkOptimizationsAfterBoot(context)
            }
        }
    }
    
    private fun startPersistenceService(context: Context) {
        try {
            val serviceIntent = Intent(context, PersistenceService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.d("BootReceiver", "✅ Persistence service started after boot")
            
            // Also try to launch the app silently to ensure it stays in memory
            launchAppSilently(context)
            
        } catch (e: Exception) {
            Log.e("BootReceiver", "❌ Error starting persistence service", e)
        }
    }
    
    private fun launchAppSilently(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("silent_launch", true)
                putExtra("reschedule_alarms", true)
            }
            
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                Log.d("BootReceiver", "🚀 App launched silently after boot")
            }
        } catch (e: Exception) {
            Log.e("BootReceiver", "Error launching app silently", e)
        }
    }
    
    private fun checkOptimizationsAfterBoot(context: Context) {
        try {
            val status = BatteryOptimizationHelper.checkAllOptimizations(context)
            Log.d("BootReceiver", "Device: ${status.manufacturer} ${status.model}, Android: ${status.androidVersion}")
            
            if (status.batteryOptimized) {
                Log.w("BootReceiver", "⚠️ Battery optimization still enabled after boot")
                // Schedule optimization request for later when user opens app
                scheduleOptimizationRequest(context)
            } else {
                Log.d("BootReceiver", "✅ Battery optimization disabled")
            }
        } catch (e: Exception) {
            Log.e("BootReceiver", "Error checking optimizations", e)
        }
    }
    
    private fun scheduleOptimizationRequest(context: Context) {
        try {
            // Store flag to show optimization request when app next opens
            val prefs = context.getSharedPreferences("alarm_settings", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("needs_optimization_check", true)
                .putLong("last_boot_time", System.currentTimeMillis())
                .apply()
            
            Log.d("BootReceiver", "📅 Scheduled optimization check for next app launch")
        } catch (e: Exception) {
            Log.e("BootReceiver", "Error scheduling optimization request", e)
        }
    }
    
    private fun scheduleAlarmRescheduling(context: Context) {
        try {
            // Store flag to reschedule alarms when app next opens
            val prefs = context.getSharedPreferences("alarm_settings", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("needs_alarm_reschedule", true)
                .putLong("last_boot_time", System.currentTimeMillis())
                .apply()
            
            Log.d("BootReceiver", "📅 Scheduled alarm rescheduling for next app launch")
        } catch (e: Exception) {
            Log.e("BootReceiver", "Error scheduling alarm rescheduling", e)
        }
    }
}