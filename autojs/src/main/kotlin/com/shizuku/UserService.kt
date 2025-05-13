package com.shizuku

import com.stardust.autojs.core.util.ProcessShell
import kotlin.system.exitProcess

/**
 * 代码实现参考了以下博客内容，感谢作者的分享：
 * [Shizuku开发](https://blog.xxin.xyz/2024/04/28/Shizuku%E5%BC%80%E5%8F%91/)
 */
class UserService : IUserService.Stub() {
    override fun destroy() {
        exitProcess(0)
    }

    override fun exit() {
        destroy()
    }

    override fun exec(command: String): String {
        return ProcessShell.execCommand(command, false).toJson()
    }
}