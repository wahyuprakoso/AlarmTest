package com.alarmtest

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.facebook.react.bridge.*
import java.util.*

class AlarmManagerModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    
    override fun getName(): String = "AlarmManagerModule"
    
    @ReactMethod
    fun scheduleAlarm(alarmId: String, title: String, timestampMs: Double, promise: Promise) {
        try {
            val context = reactApplicationContext
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Create intent for the alarm receiver
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, title)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val triggerTime = timestampMs.toLong()
            
            // Schedule the exact alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            
            Log.d("AlarmManagerModule", "Alarm scheduled for ${Date(triggerTime)} with ID: $alarmId")
            promise.resolve("Alarm scheduled successfully")
            
        } catch (e: Exception) {
            Log.e("AlarmManagerModule", "Error scheduling alarm", e)
            promise.reject("SCHEDULE_ERROR", "Failed to schedule alarm: ${e.message}")
        }
    }
    
    @ReactMethod
    fun cancelAlarm(alarmId: String, promise: Promise) {
        try {
            val context = reactApplicationContext
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            
            Log.d("AlarmManagerModule", "Alarm cancelled with ID: $alarmId")
            promise.resolve("Alarm cancelled successfully")
            
        } catch (e: Exception) {
            Log.e("AlarmManagerModule", "Error cancelling alarm", e)
            promise.reject("CANCEL_ERROR", "Failed to cancel alarm: ${e.message}")
        }
    }
    
    @ReactMethod
    fun checkExactAlarmPermission(promise: Promise) {
        try {
            val context = reactApplicationContext
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val hasPermission = alarmManager.canScheduleExactAlarms()
                promise.resolve(hasPermission)
            } else {
                promise.resolve(true) // Permission not needed for older versions
            }
        } catch (e: Exception) {
            Log.e("AlarmManagerModule", "Error checking exact alarm permission", e)
            promise.reject("PERMISSION_ERROR", "Failed to check permission: ${e.message}")
        }
    }
    
    @ReactMethod
    fun requestExactAlarmPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = reactApplicationContext
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("AlarmManagerModule", "Error requesting exact alarm permission", e)
        }
    }
}