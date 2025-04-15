package org.autojs.autojs.ui.splash

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import org.autojs.autojs.R
import org.autojs.autojs.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    private val initTimeout = 800L
    private var alreadyEnterNextActivity = false
    private var paused = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {

        setTheme(R.style.Theme_AppCompat_Light_NoActionBar)
        super.onCreate(savedInstanceState)
        window.apply {
            decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        setupUI()
        handler.postDelayed(::enterNextActivity, initTimeout)
    }

    private fun setupUI() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val screenWidth = resources.displayMetrics.widthPixels
        val imageSize = (screenWidth * 0.7).toInt()

        val logoImage = ImageView(this).apply {
            setImageResource(R.drawable.autojs_material)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(imageSize, imageSize).apply {
                gravity = Gravity.CENTER
            }
        }

        val textLogo = ImageView(this).apply {
            setImageResource(R.drawable.autojs)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                imageSize,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                topMargin = dpToPx(32)
            }
        }

        container.addView(logoImage)
        container.addView(textLogo)
        setContentView(container)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onResume() {
        super.onResume()
        if (paused) {
            paused = false
            enterNextActivity()
        }
    }

    private fun enterNextActivity() {
        if (alreadyEnterNextActivity || paused) return

        alreadyEnterNextActivity = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}