package com.alarmtest

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

class MainActivity : ReactActivity() {

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  override fun getMainComponentName(): String = "alarmTest"

  /**
   * Returns the instance of the [ReactActivityDelegate]. We use [DefaultReactActivityDelegate]
   * which allows you to enable New Architecture with a single boolean flags [fabricEnabled]
   */
  override fun createReactActivityDelegate(): ReactActivityDelegate =
      DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize emergency alarm system
    initializeEmergencyAlarmSystem()
  }
  
  private fun initializeEmergencyAlarmSystem() {
    try {
      Log.d("MainActivity", "🚨 Initializing Emergency Alarm System")
      
      // Start persistence service immediately
      val serviceIntent = Intent(this, PersistenceService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(serviceIntent)
      } else {
        startService(serviceIntent)
      }
      
      // Check if we need to show optimization requests
      checkPendingOptimizations()
      
      Log.d("MainActivity", "✅ Emergency Alarm System initialized")
    } catch (e: Exception) {
      Log.e("MainActivity", "❌ Error initializing Emergency Alarm System", e)
    }
  }
  
  private fun checkPendingOptimizations() {
    try {
      val prefs = getSharedPreferences("alarm_settings", MODE_PRIVATE)
      val needsOptimizationCheck = prefs.getBoolean("needs_optimization_check", false)
      
      if (needsOptimizationCheck) {
        Log.d("MainActivity", "⚠️ Pending optimization check detected")
        
        // Clear the flag
        prefs.edit().putBoolean("needs_optimization_check", false).apply()
        
        // Check current status
        val status = BatteryOptimizationHelper.checkAllOptimizations(this)
        if (status.batteryOptimized) {
          Log.d("MainActivity", "🔧 Showing optimization requests to user")
          BatteryOptimizationHelper.requestDisableBatteryOptimization(this)
        }
      }
    } catch (e: Exception) {
      Log.e("MainActivity", "Error checking pending optimizations", e)
    }
  }
  
  override fun onResume() {
    super.onResume()
    
    // Log app resume for debugging
    Log.d("MainActivity", "📱 App resumed - Emergency Alarm System active")
    
    // Ensure persistence service is always running
    ensurePersistenceServiceRunning()
  }
  
  private fun ensurePersistenceServiceRunning() {
    try {
      val serviceIntent = Intent(this, PersistenceService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(serviceIntent)
      } else {
        startService(serviceIntent)
      }
      Log.d("MainActivity", "✅ Persistence service ensured running")
    } catch (e: Exception) {
      Log.e("MainActivity", "Error ensuring persistence service", e)
    }
  }
  
  override fun onPause() {
    super.onPause()
    Log.d("MainActivity", "📱 App paused - Services continue running")
  }
  
  override fun onDestroy() {
    super.onDestroy()
    Log.d("MainActivity", "📱 MainActivity destroyed - Services persist")
  }
}
