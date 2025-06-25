import { EventSubscription } from 'expo-modules-core';
import ExpoZebraScannerModule from './ExpoZebraScannerModule';
import ExpoZebraRfidModule from './ExpoZebraRfidModule';
import { BarcodeEvent } from './ExpoZebraScannerEvent';
import { BroadcastEvent, BroadcastExtras } from './ExpoZebraBroadcastEvent';
import {
  CreateProfileData,
  DEFAULT_BARCODES_CONFIG,
  DEFAULT_INTENT_CONFIG,
  DEFAULT_KEYSTROKE_CONFIG,
  DEFAULT_RFID_CONFIG,
} from './ProfileConstants';

export function startBarcodeScan() {
  return ExpoZebraScannerModule.startBarcodeScan();
}

export function stopBarcodeScan() {
  return ExpoZebraScannerModule.stopBarcodeScan();
}

export function startRfidScan() {
  return ExpoZebraScannerModule.startRfidScan();
}

export function stopRfidScan() {
  return ExpoZebraScannerModule.stopRfidScan();
}

export function addListener(
  listener: (event: BarcodeEvent) => void,
): EventSubscription {
  return ExpoZebraScannerModule.addListener('onBarcodeScanned', listener);
}

export function removeListener(listener: any): void {
  listener?.remove();
}

export function sendBroadcast(bundle: BroadcastEvent) {
  ExpoZebraScannerModule.sendBroadcast(bundle);
}

export function sendActionCommand(
  extraName: string,
  extraData: BroadcastExtras | string,
) {
  ExpoZebraScannerModule.sendBroadcast({
    action: 'com.symbol.datawedge.api.ACTION',
    extras: {
      [extraName]: extraData,
    },
  });
}

/**
 * Creates a new Datawedge profile with intent output enabled
 * @param param.PROFILE_NAME - Name of the profile to create
 * @param param.PACKAGE_NAME - The package of your app
 * @param param.PARAM_LIST - Optional scanner params: https://techdocs.zebra.com/datawedge/6-3/guide/api/setconfig/#scannerinputparameters
 */
export function createIntentDatawedgeProfile({
  PROFILE_NAME,
  PACKAGE_NAME,
}: CreateProfileData) {
  sendActionCommand('com.symbol.datawedge.api.CREATE_PROFILE', PROFILE_NAME);
  sendActionCommand('com.symbol.datawedge.api.SET_CONFIG', {
    ...DEFAULT_INTENT_CONFIG,
    PROFILE_NAME,
  });
  sendActionCommand('com.symbol.datawedge.api.SET_CONFIG', {
    ...DEFAULT_KEYSTROKE_CONFIG,
    PROFILE_NAME,
  });
  sendActionCommand('com.symbol.datawedge.api.SET_CONFIG', {
    ...DEFAULT_RFID_CONFIG,
    PROFILE_NAME,
  });
  sendActionCommand('com.symbol.datawedge.api.SET_CONFIG', {
    ...DEFAULT_BARCODES_CONFIG,
    PROFILE_NAME,
    PLUGIN_CONFIG: {
      ...DEFAULT_BARCODES_CONFIG.PLUGIN_CONFIG,
      PARAM_LIST: {
        ...DEFAULT_BARCODES_CONFIG.PLUGIN_CONFIG.PARAM_LIST,
      },
    },
    APP_LIST: [
      {
        PACKAGE_NAME,
        ACTIVITY_LIST: ['*'],
      },
    ],
  });
}

// Export RFID module and types
export { default as ExpoZebraRfidModule } from './ExpoZebraRfidModule';
export type {
  RfidResponse,
  RfidSignalStrengthEvent,
  RfidLocateTagEvent,
  RfidConnectionEvent,
} from './ExpoZebraRfidModule';

export { BroadcastExtras, BroadcastEvent };