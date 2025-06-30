import { EventSubscription } from 'expo-modules-core';
export interface RfidResponse {
    status: 'success' | 'error';
    message?: string;
    isConnected?: boolean;
    isLocating?: boolean;
    targetTagId?: string | null;
    readerModel?: string;
    timestamp?: number;
    moduleVersion?: string;
}
export interface RfidSignalStrengthEvent {
    tagId: string;
    signalStrength: number;
    rssi: number;
    timestamp: number;
}
export interface RfidLocateTagEvent {
    tagId?: string;
    signalStrength?: number;
    rssi?: number;
    status: 'started' | 'stopped' | 'tag_found' | 'tag_lost';
    timestamp: number;
}
export interface RfidConnectionEvent {
    isConnected: boolean;
    readerModel?: string;
    message?: string;
    timestamp: number;
}
export interface ExpoZebraRfidModuleInterface {
    hello(name: string): Promise<RfidResponse>;
    connect(): Promise<RfidResponse>;
    getCurrentStatus(): Promise<RfidResponse>;
    startLocateTag(tagId: string): Promise<RfidResponse>;
    stopLocateTag(): Promise<RfidResponse>;
    setBeeperEnabled(enabled: boolean): Promise<RfidResponse>;
    testBeeper(): Promise<RfidResponse>;
    addListener(eventName: 'onRfidSignalStrength', listener: (event: RfidSignalStrengthEvent) => void): EventSubscription;
    addListener(eventName: 'onRfidLocateTag', listener: (event: RfidLocateTagEvent) => void): EventSubscription;
    addListener(eventName: 'onRfidConnection', listener: (event: RfidConnectionEvent) => void): EventSubscription;
}
declare const ExpoZebraRfidModule: ExpoZebraRfidModuleInterface;
export default ExpoZebraRfidModule;
//# sourceMappingURL=ExpoZebraRfidModule.d.ts.map