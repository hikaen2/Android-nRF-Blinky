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
        return if (mDevices != null) mDevices!!.size else 0
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
                if (mOnItemClickListener != null) {
                    mOnItemClickListener!!.onItemClick(mDevices!![adapterPosition])
                }
            }
        }
    }
}
