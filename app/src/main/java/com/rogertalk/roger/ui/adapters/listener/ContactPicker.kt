package com.rogertalk.roger.ui.adapters.listener

import com.rogertalk.roger.models.data.DeviceContactInfo


interface ContactPicker {

    fun selectionBegun()
    fun selectionCleared()
    fun pressedRequestPermission()

    fun selectedContact(contact: DeviceContactInfo)
    fun unSelectedContact(contact: DeviceContactInfo)
}