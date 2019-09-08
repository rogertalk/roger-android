package com.rogertalk.roger.models.json

import java.io.Serializable

class StreamsResponse(val cursor: String?,
                      val data: List<Stream>) : Serializable