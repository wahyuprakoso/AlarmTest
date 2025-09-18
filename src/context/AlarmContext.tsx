import React, { createContext, useContext, useEffect, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Alarm, AlarmContextType } from '../types/Alarm';
import { scheduleNativeAlarm, cancelNativeAlarm } from '../services/NativeAlarmService';

const AlarmContext = createContext<AlarmContextType | undefined>(undefined);

export const useAlarm = () => {
  const context = useContext(AlarmContext);
  if (!context) {
    throw new Error('useAlarm must be used within an AlarmProvider');
  }
  return context;
};

export const AlarmProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [alarms, setAlarms] = useState<Alarm[]>([]);

  useEffect(() => {
    loadAlarms();
  }, []);

  const loadAlarms = async () => {
    try {
      const storedAlarms = await AsyncStorage.getItem('alarms');
      if (storedAlarms) {
        const parsedAlarms = JSON.parse(storedAlarms).map((alarm: any) => ({
          ...alarm,
          time: new Date(alarm.time),
          createdAt: new Date(alarm.createdAt),
        }));
        setAlarms(parsedAlarms);
        
        parsedAlarms.forEach(async (alarm) => {
          if (alarm.isEnabled) {
            await scheduleNativeAlarm(alarm);
          }
        });
      }
    } catch (error) {
      console.error('Error loading alarms:', error);
    }
  };

  const saveAlarms = async (alarmsToSave: Alarm[]) => {
    try {
      await AsyncStorage.setItem('alarms', JSON.stringify(alarmsToSave));
    } catch (error) {
      console.error('Error saving alarms:', error);
    }
  };

  const addAlarm = (newAlarm: Omit<Alarm, 'id' | 'createdAt'>) => {
    const alarm: Alarm = {
      ...newAlarm,
      id: Date.now().toString(),
      createdAt: new Date(),
    };
    
    const updatedAlarms = [...alarms, alarm];
    setAlarms(updatedAlarms);
    saveAlarms(updatedAlarms);
    
    if (alarm.isEnabled) {
      scheduleNativeAlarm(alarm);
    }
  };

  const deleteAlarm = (id: string) => {
    cancelNativeAlarm(id);
    const updatedAlarms = alarms.filter(alarm => alarm.id !== id);
    setAlarms(updatedAlarms);
    saveAlarms(updatedAlarms);
  };

  const toggleAlarm = (id: string) => {
    const updatedAlarms = alarms.map(alarm => {
      if (alarm.id === id) {
        const toggledAlarm = { ...alarm, isEnabled: !alarm.isEnabled };
        if (toggledAlarm.isEnabled) {
          scheduleNativeAlarm(toggledAlarm);
        } else {
          cancelNativeAlarm(id);
        }
        return toggledAlarm;
      }
      return alarm;
    });
    setAlarms(updatedAlarms);
    saveAlarms(updatedAlarms);
  };

  const updateAlarm = (id: string, updatedAlarm: Partial<Alarm>) => {
    const updatedAlarms = alarms.map(alarm => {
      if (alarm.id === id) {
        const newAlarm = { ...alarm, ...updatedAlarm };
        cancelNativeAlarm(id);
        if (newAlarm.isEnabled) {
          scheduleNativeAlarm(newAlarm);
        }
        return newAlarm;
      }
      return alarm;
    });
    setAlarms(updatedAlarms);
    saveAlarms(updatedAlarms);
  };

  return (
    <AlarmContext.Provider value={{ alarms, addAlarm, deleteAlarm, toggleAlarm, updateAlarm }}>
      {children}
    </AlarmContext.Provider>
  );
};