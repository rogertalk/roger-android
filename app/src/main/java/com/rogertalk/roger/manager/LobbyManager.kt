package com.rogertalk.roger.manager

import com.rogertalk.roger.models.json.Stream

/**
 * Controls the data and logic for the lobby, that allows it share memory and work
 * independent of network as much as possible.
 */
object LobbyManager {

    var stream: Stream? = null

    //
    // PUBLIC METHODS
    //

    fun clearLobbyState() {
        stream = null
    }

    //
    // PRIVATE METHODS
    //

}
