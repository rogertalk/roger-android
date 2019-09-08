package com.rogertalk.roger.utils.extensions

import java.util.regex.Pattern

private val initialRegex = """\b[^\W\d_]""".toRegex()

val String.initial: String
    get() {
        val initials = initialRegex.findAll(this).map { it.value[0].toString() }
        return when {
            initials.none() -> "#"
            else -> initials.first()
        }
    }

fun String.isNumeric(): Boolean {
    val regex = Pattern.compile("[0-9]+")
    return this.matches(regex.toRegex())
}