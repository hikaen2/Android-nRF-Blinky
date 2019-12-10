package no.nordicsemi.android.blinky.profile

import no.nordicsemi.android.ble.BleManagerCallbacks
import no.nordicsemi.android.blinky.profile.callback.BlinkyButtonCallback
import no.nordicsemi.android.blinky.profile.callback.BlinkyLedCallback

interface BlinkyManagerCallbacks : BleManagerCallbacks, BlinkyButtonCallback, BlinkyLedCallback
