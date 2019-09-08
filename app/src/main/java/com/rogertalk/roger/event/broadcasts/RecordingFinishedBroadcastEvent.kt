package com.rogertalk.roger.event.broadcasts

import java.io.File

class RecordingFinishedBroadcastEvent(val streamId: Long,
                                      val file: File,
                                      val duration: Long,
                                      val createChunkToken: Boolean,
                                      val persistAudio: Boolean)
