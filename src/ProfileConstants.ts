// https://techdocs.zebra.com/datawedge/6-3/guide/api/setconfig/#scannerinputparameters
export type ScannerParams = {
  [key: string]: string;
};

export type CreateProfileData = {
  PROFILE_NAME: string;
  PACKAGE_NAME: string;
  PARAM_LIST?: ScannerParams;
};

export const DEFAULT_BARCODES_CONFIG = {
  PROFILE_ENABLED: 'true',
  CONFIG_MODE: 'UPDATE',
  PLUGIN_CONFIG: {
    PLUGIN_NAME: 'BARCODE',
    RESET_CONFIG: 'true',
    PARAM_LIST: {
      beam_timer: '15000', // Sets the amount of time (in ms) that the reader remains on
      scanner_selection: 'auto',
      decoder_code11: 'true',
      decoder_aztec: 'true',
      decoder_codabar: 'true',
      decoder_code39: 'true',
      decoder_code93: 'true',
      decoder_code128: 'true',
      decoder_datamatrix: 'true',
      decoder_ean13: 'true',
      decoder_ean8: 'true',
      decoder_interleaved2of5: 'true',
      decoder_itf14: 'true',
      decoder_maxicode: 'true',
      decoder_pdf417: 'true',
      decoder_rss14: 'true',
      decoder_rssexpanded: 'true',
      decoder_upca: 'true',
      decoder_upce: 'true',
      decoder_qrcode: 'true',
      decoder_i2of5: 'true',
    },
  },
  APP_LIST: [
    {
      PACKAGE_NAME: '',
      ACTIVITY_LIST: ['*'],
    },
  ],
};

export const DEFAULT_INTENT_CONFIG = {
  PROFILE_ENABLED: 'true',
  CONFIG_MODE: 'UPDATE',
  PLUGIN_CONFIG: {
    PLUGIN_NAME: 'INTENT',
    RESET_CONFIG: 'true',
    PARAM_LIST: {
      intent_output_enabled: 'true',
      intent_action: 'com.symbol.datawedge.ACTION_BARCODE_SCANNED', // The action specified in ExpoZebraScannerModule.kt
      intent_delivery: '2', // Broadcast
    },
  },
};

export const DEFAULT_KEYSTROKE_CONFIG = {
  PROFILE_ENABLED: 'true',
  CONFIG_MODE: 'UPDATE',
  PLUGIN_CONFIG: {
    PLUGIN_NAME: 'KEYSTROKE',
    RESET_CONFIG: 'true',
    PARAM_LIST: {
      keystroke_output_enabled: 'false', // Disable keystroke
    },
  },
};

export const DEFAULT_RFID_CONFIG = {
  PROFILE_ENABLED: 'true',
  CONFIG_MODE: 'UPDATE',
  PLUGIN_CONFIG: {
    PLUGIN_NAME: 'RFID',
    RESET_CONFIG: 'true',
    PARAM_LIST: {
      rfid_input_enabled: 'true',
      rfid_selection: 'auto',
      rfid_power_level: '270', // Power level in centi-dBm (27.0 dBm)
      rfid_session: '0', // Session 0 to avoid conflicts with native module
      rfid_tag_population: '30', // Estimated number of tags in the field
      rfid_operation_mode: 'NORMAL_MODE', // Normal mode instead of DPO for consistency
      rfid_trigger_mode: 'rfid_trigger_mode_press_release', // Press-release for better control
      rfid_tag_read_duration: '2000', // 2 seconds duration for more reliable reads
    },
  },
};
