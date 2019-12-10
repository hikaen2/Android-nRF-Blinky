package no.nordicsemi.android.blinky.profile.callback

import android.bluetooth.BluetoothDevice

interface BlinkyButtonCallback {

    /**
     * Called when a button was pressed or released on device.
     *
     * @param device the target device.
     * @param pressed true if the button was pressed, false if released.
     */
    fun onButtonStateChanged(device: BluetoothDevice, pressed: Boolean)
}
