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
