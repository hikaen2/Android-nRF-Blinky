package no.nordicsemi.android.blinky.profile.callback

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback
import no.nordicsemi.android.ble.data.Data

abstract class BlinkyButtonDataCallback : ProfileDataCallback, BlinkyButtonCallback {

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        if (data.size() != 1) {
            onInvalidDataReceived(device, data)
            return
        }

        when (data.getIntValue(Data.FORMAT_UINT8, 0)!!) {
            STATE_PRESSED -> onButtonStateChanged(device, true)
            STATE_RELEASED -> onButtonStateChanged(device, false)
            else -> onInvalidDataReceived(device, data)
        }
    }

    companion object {
        private const val STATE_RELEASED = 0x00
        private const val STATE_PRESSED = 0x01
    }
}
