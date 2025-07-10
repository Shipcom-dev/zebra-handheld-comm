import { EventSubscription } from 'expo-modules-core';
import { requireNativeModule } from 'expo-modules-core';

// Response type matching MAUI SDK patterns
export interface RfidResponse {
  status: 'success' | 'error';
  message?: string;
  isConnected?: boolean;
  isLocating?: boolean;
  targetTagId?: string | null;
  readerModel?: string;
  timestamp?: number;
  moduleVersion?: string;
  isBeeperEnabled?: boolean;
  signalStrength?: number;
}

// Signal strength event matching MAUI SDK TagData.LocationInfo.RelativeDistance
export interface RfidSignalStrengthEvent {
  tagId: string;
  signalStrength: number; // 1-100 range (EXACT MAUI SDK algorithm: (rssi + 72) * 2)
  rssi: number; // Raw RSSI value (-72 to -22 range)
  timestamp: number;
}

// Locate tag event for tag detection/loss
export interface RfidLocateTagEvent {
  tagId?: string;
  signalStrength?: number; // 1-100 range
  rssi?: number; // Raw RSSI
  status: 'started' | 'stopped' | 'tag_found' | 'tag_lost';
  timestamp: number;
}

// Connection status event
export interface RfidConnectionEvent {
  isConnected: boolean;
  readerModel?: string;
  message?: string;
  timestamp: number;
}

// Native module interface matching MAUI SDK API patterns
export interface ExpoZebraRfidModuleInterface {
  // Test connectivity function
  hello(name: string): Promise<RfidResponse>;

  // Connection management (following MAUI SDK pattern)
  connect(): Promise<RfidResponse>;
  getCurrentStatus(): Promise<RfidResponse>;
  disconnect(): Promise<RfidResponse>;
  releaseControlToDataWedge(): Promise<RfidResponse>;
  restartDataWedgeService(): Promise<RfidResponse>;

  // Locate tag functionality (following MAUI SDK TagLocationing API)
  startLocateTag(tagId: string): Promise<RfidResponse>;
  stopLocateTag(): Promise<RfidResponse>;

  // Beeper control for locate mode
  setBeeperEnabled(enabled: boolean): Promise<RfidResponse>;
  testBeeper(): Promise<RfidResponse>;
  setBeepingFrequency(signalStrength: number): Promise<RfidResponse>;

  // Event listeners
  addListener(
    eventName: 'onRfidSignalStrength',
    listener: (event: RfidSignalStrengthEvent) => void,
  ): EventSubscription;

  addListener(
    eventName: 'onRfidLocateTag',
    listener: (event: RfidLocateTagEvent) => void,
  ): EventSubscription;

  addListener(
    eventName: 'onRfidConnection',
    listener: (event: RfidConnectionEvent) => void,
  ): EventSubscription;
}

// Import the native module using Expo modules core
const ExpoZebraRfidModule: ExpoZebraRfidModuleInterface = requireNativeModule(
  'ExpoZebraRfidModule',
);

export default ExpoZebraRfidModule; 