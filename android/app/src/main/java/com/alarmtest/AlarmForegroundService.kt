package com.alarmtest

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.os.PowerManager

class AlarmForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "alarm_foreground_channel"
        const val NOTIFICATION_ID = 2
        const val ACTION_START_ALARM = "START_ALARM"
        const val ACTION_STOP_ALARM = "STOP_ALARM"
        const val EXTRA_ALARM_TITLE = "alarm_title"
        const val EXTRA_ALARM_ID = "alarm_id"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = 0
    private var originalRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Log.d("AlarmForegroundService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AlarmForegroundService", "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val title = intent.getStringExtra(EXTRA_ALARM_TITLE) ?: "Alarm Ringing"
                val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: "unknown"
                Log.d("AlarmForegroundService", "🚨 IMMEDIATE ALARM START for: $title")
                startAlarm(title, alarmId)
            }
            ACTION_STOP_ALARM -> {
                Log.d("AlarmForegroundService", "🛑 Stopping alarm via action")
                stopAlarm()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Foreground Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for alarm foreground service"
                enableVibration(false) // We'll handle vibration separately
                setSound(null, null) // We'll handle sound separately
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startAlarm(title: String, alarmId: String) {
        Log.d("AlarmForegroundService", "🚨 Starting alarm: $title")
        
        // Acquire wake lock to keep device awake
        acquireWakeLock()
        
        // Create notification for foreground service
        val notification = createForegroundNotification(title, alarmId)
        startForeground(NOTIFICATION_ID, notification)
        
        // Start alarm sound and vibration
        playAlarmSound()
        startVibration()
        
        // Auto-stop after 30 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("AlarmForegroundService", "⏰ Auto-stopping alarm after 30 seconds")
            stopAlarm()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }, 30000)
        
        // Launch app
        launchApp(title, alarmId)
    }
    
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "AlarmApp::WakeLock"
            ).apply {
                acquire(35000) // 35 seconds (5 seconds more than alarm duration)
            }
            Log.d("AlarmForegroundService", "🔋 Wake lock acquired")
        } catch (e: Exception) {
            Log.e("AlarmForegroundService", "Error acquiring wake lock", e)
        }
    }

    private fun createForegroundNotification(title: String, alarmId: String): android.app.Notification {
        // Intent to stop alarm
        val stopIntent = Intent(this, AlarmForegroundService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent to open app
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val launchPendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚨 ALARM RINGING! 🚨")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(launchPendingIntent)
            .setFullScreenIntent(launchPendingIntent, true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Alarm",
                stopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .build()
    }

    private fun playAlarmSound() {
        try {
            Log.d("AlarmForegroundService", "🔊 Starting AGGRESSIVE EMERGENCY alarm sound (BYPASSING SILENT MODE)")
            
            // STEP 1: Save original state
            originalVolume = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 0
            originalRingerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
            
            // STEP 2: AGGRESSIVELY FORCE RINGER MODE TO NORMAL (bypass silent/vibrate)
            Log.d("AlarmForegroundService", "🔊 Original ringer mode: $originalRingerMode")
            audioManager?.ringerMode = AudioManager.RINGER_MODE_NORMAL
            Log.d("AlarmForegroundService", "🔊 FORCED ringer mode to NORMAL")
            
            // STEP 3: FORCE ALL VOLUMES TO MAXIMUM
            val maxAlarmVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 15
            val maxMusicVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            val maxNotifVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) ?: 7
            val maxRingVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_RING) ?: 7
            
            // Force ALL volumes to maximum for bypass
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, (maxMusicVolume * 0.9).toInt(), 0)
            audioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotifVolume, 0)
            audioManager?.setStreamVolume(AudioManager.STREAM_RING, maxRingVolume, 0)
            
            Log.d("AlarmForegroundService", "🔊 ALL VOLUMES MAXIMIZED - Alarm=$maxAlarmVolume, Music=${(maxMusicVolume * 0.9).toInt()}")
            
            // STEP 4: REQUEST AUDIO FOCUS AGGRESSIVELY
            requestAudioFocusAggressively()
            
            // STEP 5: CREATE AGGRESSIVE MEDIA PLAYER
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED or AudioAttributes.FLAG_HW_AV_SYNC)
                        .build()
                )
                
                try {
                    setDataSource(this@AlarmForegroundService, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                } catch (e: Exception) {
                    Log.w("AlarmForegroundService", "Default alarm sound failed, using ringtone", e)
                    try {
                        setDataSource(this@AlarmForegroundService, android.provider.Settings.System.DEFAULT_RINGTONE_URI)
                    } catch (e2: Exception) {
                        Log.w("AlarmForegroundService", "Ringtone also failed, using notification sound", e2)
                        setDataSource(this@AlarmForegroundService, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                    }
                }
                
                isLooping = true
                setVolume(1.0f, 1.0f) // Maximum volume
                prepareAsync()
                setOnPreparedListener { 
                    start()
                    Log.d("AlarmForegroundService", "🔊 EMERGENCY alarm sound playing at maximum volume")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AlarmForegroundService", "MediaPlayer error: what=$what, extra=$extra")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmForegroundService", "Error playing alarm sound", e)
        }
    }

    private fun startVibration() {
        try {
            Log.d("AlarmForegroundService", "📳 Starting vibration")
            val pattern = longArrayOf(0, 1000, 1000, 1000, 1000, 1000)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibrator = vibratorManager.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmForegroundService", "Error starting vibration", e)
        }
    }

    private fun requestAudioFocusAggressively() {
        try {
            Log.d("AlarmForegroundService", "🎯 Requesting AGGRESSIVE audio focus")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Log.d("AlarmForegroundService", "Audio focus change: $focusChange")
                        // Don't release focus - keep playing regardless
                    }
                    .build()
                
                val result = audioManager?.requestAudioFocus(audioFocusRequest!!)
                Log.d("AlarmForegroundService", "🎯 Audio focus request result: $result")
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager?.requestAudioFocus(
                    { focusChange ->
                        Log.d("AlarmForegroundService", "Audio focus change: $focusChange")
                        // Don't release focus - keep playing regardless
                    },
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
                Log.d("AlarmForegroundService", "🎯 Audio focus request result: $result")
            }
        } catch (e: Exception) {
            Log.e("AlarmForegroundService", "Error requesting audio focus", e)
        }
    }

    private fun stopAlarm() {
        Log.d("AlarmForegroundService", "🔇 Stopping alarm")
        
        // Stop sound
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
            
            // Restore original volume and ringer mode
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
            audioManager?.ringerMode = originalRingerMode
            Log.d("AlarmForegroundService", "🔇 Audio settings restored to original state")
        } catch (e: Exception) {
            Log.e("AlarmForegroundService", "Error stopping alarm sound", e)
        }
        
        // Release audio focus
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager?.abandonAudioFocusRequest(it)
                    Log.d("AlarmForegroundService", "🎯 Audio focus released")
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus { }
                Log.d("AlarmForegroundService", "🎯 Audio focus abandoned (legacy)")
            }
            audioFocusRequest = null
        } catch (e: Exception) {
            Log.e("AlarmForegroundService", "Error releasing audio focus", e)
        }
        
        // Stop vibration
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e("AlarmForegroundService", "Error stopping vibration", e)
        }
        
        // Release wake lock
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("AlarmForegroundService", "🔋 Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e("AlarmForegroundService", "Error releasing wake lock", e)
        }
    }

    private fun launchApp(title: String, alarmId: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("alarmId", alarmId)
                putExtra("alarmTitle", title)
                putExtra("isAlarmTrigger", true)
            }
            
            startActivity(launchIntent)
            Log.d("AlarmForegroundService", "📱 App launched")
        } catch (e: Exception) {
            Log.e("AlarmForegroundService", "Error launching app", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        Log.d("AlarmForegroundService", "Service destroyed")
    }
}