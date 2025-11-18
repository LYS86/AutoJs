package org.autojs.autojs.ui.log

import android.os.Bundle
import com.stardust.autojs.core.console.ConsoleImpl
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.databinding.ActivityLogBinding
import org.autojs.autojs.ui.BaseActivityV2

class LogActivity : BaseActivityV2() {

    private lateinit var binding: ActivityLogBinding
    private val consoleImpl: ConsoleImpl by lazy {
        AutoJs.getInstance().globalConsole
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
    }

    private fun setupViews() {
        setToolbarAsBack(getString(R.string.text_log))
        initConsole()
        setupFab()
    }

    private fun initConsole() {
        binding.console.setConsole(consoleImpl)
        binding.console.hideInputContainer()
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            consoleImpl.clear()
        }
    }
}