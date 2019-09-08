package com.rogertalk.roger.audio

/***
 * Callback for the various events that can happen during a recording
 */
interface RecordingEventListener {

    enum class AudioRecordEvent {
        STARTED, STOPPED, ERROR_GET_MIN_BUFFER_SIZE, ERROR_CREATE_FILE, ERROR_REC_START,
        ERROR_AUDIO_RECORD, ERROR_AUDIO_ENCODE, ERROR_WRITE_FILE, ERROR_CLOSE_FILE, ERROR_RECORD_STEREO
    }

    fun recordingEventCallback(recordEvent: AudioRecordEvent)
}