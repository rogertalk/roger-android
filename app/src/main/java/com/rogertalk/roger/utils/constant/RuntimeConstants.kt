package com.rogertalk.roger.utils.constant

class RuntimeConstants {
    companion object {
        const val DEFAULT_PROFILE_PIC_SIZE: Int = 500
        const val PROFILE_PIC_JPG_QUALITY: Int = 80
        const val MINIMUM_RECORD_PERIOD_MILLIS: Long = 500
        const val LONG_PRESS_DURATION: Long = 750

        // Inner storage directories
        const val PENDING_UPLOADS_DIR = "uploads"
        const val AUDIO_DOWNLOAD_DIR = "chunks"
        const val GROUP_AVATAR_IMAGES_DIR = "gavatars"

        // Network-related
        const val MAX_NETWORK_RETRIES: Int = 20
        const val NETWORK_READ_TIMEOUT_SECS: Long = 60
    }
}