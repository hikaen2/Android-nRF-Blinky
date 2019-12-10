package no.nordicsemi.android.blinky.profile.callback

import android.bluetooth.BluetoothDevice

interface BlinkyLedCallback {

    /**
     * Called when the data has been sent to the connected device.
     *
     * @param device the target device.
     * @param on true when LED was enabled, false when disabled.
     */
    fun onLedStateChanged(device: BluetoothDevice, on: Boolean)
}
