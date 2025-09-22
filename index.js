/**
 * @format
 */

import {AppRegistry} from 'react-native';
import messaging from '@react-native-firebase/messaging';
import {NativeModules, Platform} from 'react-native';
import App from './App';
import {name as appName} from './app.json';

const {AlarmManagerModule} = NativeModules;

// CRITICAL: Background message handler MUST be set at the top level
// This handles notifications when app is in background (not killed)
messaging().setBackgroundMessageHandler(async remoteMessage => {
  console.log('🚨 BACKGROUND MESSAGE HANDLER TRIGGERED!', remoteMessage);
  
  try {
    // Extract alarm data
    const title = remoteMessage.notification?.title || remoteMessage.data?.alarm_title || remoteMessage.data?.title || 'Background Alarm';
    const message = remoteMessage.notification?.body || remoteMessage.data?.alarm_message || remoteMessage.data?.body || 'Alarm dari background';
    
    console.log(`🔥 IMMEDIATE BACKGROUND ALARM: ${title} - ${message}`);
    
    // Trigger immediate alarm using native modules
    if (Platform.OS === 'android' && AlarmManagerModule) {
      const alarmId = `bg_immediate_${Date.now()}`;
      
      console.log('📱 Calling native immediate alarm from background...');
      await AlarmManagerModule.triggerImmediateAlarm(alarmId, `${title} - ${message}`);
      
      console.log('✅ Background immediate alarm triggered successfully');
    } else {
      console.error('❌ AlarmManagerModule not available in background');
    }
    
  } catch (error) {
    console.error('❌ Error in background message handler:', error);
  }
  
  return Promise.resolve();
});

AppRegistry.registerComponent(appName, () => App);
