package com.rogertalk.roger.utils.constant

class RogerConstants {
    companion object {
        const val BASE_WEBPAGE_URL = "https://rogertalk.com"
        const val LEGAL_WEBPAGE_URL = "$BASE_WEBPAGE_URL/legal"
        const val HELP_WEBPAGE_URL = "$BASE_WEBPAGE_URL/help"
        const val SUPPORT_EMAIL = "hello@rogertalk.com"
        const val DEFAULT_PACKAGE_NAME = "com.rogertalk.roger"
        const val PLAY_STORE_LINK = "market://details?id=$DEFAULT_PACKAGE_NAME"

        // Number of streams per stream 'page'
        const val FIXED_STREAM_PAGE_SIZE = 10


        const val ALEXA_ACCOUNT_ID = 61840001L
        const val SHARE_ACCOUNT_ID = 355150003L
        const val ROGER_ACCOUNT_ID = 512180002L
        const val CHEWBACCA_ACCOUNT_ID = 23360009L
        const val VOICEMAIL_ACCOUNT_ID = 348520002L
        const val GREETING_ACCOUNT_ID = 24300006L
        const val DIRECT_SHARE_ACCOUNT_ID = 796910040L

        /**
         * List with known service accounts
         */
        val LOCAL_SERVICE_ACCOUNTS = arrayListOf(
                ALEXA_ACCOUNT_ID,
                SHARE_ACCOUNT_ID,
                ROGER_ACCOUNT_ID,
                CHEWBACCA_ACCOUNT_ID,
                VOICEMAIL_ACCOUNT_ID,
                GREETING_ACCOUNT_ID,
                DIRECT_SHARE_ACCOUNT_ID)
    }
}
