import messaging from '@react-native-firebase/messaging';
import { Alert, Platform, NativeModules } from 'react-native';
import { playAlarmSound, stopAlarmSound } from './SoundService';

const { AlarmManagerModule } = NativeModules;

class FirebaseService {
  private fcmToken: string | null = null;

  async initialize() {
    await this.requestPermission();
    await this.getFCMToken();
    this.setupMessageHandlers();
  }

  async requestPermission() {
    try {
      if (Platform.OS === 'ios') {
        const authStatus = await messaging().requestPermission();
        const enabled =
          authStatus === messaging.AuthorizationStatus.AUTHORIZED ||
          authStatus === messaging.AuthorizationStatus.PROVISIONAL;

        if (!enabled) {
          Alert.alert(
            'Permission Required',
            'Push notifications are required for alarm functionality.',
            [{ text: 'OK' }]
          );
        }
      }
    } catch (error) {
      console.error('Permission request error:', error);
    }
  }

  async getFCMToken(): Promise<string | null> {
    try {
      const token = await messaging().getToken();
      this.fcmToken = token;
      console.log('FCM Token:', token);
      return token;
    } catch (error) {
      console.error('Error getting FCM token:', error);
      return null;
    }
  }

  setupMessageHandlers() {
    // Handle messages when app is in foreground
    messaging().onMessage(async remoteMessage => {
      console.log('Foreground message received:', remoteMessage);
      this.handleAlarmMessage(remoteMessage);
    });

    // Handle messages when app is in background but not killed
    messaging().onNotificationOpenedApp(remoteMessage => {
      console.log('Background message opened app:', remoteMessage);
      this.handleAlarmMessage(remoteMessage);
    });

    // Handle messages when app is killed and opened by notification
    messaging()
      .getInitialNotification()
      .then(remoteMessage => {
        if (remoteMessage) {
          console.log('Killed state message opened app:', remoteMessage);
          this.handleAlarmMessage(remoteMessage);
        }
      });

    // Background handler is now set in index.js at top level
  }

  handleAlarmMessage(remoteMessage: any) {
    console.log('🚨 NOTIFICATION RECEIVED from Firebase!', remoteMessage);
    
    // Cek apakah ini perintah untuk schedule alarm atau trigger alarm langsung
    const messageType = remoteMessage?.data?.type || 'immediate';
    
    if (messageType === 'schedule') {
      this.handleScheduleAlarmCommand(remoteMessage);
      return;
    }
    
    // Default: trigger alarm langsung (existing behavior)
    // Get delay from message data (default 2 seconds)
    const delaySeconds = parseInt(remoteMessage?.data?.delay || '2');
    
    console.log(`📅 Scheduling immediate alarm to trigger in ${delaySeconds} seconds`);
    
    // Schedule delayed alarm
    setTimeout(() => {
      console.log('🚨 EXECUTING DELAYED ALARM NOW!');
      
      // Play alarm sound
      playAlarmSound();

      // Show alarm alert
      Alert.alert(
        '🚨 ALARM RINGING! 🚨',
        remoteMessage?.notification?.body || remoteMessage?.data?.body || 'Your alarm is going off!',
        [
          {
            text: 'Stop Alarm',
            style: 'destructive',
            onPress: () => {
              console.log('Alarm stopped by user');
              
              // Stop React Native sound service
              stopAlarmSound();
              
              // Stop React Native vibration
              if (Platform.OS === 'android') {
                const { Vibration } = require('react-native');
                Vibration.cancel();
              }
              
              // Stop native Android alarm
              if (Platform.OS === 'android' && AlarmManagerModule) {
                AlarmManagerModule.stopCurrentAlarm();
              }
            },
          },
          {
            text: 'Snooze 5 min',
            style: 'default',
            onPress: () => {
              console.log('Alarm snoozed for 5 minutes');
              // You could implement snooze logic here
            },
          },
        ],
        { cancelable: false }
      );

      // Vibrate device if available
      if (Platform.OS === 'android') {
        const { Vibration } = require('react-native');
        const pattern = [0, 1000, 1000, 1000, 1000];
        Vibration.vibrate(pattern, true);
        
        // Stop vibration after 30 seconds
        setTimeout(() => {
          Vibration.cancel();
        }, 30000);
      }
    }, delaySeconds * 1000);
  }

