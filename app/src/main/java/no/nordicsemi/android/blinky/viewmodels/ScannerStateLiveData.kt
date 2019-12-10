package no.nordicsemi.android.blinky.viewmodels

import androidx.lifecycle.LiveData

/**
 * This class keeps the current state of the scanner.
 */
class ScannerStateLiveData internal constructor(bluetoothEnabled: Boolean, private var mLocationEnabled: Boolean) : LiveData<ScannerStateLiveData>() {

    /**
     * Returns whether scanning is in progress.
     */
    internal var isScanning: Boolean = false
        private set

    private var mHasRecords: Boolean = false

    /**
     * Returns whether Bluetooth adapter is enabled.
     */
    var isBluetoothEnabled: Boolean = false
        private set

    /**
     * Returns whether Location is enabled.
     */
    var isLocationEnabled: Boolean
        get() = mLocationEnabled
        internal set(enabled) {
            mLocationEnabled = enabled
            postValue(this)
        }

    init {
        isScanning = false
        isBluetoothEnabled = bluetoothEnabled
        postValue(this)
    }

    internal fun refresh() {
        postValue(this)
    }

    internal fun scanningStarted() {
        isScanning = true
        postValue(this)
    }

    internal fun scanningStopped() {
        isScanning = false
        postValue(this)
    }

    internal fun bluetoothEnabled() {
        isBluetoothEnabled = true
        postValue(this)
    }

    @Synchronized
    internal fun bluetoothDisabled() {
        isBluetoothEnabled = false
        mHasRecords = false
        postValue(this)
    }

    internal fun recordFound() {
        mHasRecords = true
        postValue(this)
    }

    /**
     * Returns whether any records matching filter criteria has been found.
     */
    fun hasRecords(): Boolean {
        return mHasRecords
    }

    /**
     * Notifies the observer that scanner has no records to show.
     */
    fun clearRecords() {
        mHasRecords = false
        postValue(this)
    }
}
