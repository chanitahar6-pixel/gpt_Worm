package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener

class AiRepository(
    private val apiService: AiApiService = AiApiClient.service
) {

    suspend fun sendMessage(userId: String, message: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("message", message)
                if (userId.isNotBlank()) {
                    put("session_id", userId)
                }
            }.toString()

            val requestBody = AiApiClient.createJsonRequestBody(jsonPayload)
            val response = apiService.sendChatMessage(requestBody)

            if (response.isSuccessful) {
                val rawBody = response.body()?.string() ?: ""
                val parsed = extractAnswerText(rawBody)
                Result.success(parsed)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                val extractedErr = extractAnswerText(errBody).ifEmpty { "Error ${response.code()}" }
                Result.failure(Exception(extractedErr))
            }
        } catch (e: Exception) {
            // Attempt fallback to GET /ask/text
            try {
                val fallback = apiService.askText(question = message, sessionId = userId.ifBlank { null })
                if (fallback.isSuccessful) {
                    val rawFallback = fallback.body()?.string() ?: ""
                    val parsedFallback = extractAnswerText(rawFallback)
                    if (parsedFallback.isNotEmpty()) {
                        return@withContext Result.success(parsedFallback)
                    }
                }
            } catch (_: Exception) {
                // Ignore fallback error and report main error
            }

            Result.failure(Exception("AI service is temporarily unavailable: ${e.localizedMessage ?: "Connection error"}"))
        }
    }

    suspend fun createSession(userId: String): Result<String> = withContext(Dispatchers.IO) {
        resetMemory(userId)
    }

    suspend fun resetMemory(userId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("session_id", userId)
            }.toString()
            val requestBody = AiApiClient.createJsonRequestBody(jsonPayload)
            val response = apiService.clearSession(requestBody)
            if (response.isSuccessful) {
                val raw = response.body()?.string() ?: "Session cleared"
                Result.success(extractAnswerText(raw))
            } else {
                Result.failure(Exception("Failed to reset memory: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSession(userId: String): Result<String> = withContext(Dispatchers.IO) {
        resetMemory(userId)
    }

    suspend fun checkHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getHealth()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.success(false)
        }
    }

    suspend fun getVersion(): Result<String> = withContext(Dispatchers.IO) {
        Result.success("1.0.0")
    }

    suspend fun getApiInfo(): Result<String> = withContext(Dispatchers.IO) {
        Result.success("WormGPT API v1.0")
    }

    private fun extractAnswerText(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""

        var extracted = trimmed

        // Check if JSON response
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                val jsonTokener = JSONTokener(trimmed)
                val nextValue = jsonTokener.nextValue()
                if (nextValue is JSONObject) {
                    val candidateKeys = listOf(
                        "response", "reply", "message", "answer",
                        "content", "text", "result", "output", "detail"
                    )
                    for (key in candidateKeys) {
                        if (nextValue.has(key)) {
                            val candidate = nextValue.optString(key)
                            if (candidate.isNotEmpty()) {
                                extracted = candidate
                                break
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Keep raw
            }
        }

        // If the extracted text has an inner embedded JSON, e.g.
        // "❌ خطأ 429: {"error":"Free limit reached","message":"You have used all 10 free chats today. Create a free account to continue."}"
        val innerJsonStart = extracted.indexOf('{')
        val innerJsonEnd = extracted.lastIndexOf('}')
        if (innerJsonStart != -1 && innerJsonEnd > innerJsonStart) {
            try {
                val innerJsonStr = extracted.substring(innerJsonStart, innerJsonEnd + 1)
                val innerObj = JSONObject(innerJsonStr)
                val innerMsg = innerObj.optString("message").ifEmpty { innerObj.optString("error") }
                if (innerMsg.isNotEmpty()) {
                    val prefix = extracted.substring(0, innerJsonStart).trim()
                    return if (prefix.isNotEmpty()) "$prefix $innerMsg" else innerMsg
                }
            } catch (_: Exception) {
                // Keep extracted
            }
        }

        if (extracted.startsWith("\"") && extracted.endsWith("\"") && extracted.length >= 2) {
            return extracted.substring(1, extracted.length - 1).replace("\\n", "\n").replace("\\\"", "\"")
        }

        return extracted
    }
}
