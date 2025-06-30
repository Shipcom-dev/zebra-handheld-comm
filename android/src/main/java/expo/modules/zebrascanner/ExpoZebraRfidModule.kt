package expo.modules.zebrascanner

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise

import android.util.Log
import androidx.core.os.bundleOf

// Zebra RFID SDK imports (exact same as MAUI SDK)
import com.zebra.rfid.api3.*

class ExpoZebraRfidModule : Module(), Readers.RFIDReaderEventHandler, RfidEventsListener {

  private val TAG = "ExpoZebraRfidModule"
  
  // RFID SDK components (following MAUI SDK pattern exactly)
  private var readers: Readers? = null
    private var rfidReader: RFIDReader? = null
    private var readerDevice: ReaderDevice? = null
  private var isLocating = false
  private var targetTagId: String? = null
    
    override fun definition() = ModuleDefinition {

    Name("ExpoZebraRfidModule")
    
    Events("onRfidSignalStrength", "onRfidLocateTag", "onRfidConnection")

    // Test function to verify module connectivity
    AsyncFunction("hello") { name: String, promise: Promise ->
      try {
        Log.d(TAG, "Hello function called with name: $name")
        
        val response = mapOf(
                    "status" to "success",
          "message" to "Hello, $name! RFID module is working correctly.",
          "timestamp" to System.currentTimeMillis(),
          "moduleVersion" to "1.0.0"
        )
        
        Log.d(TAG, "Hello function responding with: $response")
        promise.resolve(response)
        
      } catch (error: Exception) {
        Log.e(TAG, "Error in hello function", error)
        promise.reject("HELLO_ERROR", "Failed to execute hello function: ${error.message}", error)
      }
    }

    // Connect to RFID reader (following exact MAUI SDK pattern)
    AsyncFunction("connect") { promise: Promise ->
      try {
        Log.d(TAG, "Connect function called - initializing RFID SDK")
        
        // Initialize readers (following MAUI ReaderModel.GetAvailableReaders())
        if (readers == null) {
          readers = Readers(appContext.reactContext, ENUM_TRANSPORT.ALL)
        }
        
        // Get available readers list
                val availableReaders = readers?.GetAvailableRFIDReaderList()
        Log.d(TAG, "Available readers count: ${availableReaders?.size ?: 0}")
        
        if (availableReaders != null && availableReaders.isNotEmpty()) {
          // Connect to first available reader (following MAUI pattern)
                readerDevice = availableReaders[0]
          rfidReader = readerDevice?.getRFIDReader()
          
          Log.d(TAG, "Attempting to connect to reader: ${readerDevice?.getName()}")
          
          // Connect to reader (following MAUI ReaderModel.ConnectReaderSync())
                rfidReader?.connect()
                
          // Configure reader if connected (following MAUI ConfigureReader())
                if (rfidReader?.isConnected == true) {
            configureReader()
            
            val response = mapOf(
                        "status" to "success",
              "message" to "RFID reader connected successfully: ${readerDevice?.getName()}",
              "isConnected" to true,
              "readerModel" to (readerDevice?.getName() ?: "Unknown"),
              "timestamp" to System.currentTimeMillis()
            )
            
            Log.d(TAG, "RFID reader connected successfully")
            promise.resolve(response)
                } else {
            throw Exception("Failed to establish connection to RFID reader")
          }
        } else {
          throw Exception("No RFID readers found. This feature requires a Zebra device with RFID capabilities.")
        }
        
      } catch (error: Exception) {
        Log.e(TAG, "Error in connect function", error)
        promise.reject("CONNECT_ERROR", "Failed to connect to RFID reader: ${error.message}", error)
      }
    }

    // Get current RFID status
    AsyncFunction("getCurrentStatus") { promise: Promise ->
      try {
        Log.d(TAG, "getCurrentStatus function called")
        
        val response = mapOf(
                        "status" to "success",
          "isConnected" to (rfidReader?.isConnected ?: false),
          "isLocating" to isLocating,
                        "targetTagId" to targetTagId,
          "readerModel" to (readerDevice?.getName() ?: "Unknown"),
          "timestamp" to System.currentTimeMillis()
        )
        
        Log.d(TAG, "getCurrentStatus responding with: $response")
        promise.resolve(response)
        
      } catch (error: Exception) {
        Log.e(TAG, "Error in getCurrentStatus function", error)
        promise.reject("STATUS_ERROR", "Failed to get current status: ${error.message}", error)
      }
    }

    // Start locate tag mode (following exact MAUI SDK pattern)
    AsyncFunction("startLocateTag") { tagId: String, promise: Promise ->
      try {
        Log.d(TAG, "startLocateTag function called with tagId: $tagId")
        
        if (rfidReader?.isConnected != true) {
          throw Exception("RFID reader is not connected. Please connect first.")
        }
        
        // Set target tag ID for tracking
        targetTagId = tagId
        isLocating = true
        
        // Ensure beeper is enabled before starting locate (following MAUI pattern)
        rfidReader?.Config?.setBeeperVolume(BEEPER_VOLUME.HIGH_BEEP)
        
        // Start locate tag using exact MAUI SDK API pattern
        // Following: rfidReader.Actions.TagLocationing.Perform(tagPattern, tagMask, null)
        rfidReader?.Actions?.TagLocationing?.Perform(tagId, null, null)
        
        Log.d(TAG, "TagLocationing.Perform() called successfully for tag: $tagId")
        
        val response = mapOf(
                        "status" to "success",
          "message" to "Locate tag mode started for tag: $tagId",
          "targetTagId" to tagId,
          "isLocating" to true,
          "timestamp" to System.currentTimeMillis()
        )
        
        Log.d(TAG, "startLocateTag responding with: $response")
        promise.resolve(response)
        
      } catch (error: Exception) {
        Log.e(TAG, "Error in startLocateTag function", error)
        isLocating = false
        targetTagId = null
        promise.reject("LOCATE_START_ERROR", "Failed to start locate tag: ${error.message}", error)
      }
    }

    // Stop locate tag mode (following exact MAUI SDK pattern)
    AsyncFunction("stopLocateTag") { promise: Promise ->
      try {
        Log.d(TAG, "stopLocateTag function called")
        
        if (rfidReader?.isConnected == true && isLocating) {
          // Stop locate tag using exact MAUI SDK API pattern
          // Following: rfidReader.Actions.TagLocationing.Stop()
          rfidReader?.Actions?.TagLocationing?.Stop()
          
          Log.d(TAG, "TagLocationing.Stop() called successfully")
        }
        
        // Reset locate state
        isLocating = false
        targetTagId = null
        
        val response = mapOf(
                    "status" to "success",
          "message" to "Locate tag mode stopped successfully",
          "isLocating" to false,
                    "timestamp" to System.currentTimeMillis()
                )
        
        Log.d(TAG, "stopLocateTag responding with: $response")
        promise.resolve(response)
        
      } catch (error: Exception) {
        Log.e(TAG, "Error in stopLocateTag function", error)
        // Still reset state even on error
        isLocating = false
        targetTagId = null
        promise.reject("LOCATE_STOP_ERROR", "Failed to stop locate tag: ${error.message}", error)
      }
    }

    // Configure beeper settings for locate tag mode
    AsyncFunction("setBeeperEnabled") { enabled: Boolean, promise: Promise ->
      try {
        Log.d(TAG, "setBeeperEnabled function called with enabled: $enabled")
        
        if (rfidReader?.isConnected != true) {
          throw Exception("RFID reader is not connected. Please connect first.")
        }
        
        if (enabled) {
          // Enable beeper with high volume for locate mode
          rfidReader?.Config?.setBeeperVolume(BEEPER_VOLUME.HIGH_BEEP)
        } else {
          // Disable beeper by setting to quiet
          rfidReader?.Config?.setBeeperVolume(BEEPER_VOLUME.QUIET_BEEP)
        }
        
        val response = mapOf(
          "status" to "success",
          "message" to "Beeper ${if (enabled) "enabled" else "disabled"} successfully",
          "enabled" to enabled,
          "timestamp" to System.currentTimeMillis()
        )
        
        Log.d(TAG, "setBeeperEnabled responding with: $response")
        promise.resolve(response)
        
      } catch (error: Exception) {
        Log.e(TAG, "Error in setBeeperEnabled function", error)
        promise.reject("BEEPER_ERROR", "Failed to configure beeper: ${error.message}", error)
      }
    }

    // Test beeper functionality (diagnostic function)
    AsyncFunction("testBeeper") { promise: Promise ->
      try {
        Log.d(TAG, "testBeeper function called")
        
        if (rfidReader?.isConnected != true) {
          throw Exception("RFID reader is not connected. Please connect first.")
        }
        
        // Force beeper configuration and test
        rfidReader?.Config?.setBeeperVolume(BEEPER_VOLUME.HIGH_BEEP)
        
        // Get model name for debugging
        val modelName = rfidReader?.ReaderCapabilities?.modelName ?: "Unknown"
        val beeperSupported = rfidReader?.ReaderCapabilities?.modelName?.contains("MC3300") == true ||
                             rfidReader?.ReaderCapabilities?.modelName?.contains("RFD") == true
        
        val response = mapOf(
          "status" to "success",
          "message" to "Beeper test completed",
          "modelName" to modelName,
          "beeperSupported" to beeperSupported,
          "timestamp" to System.currentTimeMillis()
        )
        
        Log.d(TAG, "testBeeper responding with: $response")
        promise.resolve(response)
        
      } catch (error: Exception) {
        Log.e(TAG, "Error in testBeeper function", error)
        promise.reject("BEEPER_TEST_ERROR", "Failed to test beeper: ${error.message}", error)
      }
    }
  }
  
