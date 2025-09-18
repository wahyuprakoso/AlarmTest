import { NativeModules, Alert, Platform } from 'react-native';
import { Alarm } from '../types/Alarm';

const { AlarmManagerModule } = NativeModules;

interface AlarmManagerInterface {
  scheduleAlarm: (alarmId: string, title: string, timestampMs: number) => Promise<string>;
  cancelAlarm: (alarmId: string) => Promise<string>;
  checkExactAlarmPermission: () => Promise<boolean>;
  requestExactAlarmPermission: () => void;
}

class NativeAlarmService {
  private alarmManager: AlarmManagerInterface;

  constructor() {
    this.alarmManager = AlarmManagerModule;
    this.checkPermissions();
  }

  async checkPermissions() {
    if (Platform.OS === 'android') {
      try {
        const hasPermission = await this.alarmManager.checkExactAlarmPermission();
        if (!hasPermission) {
          Alert.alert(
            'Permission Required',
            'This app needs permission to schedule exact alarms. Please grant permission for alarms to work reliably.',
            [
              {
                text: 'Cancel',
                style: 'cancel',
              },
              {
                text: 'Grant Permission',
                onPress: () => this.alarmManager.requestExactAlarmPermission(),
              },
            ]
          );
        }
      } catch (error) {
        console.warn('Error checking exact alarm permission:', error);
      }
    }
  }

  async scheduleAlarm(alarm: Alarm): Promise<void> {
    try {
      const now = new Date();
      let alarmTime = new Date(alarm.time);
      
      // If alarm time has passed today, schedule for tomorrow
      if (alarmTime.getTime() <= now.getTime()) {
        alarmTime.setDate(alarmTime.getDate() + 1);
      }

      console.log(`Scheduling native alarm: ${alarm.title} for ${alarmTime.toLocaleString()}`);
      
      if (Platform.OS === 'android') {
        await this.alarmManager.scheduleAlarm(
          alarm.id,
          alarm.title,
          alarmTime.getTime()
        );
        
        Alert.alert(
          'Alarm Scheduled',
          `"${alarm.title}" is set for ${alarmTime.toLocaleTimeString()}\n\nThis alarm will work even if the app is closed!`,
          [{ text: 'OK' }]
        );
      } else {
        console.warn('Native alarms only supported on Android');
      }
    } catch (error) {
      console.error('Error scheduling native alarm:', error);
      Alert.alert(
        'Error',
        'Failed to schedule alarm. Please check your device permissions.',
        [{ text: 'OK' }]
      );
    }
  }

  async cancelAlarm(alarmId: string): Promise<void> {
    try {
      if (Platform.OS === 'android') {
        await this.alarmManager.cancelAlarm(alarmId);
        console.log(`Native alarm cancelled: ${alarmId}`);
      }
    } catch (error) {
      console.error('Error cancelling native alarm:', error);
    }
  }

  async scheduleRepeatingAlarm(alarm: Alarm): Promise<void> {
    // For repeating alarms, we schedule the next occurrence
    // The receiver will handle rescheduling after it fires
    await this.scheduleAlarm(alarm);
  }
}

export const nativeAlarmService = new NativeAlarmService();

export const scheduleNativeAlarm = async (alarm: Alarm) => {
  await nativeAlarmService.scheduleAlarm(alarm);
};

export const cancelNativeAlarm = async (alarmId: string) => {
  await nativeAlarmService.cancelAlarm(alarmId);
};

export const scheduleRepeatingNativeAlarm = async (alarm: Alarm) => {
  await nativeAlarmService.scheduleRepeatingAlarm(alarm);
};