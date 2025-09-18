import { AppState, Alert, Platform, PermissionsAndroid } from 'react-native';
import BackgroundTimer from 'react-native-background-timer';
import { Alarm } from '../types/Alarm';
import { playAlarmSound } from './SoundService';

class AlarmService {
  private activeAlarms = new Map<string, { alarm: Alarm; timeoutId: number }>();
  private appStateSubscription: any;
  private checkInterval: number | null = null;

  constructor() {
    this.setupAppStateListener();
    this.startPeriodicCheck();
    this.requestNotificationPermissions();
  }

  async requestNotificationPermissions() {
    if (Platform.OS === 'android') {
      try {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
          {
            title: 'Notification Permission',
            message: 'This app needs permission to show alarm notifications',
            buttonNeutral: 'Ask Me Later',
            buttonNegative: 'Cancel',
            buttonPositive: 'OK',
          }
        );

        if (granted === PermissionsAndroid.RESULTS.GRANTED) {
          console.log('Notification permission granted');
        } else {
          console.log('Notification permission denied');
          Alert.alert(
            'Permission Required',
            'Please enable notification permission in your device settings to receive alarm notifications.',
            [{ text: 'OK' }]
          );
        }
      } catch (err) {
        console.warn('Error requesting notification permission:', err);
      }
    }
  }

  setupAppStateListener() {
    this.appStateSubscription = AppState.addEventListener('change', (nextAppState) => {
      if (nextAppState === 'active') {
        this.checkAlarmsNow();
      }
    });
  }

  startPeriodicCheck() {
    if (this.checkInterval) return;
    
    this.checkInterval = BackgroundTimer.setInterval(() => {
      this.checkAlarmsNow();
    }, 1000);
  }

  stopPeriodicCheck() {
    if (this.checkInterval) {
      BackgroundTimer.clearInterval(this.checkInterval);
      this.checkInterval = null;
    }
  }

  scheduleAlarm(alarm: Alarm) {
    const now = new Date();
    const alarmTime = new Date(alarm.time);
    
    if (alarmTime.getTime() <= now.getTime()) {
      alarmTime.setDate(alarmTime.getDate() + 1);
    }

    const timeUntilAlarm = alarmTime.getTime() - now.getTime();
    
    this.cancelAlarm(alarm.id);

    const timeoutId = BackgroundTimer.setTimeout(() => {
      this.triggerAlarm(alarm);
    }, timeUntilAlarm);

    this.activeAlarms.set(alarm.id, { alarm, timeoutId });
    
    console.log(`Alarm "${alarm.title}" scheduled for ${alarmTime.toLocaleString()}`);
    console.log(`Time until alarm: ${Math.round(timeUntilAlarm / 1000)} seconds`);
  }

  cancelAlarm(alarmId: string) {
    const activeAlarm = this.activeAlarms.get(alarmId);
    if (activeAlarm) {
      BackgroundTimer.clearTimeout(activeAlarm.timeoutId);
      this.activeAlarms.delete(alarmId);
      console.log(`Alarm ${alarmId} cancelled`);
    }
  }

  triggerAlarm(alarm: Alarm) {
    console.log(`🚨 ALARM TRIGGERED: ${alarm.title}`);
    
    playAlarmSound();
    
    Alert.alert(
      '🚨 ALARM RINGING! 🚨',
      `${alarm.title}\n\nTime: ${new Date().toLocaleTimeString()}`,
      [
        {
          text: 'Stop Alarm',
          onPress: () => {
            console.log('Alarm stopped by user');
            this.activeAlarms.delete(alarm.id);
          },
          style: 'destructive',
        },
        {
          text: 'Snooze 5 min',
          onPress: () => {
            console.log('Alarm snoozed for 5 minutes');
            const snoozeAlarm = {
              ...alarm,
              time: new Date(Date.now() + 5 * 60 * 1000),
            };
            this.scheduleAlarm(snoozeAlarm);
          },
          style: 'default',
        },
      ],
      { cancelable: false }
    );

    if (alarm.repeatDays && alarm.repeatDays.length > 0) {
      this.scheduleNextRepeat(alarm);
    } else {
      this.activeAlarms.delete(alarm.id);
    }
  }

  scheduleNextRepeat(alarm: Alarm) {
    const tomorrow = new Date(alarm.time);
    tomorrow.setDate(tomorrow.getDate() + 1);
    
    const nextAlarm = {
      ...alarm,
      time: tomorrow,
    };
    
    this.scheduleAlarm(nextAlarm);
  }

  checkAlarmsNow() {
    const now = new Date();
    
    this.activeAlarms.forEach(({ alarm }, alarmId) => {
      const alarmTime = new Date(alarm.time);
      const timeDiff = now.getTime() - alarmTime.getTime();
      
      if (timeDiff >= 0 && timeDiff < 60000) {
        console.log(`Missed alarm detected: ${alarm.title}`);
        this.triggerAlarm(alarm);
      }
    });
  }

  getAllActiveAlarms() {
    return Array.from(this.activeAlarms.entries()).map(([id, { alarm }]) => ({
      id,
      title: alarm.title,
      time: alarm.time,
      scheduledFor: new Date(alarm.time).toLocaleString(),
    }));
  }

  cleanup() {
    this.stopPeriodicCheck();
    if (this.appStateSubscription) {
      this.appStateSubscription.remove();
    }
    
    this.activeAlarms.forEach(({ timeoutId }) => {
      BackgroundTimer.clearTimeout(timeoutId);
    });
    this.activeAlarms.clear();
  }
}

export const alarmService = new AlarmService();

export const scheduleAlarm = (alarm: Alarm) => {
  alarmService.scheduleAlarm(alarm);
};

export const cancelAlarm = (alarmId: string) => {
  alarmService.cancelAlarm(alarmId);
};

export const getActiveAlarms = () => {
  return alarmService.getAllActiveAlarms();
};