package no.nordicsemi.android.blinky

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler

class SplashScreenActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler().postDelayed({
            val intent = Intent(this, ScannerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            finish()
        }, DURATION.toLong())
    }

    override fun onBackPressed() {
        // We don't want the splash screen to be interrupted
    }

    companion object {
        private const val DURATION = 1000
    }
}
