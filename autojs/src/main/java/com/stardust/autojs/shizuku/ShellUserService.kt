package com.stardust.autojs.shizuku

import java.io.BufferedReader
import kotlin.system.exitProcess

class ShellUserService : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String): Result {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = proc.inputStream.bufferedReader().use(BufferedReader::readText)
            val errorText = proc.errorStream.bufferedReader().use(BufferedReader::readText)
            val exitCode = proc.waitFor()
            Result(exitCode, errorText.trimEnd().takeIf { it.isNotEmpty() } ?: "", output.trimEnd())
        } catch (e: Exception) {
            Result(-1, e.message ?: "", "")
        }
    }
}
