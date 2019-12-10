package no.nordicsemi.android.blinky.profile.data

import no.nordicsemi.android.ble.data.Data

object BlinkyLED {
    private const val STATE_OFF: Byte = 0x00
    private const val STATE_ON: Byte = 0x01

    fun turnOn(): Data {
        return Data.opCode(STATE_ON)
    }

    fun turnOff(): Data {
        return Data.opCode(STATE_OFF)
    }
}
