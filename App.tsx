import React from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import TokenScreen from './src/screens/TokenScreen';

function App() {
  return (
    <SafeAreaProvider>
      <TokenScreen />
    </SafeAreaProvider>
  );
}

export default App;
