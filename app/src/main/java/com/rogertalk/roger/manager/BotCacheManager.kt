package com.rogertalk.roger.manager

import com.rogertalk.roger.models.json.Bot

object BotCacheManager {

    val CLIENT_CODE_IDS = listOf("voicemail")

    var cachedBotList = emptyList<Bot>()
    var cachedServicesList = emptyList<Bot>()

    val servicesList: List<Bot>
        get() = cachedServicesList.filter(Bot::canDisplay)


    //
    // PUBLIC METHODS
    //

    /**
     * Update local cache and mark bot as connected
     */
    fun markBotAsConnected(botId: String) {
        for (bot in cachedBotList) {
            if (bot.nameId == botId) {
                bot.connected = true
            }
        }
    }

    //
    // PRIVATE METHODS
    //

}
