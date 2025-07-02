package org.autojs.autojs.pluginclient

import android.text.TextUtils
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.project.ProjectLauncher
import com.stardust.autojs.script.StringScriptSource
import com.stardust.io.Zip
import com.stardust.pio.PFiles
import com.stardust.util.MD5
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.autojs.autojs.Pref
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

    suspend fun handle(data: JsonObject): Boolean {
        val type = data.get("type")?.asString ?: return false
        when (type) {
            "command" -> {
                val command = data.getAsJsonObject("data")
                when (command.getString("command")) {
                    "run" -> {
                        val script = command.getString("script")
                        val name = command.getString("name")
                        val id = command.getString("id")
                        runScript(id, name, script)
                        return true
                    }

                    "stop" -> {
                        val id = command.getString("id")
                        stopScript(id)
                        return true
                    }

                    "save" -> {
                        val script = command.getString("script")
                        val name = command.getString("name")
                        saveScript(name, script)
                        return true
                    }

                    "rerun" -> {
                        val id = command.getString("id")
                        val script = command.getString("script")
                        val name = command.getString("name")
                        stopScript(id)
                        runScript(id, name, script)
                        return true
                    }

                    "stopAll" -> {
                        AutoJs.getInstance().scriptEngineService.stopAllAndToast()
                        return true
                    }

                    else -> {
                        Timber.e("Unknown command: $command")
                    }
                }
            }

            "bytes_command" -> {
                val command = data.getAsJsonObject("data")
                when (command.getString("command")) {
                    "run_project" -> {
                        launchProject(command.get("dir").asString)
                        return true
                    }

                    "save_project" -> {
                        val name = command.getString("name")
                        val dir = command.getString("dir")
                        saveProject(name, dir)
                        return true
                    }

                    else -> {
                        Timber.e("Unknown bytes command: $command")
                    }
                }
            }
        }
        return false
    }

    suspend fun handleBytes(data: JsonObject, bytes: JsonWebSocket2.Bytes): File =
        withContext(Dispatchers.IO) {
            val id = data.getAsJsonObject("data").get("id").asString
            val idMd5 = MD5.md5(id)
            val dir = File(cacheDir, idMd5)
            Zip.unzip(ByteArrayInputStream(bytes.byteString.toByteArray()), dir)
            dir
        }

    fun runScript(viewId: String, name: String?, script: String) {
        val scriptName = if (TextUtils.isEmpty(name)) {
            "[$viewId]"
        } else {
            PFiles.getNameWithoutExtension(name)
        }
        Scripts.run(StringScriptSource("[remote]$scriptName", script))?.let {
            scriptExecutions[viewId] = it
        }
    }

    fun launchProject(dir: String) {
        try {
            ProjectLauncher(dir).launch(AutoJs.getInstance().scriptEngineService)
        } catch (e: Exception) {
            e.printStackTrace()
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


    fun saveScript(name: String?, script: String) {
        var fileName =
            if (TextUtils.isEmpty(name)) "untitled" else PFiles.getNameWithoutExtension(name)
        if (!fileName.endsWith(".js")) fileName += ".js"
        val file = File(Pref.getScriptDirPath(), fileName)
        PFiles.ensureDir(file.path)
        PFiles.write(file, script)
        GlobalAppContext.toast(R.string.text_script_save_successfully)
    }

    suspend fun saveProject(name: String?, dir: String) = withContext(Dispatchers.IO) {
        var projectName =
            if (TextUtils.isEmpty(name)) "untitled" else PFiles.getNameWithoutExtension(name)
        val toDir = File(Pref.getScriptDirPath(), projectName)
        try {
            copyDir(File(dir), toDir)
            withContext(Dispatchers.Main) {
                GlobalAppContext.toast(R.string.text_project_save_success, toDir.path)
            }
        } catch (err: Exception) {
            withContext(Dispatchers.Main) {
                GlobalAppContext.toast(R.string.text_project_save_error, err.message ?: "")
            }
        }
    }

    private fun JsonObject.getString(key: String): String {
        return get(key)?.asString ?: ""
    }

    private fun copyDir(fromDir: File, toDir: File) {
        toDir.mkdirs()
        val files = fromDir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                copyDir(file, File(toDir, file.name))
            } else {
                FileOutputStream(File(toDir, file.name)).use { fos ->
                    PFiles.write(FileInputStream(file), fos, true)
                }
            }
        }
    }
} 