  /**
   * Handle push notification command untuk schedule alarm
   */
  async handleScheduleAlarmCommand(remoteMessage: any) {
    try {
      console.log('📅 PROCESSING SCHEDULE ALARM COMMAND:', remoteMessage.data);

      const data = remoteMessage.data;
      
      // Validasi required fields
      if (!data.alarm_time) {
        throw new Error('alarm_time is required');
      }

      const title = data.alarm_title || remoteMessage.notification?.title || 'Remote Alarm';
      const message = data.alarm_message || remoteMessage.notification?.body || 'Alarm dari push notification';
      
      // Parse waktu alarm
      let alarmDateTime: Date;
      
      if (data.alarm_date) {
        // Format: "2024-01-15 07:30" atau "2024-01-15T07:30:00"
        alarmDateTime = new Date(data.alarm_date + ' ' + data.alarm_time);
      } else {
        // Hanya waktu, gunakan hari ini atau besok jika waktu sudah lewat
        const [hours, minutes] = data.alarm_time.split(':').map(Number);
        alarmDateTime = new Date();
        alarmDateTime.setHours(hours, minutes, 0, 0);
        
        // Jika waktu sudah lewat hari ini, set untuk besok
        if (alarmDateTime <= new Date()) {
          alarmDateTime.setDate(alarmDateTime.getDate() + 1);
        }
      }

      // Validasi waktu
      if (alarmDateTime <= new Date()) {
        throw new Error('Alarm time must be in the future');
      }

      // Schedule alarm menggunakan AlarmManagerModule langsung
      const alarmId = `remote_${Date.now()}`;
      
      try {
        if (Platform.OS === 'android' && AlarmManagerModule) {
          const result = await AlarmManagerModule.scheduleAlarm(
            alarmId,
            title,
            alarmDateTime.getTime()
          );
          
          // Show success notification
          Alert.alert(
            '✅ Alarm Terjadwal!',
            `Alarm "${title}" berhasil dijadwalkan untuk ${alarmDateTime.toLocaleString('id-ID')}`,
            [{ text: 'OK' }]
          );

          console.log(`✅ Remote alarm scheduled successfully: ${alarmId}`, result);
        } else {
          throw new Error('AlarmManagerModule not available');
        }
      } catch (error) {
        throw new Error(`Failed to schedule alarm: ${error.message}`);
      }

    } catch (error) {
      console.error('❌ Error processing schedule alarm command:', error);
      
      Alert.alert(
        '❌ Gagal Membuat Alarm',
        `Error: ${error.message}`,
        [{ text: 'OK' }]
      );
    }
  }

  getCurrentToken(): string | null {
    return this.fcmToken;
  }

  async refreshToken(): Promise<string | null> {
    return await this.getFCMToken();
  }
}

export const firebaseService = new FirebaseService();

export const initializeFirebase = async () => {
  await firebaseService.initialize();
};

export const getFCMToken = async () => {
  return await firebaseService.getFCMToken();
};

export const getCurrentFCMToken = () => {
  return firebaseService.getCurrentToken();
};

export const testDelayedAlarm = async (delaySeconds: number = 2) => {
  const token = firebaseService.getCurrentToken();
  
  if (!token) {
    Alert.alert('Error', 'No FCM token available. Please initialize Firebase first.');
    return;
  }

  Alert.alert(
    'Test Delayed Alarm',
    `To test the ${delaySeconds}-second delayed alarm, send a push notification with these details:\n\nToken: ${token.substring(0, 20)}...\n\nData payload:\n- title: "Test Alarm"\n- body: "Testing delayed alarm"\n- delay: "${delaySeconds}"\n\nUse Firebase Console or your server to send the notification.`,
    [
      {
        text: 'Copy Token',
        onPress: () => {
          console.log('Full FCM Token:', token);
        }
      },
      { text: 'OK' }
    ]
  );
};

export const stopCurrentAlarm = () => {
  console.log('🛑 Stopping current alarm manually');
  
  // Stop React Native sound service
  stopAlarmSound();
  
  // Stop React Native vibration
  if (Platform.OS === 'android') {
    const { Vibration } = require('react-native');
    Vibration.cancel();
  }
  
  // Stop native Android alarm
  if (Platform.OS === 'android' && AlarmManagerModule) {
    AlarmManagerModule.stopCurrentAlarm();
  }
};

/**
 * Helper untuk membuat payload push notification untuk schedule alarm
 */
export const createScheduleAlarmPayload = (
  title: string,
  message: string,
  alarmTime: string, // Format: "07:30" atau "14:45"
  alarmDate?: string, // Format: "2024-01-15" (optional, default: today/tomorrow)
  repeatDays?: number[] // Array hari: [1,2,3,4,5] = Mon-Fri
) => {
  const payload = {
    notification: {
      title: '📅 Alarm Dijadwalkan',
      body: `Alarm "${title}" akan dibuat untuk ${alarmTime}`
    },
    data: {
      type: 'schedule',
      alarm_title: title,
      alarm_message: message,
      alarm_time: alarmTime,
      ...(alarmDate && { alarm_date: alarmDate }),
      ...(repeatDays && { repeat_days: repeatDays.join(',') })
    }
  };

  return payload;
};

/**
 * Helper untuk generate contoh payload
 */
export const getScheduleAlarmExamples = () => {
  return {
    oneTimeAlarm: {
      title: 'Contoh Alarm Sekali',
      payload: createScheduleAlarmPayload(
        'Bangun Pagi',
        'Saatnya bangun dan memulai hari!',
        '06:30',
        '2024-01-20'
      )
    },
    recurringAlarm: {
      title: 'Contoh Alarm Berulang',
      payload: createScheduleAlarmPayload(
        'Meeting Daily Standup',
        'Waktunya meeting tim!',
        '09:00',
        undefined,
        [1, 2, 3, 4, 5] // Senin-Jumat
      )
    },
    todayTomorrowAlarm: {
      title: 'Contoh Alarm Hari Ini/Besok',
      payload: createScheduleAlarmPayload(
        'Minum Obat',
        'Jangan lupa minum obat!',
        '20:00' // Jika sekarang sudah lewat jam 8 malam, akan dijadwalkan besok
      )
    }
  };
};