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

package no.nordicsemi.android.blinky

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import no.nordicsemi.android.blinky.adapter.DevicesAdapter
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice
import no.nordicsemi.android.blinky.utils.Utils
import no.nordicsemi.android.blinky.viewmodels.ScannerStateLiveData
import no.nordicsemi.android.blinky.viewmodels.ScannerViewModel

class ScannerActivity : AppCompatActivity(), DevicesAdapter.OnItemClickListener {

    private lateinit var mScannerViewModel: ScannerViewModel

    @BindView(R.id.state_scanning)
    internal lateinit var mScanningView: View
    @BindView(R.id.no_devices)
    internal lateinit var mEmptyView: View
    @BindView(R.id.no_location_permission)
    internal lateinit var mNoLocationPermissionView: View
    @BindView(R.id.action_grant_location_permission)
    internal lateinit var mGrantPermissionButton: Button
    @BindView(R.id.action_permission_settings)
    internal lateinit var mPermissionSettingsButton: Button
    @BindView(R.id.no_location)
    internal lateinit var mNoLocationView: View
    @BindView(R.id.bluetooth_off)
    internal lateinit var mNoBluetoothView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        ButterKnife.bind(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setTitle(R.string.app_name)

        // Create view model containing utility methods for scanning
        mScannerViewModel = ViewModelProviders.of(this).get(ScannerViewModel::class.java)
        mScannerViewModel.scannerState.observe(this, Observer<ScannerStateLiveData> { this.startScan(it) })

        // Configure the recycler view
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_ble_devices)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        val adapter = DevicesAdapter(this, mScannerViewModel.devices)
        adapter.setOnItemClickListener(this)
        recyclerView.adapter = adapter
    }

    override fun onRestart() {
        super.onRestart()
        clear()
    }

    override fun onStop() {
        super.onStop()
        stopScan()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.filter, menu)
        menu.findItem(R.id.filter_uuid).isChecked = mScannerViewModel.isUuidFilterEnabled
        menu.findItem(R.id.filter_nearby).isChecked = mScannerViewModel.isNearbyFilterEnabled
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filter_uuid -> {
                item.isChecked = !item.isChecked
                mScannerViewModel.filterByUuid(item.isChecked)
                return true
            }
            R.id.filter_nearby -> {
                item.isChecked = !item.isChecked
                mScannerViewModel.filterByDistance(item.isChecked)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(device: DiscoveredBluetoothDevice) {
        val controlBlinkIntent = Intent(this, BlinkyActivity::class.java)
        controlBlinkIntent.putExtra(BlinkyActivity.EXTRA_DEVICE, device)
        startActivity(controlBlinkIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_ACCESS_COARSE_LOCATION) {
            mScannerViewModel.refresh()
        }
    }

    @OnClick(R.id.action_enable_location)
    fun onEnableLocationClicked() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    @OnClick(R.id.action_enable_bluetooth)
    fun onEnableBluetoothClicked() {
        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivity(enableIntent)
    }

    @OnClick(R.id.action_grant_location_permission)
    fun onGrantLocationPermissionClicked() {
        Utils.markLocationPermissionRequested(this)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_ACCESS_COARSE_LOCATION
        )
    }

    @OnClick(R.id.action_permission_settings)
    fun onPermissionSettingsClicked() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    /**
     * Start scanning for Bluetooth devices or displays a message based on the scanner state.
     */
    private fun startScan(state: ScannerStateLiveData) {
        // First, check the Location permission. This is required on Marshmallow onwards in order
        // to scan for Bluetooth LE devices.
        if (!Utils.isLocationPermissionsGranted(this)) {
            mNoLocationPermissionView.visibility = View.VISIBLE
            mNoBluetoothView.visibility = View.GONE
            mScanningView.visibility = View.INVISIBLE
            mEmptyView.visibility = View.GONE

            val deniedForever = Utils.isLocationPermissionDeniedForever(this)
            mGrantPermissionButton.visibility = if (deniedForever) View.GONE else View.VISIBLE
            mPermissionSettingsButton.visibility = if (deniedForever) View.VISIBLE else View.GONE
            return
        }
        mNoLocationPermissionView.visibility = View.GONE

        // Bluetooth must be enabled
        if (!state.isBluetoothEnabled) {
            mNoBluetoothView.visibility = View.VISIBLE
            mScanningView.visibility = View.INVISIBLE
            mEmptyView.visibility = View.GONE
            clear()
            return
        }
        mNoBluetoothView.visibility = View.GONE

        // We are now OK to start scanning
        mScannerViewModel.startScan()
        mScanningView.visibility = View.VISIBLE

        if (state.hasRecords()) {
            mEmptyView.visibility = View.GONE
            return
        }
        mEmptyView.visibility = View.VISIBLE

        if (!Utils.isLocationRequired(this) || Utils.isLocationEnabled(this)) {
            mNoLocationView.visibility = View.INVISIBLE
        } else {
            mNoLocationView.visibility = View.VISIBLE
        }
    }

    /**
     * stop scanning for bluetooth devices.
     */
    private fun stopScan() {
        mScannerViewModel.stopScan()
    }

    /**
     * Clears the list of devices, which will notify the observer.
     */
    private fun clear() {
        mScannerViewModel.devices.clear()
        mScannerViewModel.scannerState.clearRecords()
    }

    companion object {
        private const val REQUEST_ACCESS_COARSE_LOCATION = 1022 // random number
    }
}
