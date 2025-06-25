import { EventSubscription } from 'expo-modules-core';
import { BarcodeEvent } from './ExpoZebraScannerEvent';
import { BroadcastEvent, BroadcastExtras } from './ExpoZebraBroadcastEvent';
import { CreateProfileData } from './ProfileConstants';
export declare function startBarcodeScan(): any;
export declare function stopBarcodeScan(): any;
export declare function startRfidScan(): any;
export declare function stopRfidScan(): any;
export declare function addListener(listener: (event: BarcodeEvent) => void): EventSubscription;
export declare function removeListener(listener: any): void;
export declare function sendBroadcast(bundle: BroadcastEvent): void;
export declare function sendActionCommand(extraName: string, extraData: BroadcastExtras | string): void;
/**
 * Creates a new Datawedge profile with intent output enabled
 * @param param.PROFILE_NAME - Name of the profile to create
 * @param param.PACKAGE_NAME - The package of your app
 * @param param.PARAM_LIST - Optional scanner params: https://techdocs.zebra.com/datawedge/6-3/guide/api/setconfig/#scannerinputparameters
 */
export declare function createIntentDatawedgeProfile({ PROFILE_NAME, PACKAGE_NAME, }: CreateProfileData): void;
export { default as ExpoZebraRfidModule } from './ExpoZebraRfidModule';
export type { RfidResponse, RfidSignalStrengthEvent, RfidLocateTagEvent, RfidConnectionEvent, } from './ExpoZebraRfidModule';
export { BroadcastExtras, BroadcastEvent };
//# sourceMappingURL=index.d.ts.map