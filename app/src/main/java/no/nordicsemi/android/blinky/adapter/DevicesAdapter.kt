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

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import no.nordicsemi.android.blinky.R
import no.nordicsemi.android.blinky.ScannerActivity
import no.nordicsemi.android.blinky.viewmodels.DevicesLiveData

class DevicesAdapter(
    private val activity: ScannerActivity,
    devicesLiveData: DevicesLiveData
) : RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {
    private var mDevices: List<DiscoveredBluetoothDevice>? = null
    private var mOnItemClickListener: OnItemClickListener? = null

    val isEmpty: Boolean
        get() = itemCount == 0

    @FunctionalInterface
    interface OnItemClickListener {
        fun onItemClick(device: DiscoveredBluetoothDevice)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mOnItemClickListener = listener
    }

    init {
        setHasStableIds(true)
        devicesLiveData.observe(activity, Observer { devices ->
            val result = DiffUtil.calculateDiff(
                DeviceDiffCallback(mDevices, devices), false
            )
            mDevices = devices
            result.dispatchUpdatesTo(this)
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutView = LayoutInflater.from(activity)
            .inflate(R.layout.device_item, parent, false)
        return ViewHolder(layoutView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = mDevices!![position]
        val deviceName = device.name

        if (!TextUtils.isEmpty(deviceName))
            holder.deviceName.text = deviceName
        else
            holder.deviceName.setText(R.string.unknown_device)
        holder.deviceAddress.text = device.address
        val rssiPercent = (100.0f * (127.0f + device.rssi) / (127.0f + 20.0f)).toInt()
        holder.rssi.setImageLevel(rssiPercent)
    }

    override fun getItemId(position: Int): Long {
        return mDevices!![position].hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return mDevices?.size ?: 0
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @BindView(R.id.device_address)
        lateinit var deviceAddress: TextView
        @BindView(R.id.device_name)
        lateinit var deviceName: TextView
        @BindView(R.id.rssi)
        lateinit var rssi: ImageView

        init {
            ButterKnife.bind(this, view)

            view.findViewById<View>(R.id.device_container).setOnClickListener {
                mOnItemClickListener?.onItemClick(mDevices!![adapterPosition])
            }
        }
    }
}
