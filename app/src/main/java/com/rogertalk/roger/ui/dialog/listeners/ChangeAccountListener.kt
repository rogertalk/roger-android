package com.rogertalk.roger.ui.dialog.listeners

import com.rogertalk.roger.models.json.Session

interface ChangeAccountListener {
    fun changeAccount(newSession: Session)
    fun keepCurrentAccount()
}