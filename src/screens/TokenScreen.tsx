import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  StatusBar,
  ScrollView,
} from 'react-native';
import Clipboard from '@react-native-clipboard/clipboard';
import { getFCMToken } from '../services/FirebaseService';

const TokenScreen: React.FC = () => {
  const [fcmToken, setFcmToken] = useState<string>('Loading...');

  useEffect(() => {
    loadToken();
  }, []);

  const loadToken = async () => {
    try {
      const token = await getFCMToken();
      if (token) {
        setFcmToken(token);
      } else {
        setFcmToken('Failed to get token');
      }
    } catch (error) {
      console.error('Firebase setup error:', error);
      setFcmToken('Error getting token');
    }
  };

  const copyTokenToClipboard = () => {
    if (
      fcmToken &&
      fcmToken !== 'Loading...' &&
      fcmToken !== 'Failed to get token' &&
      fcmToken !== 'Error getting token'
    ) {
      Clipboard.setString(fcmToken);
      Alert.alert('Copied!', 'FCM Token copied to clipboard');
    }
  };

  const refreshToken = async () => {
    setFcmToken('Loading...');
    await loadToken();
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#6f42c1" />

      <View style={styles.header}>
        <Text style={styles.headerTitle}>📡 Remote Alarm</Text>
        <Text style={styles.headerSubtitle}>Copy FCM Token untuk set alarm via push notification</Text>
      </View>

      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.tokenSection}>
          <Text style={styles.sectionTitle}>FCM Token</Text>
          <Text style={styles.description}>
            Copy token ini untuk mengirim push notification yang akan membuat alarm:
          </Text>
          
          <View style={styles.tokenContainer}>
            <Text style={styles.tokenText} numberOfLines={6}>
              {fcmToken}
            </Text>
          </View>

          <View style={styles.buttonContainer}>
            <TouchableOpacity
              style={[
                styles.copyButton,
                (fcmToken === 'Loading...' ||
                  fcmToken === 'Failed to get token' ||
                  fcmToken === 'Error getting token') &&
                  styles.copyButtonDisabled,
              ]}
              onPress={copyTokenToClipboard}
              disabled={
                fcmToken === 'Loading...' ||
                fcmToken === 'Failed to get token' ||
                fcmToken === 'Error getting token'
              }
            >
              <Text style={styles.copyButtonText}>📋 Copy Token</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.refreshButton}
              onPress={refreshToken}
            >
              <Text style={styles.refreshButtonText}>🔄 Refresh</Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.instructionSection}>
          <Text style={styles.sectionTitle}>📋 Cara Menggunakan</Text>
          <View style={styles.stepContainer}>
            <Text style={styles.stepNumber}>1</Text>
            <Text style={styles.stepText}>Copy FCM Token di atas</Text>
          </View>
          <View style={styles.stepContainer}>
            <Text style={styles.stepNumber}>2</Text>
            <Text style={styles.stepText}>Kirim push notification dengan payload JSON</Text>
          </View>
          <View style={styles.stepContainer}>
            <Text style={styles.stepNumber}>3</Text>
            <Text style={styles.stepText}>Alarm akan berbunyi SEGERA</Text>
          </View>
        </View>

        <View style={styles.exampleSection}>
          <Text style={styles.sectionTitle}>💡 Contoh Payload</Text>
          <Text style={styles.description}>
            Contoh JSON untuk membuat alarm:
          </Text>
          
          <View style={styles.codeContainer}>
            <Text style={styles.codeText}>
{`{
  "notification": {
    "title": "Alarm Trigger",
    "body": "Alarm akan berbunyi SEGERA!"
  },
  "data": {
    "alarm_title": "Bangun Pagi",
    "alarm_message": "Saatnya bangun!"
  }
}`}
            </Text>
          </View>

          <TouchableOpacity
            style={styles.copyExampleButton}
            onPress={() => {
              const examplePayload = {
                notification: {
                  title: "Alarm Trigger",
                  body: "Alarm akan berbunyi SEGERA!"
                },
                data: {
                  alarm_title: "Bangun Pagi",
                  alarm_message: "Saatnya bangun!"
                }
              };
              Clipboard.setString(JSON.stringify(examplePayload, null, 2));
              Alert.alert('Copied!', 'Example payload copied to clipboard');
            }}
          >
            <Text style={styles.copyExampleButtonText}>📋 Copy Contoh Payload</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.fieldSection}>
          <Text style={styles.sectionTitle}>📄 Field Wajib</Text>
          <View style={styles.fieldItem}>
            <Text style={styles.fieldName}>alarm_title</Text>
            <Text style={styles.fieldValue}>Judul alarm</Text>
          </View>
          
          <Text style={styles.optionalLabel}>Field Opsional:</Text>
          <View style={styles.fieldItem}>
            <Text style={styles.fieldName}>alarm_message</Text>
            <Text style={styles.fieldValue}>Pesan alarm</Text>
          </View>
        </View>

        <View style={styles.bottomSpace} />
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  header: {
    backgroundColor: '#6f42c1',
    paddingTop: 50,
    paddingBottom: 30,
    paddingHorizontal: 20,
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#fff',
    textAlign: 'center',
  },
  headerSubtitle: {
    fontSize: 16,
    color: '#E6F3FF',
    textAlign: 'center',
    marginTop: 8,
    lineHeight: 22,
  },
  content: {
    flex: 1,
    padding: 20,
  },
  tokenSection: {
    backgroundColor: '#fff',
    borderRadius: 15,
    padding: 25,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 8,
    elevation: 8,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 15,
  },
  description: {
    fontSize: 15,
    color: '#666',
    marginBottom: 20,
    lineHeight: 22,
  },
  tokenContainer: {
    backgroundColor: '#f8f9fa',
    padding: 20,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#e9ecef',
    marginBottom: 20,
  },
  tokenText: {
    fontSize: 13,
    fontFamily: 'monospace',
    color: '#333',
    lineHeight: 20,
  },
  buttonContainer: {
    flexDirection: 'row',
    gap: 10,
  },
  copyButton: {
    flex: 1,
    backgroundColor: '#6f42c1',
    padding: 15,
    borderRadius: 10,
    alignItems: 'center',
  },
  copyButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  copyButtonDisabled: {
    backgroundColor: '#6c757d',
    opacity: 0.6,
  },
  refreshButton: {
    backgroundColor: '#28a745',
    padding: 15,
    borderRadius: 10,
    alignItems: 'center',
    minWidth: 100,
  },
  refreshButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  instructionSection: {
    backgroundColor: '#fff',
    borderRadius: 15,
    padding: 25,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 4,
  },
  stepContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 15,
  },
  stepNumber: {
    width: 30,
    height: 30,
    backgroundColor: '#6f42c1',
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
    textAlign: 'center',
    lineHeight: 30,
    borderRadius: 15,
    marginRight: 15,
  },
  stepText: {
    flex: 1,
    fontSize: 16,
    color: '#333',
    lineHeight: 22,
  },
  exampleSection: {
    backgroundColor: '#fff',
    borderRadius: 15,
    padding: 25,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 4,
  },
  codeContainer: {
    backgroundColor: '#2d3748',
    padding: 20,
    borderRadius: 10,
    marginBottom: 20,
  },
  codeText: {
    fontSize: 13,
    fontFamily: 'monospace',
    color: '#e2e8f0',
    lineHeight: 20,
  },
  copyExampleButton: {
    backgroundColor: '#17a2b8',
    padding: 15,
    borderRadius: 10,
    alignItems: 'center',
  },
  copyExampleButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  fieldSection: {
    backgroundColor: '#fff',
    borderRadius: 15,
    padding: 25,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 4,
  },
  fieldItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f1f3f4',
  },
  fieldName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    fontFamily: 'monospace',
  },
  fieldValue: {
    fontSize: 14,
    color: '#666',
    fontStyle: 'italic',
  },
  optionalLabel: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#666',
    marginTop: 20,
    marginBottom: 10,
  },
  bottomSpace: {
    height: 50,
  },
});

export default TokenScreen;