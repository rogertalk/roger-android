package com.rogertalk.roger.utils.extensions

import io.realm.Realm

fun getRealm(): Realm {
    return appHelper().getRealm()
}