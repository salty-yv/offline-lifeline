package com.example.offlinelifeline.core.common

fun interface TimeProvider {
    fun nowMillis(): Long

    companion object {
        val System: TimeProvider = TimeProvider { java.lang.System.currentTimeMillis() }
    }
}
