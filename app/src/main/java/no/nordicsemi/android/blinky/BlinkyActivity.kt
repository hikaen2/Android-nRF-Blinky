package no.nordicsemi.android.blinky

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice
import no.nordicsemi.android.blinky.viewmodels.BlinkyViewModel

class BlinkyActivity : AppCompatActivity() {

    private lateinit var mViewModel: BlinkyViewModel

    @BindView(R.id.led_switch)
    internal lateinit var mLed: Switch
    @BindView(R.id.button_state)
    internal lateinit var mButtonState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blinky)
        ButterKnife.bind(this)

        val device = intent.getParcelableExtra<DiscoveredBluetoothDevice>(EXTRA_DEVICE)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = device!!.name
        supportActionBar!!.subtitle = device.address
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Configure the view model
        mViewModel = ViewModelProviders.of(this).get(BlinkyViewModel::class.java)
        mViewModel.connect(device)

        // Set up views
        val ledState = findViewById<TextView>(R.id.led_state)
        val progressContainer = findViewById<LinearLayout>(R.id.progress_container)
        val connectionState = findViewById<TextView>(R.id.connection_state)
        val content = findViewById<View>(R.id.device_container)
        val notSupported = findViewById<View>(R.id.not_supported)

        mLed.setOnCheckedChangeListener { _, isChecked -> mViewModel.toggleLED(isChecked) }
        mViewModel.isDeviceReady.observe(this, Observer {
            progressContainer.visibility = View.GONE
            content.visibility = View.VISIBLE
        })
        mViewModel.connectionState.observe(this, Observer { text ->
            if (text != null) {
                progressContainer.visibility = View.VISIBLE
                notSupported.visibility = View.GONE
                connectionState.text = text
            }
        })
        mViewModel.isConnected.observe(this, Observer<Boolean> { this.onConnectionStateChanged(it) })
        mViewModel.isSupported.observe(this, Observer { supported ->
            if (!supported) {
                progressContainer.visibility = View.GONE
                notSupported.visibility = View.VISIBLE
            }
        })
        mViewModel.ledState.observe(this, Observer { isOn ->
            ledState.setText(if (isOn) R.string.turn_on else R.string.turn_off)
            mLed.isChecked = isOn!!
        })
        mViewModel.buttonState.observe(this,
            Observer { pressed ->
                mButtonState.setText(
                    if (pressed)
                        R.string.button_pressed
                    else
                        R.string.button_released
                )
            })
    }

    @OnClick(R.id.action_clear_cache)
    fun onTryAgainClicked() {
        mViewModel.reconnect()
    }

    private fun onConnectionStateChanged(connected: Boolean) {
        mLed.isEnabled = connected
        if (!connected) {
            mLed.isChecked = false
            mButtonState.setText(R.string.button_unknown)
        }
    }

    companion object {
        const val EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE"
    }
}
