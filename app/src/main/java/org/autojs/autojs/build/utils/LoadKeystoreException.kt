package org.autojs.autojs.build.utils

import java.io.IOException

/**
 * Thrown by JKS.engineLoad() for errors that occur after determining the keystore is actually a JKS keystore.
 *
 * Source: [AutoJs6](https://github.com/SuperMonster003/AutoJs6)
 */
class LoadKeystoreException : IOException {
    constructor()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}
