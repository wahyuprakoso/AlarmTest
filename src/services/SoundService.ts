import Sound from 'react-native-sound';
import { Vibration } from 'react-native';

class SoundService {
  private alarmSound: Sound | null = null;
  private isPlaying: boolean = false;

  constructor() {
    Sound.setCategory('Playback');
    this.initializeSound();
  }

  private initializeSound() {
    this.alarmSound = new Sound('alarm.mp3', Sound.MAIN_BUNDLE, (error) => {
      if (error) {
        console.log('Failed to load the sound', error);
        this.alarmSound = new Sound('default', '', (error) => {
          if (error) {
            console.log('Failed to load default sound', error);
          }
        });
      }
    });
  }

  playAlarmSound() {
    if (this.isPlaying) {
      return;
    }

    this.isPlaying = true;
    
    this.startVibration();

    if (this.alarmSound) {
      this.alarmSound.setNumberOfLoops(-1);
      this.alarmSound.setVolume(1.0);
      
      this.alarmSound.play((success) => {
        if (!success) {
          console.log('Sound playback failed');
          this.isPlaying = false;
        }
      });
    }

    setTimeout(() => {
      this.stopAlarmSound();
    }, 60000);
  }

  stopAlarmSound() {
    if (this.alarmSound && this.isPlaying) {
      this.alarmSound.stop(() => {
        console.log('Alarm sound stopped');
      });
    }
    
    this.stopVibration();
    this.isPlaying = false;
  }

  private startVibration() {
    const pattern = [1000, 1000, 1000, 1000];
    Vibration.vibrate(pattern, true);
  }

  private stopVibration() {
    Vibration.cancel();
  }

  isAlarmPlaying(): boolean {
    return this.isPlaying;
  }
}

export const soundService = new SoundService();

export const playAlarmSound = () => {
  soundService.playAlarmSound();
};

export const stopAlarmSound = () => {
  soundService.stopAlarmSound();
};

export const isAlarmPlaying = () => {
  return soundService.isAlarmPlaying();
};