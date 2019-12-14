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

package no.nordicsemi.android.blinky.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import no.nordicsemi.android.blinky.R
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice
import no.nordicsemi.android.blinky.profile.BlinkyManager
import no.nordicsemi.android.blinky.profile.BlinkyManagerCallbacks
import no.nordicsemi.android.log.Logger

class BlinkyViewModel(application: Application) : AndroidViewModel(application), BlinkyManagerCallbacks {
    private val mBlinkyManager: BlinkyManager = BlinkyManager(getApplication())
    private var mDevice: BluetoothDevice? = null

    // Connection states Connecting, Connected, Disconnecting, Disconnected etc.
    private val mConnectionState = MutableLiveData<String>()
    val connectionState: LiveData<String>
        get() = mConnectionState

    // Flag to determine if the device is connected
    private val mIsConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean>
        get() = mIsConnected

    // Flag to determine if the device has required services
    private val mIsSupported = MutableLiveData<Boolean>()
    val isSupported: LiveData<Boolean>
        get() = mIsSupported

    // Flag to determine if the device is ready
    private val mOnDeviceReady = MutableLiveData<Void>()
    val isDeviceReady: LiveData<Void>
        get() = mOnDeviceReady

    // Flag that holds the on off state of the LED. On is true, Off is False
    private val mLEDState = MutableLiveData<Boolean>()
    val ledState: LiveData<Boolean>
        get() = mLEDState

    // Flag that holds the pressed released state of the button on the devkit.
    // Pressed is true, Released is false
    private val mButtonState = MutableLiveData<Boolean>()
    val buttonState: LiveData<Boolean>
        get() = mButtonState

    init {

        // Initialize the manager
        mBlinkyManager.setGattCallbacks(this)
    }

    /**
     * Connect to peripheral.
     */
    fun connect(device: DiscoveredBluetoothDevice) {
        // Prevent from calling again when called again (screen orientation changed)
        if (mDevice == null) {
            mDevice = device.device
            val logSession = Logger.newSession(getApplication(), null, device.address, device.name ?: "")
            mBlinkyManager.setLogger(logSession)
            reconnect()
        }
    }

    /**
     * Reconnects to previously connected device.
     * If this device was not supported, its services were cleared on disconnection, so
     * reconnection may help.
     */
    fun reconnect() {
        if (mDevice != null) {
            mBlinkyManager.connect(mDevice!!)
                .retry(3, 100)
                .useAutoConnect(false)
                .enqueue()
        }
    }

    /**
     * Disconnect from peripheral.
     */
    private fun disconnect() {
        mDevice = null
        mBlinkyManager.disconnect().enqueue()
    }

    fun toggleLED(isOn: Boolean) {
        mBlinkyManager.send(isOn)
        mLEDState.value = isOn
    }

    override fun onCleared() {
        super.onCleared()
        if (mBlinkyManager.isConnected) {
            disconnect()
        }
    }

    override fun onButtonStateChanged(device: BluetoothDevice, pressed: Boolean) {
        mButtonState.postValue(pressed)
    }

    override fun onLedStateChanged(device: BluetoothDevice, on: Boolean) {
        mLEDState.postValue(on)
    }

    override fun onDeviceConnecting(device: BluetoothDevice) {
        mConnectionState.postValue(getApplication<Application>().getString(R.string.state_connecting))
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        mIsConnected.postValue(true)
        mConnectionState.postValue(getApplication<Application>().getString(R.string.state_discovering_services))
    }

    override fun onDeviceDisconnecting(device: BluetoothDevice) {
        mIsConnected.postValue(false)
    }

    override fun onDeviceDisconnected(device: BluetoothDevice) {
        mIsConnected.postValue(false)
    }

    override fun onLinkLossOccurred(device: BluetoothDevice) {
        mIsConnected.postValue(false)
    }

    override fun onServicesDiscovered(device: BluetoothDevice, optionalServicesFound: Boolean) {
        mConnectionState.postValue(getApplication<Application>().getString(R.string.state_initializing))
    }

    override fun onDeviceReady(device: BluetoothDevice) {
        mIsSupported.postValue(true)
        mConnectionState.postValue(null)
        mOnDeviceReady.postValue(null)
    }

    override fun onBondingRequired(device: BluetoothDevice) {
        // Blinky does not require bonding
    }

    override fun onBonded(device: BluetoothDevice) {
        // Blinky does not require bonding
    }

    override fun onBondingFailed(device: BluetoothDevice) {
        // Blinky does not require bonding
    }

    override fun onError(device: BluetoothDevice, message: String, errorCode: Int) {
        // TODO implement
    }

    override fun onDeviceNotSupported(device: BluetoothDevice) {
        mConnectionState.postValue(null)
        mIsSupported.postValue(false)
    }
}
