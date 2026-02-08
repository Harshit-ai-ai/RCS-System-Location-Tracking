package com.family.safetysms.contacts

import android.content.Context

object TrustedContactsManager {

    private const val PREF = "trusted_contacts"
    private const val KEY = "numbers"

    fun addNumber(context: Context, number: String) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY, mutableSetOf())!!.toMutableSet()

        set.add(normalize(number))
        prefs.edit().putStringSet(KEY, set).apply()
    }

    fun isTrusted(context: Context, number: String): Boolean {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY, emptySet()) ?: return false

        return set.contains(normalize(number))
    }

    fun getAll(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY, emptySet()) ?: emptySet()
    }

    private fun normalize(num: String): String =
        num.replace("\\s".toRegex(), "")
            .replace("+91", "")
            .takeLast(10)
}