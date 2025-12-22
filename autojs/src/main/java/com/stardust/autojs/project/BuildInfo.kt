package com.stardust.autojs.project

import com.google.gson.annotations.SerializedName
import java.util.zip.CRC32

data class BuildInfo(
    @SerializedName("build_time")
    var buildTime: Long = 0,
    @SerializedName("build_id")
    var buildId: String = "",
    @SerializedName("build_number")
    var buildNumber: Long = 0
) {
    companion object {
        @JvmStatic
        fun generate(buildNumber: Long): BuildInfo {
            val time = System.currentTimeMillis()
            val id = generateBuildId(buildNumber, time)
            return BuildInfo(time, id, buildNumber)
        }

        private fun generateBuildId(buildNumber: Long, buildTime: Long): String {
            val crc32 = CRC32()
            crc32.update("$buildNumber$buildTime".toByteArray())
            return "%08X-%d".format(crc32.value, buildNumber)
        }
    }
}