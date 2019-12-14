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

package no.nordicsemi.android.blinky.adapter

import android.bluetooth.BluetoothDevice
import android.os.Parcel
import android.os.Parcelable
import no.nordicsemi.android.support.v18.scanner.ScanResult
import kotlin.math.max

class DiscoveredBluetoothDevice : Parcelable {
    val device: BluetoothDevice?
    var scanResult: ScanResult? = null
        private set
    var name: String? = null
        private set
    var rssi: Int = 0
        private set
    private var previousRssi: Int = 0

    /**
     * Returns the highest recorded RSSI value during the scan.
     *
     * @return Highest RSSI value.
     */
    var highestRssi = -128
        private set

    val address: String
        get() = device!!.address

    constructor(scanResult: ScanResult) {
        device = scanResult.device
        update(scanResult)
    }

    /**
     * This method returns true if the RSSI range has changed. The RSSI range depends on drawable
     * levels from [no.nordicsemi.android.blinky.R.drawable.ic_signal_bar].
     *
     * @return True, if the RSSI range has changed.
     */
    internal fun hasRssiLevelChanged(): Boolean {
        fun level(rssi: Int): Int = when {
            rssi <= 10 -> 0
            rssi <= 28 -> 1
            rssi <= 45 -> 2
            rssi <= 65 -> 3
            else -> 4
        }
        return level(rssi) != level(previousRssi)
    }

    /**
     * Updates the device values based on the scan result.
     *
     * @param scanResult the new received scan result.
     */
    fun update(scanResult: ScanResult) {
        this.scanResult = scanResult
        name = scanResult.scanRecord?.deviceName
        previousRssi = rssi
        rssi = scanResult.rssi
        highestRssi = max(highestRssi, rssi)
    }

    fun matches(scanResult: ScanResult): Boolean {
        return device!!.address == scanResult.device.address
    }

    override fun hashCode(): Int {
        return device!!.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is DiscoveredBluetoothDevice) {
            val that = other as DiscoveredBluetoothDevice?
            return device!!.address == that!!.device!!.address
        }
        return super.equals(other)
    }

    // Parcelable implementation

    private constructor(input: Parcel) {
        device = input.readParcelable(BluetoothDevice::class.java.classLoader)
        scanResult = input.readParcelable(ScanResult::class.java.classLoader)
        name = input.readString()
        rssi = input.readInt()
        previousRssi = input.readInt()
        highestRssi = input.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(device, flags)
        parcel.writeParcelable(scanResult, flags)
        parcel.writeString(name)
        parcel.writeInt(rssi)
        parcel.writeInt(previousRssi)
        parcel.writeInt(highestRssi)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DiscoveredBluetoothDevice> {
        override fun createFromParcel(parcel: Parcel): DiscoveredBluetoothDevice {
            return DiscoveredBluetoothDevice(parcel)
        }

        override fun newArray(size: Int): Array<DiscoveredBluetoothDevice?> {
            return arrayOfNulls(size)
        }
    }
}
