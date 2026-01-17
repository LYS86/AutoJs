package org.autojs.autojs.pluginclient

import com.stardust.app.GlobalAppContext
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.project.ProjectLauncher
import com.stardust.autojs.script.StringScriptSource
import com.stardust.io.Zip
import com.stardust.pio.PFiles
import com.stardust.util.MD5
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.autojs.autojs.PrefV2
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.model.script.Scripts
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DevPluginResponseHandler2(private val cacheDir: File) {
    private val scriptExecutions = HashMap<String, ScriptExecution>()

    init {
        if (cacheDir.exists()) {
            if (cacheDir.isDirectory) {
                PFiles.deleteFilesOfDir(cacheDir)
            } else {
                cacheDir.delete()
                cacheDir.mkdirs()
            }
        }
    }

    suspend fun handle(message: ServerMessage, dir: File? = null): Boolean {
        val data = message.data
        when (message.type) {
            "command" -> {
                when (data.command) {
                    "run" -> {
                        runScript(data.id, data.name, data.script)
                        return true
                    }

                    "stop" -> {
                        stopScript(data.id)
                        return true
                    }

                    "save" -> {
                        saveScript(data.name, data.script)
                        return true
                    }

                    "rerun" -> {
                        stopScript(data.id)
                        runScript(data.id, data.name, data.script)
                        return true
                    }

                    "stopAll" -> {
                        AutoJs.getInstance().scriptEngineService.stopAllAndToast()
                        return true
                    }

                    else -> {
                        Timber.e("Unknown command: ${data.command}")
                    }
                }
            }

            "bytes_command" -> {
                if (dir == null) {
                    Timber.e("bytes_command requires dir parameter")
                    return false
                }
                when (data.command) {
                    "run_project" -> {
                        launchProject(dir.path)
                        return true
                    }

                    "save_project" -> {
                        saveProject(data.name, dir.path)
                        return true
                    }

                    else -> {
                        Timber.e("Unknown bytes command: ${data.command}")
                    }
                }
            }

            else -> {
                Timber.e("Unknown message type: ${message.type}")
            }
        }
        return false
    }

    suspend fun handleBytes(data: Data, bytes: JsonWebSocket2.Bytes): File =
        withContext(Dispatchers.IO) {
            val id = data.id
            val idMd5 = MD5.md5(id)
            val dir = File(cacheDir, idMd5)
            Zip.unzip(ByteArrayInputStream(bytes.byteString.toByteArray()), dir)
            dir
        }

    fun runScript(id: String, name: String, script: String) {
        val scriptName = PFiles.getNameWithoutExtension(name.ifEmpty { "[$id]" })
        Scripts.run(StringScriptSource("temp_$scriptName", script))?.let {
            scriptExecutions[id] = it
        }
    }

    fun launchProject(dir: String) {
        try {
            ProjectLauncher(dir).launch(AutoJs.getInstance().scriptEngineService)
        } catch (e: Exception) {
            Timber.e(e, "launch project error: $dir")
            GlobalAppContext.toast(R.string.text_invalid_project)
        }
    }

    fun stopScript(viewId: String) {
        val execution = scriptExecutions[viewId]
        if (execution != null) {
            execution.engine.forceStop()
            scriptExecutions.remove(viewId)
        }
    }


    fun saveScript(name: String, script: String) {
        val fileName = if (name.endsWith(".js")) name else "$name.js"
        File(PrefV2.getScriptDirPath(), fileName).writeText(script)
        GlobalAppContext.toast(R.string.text_script_save_successfully)
    }

    suspend fun saveProject(name: String, dir: String) = withContext(Dispatchers.IO) {
        val projectName = PFiles.getNameWithoutExtension(name)
        val toDir = File(PrefV2.getScriptDirPath(), projectName)
        try {
            File(dir).copyRecursively(toDir, true)
            GlobalAppContext.toast(R.string.text_project_save_success, toDir.path)
        } catch (err: Exception) {
            Timber.e(err, "save project error: $dir")
            GlobalAppContext.toast(R.string.text_project_save_error, err.message ?: "")
        }
    }
}