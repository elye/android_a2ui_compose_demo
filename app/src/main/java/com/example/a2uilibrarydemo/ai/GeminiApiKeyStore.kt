package com.example.a2uilibrarydemo.ai

import android.content.Context
import android.content.SharedPreferences

/**
 * Minimal persistence for the user-supplied Gemini API key, so the "AI Prompt" demo screen can
 * remember it across app restarts without requiring the user to re-enter it every time.
 *
 * This is a plain [SharedPreferences] file (private mode) — adequate for this tutorial demo app,
 * but a production app handling a real user's API key should prefer
 * `androidx.security:security-crypto`'s `EncryptedSharedPreferences` instead.
 */
object GeminiApiKeyStore {
    private const val PREFS_NAME = "gemini_api_key_store"
    private const val KEY_API_KEY = "api_key"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiKey(context: Context): String =
        prefs(context).getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(context: Context, apiKey: String) {
        prefs(context).edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }
}
