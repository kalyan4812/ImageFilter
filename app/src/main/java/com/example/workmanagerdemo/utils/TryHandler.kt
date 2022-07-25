package com.example.workmanagerdemo.utils

object TryHandler {

    @Throws(Exception::class)
     fun exceptionalCode(code: () -> Unit): Exception {
        try {
            code.invoke()
        } catch (e: Exception) {
            println(e.localizedMessage)
            return e
        }
        return Exception()
    }

     fun Exception.ifError(code: Exception.() -> Unit) {
        (this.message)?.let {
            code.invoke(this)
        }
    }
}