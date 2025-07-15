package expo.modules.zebrascanner

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise

import android.util.Log
import androidx.core.os.bundleOf
import android.content.Intent

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
  
  // Remove signal strength throttling - revert to ~50ms updates like 123RFID
  // private var lastSignalUpdateTime = 0L
  // private val SIGNAL_UPDATE_INTERVAL = 500L // Update every 500ms instead of ~50ms
    
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

    // Disconnect from RFID reader to release control back to DataWedge
    AsyncFunction("disconnect") { promise: Promise ->
      try {
        Log.d(TAG, "Disconnect function called")
        
        // Stop any ongoing locate operations
        if (isLocating && rfidReader?.isConnected == true) {
          try {
            rfidReader?.Actions?.TagLocationing?.Stop()
            Log.d(TAG, "Stopped locate tag operation during disconnect")
          } catch (e: Exception) {
            Log.w(TAG, "Error stopping locate tag during disconnect: ${e.message}")
          }
        }
        
        // Stop any ongoing inventory operations
        try {
          rfidReader?.Actions?.Inventory?.stop()
          Log.d(TAG, "Stopped inventory operations during disconnect")
        } catch (e: Exception) {
          Log.w(TAG, "Error stopping inventory during disconnect: ${e.message}")
        }
        
        // Disable beeper to release control
        try {
          rfidReader?.Config?.setBeeperVolume(BEEPER_VOLUME.QUIET_BEEP)
          Log.d(TAG, "Disabled beeper during disconnect")
        } catch (e: Exception) {
          Log.w(TAG, "Error disabling beeper during disconnect: ${e.message}")
        }
        
        // Disconnect from RFID reader
        if (rfidReader?.isConnected == true) {
          try {
            rfidReader?.disconnect()
            Log.d(TAG, "RFID reader disconnected successfully")
          } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting RFID reader: ${e.message}")
          }
        }
        
        // Reset all state
        isLocating = false
        targetTagId = null
        // lastBeepTime = 0L // Removed
        // lastSignalStrength = 0 // Removed
        
        // Clear references
        rfidReader = null
        readerDevice = null
        
        val response = mapOf(
          "status" to "success",
          "message" to "RFID reader disconnected successfully. DataWedge can now take control for barcode scanning.",
          "isConnected" to false,
          "isLocating" to false,
          "timestamp" to System.currentTimeMillis()
        )
        
        Log.d(TAG, "Disconnect responding with: $response")
        promise.resolve(response)
        
      } catch (error: Exception) {
        Log.e(TAG, "Error in disconnect function", error)
        promise.reject("DISCONNECT_ERROR", "Failed to disconnect RFID reader: ${error.message}", error)
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
        
        // Reset signal strength tracking
        // lastBeepTime = 0L // Removed
        // lastSignalStrength = 0 // Removed
        
        // Reset signal update timer for immediate first update
        // lastSignalUpdateTime = 0L // Removed
        
        // CRITICAL: Disable DataWedge to prevent interference during signal strength operations
        try {
          val intent = Intent("com.symbol.datawedge.api.ACTION")
          intent.putExtra("com.symbol.datawedge.api.DISABLE_PLUGIN", "RFID")
          intent.putExtra("com.symbol.datawedge.api.DISABLE_PLUGIN", "BARCODE")
          appContext.reactContext?.sendBroadcast(intent)
          Log.d(TAG, "Disabled DataWedge plugins for signal strength operations")
        } catch (e: Exception) {
          Log.w(TAG, "Could not disable DataWedge: ${e.message}")
        }
        
        // Disable beeper for locate mode (no sound)
        rfidReader?.Config?.setBeeperVolume(BEEPER_VOLUME.QUIET_BEEP)
        
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
        
        // Disable beeper completely (no sound)
        try {
          rfidReader?.Config?.setBeeperVolume(BEEPER_VOLUME.QUIET_BEEP)
          Log.d(TAG, "Disabled beeper after stopping locate tag")
        } catch (e: Exception) {
          Log.w(TAG, "Error disabling beeper: ${e.message}")
        }
        
        // CRITICAL: Re-enable DataWedge for barcode scanning after signal strength operations
        try {
          val intent = Intent("com.symbol.datawedge.api.ACTION")
          intent.putExtra("com.symbol.datawedge.api.ENABLE_PLUGIN", "BARCODE")
          appContext.reactContext?.sendBroadcast(intent)
          Log.d(TAG, "Re-enabled DataWedge barcode scanning after signal strength operations")
        } catch (e: Exception) {
          Log.w(TAG, "Could not re-enable DataWedge: ${e.message}")
        }
        
        // Reset locate state
        isLocating = false
        targetTagId = null
        // lastBeepTime = 0L // Removed
        // lastSignalStrength = 0 // Removed
        
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

    // Test beeper functionality (diagnostic function)
    AsyncFunction("testBeeper") { promise: Promise ->
      try {
        Log.d(TAG, "testBeeper function called")
        
        if (rfidReader?.isConnected != true) {
          throw Exception("RFID reader is not connected. Please connect first.")
        }
        
        // Disable beeper completely (no sound)
        rfidReader?.Config?.setBeeperVolume(BEEPER_VOLUME.QUIET_BEEP)
        
        // Test built-in beeper by performing a brief inventory operation
        try {
          rfidReader?.Actions?.Inventory?.perform()
          
          // Stop after 200ms to get a brief beep
          android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
              rfidReader?.Actions?.Inventory?.stop()
            } catch (e: Exception) {
              Log.w(TAG, "Could not stop inventory test: ${e.message}")
            }
          }, 200)
          
          Log.d(TAG, "Tested built-in beeper with inventory operation")
        } catch (e: Exception) {
          Log.w(TAG, "Could not test built-in beeper: ${e.message}")
        }
        
        // Get model name for debugging
        val modelName = rfidReader?.ReaderCapabilities?.modelName ?: "Unknown"
        
        val response = mapOf(
          "status" to "success",
          "message" to "Built-in beeper is disabled (no sound)",
          "modelName" to modelName,
          "timestamp" to System.currentTimeMillis()
        )
        
        Log.d(TAG, "testBeeper responding with: $response")
        promise.resolve(response)
        
      } catch (error: Exception) {
        Log.e(TAG, "Error in testBeeper function", error)
        promise.reject("BEEPER_TEST_ERROR", "Failed to test beeper: ${error.message}", error)
      }
    }

    // Remove setBeepingFrequency function - following 123RFID approach
    // 123RFID relies purely on automatic beeping during locate operations
    // No manual frequency control needed
    // AsyncFunction("setBeepingFrequency") { signalStrength: Int, promise: Promise ->
    //   // Removed - following 123RFID approach
    // }

    // Release control back to DataWedge for barcode scanning
    AsyncFunction("releaseControlToDataWedge") { promise: Promise ->
      try {
        Log.d(TAG, "Release control to DataWedge function called")
        
        // Step 1: Stop any ongoing locate operations
        if (isLocating && rfidReader?.isConnected == true) {
          try {
            rfidReader?.Actions?.TagLocationing?.Stop()
            Log.d(TAG, "Stopped locate tag operation during release")
          } catch (e: Exception) {
            Log.w(TAG, "Error stopping locate tag during release: ${e.message}")
          }
        }
        
        // Step 2: Stop any ongoing inventory operations
        try {
          rfidReader?.Actions?.Inventory?.stop()
          Log.d(TAG, "Stopped inventory operations during release")
        } catch (e: Exception) {
          Log.w(TAG, "Error stopping inventory during release: ${e.message}")
        }
        
        // Step 3: Disable beeper completely (no sound)
        try {
          rfidReader?.Config?.setBeeperVolume(BEEPER_VOLUME.QUIET_BEEP)
          Log.d(TAG, "Disabled beeper during release")
        } catch (e: Exception) {
          Log.w(TAG, "Error disabling beeper during release: ${e.message}")
        }
        
        // Step 4: Force disconnect from RFID reader
        if (rfidReader?.isConnected == true) {
          try {
            rfidReader?.disconnect()
            Log.d(TAG, "RFID reader disconnected during release")
          } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting RFID reader during release: ${e.message}")
          }
        }
        
        // Step 5: Reset all state
        isLocating = false
        targetTagId = null
        // lastBeepTime = 0L // Removed
        // lastSignalStrength = 0 // Removed
        
        // Step 6: Clear references
        rfidReader = null
        readerDevice = null
        
        // Step 7: Send broadcast to restart DataWedge scanning
        try {
          val intent = Intent()
          intent.action = "com.symbol.datawedge.api.ACTION"
          intent.putExtra("com.symbol.datawedge.api.SOFT_SCAN_TRIGGER", "START_SCANNING")
          appContext?.reactContext?.sendBroadcast(intent)
          Log.d(TAG, "Sent DataWedge restart broadcast")
        } catch (e: Exception) {
          Log.w(TAG, "Error sending DataWedge broadcast: ${e.message}")
        }
        
        val response = mapOf(
          "status" to "success",
          "message" to "Control released to DataWedge. Barcode scanning should now work.",
          "isConnected" to false,
          "isLocating" to false,
          "timestamp" to System.currentTimeMillis()
        )
        
        Log.d(TAG, "Release control responding with: $response")
        promise.resolve(response)
        
      } catch (error: Exception) {
        Log.e(TAG, "Error in releaseControlToDataWedge function", error)
        promise.reject("RELEASE_CONTROL_ERROR", "Failed to release control to DataWedge: ${error.message}", error)
      }
    }

    // Restart DataWedge service completely
    AsyncFunction("restartDataWedgeService") { promise: Promise ->
      try {
        Log.d(TAG, "Restart DataWedge service function called")
        
        // Send broadcast to restart DataWedge service
        val intent = Intent()
        intent.action = "com.symbol.datawedge.api.ACTION"
        intent.putExtra("com.symbol.datawedge.api.RESTART_DATAWEDGE", "")
        appContext?.reactContext?.sendBroadcast(intent)
        
        // Wait a moment for the restart to take effect
        Thread.sleep(2000)
        
        // Try to start barcode scanning after restart
        val scanIntent = Intent()
        scanIntent.action = "com.symbol.datawedge.api.ACTION"
        scanIntent.putExtra("com.symbol.datawedge.api.SOFT_SCAN_TRIGGER", "START_SCANNING")
        appContext?.reactContext?.sendBroadcast(scanIntent)
        
        val response = mapOf(
          "status" to "success",
          "message" to "DataWedge service restarted and barcode scanning initiated",
          "timestamp" to System.currentTimeMillis()
        )
        
        Log.d(TAG, "Restart DataWedge service responding with: $response")
        promise.resolve(response)
        
      } catch (error: Exception) {
        Log.e(TAG, "Error in restartDataWedgeService function", error)
        promise.reject("RESTART_DATAWEDGE_ERROR", "Failed to restart DataWedge service: ${error.message}", error)
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
        singulation.session = SESSION.SESSION_S1  // Use S1 to avoid conflicts with DataWedge S0
        singulation.Action.inventoryState = INVENTORY_STATE.INVENTORY_STATE_A
        singulation.Action.setPerformStateAwareSingulationAction(false)
        reader.Config.Antennas.setSingulationControl(1, singulation)
        
        // Configure tag storage settings (following MAUI pattern)
        val tagFields = arrayOf(TAG_FIELD.PEAK_RSSI, TAG_FIELD.TAG_SEEN_COUNT)
        reader.Config.tagStorageSettings.setTagFields(tagFields)
        
        // Configure beeper settings (following MAUI SDK pattern)
        // Disable built-in Zebra beeper for locate operations (no sound)
        reader.Config.setBeeperVolume(BEEPER_VOLUME.QUIET_BEEP)
        
        // Additional beeper configuration for MC3300 and other models
        try {
          // For MC3300 and similar devices, ensure beeper is configured properly
          val modelName = reader.ReaderCapabilities?.modelName ?: ""
          Log.d(TAG, "Configuring beeper for model: $modelName")
          
          // Apply enhanced beeper configuration for all supported devices
          // Disable DPO and batch mode for better beeper performance (following MAUI pattern)
          reader.Config.setBatchMode(BATCH_MODE.DISABLE)
          reader.Config.dpoState = DYNAMIC_POWER_OPTIMIZATION.DISABLE
          
          // Disable built-in beeper for all models (no sound)
          reader.Config.setBeeperVolume(BEEPER_VOLUME.QUIET_BEEP)
          
          Log.d(TAG, "Applied enhanced beeper configuration: disabled batch mode, DPO, enabled built-in beeper")
        } catch (e: Exception) {
          Log.w(TAG, "Could not configure advanced beeper settings: ${e.message}")
        }
        
        Log.d(TAG, "RFID reader configured successfully with built-in beeper disabled")
      }
    } catch (error: Exception) {
      Log.e(TAG, "Error configuring RFID reader", error)
    }
  }

  // Remove signal strength-based beeping control - following 123RFID approach
  // 123RFID relies purely on automatic beeping during locate operations
  // private fun updateBuiltInBeeping(signalStrength: Int) {
  //   // Removed - following 123RFID approach
  // }
  
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
        Log.d(TAG, "Received ${tags.size} tags in eventReadNotify")
        
        // Process tags for locate mode (following MAUI TagReadEvent pattern)
        if (isLocating && targetTagId != null) {
          var foundTargetTag = false
          
          for (tag in tags) {
            // CRITICAL FIX: In locate mode, LocationInfo presence indicates we found the target tag
            // The tag.tagID can be null in locate mode, but LocationInfo means we found our target
            if (tag.LocationInfo != null) {
              foundTargetTag = true
              
              // Use RelativeDistance directly from LocationInfo (this is the 123RFID algorithm output)
              val relativeDistance = tag.LocationInfo.relativeDistance.toInt()
              val rssi = tag.peakRSSI.toInt()
              
              // The RelativeDistance from LocationInfo is already the 1-100 signal strength
              // This matches exactly what 123RFID Mobile app shows
              val signalStrength = relativeDistance
              
              Log.d(TAG, "Target tag found - Signal: $signalStrength, RSSI: $rssi, Distance: $relativeDistance")
              
              // Throttle signal strength updates to reduce frequency
              // val currentTime = System.currentTimeMillis()
              // if (currentTime - lastSignalUpdateTime >= SIGNAL_UPDATE_INTERVAL) {
              //   lastSignalUpdateTime = currentTime
                
                // Emit signal strength event to React Native
                val eventData = bundleOf(
                  "tagId" to targetTagId, // Use our target tag ID, not tag.tagID which can be null
                  "signalStrength" to signalStrength,
                  "rssi" to rssi,
                  "relativeDistance" to relativeDistance,
                  "timestamp" to System.currentTimeMillis()
                )
                
                sendEvent("onRfidSignalStrength", eventData)
                Log.d(TAG, "Emitted throttled signal strength event: $signalStrength")
              // } else {
              //   Log.d(TAG, "Skipped signal strength update (throttled): $signalStrength")
              // }
            }
          }
          
          if (!foundTargetTag) {
            Log.d(TAG, "No target tag found in this read cycle")
          }
        } else {
          Log.d(TAG, "Not in locate mode or no target tag set")
        }
      } else {
        Log.d(TAG, "No tags received in eventReadNotify")
      }
    } catch (error: Exception) {
      Log.e(TAG, "Error in eventReadNotify", error)
    }
  }
  
  override fun eventStatusNotify(rfidStatusEvents: RfidStatusEvents?) {
    Log.d(TAG, "RFID status event: ${rfidStatusEvents?.StatusEventData?.statusEventType}")
  }
} 