package com.aiLocal.ialocalnetwork

import android.content.Context

class ConfigManager(context: Context) {
    private val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)

    fun saveIp(ip: String) {
        prefs.edit().putString("ollama_ip", ip).apply()
    }

    fun getIp(): String {
        // "10.0.2.2" es el default para el emulador, pero cámbialo a tu IP real si quieres
        return prefs.getString("ollama_ip", "192.168.1.100") ?: "192.168.1.100"
    }
}