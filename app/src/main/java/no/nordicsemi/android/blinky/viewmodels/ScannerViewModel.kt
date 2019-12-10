package no.nordicsemi.android.blinky.viewmodels

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.location.LocationManager
import android.preference.PreferenceManager
import androidx.lifecycle.AndroidViewModel
import no.nordicsemi.android.blinky.utils.Utils
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * MutableLiveData containing the list of devices.
     */
    lateinit var devices: DevicesLiveData

    /**
     * MutableLiveData containing the scanner state.
     */
    lateinit var scannerState: ScannerStateLiveData

    private val mPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    val isUuidFilterEnabled: Boolean
        get() = mPreferences.getBoolean(PREFS_FILTER_UUID_REQUIRED, true)

    val isNearbyFilterEnabled: Boolean
        get() = mPreferences.getBoolean(PREFS_FILTER_NEARBY_ONLY, false)

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // This callback will be called only if the scan report delay is not set or is set to 0.

            // If the packet has been obtained while Location was disabled, mark Location as not required
            if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(getApplication()))
                Utils.markLocationNotRequired(getApplication())

            if (devices.deviceDiscovered(result)) {
                devices.applyFilter()
                scannerState.recordFound()
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            // This callback will be called only if the report delay set above is greater then 0.

            // If the packet has been obtained while Location was disabled, mark Location as not required
            if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(getApplication()))
                Utils.markLocationNotRequired(getApplication())

            var atLeastOneMatchedFilter = false
            for (result in results)
                atLeastOneMatchedFilter = devices.deviceDiscovered(result) || atLeastOneMatchedFilter
            if (atLeastOneMatchedFilter) {
                devices.applyFilter()
                scannerState.recordFound()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            // TODO This should be handled
            scannerState.scanningStopped()
        }
    }

    /**
     * Broadcast receiver to monitor the changes in the location provider.
     */
    private val mLocationProviderChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val enabled = Utils.isLocationEnabled(context)
            scannerState.isLocationEnabled = enabled
        }
    }

    /**
     * Broadcast receiver to monitor the changes in the bluetooth adapter.
     */
    private val mBluetoothStateBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            val previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF)

            when (state) {
                BluetoothAdapter.STATE_ON -> scannerState.bluetoothEnabled()
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
                    stopScan()
                    scannerState.bluetoothDisabled()
                }
            }
        }
    }

    init {

        val filterUuidRequired = isUuidFilterEnabled
        val filerNearbyOnly = isNearbyFilterEnabled

        scannerState = ScannerStateLiveData(
            Utils.isBleEnabled,
            Utils.isLocationEnabled(application)
        )
        devices = DevicesLiveData(filterUuidRequired, filerNearbyOnly)
        registerBroadcastReceivers(application)
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(mBluetoothStateBroadcastReceiver)

        if (Utils.isMarshmallowOrAbove) {
            getApplication<Application>().unregisterReceiver(mLocationProviderChangedReceiver)
        }
    }

    /**
     * Forces the observers to be notified. This method is used to refresh the screen after the
     * location permission has been granted. In result, the observer in
     * [no.nordicsemi.android.blinky.ScannerActivity] will try to start scanning.
     */
    fun refresh() {
        scannerState.refresh()
    }

    /**
     * Updates the device filter. Devices that once passed the filter will still be shown
     * even if they move away from the phone, or change the advertising packet. This is to
     * avoid removing devices from the list.
     *
     * @param uuidRequired if true, the list will display only devices with Led-Button Service UUID
     * in the advertising packet.
     */
    fun filterByUuid(uuidRequired: Boolean) {
        mPreferences.edit().putBoolean(PREFS_FILTER_UUID_REQUIRED, uuidRequired).apply()
        if (devices.filterByUuid(uuidRequired))
            scannerState.recordFound()
        else
            scannerState.clearRecords()
    }

    /**
     * Updates the device filter. Devices that once passed the filter will still be shown
     * even if they move away from the phone, or change the advertising packet. This is to
     * avoid removing devices from the list.
     *
     * @param nearbyOnly if true, the list will show only devices with high RSSI.
     */
    fun filterByDistance(nearbyOnly: Boolean) {
        mPreferences.edit().putBoolean(PREFS_FILTER_NEARBY_ONLY, nearbyOnly).apply()
        if (devices.filterByDistance(nearbyOnly))
            scannerState.recordFound()
        else
            scannerState.clearRecords()
    }

    /**
     * Start scanning for Bluetooth devices.
     */
    fun startScan() {
        if (scannerState.isScanning) {
            return
        }

        // Scanning settings
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(500)
            .setUseHardwareBatchingIfSupported(false)
            .build()

        val scanner = BluetoothLeScannerCompat.getScanner()
        scanner.startScan(null, settings, scanCallback)
        scannerState.scanningStarted()
    }

    /**
     * Stop scanning for bluetooth devices.
     */
    fun stopScan() {
        if (scannerState.isScanning && scannerState.isBluetoothEnabled) {
            val scanner = BluetoothLeScannerCompat.getScanner()
            scanner.stopScan(scanCallback)
            scannerState.scanningStopped()
        }
    }

    /**
     * Register for required broadcast receivers.
     */
    private fun registerBroadcastReceivers(application: Application) {
        application.registerReceiver(mBluetoothStateBroadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        if (Utils.isMarshmallowOrAbove) {
            application.registerReceiver(mLocationProviderChangedReceiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION))
        }
    }

    companion object {
        private const val PREFS_FILTER_UUID_REQUIRED = "filter_uuid"
        private const val PREFS_FILTER_NEARBY_ONLY = "filter_nearby"
    }
}
