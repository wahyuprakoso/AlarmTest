import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Switch,
  Alert,
} from 'react-native';
import DateTimePickerModal from 'react-native-modal-datetime-picker';
import { useAlarm } from '../context/AlarmContext';

const AlarmForm: React.FC = () => {
  const { addAlarm } = useAlarm();
  const [title, setTitle] = useState('');
  const [selectedTime, setSelectedTime] = useState(new Date());
  const [isTimePickerVisible, setTimePickerVisibility] = useState(false);
  const [isEnabled, setIsEnabled] = useState(true);
  const [vibration, setVibration] = useState(true);

  const showTimePicker = () => {
    setTimePickerVisibility(true);
  };

  const hideTimePicker = () => {
    setTimePickerVisibility(false);
  };

  const handleConfirm = (time: Date) => {
    setSelectedTime(time);
    hideTimePicker();
  };

  const handleSaveAlarm = () => {
    if (!title.trim()) {
      Alert.alert('Error', 'Please enter a title for the alarm');
      return;
    }

    addAlarm({
      title: title.trim(),
      time: selectedTime,
      isEnabled,
      repeat: [],
      soundName: 'default',
      vibration,
    });

    setTitle('');
    setSelectedTime(new Date());
    setIsEnabled(true);
    setVibration(true);
    
    Alert.alert('Success', 'Alarm created successfully!');
  };

  const formatTime = (time: Date) => {
    return time.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <View style={styles.container}>
      <Text style={styles.header}>Set New Alarm</Text>
      
      <View style={styles.inputContainer}>
        <Text style={styles.label}>Title</Text>
        <TextInput
          style={styles.input}
          value={title}
          onChangeText={setTitle}
          placeholder="Enter alarm title"
          maxLength={50}
        />
      </View>

      <View style={styles.inputContainer}>
        <Text style={styles.label}>Time</Text>
        <TouchableOpacity style={styles.timeButton} onPress={showTimePicker}>
          <Text style={styles.timeText}>{formatTime(selectedTime)}</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.switchContainer}>
        <Text style={styles.label}>Enable Alarm</Text>
        <Switch value={isEnabled} onValueChange={setIsEnabled} />
      </View>

      <View style={styles.switchContainer}>
        <Text style={styles.label}>Vibration</Text>
        <Switch value={vibration} onValueChange={setVibration} />
      </View>

      <TouchableOpacity style={styles.saveButton} onPress={handleSaveAlarm}>
        <Text style={styles.saveButtonText}>Save Alarm</Text>
      </TouchableOpacity>

      <DateTimePickerModal
        isVisible={isTimePickerVisible}
        mode="time"
        onConfirm={handleConfirm}
        onCancel={hideTimePicker}
        date={selectedTime}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 20,
    backgroundColor: '#f5f5f5',
    borderRadius: 10,
    margin: 15,
  },
  header: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
    color: '#333',
  },
  inputContainer: {
    marginBottom: 15,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
    color: '#333',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#fff',
  },
  timeButton: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    backgroundColor: '#fff',
    alignItems: 'center',
  },
  timeText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
  },
  switchContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 15,
  },
  saveButton: {
    backgroundColor: '#007AFF',
    borderRadius: 8,
    padding: 15,
    alignItems: 'center',
    marginTop: 10,
  },
  saveButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
});

export default AlarmForm;