package com.topjohnwu.magisk.navigation

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object FlashPayloadStore {
    private const val PREFIX = "payload:"
    private val urls = ConcurrentHashMap<String, List<String>>()

    fun putUrls(values: List<String>): String {
        val id = UUID.randomUUID().toString()
        urls[id] = values
        return PREFIX + id
    }

    fun takeUrls(token: String): List<String>? {
        if (!token.startsWith(PREFIX)) return null
        return urls.remove(token.removePrefix(PREFIX))
    }
}
