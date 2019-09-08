package com.rogertalk.roger.models.data

enum class AccountStatus(val text: String) {
    ACTIVE("active"),
    INVITED("invited"),
    BOT("bot"),
    EMPLOYEE("employee")
}