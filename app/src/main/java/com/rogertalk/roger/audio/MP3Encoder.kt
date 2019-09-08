package com.rogertalk.roger.audio

import android.os.SystemClock
import com.rogertalk.roger.utils.constant.RuntimeConstants.Companion.PENDING_UPLOADS_DIR
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.log.logError
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MP3Encoder(sampleRate: Int, bufferSize: Int, ID3title: String, ID3comment: String, val isStereo: Boolean) {

    companion object {

        // Bitrate of the output
        private val DEFAULT_OUT_BITRATE = 256

        init {
            System.loadLibrary("mp3lame")
        }
    }

    // Resulting file
    var outputStream: FileOutputStream? = null

    private val bufferLeft: ShortArray
    private val bufferRight: ShortArray

    // Buffer for the MP3 encoded result
    private val mp3buffer: ByteArray

    private var index = 0

    private val startTimestamp: Long
    private var endTimestamp = 0L

    init {
        val numChannels = if (isStereo) {
            2
        } else {
            1
        }
        RogerLame.init(sampleRate, numChannels, 44100, DEFAULT_OUT_BITRATE, 2, ID3title, ID3comment)

        if (isStereo) {
            // SampleRate[Hz] * 16bit * 1 Channel * X seconds
            bufferLeft = ShortArray(sampleRate * (16 / 8) * RecordAudioTask.BUFFER_SECONDS)
            bufferRight = ShortArray(sampleRate * (16 / 8) * RecordAudioTask.BUFFER_SECONDS)
        } else {
            // Don't occupy unnecessary memory if not recording in stereo
            bufferLeft = ShortArray(1)
            bufferRight = ShortArray(1)
        }
        mp3buffer = ByteArray((7200 + bufferSize.toDouble() * 1.25).toInt())

        startTimestamp = SystemClock.elapsedRealtime()
    }

    /**
     * @return False if failed to prepare file
     */
    fun prepareFile(path: String): Boolean {
        try {
            val audioFileDir = File(appController().filesDir, PENDING_UPLOADS_DIR)
            if (!audioFileDir.exists()) {
                audioFileDir.mkdir()
            }

            val file = File(appController().filesDir, "$PENDING_UPLOADS_DIR/$path")
            outputStream = FileOutputStream(file)
        } catch (e: FileNotFoundException) {
            logError { "Initial file not created" }
            return false
        }
        return true
    }

    /**
     * Duration of recording in milliseconds.
     * Only call after closing the file!
     */
    fun duration(): Int {
        return (endTimestamp - startTimestamp).toInt()
    }

    fun encode(entireBuffer: ShortArray, readSize: Int): Int {
        val encodingResult: Int
        if (isStereo) {
            // Separate stereo channels
            // LEFT: 0..2  4..6
            // RIGHT: 1..3  5..7
            index = 0
            while (index < readSize / 2) {
                bufferLeft[index] = entireBuffer[2 * index]
                bufferLeft[index + 1] = entireBuffer[2 * index + 2]
                bufferRight[index] = entireBuffer[2 * index + 1]
                bufferRight[index + 1] = entireBuffer[2 * index + 3]
                index += 2
            }

            encodingResult = RogerLame.encode(bufferLeft, bufferRight, readSize / 2, mp3buffer)
        } else {
            encodingResult = RogerLame.encode(entireBuffer, entireBuffer, readSize, mp3buffer)
        }

        // Save to file
        if (encodingResult > 0) {
            try {
                outputStream?.write(mp3buffer, 0, encodingResult)
            } catch (e: IOException) {
                logError { "Failed to write to file!" }
            }
        }

        return encodingResult
    }

    fun closeFile(): Int {
        endTimestamp = SystemClock.elapsedRealtime()
        val flushResult = RogerLame.flush(mp3buffer)
        if (flushResult != 0) {
            try {
                outputStream?.write(mp3buffer, 0, flushResult)
            } catch (e: IOException) {
                logError(e) { "Error writing file!" }
            } catch (e: ArrayIndexOutOfBoundsException) {
                logError(e) { "Error writing file!" }
            }
        }

        try {
            outputStream?.close()
        } catch (e: IOException) {
            logError { "Error closing file" }
        }

        return flushResult
    }
}