package com.rogertalk.roger.models.data

enum class StreamStatus(val statusText: String) {
    VIEWING_ATTACHMENT("viewing-attachment"),
    IDLE("idle"),
    LISTENING("listening"),
    TALKING("talking"),
}