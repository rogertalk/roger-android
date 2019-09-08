package com.rogertalk.roger.utils.extensions

import org.json.JSONObject

fun JSONObject.possibleString(name: String): String? {
    try {
        return getString(name)
    } catch(e: Exception) {
    }
    return null
}