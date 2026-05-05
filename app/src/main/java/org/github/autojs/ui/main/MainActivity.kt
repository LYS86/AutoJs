package org.github.autojs.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.stardust.autojs.compose.theme.AppTheme
import org.autojs.autojs.R

class MainActivity : ComponentActivity() {

    private var lastBackPressedTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressedTime < 1000) {
                    finishAffinity()
                } else {
                    lastBackPressedTime = currentTime
                    Toast.makeText(
                        this@MainActivity,
                        R.string.text_press_again_to_exit,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
        setContent {
            AppTheme {
                MainScreen()
            }
        }
    }
}
