package com.alarmtest

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import android.util.Log
import android.os.Handler
import android.os.Looper

class PersistenceService : Service() {
    companion object {
        const val CHANNEL_ID = "persistence_channel"
        const val NOTIFICATION_ID = 3
        private const val HEARTBEAT_INTERVAL = 15000L // 15 seconds (more aggressive)
    }
    
    private var handler: Handler? = null
    private var heartbeatRunnable: Runnable? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d("PersistenceService", "🔄 Persistence service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("PersistenceService", "🔄 Persistence service started")
        
        // Start as foreground service
        val notification = createPersistenceNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // Start heartbeat to keep service alive
        startHeartbeat()
        
        // Return sticky to restart if killed
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Alarm Service",
                NotificationManager.IMPORTANCE_LOW // Low importance for less intrusive
            ).apply {
                description = "Keeps emergency alarm service active in background"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createPersistenceNotification(): android.app.Notification {
        // Intent to open app
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val launchPendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Emergency Alarm Guardian")
            .setContentText("🔋 Background protection active - Ready for instant alerts")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(launchPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()
    }
    
    private fun startHeartbeat() {
        handler = Handler(Looper.getMainLooper())
        heartbeatRunnable = object : Runnable {
            override fun run() {
                try {
                    // Log heartbeat
                    Log.d("PersistenceService", "💓 Service heartbeat")
                    
                    // Check if Firebase service is still running
                    checkFirebaseServiceHealth()
                    
                    // Update notification timestamp
                    updateNotification()
                    
                    // Schedule next heartbeat
                    handler?.postDelayed(this, HEARTBEAT_INTERVAL)
                } catch (e: Exception) {
                    Log.e("PersistenceService", "Error in heartbeat", e)
                    // Try to restart heartbeat
                    handler?.postDelayed(this, HEARTBEAT_INTERVAL)
                }
            }
        }
        
        handler?.post(heartbeatRunnable!!)
        Log.d("PersistenceService", "💓 Heartbeat started")
    }
    
    private fun checkFirebaseServiceHealth() {
        try {
            // Verify that our Firebase service is still registered
            // This is a passive check - Firebase handles its own lifecycle
            Log.d("PersistenceService", "🔍 Firebase service health check")
            
            // Log device info for debugging
            val prefs = getSharedPreferences("alarm_settings", MODE_PRIVATE)
            val bootTime = prefs.getLong("last_boot_time", 0)
            val currentTime = System.currentTimeMillis()
            val uptime = (currentTime - bootTime) / 1000 / 60 // minutes
            
            Log.d("PersistenceService", "📊 Device uptime: ${uptime} minutes")
        } catch (e: Exception) {
            Log.e("PersistenceService", "Error checking Firebase health", e)
        }
    }
    
    private fun updateNotification() {
        try {
            val notification = createPersistenceNotification()
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("PersistenceService", "Error updating notification", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Stop heartbeat
        handler?.removeCallbacks(heartbeatRunnable!!)
        handler = null
        
        Log.d("PersistenceService", "🛑 Persistence service destroyed - attempting restart")
        
        // Aggressively restart service if killed
        try {
            val restartIntent = Intent(this, PersistenceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
            Log.d("PersistenceService", "🔄 Service restart initiated")
        } catch (e: Exception) {
            Log.e("PersistenceService", "Failed to restart service", e)
        }
        
        // Schedule delayed restart as backup
        try {
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                try {
                    val delayedRestartIntent = Intent(this, PersistenceService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(delayedRestartIntent)
                    } else {
                        startService(delayedRestartIntent)
                    }
                    Log.d("PersistenceService", "🔄 Delayed restart executed")
                } catch (e: Exception) {
                    Log.e("PersistenceService", "Delayed restart failed", e)
                }
            }, 2000) // 2 second delay
        } catch (e: Exception) {
            Log.e("PersistenceService", "Failed to schedule delayed restart", e)
        }
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("PersistenceService", "📱 App task removed, service continuing")
        
        // Service should continue running even if app is removed from recent apps
        // This is normal behavior for foreground services
    }
}