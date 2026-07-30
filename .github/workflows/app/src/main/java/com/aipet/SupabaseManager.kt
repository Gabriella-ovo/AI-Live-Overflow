package com.aipet

import kotlinx.coroutines.*
import kotlinx.coroutines.android.awaitFrame
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class SupabaseManager(
    private val onMessage: (String) -> Unit
) {
    private var lastMessageId: String? = null
    private var job: Job? = null
    private val supabaseUrl = "https://ldcedptzszfafviaudzp.supabase.co"
    private val supabaseKey = "sbp_f9688b96a2ce68b161b961c31d234e9bcbf36d8b"

    fun startPolling(scope: CoroutineScope) {
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    pollMessages()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000)
            }
        }
    }

    fun stopPolling() {
        job?.cancel()
    }

    private fun pollMessages() {
        val url = URL("$supabaseUrl/rest/v1/clawd_state?select=*&order=created_at.desc&limit=1")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("apikey", supabaseKey)
        conn.setRequestProperty("Authorization", "Bearer $supabaseKey")
        conn.setRequestProperty("Content-Type", "application/json")

        val response = BufferedReader(InputStreamReader(conn.inputStream)).readText()
        if (response.startsWith("[")) {
            val jsonStr = response.substring(1, response.length - 1)
            val json = JSONObject(jsonStr)
            val currentId = json.optString("id", "")
            val message = json.optString("message", "")

            if (lastMessageId == null) {
                lastMessageId = currentId
            } else if (currentId != lastMessageId && message.isNotEmpty()) {
                lastMessageId = currentId
                onMessage(message)
            }
        }
        conn.disconnect()
    }
}
