package no.nordicsemi.android.blinky.profile.callback

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.callback.DataSentCallback
import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback
import no.nordicsemi.android.ble.data.Data

abstract class BlinkyLedDataCallback : ProfileDataCallback, DataSentCallback, BlinkyLedCallback {

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        parse(device, data)
    }

    override fun onDataSent(device: BluetoothDevice, data: Data) {
        parse(device, data)
    }

    private fun parse(device: BluetoothDevice, data: Data) {
        if (data.size() != 1) {
            onInvalidDataReceived(device, data)
            return
        }

        when (data.getIntValue(Data.FORMAT_UINT8, 0)!!) {
            STATE_ON.toInt() -> onLedStateChanged(device, true)
            STATE_OFF.toInt() -> onLedStateChanged(device, false)
            else -> onInvalidDataReceived(device, data)
        }
    }

    companion object {
        private const val STATE_OFF: Byte = 0x00
        private const val STATE_ON: Byte = 0x01
    }
}
