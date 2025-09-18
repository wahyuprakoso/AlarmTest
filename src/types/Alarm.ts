export interface Alarm {
  id: string;
  title: string;
  time: Date;
  isEnabled: boolean;
  repeat: string[];
  soundName: string;
  vibration: boolean;
  createdAt: Date;
}

export interface AlarmContextType {
  alarms: Alarm[];
  addAlarm: (alarm: Omit<Alarm, 'id' | 'createdAt'>) => void;
  deleteAlarm: (id: string) => void;
  toggleAlarm: (id: string) => void;
  updateAlarm: (id: string, updatedAlarm: Partial<Alarm>) => void;
}