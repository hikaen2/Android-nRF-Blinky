/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.blinky.profile

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.blinky.profile.callback.BlinkyButtonDataCallback
import no.nordicsemi.android.blinky.profile.callback.BlinkyLedDataCallback
import no.nordicsemi.android.blinky.profile.data.BlinkyLED
import no.nordicsemi.android.log.LogContract
import no.nordicsemi.android.log.LogSession
import no.nordicsemi.android.log.Logger
import java.util.*

class BlinkyManager(context: Context) : BleManager<BlinkyManagerCallbacks>(context) {

    private var mButtonCharacteristic: BluetoothGattCharacteristic? = null
    private var mLedCharacteristic: BluetoothGattCharacteristic? = null
    private var mLogSession: LogSession? = null
    private var mSupported: Boolean = false
    private var mLedOn: Boolean = false

    /**
     * The Button callback will be notified when a notification from Button characteristic
     * has been received, or its data was read.
     *
     *
     * If the data received are valid (single byte equal to 0x00 or 0x01), the
     * [BlinkyButtonDataCallback.onButtonStateChanged] will be called.
     * Otherwise, the [BlinkyButtonDataCallback.onInvalidDataReceived]
     * will be called with the data received.
     */
    private val mButtonCallback = object : BlinkyButtonDataCallback() {
        override fun onButtonStateChanged(device: BluetoothDevice, pressed: Boolean) {
            log(LogContract.Log.Level.APPLICATION, "Button " + if (pressed) "pressed" else "released")
            mCallbacks.onButtonStateChanged(device, pressed)
        }

        override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
            log(Log.WARN, "Invalid data received: $data")
        }
    }

    /**
     * The LED callback will be notified when the LED state was read or sent to the target device.
     *
     *
     * This callback implements both [no.nordicsemi.android.ble.callback.DataReceivedCallback]
     * and [no.nordicsemi.android.ble.callback.DataSentCallback] and calls the same
     * method on success.
     *
     *
     * If the data received were invalid, the
     * [BlinkyLedDataCallback.onInvalidDataReceived] will be
     * called.
     */
    private val mLedCallback = object : BlinkyLedDataCallback() {
        override fun onLedStateChanged(device: BluetoothDevice, on: Boolean) {
            mLedOn = on
            log(LogContract.Log.Level.APPLICATION, "LED " + if (on) "ON" else "OFF")
            mCallbacks.onLedStateChanged(device, on)
        }

        override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
            // Data can only invalid if we read them. We assume the app always sends correct data.
            log(Log.WARN, "Invalid data received: $data")
        }
    }

    /**
     * BluetoothGatt callbacks object.
     */
    private val mGattCallback = object : BleManagerGattCallback() {
        override fun initialize() {
            setNotificationCallback(mButtonCharacteristic).with(mButtonCallback)
            readCharacteristic(mLedCharacteristic).with(mLedCallback).enqueue()
            readCharacteristic(mButtonCharacteristic).with(mButtonCallback).enqueue()
            enableNotifications(mButtonCharacteristic).enqueue()
        }

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(LBS_UUID_SERVICE)
            if (service != null) {
                mButtonCharacteristic = service.getCharacteristic(LBS_UUID_BUTTON_CHAR)
                mLedCharacteristic = service.getCharacteristic(LBS_UUID_LED_CHAR)
            }

            var writeRequest = false
            if (mLedCharacteristic != null) {
                val rxProperties = mLedCharacteristic!!.properties
                writeRequest = rxProperties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0
            }

            mSupported = mButtonCharacteristic != null && mLedCharacteristic != null && writeRequest
            return mSupported
        }

        override fun onDeviceDisconnected() {
            mButtonCharacteristic = null
            mLedCharacteristic = null
        }
    }

    override fun getGattCallback(): BleManagerGattCallback {
        return mGattCallback
    }

    /**
     * Sets the log session to be used for low level logging.
     * @param session the session, or null, if nRF Logger is not installed.
     */
    fun setLogger(session: LogSession?) {
        this.mLogSession = session
    }

    override fun log(priority: Int, message: String) {
        // The priority is a Log.X constant, while the Logger accepts it's log levels.
        Logger.log(mLogSession, LogContract.Log.Level.fromPriority(priority), message)
    }

    override fun shouldClearCacheWhenDisconnected(): Boolean {
        return !mSupported
    }

    /**
     * Sends a request to the device to turn the LED on or off.
     *
     * @param on true to turn the LED on, false to turn it off.
     */
    fun send(on: Boolean) {
        // Are we connected?
        if (mLedCharacteristic == null)
            return

        // No need to change?
        if (mLedOn == on)
            return

        log(Log.VERBOSE, "Turning LED " + (if (on) "ON" else "OFF") + "...")
        writeCharacteristic(mLedCharacteristic, if (on) BlinkyLED.turnOn() else BlinkyLED.turnOff())
            .with(mLedCallback).enqueue()
    }

    companion object {
        /** Nordic Blinky Service UUID.  */
        val LBS_UUID_SERVICE: UUID = UUID.fromString("00001523-1212-efde-1523-785feabcd123")
        /** BUTTON characteristic UUID.  */
        private val LBS_UUID_BUTTON_CHAR = UUID.fromString("00001524-1212-efde-1523-785feabcd123")
        /** LED characteristic UUID.  */
        private val LBS_UUID_LED_CHAR = UUID.fromString("00001525-1212-efde-1523-785feabcd123")
    }
}
