import React from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AlarmProvider } from './src/context/AlarmContext';
import HomeScreen from './src/screens/HomeScreen';

function App() {
  return (
    <SafeAreaProvider>
      <AlarmProvider>
        <HomeScreen />
      </AlarmProvider>
    </SafeAreaProvider>
  );
}

export default App;
