package com.rogertalk.roger.helper.audio


interface AudioFocusListener {

    fun gainedAudioFocus()

    fun lostAudioFocus()
}