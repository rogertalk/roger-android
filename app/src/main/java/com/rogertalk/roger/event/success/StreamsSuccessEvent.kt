package com.rogertalk.roger.event.success

import com.rogertalk.roger.models.json.Stream

data class StreamsSuccessEvent(val streams: List<Stream>)
