package com.aiLocal.ialocalnetwork

import android.content.Context

class ConfigManager(context: Context) {
    private val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)

    fun saveIp(ip: String) {
        prefs.edit().putString("ollama_ip", ip).apply()
    }

    fun getIp(): String {
        // "10.0.2.2" es el default para acceder al PC desde el emulador de Android.
        // Si usas un dispositivo real, pon la IP local de tu PC (ej: 192.168.1.XX).
        return prefs.getString("ollama_ip", "10.0.2.2") ?: "10.0.2.2"
    }
}