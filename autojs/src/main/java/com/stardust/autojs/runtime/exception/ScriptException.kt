package com.stardust.autojs.runtime.exception

open class ScriptException : RuntimeException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

    constructor() : super()

    constructor(cause: Throwable) : super(cause)
}
