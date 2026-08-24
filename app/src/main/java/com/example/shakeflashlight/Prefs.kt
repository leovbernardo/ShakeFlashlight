package com.example.shakeflashlight

import android.content.Context
import android.content.SharedPreferences

/**
 * Small wrapper around SharedPreferences for the settings this app needs.
 * Sensitivity is stored as a 0-9 seek bar position and converted to an
 * acceleration threshold (m/s^2 above gravity) that the shake detector uses.
 */
object Prefs {
    private const val PREFS_NAME = "shake_flashlight_prefs"
    private const val KEY_SENSITIVITY = "sensitivity_seek"
    private const val KEY_SERVICE_ENABLED = "service_enabled"

    const val MIN_THRESHOLD = 10f // most sensitive (easiest to trigger)
    const val MAX_THRESHOLD = 30f // least sensitive (hardest to trigger, fewer accidents)
    const val DEFAULT_SEEK = 4
    const val MAX_SEEK = 9

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSensitivitySeek(context: Context): Int =
        prefs(context).getInt(KEY_SENSITIVITY, DEFAULT_SEEK)

    fun setSensitivitySeek(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_SENSITIVITY, value).apply()
    }

    /** Converts the 0-9 seek position into an acceleration threshold. */
    fun seekToThreshold(seek: Int): Float {
        val fraction = seek.toFloat() / MAX_SEEK.toFloat()
        return MAX_THRESHOLD - (MAX_THRESHOLD - MIN_THRESHOLD) * fraction
    }

    fun getThreshold(context: Context): Float =
        seekToThreshold(getSensitivitySeek(context))

    fun isServiceEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SERVICE_ENABLED, false)

    fun setServiceEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }

    fun registerListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }

    const val KEY_SENSITIVITY_PUBLIC = KEY_SENSITIVITY
}
