package com.example.base.shared.logging

interface AppLogger {
    fun debug(message: String, metadata: Map<String, Any?> = emptyMap())
    fun info(message: String, metadata: Map<String, Any?> = emptyMap())
    fun warning(message: String, throwable: Throwable? = null)
    fun error(message: String, throwable: Throwable? = null)
}

object NoOpLogger : AppLogger {
    override fun debug(message: String, metadata: Map<String, Any?>) = Unit
    override fun info(message: String, metadata: Map<String, Any?>) = Unit
    override fun warning(message: String, throwable: Throwable?) = Unit
    override fun error(message: String, throwable: Throwable?) = Unit
}

class ConsoleLogger : AppLogger {
    override fun debug(message: String, metadata: Map<String, Any?>) = println("DEBUG: $message $metadata")
    override fun info(message: String, metadata: Map<String, Any?>) = println("INFO: $message $metadata")
    override fun warning(message: String, throwable: Throwable?) = println("WARN: $message ${throwable?.message.orEmpty()}")
    override fun error(message: String, throwable: Throwable?) = println("ERROR: $message ${throwable?.message.orEmpty()}")
}