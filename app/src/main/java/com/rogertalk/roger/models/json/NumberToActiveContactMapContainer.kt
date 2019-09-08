package com.rogertalk.roger.models.json

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Maps contact number with active contact info.
 */
class NumberToActiveContactMapContainer(@SerializedName("map") val activeContactsMap: Map<String, ActiveContact>) : Serializable