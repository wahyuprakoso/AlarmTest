package com.alarmtest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import android.util.Log
import android.os.PowerManager
import android.view.WindowManager
import android.app.KeyguardManager

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "firebase_alarm_channel"
        const val SCHEDULED_ALARM_CHANNEL_ID = "scheduled_alarm_channel"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_TITLE = "alarm_title"
        const val ACTION_FIREBASE_ALARM = "com.alarmtest.FIREBASE_ALARM"
        const val ACTION_SCHEDULED_ALARM = "com.alarmtest.SCHEDULED_ALARM"
        const val ACTION_STOP_ALARM = "com.alarmtest.STOP_ALARM"
        
        private var currentMediaPlayer: MediaPlayer? = null
        private var currentVibrator: Vibrator? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "🚨 EMERGENCY ALARM received! Action: ${intent.action}")
        
        when (intent.action) {
            ACTION_FIREBASE_ALARM -> {
                handleFirebaseAlarm(context, intent)
            }
            ACTION_SCHEDULED_ALARM -> {
                handleScheduledAlarm(context, intent)
            }
            ACTION_STOP_ALARM -> {
                handleStopAlarm(context)
            }
            else -> {
                Log.d("AlarmReceiver", "Ignoring non-alarm action: ${intent.action}")
                return
            }
        }
    }
    
    private fun handleFirebaseAlarm(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: "unknown"
        val alarmTitle = intent.getStringExtra(EXTRA_ALARM_TITLE) ?: "Alarm"
        
        Log.d("AlarmReceiver", "🔥 IMMEDIATE EMERGENCY PROCESSING - ID: $alarmId, Title: $alarmTitle")
        
        // FIRST: Wake up device and bypass lock screen
        wakeUpDevice(context)
        
        // THEN: Execute all alarm actions
        createNotificationChannel(context)
        showAlarmNotification(context, alarmId, alarmTitle)
        playAlarmSound(context)
        vibrateDevice(context)
        
        // Launch the app when alarm triggers
        launchApp(context, alarmId, alarmTitle)
        
        Log.d("AlarmReceiver", "✅ EMERGENCY alarm processing completed")
    }
    
    private fun handleScheduledAlarm(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: "scheduled_${System.currentTimeMillis()}"
        val alarmTitle = intent.getStringExtra(EXTRA_ALARM_TITLE) ?: "Scheduled Alarm"
        
        Log.d("AlarmReceiver", "⏰ SCHEDULED ALARM TRIGGERED - ID: $alarmId, Title: $alarmTitle")
        
        // FIRST: Wake up device and bypass lock screen
        wakeUpDevice(context)
        
        // THEN: Execute all alarm actions with scheduled alarm channel
        createScheduledAlarmNotificationChannel(context)
        showScheduledAlarmNotification(context, alarmId, alarmTitle)
        playAlarmSound(context)
        vibrateDevice(context)
        
        // Launch the app when alarm triggers
        launchApp(context, alarmId, alarmTitle)
        
        Log.d("AlarmReceiver", "✅ Scheduled alarm processing completed")
    }
    
    private fun handleStopAlarm(context: Context) {
        Log.d("AlarmReceiver", "🛑 STOPPING ALARM")
        
        // Stop media player
        currentMediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                    Log.d("AlarmReceiver", "🔇 Media player stopped")
                }
                player.release()
                currentMediaPlayer = null
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error stopping media player", e)
            }
        }
        
        // Stop vibration
        currentVibrator?.let { vibrator ->
            try {
                vibrator.cancel()
                Log.d("AlarmReceiver", "📳 Vibration stopped")
                currentVibrator = null
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error stopping vibration", e)
            }
        }
        
        // Cancel notification
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()
        
        Log.d("AlarmReceiver", "✅ Alarm stopped successfully")
    }
    
    private fun wakeUpDevice(context: Context) {
        try {
            Log.d("AlarmReceiver", "🔋 Waking up device for emergency alarm")
            
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            
            // Create wake lock to turn on screen
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or 
                PowerManager.ACQUIRE_CAUSES_WAKEUP or 
                PowerManager.ON_AFTER_RELEASE,
                "AlarmApp::EmergencyWakeLock"
            )
            
            // Acquire wake lock for 60 seconds
            wakeLock.acquire(60000)
            
            Log.d("AlarmReceiver", "📱 Screen wake lock acquired")
            
            // Release wake lock after 60 seconds
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                        Log.d("AlarmReceiver", "🔋 Wake lock released")
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error releasing wake lock", e)
                }
            }, 60000)
            
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Error waking up device", e)
        }
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Alarm Notifications",
                NotificationManager.IMPORTANCE_MAX // Maximum importance for bypassing silent mode
            ).apply {
                description = "Critical emergency alarm notifications - bypasses silent mode"
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
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            
            Log.d("AlarmReceiver", "🔔 Emergency notification channel created with MAXIMUM importance")
        }
    }
    
    private fun createScheduledAlarmNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SCHEDULED_ALARM_CHANNEL_ID,
                "Scheduled Alarm Notifications",
                NotificationManager.IMPORTANCE_MAX // Maximum importance for bypassing silent mode
            ).apply {
                description = "Scheduled alarm notifications - bypasses silent mode"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 1000, 1000)
                setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI, null)
                setBypassDnd(true) // CRITICAL: Bypass Do Not Disturb mode
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                
                // SCHEDULED: Allow notification to override silent mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            
            Log.d("AlarmReceiver", "⏰ Scheduled alarm notification channel created with MAXIMUM importance")
        }
    }
    
    private fun showAlarmNotification(context: Context, alarmId: String, title: String) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        
        // Intent to open the app when notification is clicked
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            alarmId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🚨 EMERGENCY ALARM RINGING! 🚨")
            .setContentText("$title - BYPASSING SILENT MODE")
            .setPriority(NotificationCompat.PRIORITY_MAX) // Maximum priority
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false) // Don't auto cancel - user must interact
            .setOngoing(true)
            .setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            .setVibrate(longArrayOf(0, 1000, 1000, 1000, 1000))
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // Force full screen
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .setDefaults(NotificationCompat.DEFAULT_ALL) // All defaults for maximum impact
            .setLights(android.graphics.Color.RED, 1000, 1000) // Red flashing light
            .setTimeoutAfter(30000) // Auto-dismiss after 30 seconds
            .build()
        
        notificationManager.notify(alarmId.hashCode(), notification)
    }
    
    private fun showScheduledAlarmNotification(context: Context, alarmId: String, title: String) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        
        // Intent to open the app when notification is clicked
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            alarmId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent untuk stop alarm
        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode() + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, SCHEDULED_ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ ALARM TERJADWAL BERBUNYI! ⏰")
            .setContentText("$title - Alarm yang Anda jadwalkan")
            .setPriority(NotificationCompat.PRIORITY_MAX) // Maximum priority
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false) // Don't auto cancel - user must interact
            .setOngoing(true)
            .setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            .setVibrate(longArrayOf(0, 1000, 1000, 1000, 1000))
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // Force full screen
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .setDefaults(NotificationCompat.DEFAULT_ALL) // All defaults for maximum impact
            .setLights(android.graphics.Color.BLUE, 1000, 1000) // Blue flashing light
            .addAction(
                android.R.drawable.ic_media_pause,
                "STOP",
                stopPendingIntent
            )
            .setTimeoutAfter(60000) // Auto-dismiss after 60 seconds
            .build()
        
        notificationManager.notify(alarmId.hashCode(), notification)
        
        Log.d("AlarmReceiver", "⏰ Scheduled alarm notification shown with stop action")
    }
    
    private fun playAlarmSound(context: Context) {
        try {
            Log.d("AlarmReceiver", "🔊 Starting EMERGENCY alarm sound (bypassing silent mode)")
            
            // Set audio to alarm volume and FORCE maximum audibility
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            
            // FORCE alarm volume to 90% of maximum regardless of current settings
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, (maxVolume * 0.9).toInt(), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
            
            // ALSO force other audio streams to ensure sound plays
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 0.8).toInt(), 0)
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION), 0)
            
            Log.d("AlarmReceiver", "🔊 FORCED volume: Alarm=${audioManager.getStreamVolume(AudioManager.STREAM_ALARM)}/${maxVolume}")
            
            // Stop any existing alarm first
            currentMediaPlayer?.let { 
                try {
                    if (it.isPlaying) it.stop()
                    it.release()
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error stopping previous alarm", e)
                }
            }
            
            currentMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                
                // Try multiple sound sources for maximum reliability
                try {
                    setDataSource(context, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                } catch (e: Exception) {
                    Log.w("AlarmReceiver", "Default alarm sound failed, using ringtone", e)
                    try {
                        setDataSource(context, android.provider.Settings.System.DEFAULT_RINGTONE_URI)
                    } catch (e2: Exception) {
                        Log.w("AlarmReceiver", "Ringtone also failed, using notification sound", e2)
                        setDataSource(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                    }
                }
                
                isLooping = true
                setVolume(1.0f, 1.0f) // Maximum volume
                prepareAsync()
                setOnPreparedListener { 
                    start()
                    Log.d("AlarmReceiver", "🔊 EMERGENCY alarm sound playing at MAXIMUM volume")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AlarmReceiver", "MediaPlayer error: what=$what, extra=$extra")
                    false
                }
            }
            
            // Stop the sound after 30 seconds and restore volume
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    currentMediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            player.stop()
                            Log.d("AlarmReceiver", "🔇 Emergency alarm sound stopped")
                        }
                        player.release()
                        currentMediaPlayer = null
                    }
                    
                    // Restore original volume
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error stopping alarm sound", e)
                }
            }, 30000)
            
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Error playing emergency alarm sound", e)
        }
    }
    
    private fun vibrateDevice(context: Context) {
        val pattern = longArrayOf(0, 1000, 1000, 1000, 1000, 1000)
        
        // Stop any existing vibration first
        currentVibrator?.cancel()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            currentVibrator = vibratorManager.defaultVibrator
            currentVibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            currentVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentVibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                currentVibrator?.vibrate(pattern, 0)
            }
        }
    }
    
    private fun launchApp(context: Context, alarmId: String, title: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("alarmId", alarmId)
            putExtra("alarmTitle", title)
            putExtra("isAlarmTrigger", true)
        }
        
        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Error launching app", e)
        }
    }
}