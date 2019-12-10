package no.nordicsemi.android.blinky.adapter

import androidx.recyclerview.widget.DiffUtil

class DeviceDiffCallback internal constructor(
    private val oldList: List<DiscoveredBluetoothDevice>?,
    private val newList: List<DiscoveredBluetoothDevice>?
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList?.size ?: 0
    }

    override fun getNewListSize(): Int {
        return newList?.size ?: 0
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList!![oldItemPosition] === newList!![newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val device = oldList!![oldItemPosition]
        return device.hasRssiLevelChanged()
    }
}
