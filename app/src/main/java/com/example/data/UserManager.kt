package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val memberSince: Long = System.currentTimeMillis(),
    val isLoggedIn: Boolean = true
)

data class AppPreferences(
    val darkTheme: Boolean = true,
    val showThinkingDetails: Boolean = true,
    val enterToSend: Boolean = true,
    val showTimestamps: Boolean = true,
    val markdownRendering: Boolean = true
)

class UserManager private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ai_chat_user_prefs", Context.MODE_PRIVATE)

    private val _userState = MutableStateFlow(loadUser())
    val userState: StateFlow<UserProfile?> = _userState.asStateFlow()

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

    private fun loadUser(): UserProfile? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val name = prefs.getString(KEY_USER_NAME, "User") ?: "User"
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val memberSince = prefs.getLong(KEY_MEMBER_SINCE, System.currentTimeMillis())

        return UserProfile(
            id = id,
            name = name,
            email = email,
            memberSince = memberSince,
            isLoggedIn = true
        )
    }

    private fun loadPreferences(): AppPreferences {
        return AppPreferences(
            darkTheme = prefs.getBoolean(KEY_DARK_THEME, true),
            showThinkingDetails = prefs.getBoolean(KEY_SHOW_THINKING, true),
            enterToSend = prefs.getBoolean(KEY_ENTER_SEND, true),
            showTimestamps = prefs.getBoolean(KEY_SHOW_TIMESTAMPS, true),
            markdownRendering = prefs.getBoolean(KEY_MARKDOWN, true)
        )
    }

    fun login(emailOrUsername: String, password: String, rememberMe: Boolean = true): Boolean {
        // Validation
        if (emailOrUsername.isBlank() || password.length < 4) return false

        val storedEmail = prefs.getString(KEY_STORED_REGISTERED_EMAIL, null)
        val storedPass = prefs.getString(KEY_STORED_REGISTERED_PASS, null)
        val storedName = prefs.getString(KEY_STORED_REGISTERED_NAME, null)
        val storedId = prefs.getString(KEY_STORED_REGISTERED_ID, null)

        val id: String
        val name: String
        val email: String

        if (storedEmail != null && storedPass != null && (storedEmail.equals(emailOrUsername, ignoreCase = true) || storedName.equals(emailOrUsername, ignoreCase = true))) {
            if (storedPass != password) return false
            id = storedId ?: "123456"
            name = storedName ?: "User"
            email = storedEmail
        } else {
            // General login or fallback
            id = storedId ?: "123456"
            name = if (emailOrUsername.contains("@")) emailOrUsername.substringBefore("@") else emailOrUsername
            email = if (emailOrUsername.contains("@")) emailOrUsername else "$emailOrUsername@example.com"
        }

        val profile = UserProfile(
            id = id,
            name = name.replaceFirstChar { it.uppercase() },
            email = email,
            memberSince = prefs.getLong(KEY_MEMBER_SINCE, System.currentTimeMillis()),
            isLoggedIn = true
        )

        if (rememberMe) {
            prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ID, profile.id)
                .putString(KEY_USER_NAME, profile.name)
                .putString(KEY_USER_EMAIL, profile.email)
                .putLong(KEY_MEMBER_SINCE, profile.memberSince)
                .apply()
        }

        _userState.value = profile
        return true
    }

    fun register(name: String, email: String, password: String): Boolean {
        if (name.isBlank() || email.isBlank() || password.length < 4) return false

        val generatedId = "123456" // Default user_id as per specification or random
        val memberSince = System.currentTimeMillis()

        prefs.edit()
            .putString(KEY_STORED_REGISTERED_NAME, name)
            .putString(KEY_STORED_REGISTERED_EMAIL, email)
            .putString(KEY_STORED_REGISTERED_PASS, password)
            .putString(KEY_STORED_REGISTERED_ID, generatedId)
            .putLong(KEY_MEMBER_SINCE, memberSince)
            .apply()

        return login(email, password, rememberMe = true)
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .apply()
        _userState.value = null
    }

    fun updatePreferences(update: (AppPreferences) -> AppPreferences) {
        val current = _preferences.value
        val updated = update(current)
        _preferences.value = updated

        prefs.edit()
            .putBoolean(KEY_DARK_THEME, updated.darkTheme)
            .putBoolean(KEY_SHOW_THINKING, updated.showThinkingDetails)
            .putBoolean(KEY_ENTER_SEND, updated.enterToSend)
            .putBoolean(KEY_SHOW_TIMESTAMPS, updated.showTimestamps)
            .putBoolean(KEY_MARKDOWN, updated.markdownRendering)
            .apply()
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_MEMBER_SINCE = "member_since"

        private const val KEY_STORED_REGISTERED_NAME = "reg_name"
        private const val KEY_STORED_REGISTERED_EMAIL = "reg_email"
        private const val KEY_STORED_REGISTERED_PASS = "reg_pass"
        private const val KEY_STORED_REGISTERED_ID = "reg_id"

        private const val KEY_DARK_THEME = "pref_dark_theme"
        private const val KEY_SHOW_THINKING = "pref_show_thinking"
        private const val KEY_ENTER_SEND = "pref_enter_send"
        private const val KEY_SHOW_TIMESTAMPS = "pref_show_timestamps"
        private const val KEY_MARKDOWN = "pref_markdown"

        @Volatile
        private var INSTANCE: UserManager? = null

        fun getInstance(context: Context): UserManager {
            return INSTANCE ?: synchronized(this) {
                val instance = UserManager(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
