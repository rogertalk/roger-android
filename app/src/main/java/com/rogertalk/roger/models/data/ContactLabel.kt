package com.rogertalk.roger.models.data

import java.io.Serializable

class ContactLabel(val value: String, val label: String) : Serializable {

    override fun toString(): String {
        return "$label:$value"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ContactLabel

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}