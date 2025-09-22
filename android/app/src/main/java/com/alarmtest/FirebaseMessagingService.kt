package com.alarmtest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCMService", "From: ${remoteMessage.from}")
        Log.d("FCMService", "Message received: ${remoteMessage.data}")
        Log.d("FCMService", "Notification: ${remoteMessage.notification}")

        // Always trigger immediate alarm for any notification
        Log.d("FCMService", "🚨 IMMEDIATE ALARM EXECUTION - BYPASSING ALL DELAYS!")
        handleImmediateAlarm(remoteMessage)
        
        // ALSO trigger direct execution for maximum reliability
        triggerDirectAlarm(remoteMessage)
        
        // Force create notification manager for debugging
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        Log.d("FCMService", "Notifications enabled: ${notificationManager.areNotificationsEnabled()}")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            Log.d("FCMService", "Channel importance: ${channel?.importance}")
        }
    }

    override fun onNewToken(token: String) {
        Log.d("FCMService", "Refreshed token: $token")
        // Send token to your app server if needed
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Emergency Alarm Notifications"
            val descriptionText = "Critical emergency alarm notifications - bypasses silent mode"
            val importance = NotificationManager.IMPORTANCE_MAX // Maximum importance
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 1000, 1000)
                setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI, null)
                setBypassDnd(true) // CRITICAL: Bypass Do Not Disturb mode
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                
                // EMERGENCY: Allow notification to override silent mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }
            
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d("FCMService", "🔔 Emergency notification channel created with MAXIMUM importance")
        }
    }
    
    private fun showNotification(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Alarm"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Your alarm is ringing!"
        
        // Create intent to open app when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true) // Make notification persistent
            .setVibrate(longArrayOf(0, 1000, 1000, 1000))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun handleImmediateAlarm(remoteMessage: RemoteMessage) {
        Log.d("FCMService", "🚨 IMMEDIATE ALARM EXECUTION - NO DELAY, NO SCHEDULING!")
        
        val title = remoteMessage.notification?.title ?: remoteMessage.data["alarm_title"] ?: remoteMessage.data["title"] ?: "Alarm"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["alarm_message"] ?: remoteMessage.data["body"] ?: "Your alarm is ringing!"
        val alarmId = "immediate_${System.currentTimeMillis()}"
        val fullTitle = "$title - $body"
        
        Log.d("FCMService", "🔥 EXECUTING ALARM IMMEDIATELY: $fullTitle")
        
        try {
            // IMMEDIATE execution - no delays, no scheduling
            executeImmediateAlarm(alarmId, fullTitle)
            
            // Also show notification
            showNotification(remoteMessage)
            
            Log.d("FCMService", "⚡ Immediate alarm execution completed")
            
        } catch (e: Exception) {
            Log.e("FCMService", "❌ Error in immediate alarm execution", e)
            // Fallback: still try to execute
            executeImmediateAlarm(alarmId, fullTitle)
        }
    }
    
    private fun scheduleExactAlarm(alarmId: String, fullTitle: String, delaySeconds: Int) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)
            
            // Create intent for AlarmReceiver
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_FIREBASE_ALARM
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, fullTitle)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                alarmId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Use exact alarm for precise timing even in background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            
            Log.d("FCMService", "⏰ EXACT alarm scheduled for ${delaySeconds}s using AlarmManager (background-safe)")
            
        } catch (e: Exception) {
            Log.e("FCMService", "Error scheduling exact alarm, using immediate execution", e)
            executeImmediateAlarm(alarmId, fullTitle)
        }
    }
    
    private fun executeImmediateAlarm(alarmId: String, fullTitle: String) {
        Log.d("FCMService", "🚨 EXECUTING IMMEDIATE ALARM NOW! ID: $alarmId")
        
        try {
            // Start foreground service IMMEDIATELY for background alarm
            val serviceIntent = Intent(this, AlarmForegroundService::class.java).apply {
                action = AlarmForegroundService.ACTION_START_ALARM
                putExtra(AlarmForegroundService.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmForegroundService.EXTRA_ALARM_TITLE, fullTitle)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d("FCMService", "✅ Immediate foreground service started")
            
            // ALSO trigger direct AlarmReceiver for immediate response
            val directIntent = Intent().apply {
                action = AlarmReceiver.ACTION_FIREBASE_ALARM
                setClass(this@FirebaseMessagingService, AlarmReceiver::class.java)
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, fullTitle)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            
            val alarmReceiver = AlarmReceiver()
            alarmReceiver.onReceive(this, directIntent)
            Log.d("FCMService", "✅ Direct AlarmReceiver call completed")
            
        } catch (e: Exception) {
            Log.e("FCMService", "❌ Error in immediate alarm execution", e)
            
            // Last resort: broadcast
            try {
                val intent = Intent().apply {
                    action = AlarmReceiver.ACTION_FIREBASE_ALARM
                    setClass(this@FirebaseMessagingService, AlarmReceiver::class.java)
                    putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, fullTitle)
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
                sendBroadcast(intent)
                Log.d("FCMService", "✅ Last resort broadcast sent")
            } catch (broadcastException: Exception) {
                Log.e("FCMService", "❌ All alarm methods failed", broadcastException)
            }
        }
    }
    
    private fun performComprehensiveOptimizationCheck() {
        try {
            val status = BatteryOptimizationHelper.checkAllOptimizations(this)
            Log.d("FCMService", "📱 Device: ${status.manufacturer} ${status.model}, Android: ${status.androidVersion}")
            
            if (status.batteryOptimized) {
                Log.w("FCMService", "⚠️ Battery optimization enabled - requesting exemption")
                BatteryOptimizationHelper.requestDisableBatteryOptimization(this)
                
                // Also request OEM-specific permissions
                requestOEMOptimizations(status.manufacturer)
            } else {
                Log.d("FCMService", "✅ Battery optimization already disabled")
            }
        } catch (e: Exception) {
            Log.e("FCMService", "Error checking optimizations", e)
        }
    }
    
    private fun requestOEMOptimizations(manufacturer: String) {
        try {
            // Store request for later when user opens app
            val prefs = getSharedPreferences("alarm_settings", MODE_PRIVATE)
            val requestCount = prefs.getInt("oem_request_count", 0)
            
            // Only request OEM settings max 3 times to avoid annoying user
            if (requestCount < 3) {
                Log.d("FCMService", "🔧 Requesting OEM-specific optimizations for $manufacturer")
                BatteryOptimizationHelper.requestAutoStartPermission(this)
                
                prefs.edit()
                    .putInt("oem_request_count", requestCount + 1)
                    .putLong("last_oem_request", System.currentTimeMillis())
                    .apply()
            } else {
                Log.d("FCMService", "⏭️ OEM optimization requests limit reached")
            }
        } catch (e: Exception) {
            Log.e("FCMService", "Error requesting OEM optimizations", e)
        }
    }
    
    private fun startPersistenceServiceIfNeeded() {
        try {
            val serviceIntent = Intent(this, PersistenceService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            Log.d("FCMService", "🔄 Persistence service started/maintained")
        } catch (e: Exception) {
            Log.e("FCMService", "Error starting persistence service", e)
        }
    }
    
    private fun handleAlarmFromBackground(remoteMessage: RemoteMessage) {
        try {
            Log.d("FCMService", "🚨 PROCESSING ALARM FROM BACKGROUND - IMMEDIATE EXECUTION")
            
            val data = remoteMessage.data
            val title = data["alarm_title"] ?: remoteMessage.notification?.title ?: "Remote Alarm"
            val message = data["alarm_message"] ?: remoteMessage.notification?.body ?: "Alarm dari push notification"
            
            // Get delay from data (default 3 seconds for background processing)
            val delaySeconds = data["delay"]?.toIntOrNull() ?: 3
            
            Log.d("FCMService", "🚨 IMMEDIATE BACKGROUND ALARM: $title in ${delaySeconds}s")
            
            // Execute immediate alarm with delay
            executeImmediateAlarmWithDelay(title, message, delaySeconds)
            
            // Show notification that alarm will trigger
            showAlarmTriggerNotification(title, delaySeconds)
            
        } catch (e: Exception) {
            Log.e("FCMService", "❌ Error processing immediate alarm from background", e)
        }
    }
    
    private fun executeImmediateAlarmWithDelay(title: String, message: String, delaySeconds: Int) {
        try {
            val alarmId = "immediate_bg_${System.currentTimeMillis()}"
            
            Log.d("FCMService", "⚡ EXECUTING IMMEDIATE ALARM with ${delaySeconds}s delay")
            
            // Use AlarmManager for precise timing even in background
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)
            
            // Create intent for immediate alarm execution
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_FIREBASE_ALARM
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, "$title - $message")
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                alarmId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Use exact alarm for immediate execution
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            
            Log.d("FCMService", "⏰ IMMEDIATE alarm scheduled for ${delaySeconds}s using AlarmManager")
            
            // ALSO try direct execution as backup
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                Log.d("FCMService", "🔄 Backup direct alarm execution")
                executeImmediateAlarm(alarmId, "$title - $message")
            }, (delaySeconds * 1000L) + 500) // +500ms delay for backup
            
        } catch (e: Exception) {
            Log.e("FCMService", "Error executing immediate alarm", e)
            // Fallback to direct execution
            val alarmId = "fallback_${System.currentTimeMillis()}"
            executeImmediateAlarm(alarmId, "$title - $message")
        }
    }
    
    private fun showAlarmTriggerNotification(title: String, delaySeconds: Int) {
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("⏰ Alarm akan berbunyi")
                .setContentText("$title dalam ${delaySeconds} detik")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setTimeoutAfter(delaySeconds * 1000L + 2000) // Auto dismiss after alarm + 2s
                .build()
            
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify("trigger_${System.currentTimeMillis()}".hashCode(), notification)
            
            Log.d("FCMService", "📱 Alarm trigger notification shown")
            
        } catch (e: Exception) {
            Log.e("FCMService", "Error showing alarm trigger notification", e)
        }
    }
    
    private fun triggerDirectAlarm(remoteMessage: RemoteMessage) {
        Log.d("FCMService", "🔥 DIRECT ALARM EXECUTION - NO DELAYS!")
        
        try {
            val title = remoteMessage.notification?.title ?: remoteMessage.data["alarm_title"] ?: "Direct Alarm"
            val message = remoteMessage.notification?.body ?: remoteMessage.data["alarm_message"] ?: "Direct alarm from FCM"
            val alarmId = "direct_${System.currentTimeMillis()}"
            
            Log.d("FCMService", "💥 EXECUTING DIRECT ALARM: $title")
            
            // Start AlarmForegroundService IMMEDIATELY with highest priority
            val serviceIntent = Intent(this, AlarmForegroundService::class.java).apply {
                action = AlarmForegroundService.ACTION_START_ALARM
                putExtra(AlarmForegroundService.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmForegroundService.EXTRA_ALARM_TITLE, "$title - $message")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
                Log.d("FCMService", "🚀 FOREGROUND SERVICE STARTED IMMEDIATELY")
            } else {
                startService(serviceIntent)
                Log.d("FCMService", "🚀 SERVICE STARTED IMMEDIATELY")
            }
            
            // TRIPLE EXECUTION for maximum reliability
            // 1. Direct AlarmReceiver call
            val directIntent = Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_FIREBASE_ALARM
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, "$title - $message")
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            
            val receiver = AlarmReceiver()
            receiver.onReceive(this, directIntent)
            Log.d("FCMService", "💥 DIRECT RECEIVER EXECUTED")
            
            // 2. Broadcast as backup
            sendBroadcast(directIntent)
            Log.d("FCMService", "📡 BROADCAST SENT")
            
        } catch (e: Exception) {
            Log.e("FCMService", "❌ Error in direct alarm execution", e)
        }
    }
    
}