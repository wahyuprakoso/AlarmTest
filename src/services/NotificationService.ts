import BackgroundTimer from 'react-native-background-timer';
import { AppState, Platform, Alert } from 'react-native';
import PushNotificationIOS from '@react-native-community/push-notification-ios';
import { Alarm } from '../types/Alarm';
import { playAlarmSound } from './SoundService';

class NotificationService {
  private activeAlarms = new Map<string, number>();
  private backgroundTimer: number | null = null;
  private appStateSubscription: any;

  constructor() {
    this.setupNotifications();
    this.setupBackgroundHandling();
  }

  setupNotifications() {
    if (Platform.OS === 'ios') {
      PushNotificationIOS.addEventListener('notification', this.onNotificationReceived);
      PushNotificationIOS.requestPermissions({
        alert: true,
        badge: true,
        sound: true,
      });
    } else {
      PushNotification.configure({
        onNotification: this.onNotificationReceived,
        requestPermissions: Platform.OS === 'ios',
      });

      PushNotification.createChannel(
        {
          channelId: 'alarm-channel',
          channelName: 'Alarm Notifications',
          channelDescription: 'Alarm notifications for the app',
          playSound: true,
          soundName: 'default',
          importance: 4,
          vibrate: true,
        },
        (created) => console.log(`createChannel returned '${created}'`)
      );
    }
  }

  onNotificationReceived = (notification: any) => {
    console.log('Notification received:', notification);
    
    if (notification.getData()?.isAlarm) {
      playAlarmSound();
    }
    
    if (Platform.OS === 'ios') {
      notification.finish(PushNotificationIOS.FetchResult.NoData);
    }
  };


  setupBackgroundHandling() {
    this.appStateSubscription = AppState.addEventListener('change', this.handleAppStateChange);
    
    if (Platform.OS === 'android') {
      this.startBackgroundTimer();
    }
  }

  handleAppStateChange = (nextAppState: string) => {
    if (nextAppState === 'background') {
      this.startBackgroundTimer();
    } else if (nextAppState === 'active') {
      this.stopBackgroundTimer();
    }
  };

  startBackgroundTimer() {
    if (this.backgroundTimer) return;
    
    this.backgroundTimer = BackgroundTimer.setInterval(() => {
      this.checkActiveAlarms();
    }, 1000);
  }

  stopBackgroundTimer() {
    if (this.backgroundTimer) {
      BackgroundTimer.clearInterval(this.backgroundTimer);
      this.backgroundTimer = null;
    }
  }

  checkActiveAlarms() {
    const now = new Date();
    
    this.activeAlarms.forEach((scheduledTime, alarmId) => {
      if (now.getTime() >= scheduledTime) {
        this.triggerBackgroundAlarm(alarmId);
        this.activeAlarms.delete(alarmId);
      }
    });
  }

  triggerBackgroundAlarm(alarmId: string) {
    if (Platform.OS === 'ios') {
      PushNotificationIOS.addNotificationRequest({
        id: `background-${alarmId}`,
        title: '🚨 ALARM RINGING! 🚨',
        body: 'Your alarm is going off!',
        sound: 'default',
        userInfo: {
          isAlarm: true,
          alarmId: alarmId,
        },
      });
    } else {
      PushNotification.localNotification({
        id: `background-${alarmId}`,
        title: '🚨 ALARM RINGING! 🚨',
        message: 'Your alarm is going off!',
        soundName: 'default',
        playSound: true,
        vibrate: true,
        vibration: 300,
        channelId: 'alarm-channel',
        userInfo: {
          isAlarm: true,
          alarmId: alarmId,
        },
      });
    }
    
    playAlarmSound();
  }

  scheduleAlarm(alarm: Alarm) {
    const now = new Date();
    const alarmTime = new Date(alarm.time);
    
    if (alarmTime.getTime() < now.getTime()) {
      alarmTime.setDate(alarmTime.getDate() + 1);
    }

    this.activeAlarms.set(alarm.id, alarmTime.getTime());

    if (Platform.OS === 'ios') {
      PushNotificationIOS.removePendingNotificationRequests([alarm.id]);
      PushNotificationIOS.addNotificationRequest({
        id: alarm.id,
        title: '⏰ Alarm',
        body: alarm.title,
        fireDate: alarmTime,
        repeats: true,
        sound: 'default',
        userInfo: {
          isAlarm: true,
          alarmId: alarm.id,
        },
      });
    } else {
      PushNotification.cancelLocalNotifications({ id: alarm.id });
      PushNotification.localNotificationSchedule({
        id: alarm.id,
        title: '⏰ Alarm',
        message: alarm.title,
        date: alarmTime,
        soundName: 'default',
        playSound: true,
        vibrate: true,
        vibration: 300,
        repeatType: 'day',
        channelId: 'alarm-channel',
        userInfo: {
          isAlarm: true,
          alarmId: alarm.id,
        },
      });
    }

    console.log(`Alarm scheduled for ${alarmTime.toLocaleString()}`);
  }

  cancelAlarm(alarmId: string) {
    if (Platform.OS === 'ios') {
      PushNotificationIOS.removePendingNotificationRequests([alarmId]);
    } else {
      PushNotification.cancelLocalNotifications({ id: alarmId });
    }
    this.activeAlarms.delete(alarmId);
  }

  cleanup() {
    this.stopBackgroundTimer();
    if (this.appStateSubscription) {
      this.appStateSubscription.remove();
    }
    this.activeAlarms.clear();
  }

  showIntrusiveNotification(alarm: Alarm) {
    if (Platform.OS === 'ios') {
      PushNotificationIOS.addNotificationRequest({
        id: `intrusive-${alarm.id}`,
        title: '🚨 ALARM RINGING! 🚨',
        body: `${alarm.title} - Tap to stop`,
        sound: 'default',
        userInfo: {
          isAlarm: true,
          alarmId: alarm.id,
        },
      });
    } else {
      PushNotification.localNotification({
        id: `intrusive-${alarm.id}`,
        title: '🚨 ALARM RINGING! 🚨',
        message: `${alarm.title} - Tap to stop`,
        soundName: 'default',
        playSound: true,
        vibrate: true,
        vibration: 300,
        channelId: 'alarm-channel',
        importance: 'high',
        userInfo: {
          isAlarm: true,
          alarmId: alarm.id,
        },
      });
    }
  }
}

export const notificationService = new NotificationService();

export const scheduleAlarmNotification = (alarm: Alarm) => {
  notificationService.scheduleAlarm(alarm);
};

export const cancelAlarmNotification = (alarmId: string) => {
  notificationService.cancelAlarm(alarmId);
};

export const showIntrusiveAlarm = (alarm: Alarm) => {
  notificationService.showIntrusiveNotification(alarm);
};