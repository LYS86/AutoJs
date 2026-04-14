package org.autojs.autojs.ui.log

import android.util.Log
import android.util.SparseArray
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stardust.autojs.core.console.ConsoleImpl
import com.stardust.autojs.core.console.ConsoleView
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs

@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    val consoleImpl = AutoJs.getInstance().globalConsole

    Scaffold(modifier = modifier, floatingActionButton = {
        FloatingActionButton(onClick = { consoleImpl.clear() }) {
            Icon(Icons.Filled.Clear, stringResource(R.string.text_clear))
        }
    }) { padding ->
        ConsoleViewWrapper(
            console = consoleImpl, modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun ConsoleViewWrapper(console: ConsoleImpl, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            ConsoleView(ctx).apply {
                val colors = SparseArray<Int>().apply {
                    put(Log.VERBOSE, 0xdfc0c0c0.toInt())
                    put(Log.DEBUG, 0xcc000000.toInt())
                    put(Log.INFO, 0xff64dd17.toInt())
                    put(Log.WARN, 0xff2962ff.toInt())
                    put(Log.ERROR, 0xffd50000.toInt())
                    put(Log.ASSERT, 0xffff534e.toInt())
                }
                setColors(colors)
                setConsole(console)
                findViewById<View>(R.id.input_container).visibility = View.GONE
            }
        }, modifier = modifier
    )
}