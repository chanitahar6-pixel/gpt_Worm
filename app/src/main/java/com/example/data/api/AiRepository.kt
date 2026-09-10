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
            val response = apiService.sendChatMessage(ChatApiRequest(user_id = userId, message = message))
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
            // As per requirement: "وفي حالة توقف API: اعرض للمستخدم: 'AI service is temporarily unavailable'"
            Result.failure(Exception("AI service is temporarily unavailable: ${e.localizedMessage ?: "Connection error"}"))
        }
    }

    suspend fun createSession(userId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createSession(UserIdRequest(user_id = userId))
            if (response.isSuccessful) {
                val raw = response.body()?.string() ?: "Session initialized"
                Result.success(extractAnswerText(raw))
            } else {
                Result.failure(Exception("Failed to create session: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetMemory(userId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.resetMemory(UserIdRequest(user_id = userId))
            if (response.isSuccessful) {
                val raw = response.body()?.string() ?: "Memory reset successfully"
                Result.success(extractAnswerText(raw))
            } else {
                Result.failure(Exception("Failed to reset memory: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSession(userId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteSession(userId)
            if (response.isSuccessful) {
                val raw = response.body()?.string() ?: "Session deleted"
                Result.success(extractAnswerText(raw))
            } else {
                Result.failure(Exception("Failed to delete session: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        try {
            val response = apiService.getVersion()
            if (response.isSuccessful) {
                Result.success(response.body()?.string() ?: "1.0.0")
            } else {
                Result.failure(Exception("Status ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApiInfo(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getApiInfo()
            if (response.isSuccessful) {
                Result.success(response.body()?.string() ?: "AI Chat API v1")
            } else {
                Result.failure(Exception("Status ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractAnswerText(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""

        // If it starts with JSON syntax, try parsing
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
                            if (candidate.isNotEmpty()) return candidate
                        }
                    }
                }
            } catch (_: Exception) {
                // Return trimmed raw string
            }
        }

        // If JSON has escaped quotes or plain string
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
            return trimmed.substring(1, trimmed.length - 1).replace("\\n", "\n").replace("\\\"", "\"")
        }

        return trimmed
    }
}
