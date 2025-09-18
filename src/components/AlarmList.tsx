import React from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  Switch,
  Alert,
} from 'react-native';
import { useAlarm } from '../context/AlarmContext';
import { Alarm } from '../types/Alarm';

const AlarmItem: React.FC<{ alarm: Alarm }> = ({ alarm }) => {
  const { toggleAlarm, deleteAlarm } = useAlarm();

  const formatTime = (time: Date) => {
    return time.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const handleDelete = () => {
    Alert.alert(
      'Delete Alarm',
      `Are you sure you want to delete "${alarm.title}"?`,
      [
        {
          text: 'Cancel',
          style: 'cancel',
        },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => deleteAlarm(alarm.id),
        },
      ]
    );
  };

  return (
    <View style={[styles.alarmItem, !alarm.isEnabled && styles.disabledAlarm]}>
      <View style={styles.alarmInfo}>
        <Text style={[styles.timeText, !alarm.isEnabled && styles.disabledText]}>
          {formatTime(alarm.time)}
        </Text>
        <Text style={[styles.titleText, !alarm.isEnabled && styles.disabledText]}>
          {alarm.title}
        </Text>
        <View style={styles.badgeContainer}>
          {alarm.vibration && (
            <View style={styles.badge}>
              <Text style={styles.badgeText}>Vibration</Text>
            </View>
          )}
        </View>
      </View>
      
      <View style={styles.controls}>
        <Switch
          value={alarm.isEnabled}
          onValueChange={() => toggleAlarm(alarm.id)}
          style={styles.switch}
        />
        <TouchableOpacity
          style={styles.deleteButton}
          onPress={handleDelete}
        >
          <Text style={styles.deleteButtonText}>Delete</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const AlarmList: React.FC = () => {
  const { alarms } = useAlarm();

  const sortedAlarms = [...alarms].sort((a, b) => {
    const aTime = new Date(a.time).getTime();
    const bTime = new Date(b.time).getTime();
    return aTime - bTime;
  });

  if (alarms.length === 0) {
    return (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyText}>No alarms set</Text>
        <Text style={styles.emptySubText}>Create your first alarm above</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.header}>Your Alarms ({alarms.length})</Text>
      <FlatList
        data={sortedAlarms}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => <AlarmItem alarm={item} />}
        showsVerticalScrollIndicator={false}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 15,
  },
  header: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 15,
    color: '#333',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  emptyText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#666',
    textAlign: 'center',
  },
  emptySubText: {
    fontSize: 14,
    color: '#999',
    textAlign: 'center',
    marginTop: 8,
  },
  alarmItem: {
    backgroundColor: '#fff',
    borderRadius: 10,
    padding: 15,
    marginBottom: 10,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  disabledAlarm: {
    backgroundColor: '#f8f8f8',
    opacity: 0.7,
  },
  alarmInfo: {
    flex: 1,
  },
  timeText: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
  },
  titleText: {
    fontSize: 16,
    color: '#666',
    marginTop: 4,
  },
  disabledText: {
    color: '#999',
  },
  badgeContainer: {
    flexDirection: 'row',
    marginTop: 8,
  },
  badge: {
    backgroundColor: '#007AFF',
    borderRadius: 12,
    paddingHorizontal: 8,
    paddingVertical: 4,
    marginRight: 8,
  },
  badgeText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  controls: {
    alignItems: 'flex-end',
  },
  switch: {
    marginBottom: 10,
  },
  deleteButton: {
    backgroundColor: '#FF3B30',
    borderRadius: 6,
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  deleteButtonText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
});

export default AlarmList;