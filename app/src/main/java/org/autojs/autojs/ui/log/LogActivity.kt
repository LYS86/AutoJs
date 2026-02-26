package org.autojs.autojs.ui.log

import android.os.Bundle
import android.view.View
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.databinding.ActivityLogBinding
import org.autojs.autojs.ui.BaseActivity

class LogActivity : BaseActivity() {

    private lateinit var binding: ActivityLogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyDayNightMode()
        setupViews()
    }

    private fun setupViews() {
        setToolbarAsBack(getString(R.string.text_log))
        val consoleImpl = AutoJs.getInstance().globalConsole
        binding.console.setConsole(consoleImpl)
        binding.console.findViewById<View>(R.id.input_container).visibility = View.GONE
        binding.fab.setOnClickListener {
            consoleImpl.clear()
        }
    }
}