  // Configure reader (following MAUI SDK ConfigureReader pattern)
  private fun configureReader() {
    try {
      Log.d(TAG, "Configuring RFID reader...")
      
      rfidReader?.let { reader ->
        // Set up event listeners (following MAUI pattern)
        reader.Events.addEventsListener(this)
        reader.Events.setHandheldEvent(true)
        reader.Events.setTagReadEvent(true)
        reader.Events.setAttachTagDataWithReadEvent(false)
        
        // Set trigger mode to RFID (following MAUI pattern)
        reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true)
        
        // Configure antenna settings (following MAUI pattern)
        val antenna = reader.Config.Antennas.getAntennaRfConfig(1)
        antenna.transmitPowerIndex = reader.ReaderCapabilities.getTransmitPowerLevelValues().size - 1
        reader.Config.Antennas.setAntennaRfConfig(1, antenna)
        
        // Configure singulation (following MAUI pattern)  
        val singulation = reader.Config.Antennas.getSingulationControl(1)
        singulation.session = SESSION.SESSION_S0
        singulation.Action.inventoryState = INVENTORY_STATE.INVENTORY_STATE_A
        singulation.Action.setPerformStateAwareSingulationAction(false)
        reader.Config.Antennas.setSingulationControl(1, singulation)
        
        // Configure tag storage settings (following MAUI pattern)
        val tagFields = arrayOf(TAG_FIELD.PEAK_RSSI, TAG_FIELD.TAG_SEEN_COUNT)
        reader.Config.tagStorageSettings.setTagFields(tagFields)
        
        // Configure beeper settings (following MAUI pattern)
        // This enables the built-in Zebra beeping functionality like 123RFID Mobile app
        // The SDK automatically varies beeping frequency based on signal strength during locate mode
        reader.Config.setBeeperVolume(BEEPER_VOLUME.HIGH_BEEP)
        
        // Additional beeper configuration for MC3300 and other models
        try {
          // Enable beeper for locate operations (following MAUI SDK pattern)
          reader.Config.setBeeperVolume(BEEPER_VOLUME.HIGH_BEEP)
          
          // For MC3300 and similar devices, ensure beeper is configured properly
          val modelName = reader.ReaderCapabilities?.modelName ?: ""
          Log.d(TAG, "Configuring beeper for model: $modelName")
          
          // Apply enhanced beeper configuration for all supported devices
          // Disable DPO and batch mode for better beeper performance (following MAUI pattern)
          reader.Config.setBatchMode(BATCH_MODE.DISABLE)
          reader.Config.dpoState = DYNAMIC_POWER_OPTIMIZATION.DISABLE
          
          // Force beeper configuration for all models
          reader.Config.setBeeperVolume(BEEPER_VOLUME.HIGH_BEEP)
          
          Log.d(TAG, "Applied enhanced beeper configuration: disabled batch mode and DPO, set HIGH_BEEP")
        } catch (e: Exception) {
          Log.w(TAG, "Could not configure advanced beeper settings: ${e.message}")
        }
        
        Log.d(TAG, "RFID reader configured successfully with beeper enabled")
      }
    } catch (error: Exception) {
      Log.e(TAG, "Error configuring RFID reader", error)
    }
  }
  
  // RFID Reader Event Handler Implementation (following MAUI SDK pattern)
  override fun RFIDReaderAppeared(readerDevice: ReaderDevice?) {
    Log.d(TAG, "RFID reader appeared: ${readerDevice?.getName()}")
  }
  
  override fun RFIDReaderDisappeared(readerDevice: ReaderDevice?) {
    Log.d(TAG, "RFID reader disappeared: ${readerDevice?.getName()}")
  }
  
  // RFID Events Listener Implementation (following MAUI SDK pattern)
  override fun eventReadNotify(rfidReadEvents: RfidReadEvents?) {
    try {
      // Get tags from read event (following MAUI EventReadNotify pattern)
      val tags = rfidReader?.Actions?.getReadTags(100)
      
      if (tags != null && tags.isNotEmpty()) {
        // Process tags for locate mode (following MAUI TagReadEvent pattern)
        if (isLocating && targetTagId != null) {
          for (tag in tags) {
            // CRITICAL FIX: In locate mode, LocationInfo presence indicates we found the target tag
            // The tag.tagID can be null in locate mode, but LocationInfo means we found our target
            if (tag.LocationInfo != null) {
              // Use RelativeDistance directly from LocationInfo (this is the 123RFID algorithm output)
              val relativeDistance = tag.LocationInfo.relativeDistance.toInt()
              val rssi = tag.peakRSSI.toInt()
              
              // The RelativeDistance from LocationInfo is already the 1-100 signal strength
              // This matches exactly what 123RFID Mobile app shows
              val signalStrength = relativeDistance
              
              // Emit signal strength event to React Native
              val eventData = bundleOf(
                "tagId" to targetTagId, // Use our target tag ID, not tag.tagID which can be null
                "signalStrength" to signalStrength,
                "rssi" to rssi,
                "relativeDistance" to relativeDistance,
                "timestamp" to System.currentTimeMillis()
              )
              
              sendEvent("onRfidSignalStrength", eventData)
            }
          }
        }
      }
    } catch (error: Exception) {
      Log.e(TAG, "Error in eventReadNotify", error)
    }
  }
  
  override fun eventStatusNotify(rfidStatusEvents: RfidStatusEvents?) {
    Log.d(TAG, "RFID status event: ${rfidStatusEvents?.StatusEventData?.statusEventType}")
    }
} 