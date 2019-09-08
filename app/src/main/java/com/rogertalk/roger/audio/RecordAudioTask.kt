package com.rogertalk.roger.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Process
import android.os.Process.setThreadPriority
import android.os.SystemClock
import com.rogertalk.roger.audio.RecordAudioTask.RTStatus.*
import com.rogertalk.roger.audio.RecordingEventListener.AudioRecordEvent.*
import com.rogertalk.roger.event.broadcasts.RecordingFinishedBroadcastEvent
import com.rogertalk.roger.event.broadcasts.audio.AudioAmplitudeEvent
import com.rogertalk.roger.manager.PerformanceManager
import com.rogertalk.roger.manager.RealTimeManager
import com.rogertalk.roger.manager.audio.PlaybackStateManager
import com.rogertalk.roger.models.data.VisualizerType
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.network.request.SendAudioChunkRequest
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.constant.AudioConstants
import com.rogertalk.roger.utils.constant.NO_TIME
import com.rogertalk.roger.utils.constant.RogerConstants
import com.rogertalk.roger.utils.constant.RuntimeConstants.Companion.PENDING_UPLOADS_DIR
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.extensions.runOnUiThread
import com.rogertalk.roger.utils.log.*
import java.io.File
import java.util.*

class RecordAudioTask(val stream: Stream, var createChunkTokenAfter: Boolean) {

    companion object {

        fun getSliceTime(rtStatus: RTStatus): Int {
            return when (rtStatus) {
                RecordAudioTask.RTStatus.NO_SLICING -> LONG_AUDIO_PART_DURATION
                RecordAudioTask.RTStatus.INITIAL_SLICING -> INITIAL_AUDIO_PART_DURATION
                RecordAudioTask.RTStatus.RT_SLICING -> RT_AUDIO_PART_DURATION
            }
        }

        // Silence threshold, if recording does not go above this we don't consider that the person talked at all
        private val SILENCE_THRESHOLD = -44
        private val BUFFER_STORE_SIZE = 4 // Number of buffers to store when doing silence recording
        private var storedBufferPointer = 0

        // Chunk partition durations
        private val INITIAL_AUDIO_PART_DURATION = 40000
        private val RT_AUDIO_PART_DURATION = 8000
        private val LONG_AUDIO_PART_DURATION = 1800000 // half an hour
        private val FOUR_HOUR_DURATION = 14400000

        // Amount of seconds to account for the buffer content
        val BUFFER_SECONDS = 1

        private val MAX_AMPLITUDE_FAILURES = 12

        private val CD_QUALITY_SR = 44100
        private val HALF_CD_QUALITY_SR = 22050
        private val WIDEBAND_SR = 16000
        private val QUARTER_CD_SR = 11025
        private val SAMPLING_RATES = intArrayOf(CD_QUALITY_SR, HALF_CD_QUALITY_SR,
                WIDEBAND_SR, QUARTER_CD_SR)
    }

    enum class RTStatus {
        NO_SLICING, // Services, Bots and Groups
        INITIAL_SLICING, // Slicing if the receiver is  NOT yet listening
        RT_SLICING // More frequent slicing, since the other person is listening
    }

    private var listener: RecordingEventListener? = null
    private var baseFilePath: String? = null
    private var sampleRateIndex = 0
    private var isRecording = false
    private var startTimestamp = NO_TIME
    private var shouldStopTimestamp = NO_TIME
    private var minBufferSize = 0

    private var realTimeStatus: RTStatus

    // Number of seconds already recorded and ready to upload
    private var totalSentDuration = 0

    //Since slices vary in size, we must store the accumulation of sizes
    private var accumulatedRecordingTime = 0L

    // Detect failure to recording in stereo
    private var totalAmplitudeFails = 0

    init {
        // Initialize recording state

        // Get the latest accepted sampling rate index
        sampleRateIndex = PrefRepo.samplingRateIndex
        if (sampleRateIndex >= SAMPLING_RATES.size) {
            // Reset sampling index
            logInfo { "Resetting sampling index" }
            sampleRateIndex = 0
            PrefRepo.samplingRateIndex = 0
        }

        baseFilePath = "${UUID.randomUUID()}.${AudioConstants.AUDIO_FILE_EXTENSION}"

        // Initialize part duration
        if (stream.isGroup) {
            realTimeStatus = NO_SLICING
        } else {
            if (createChunkTokenAfter) {
                realTimeStatus = NO_SLICING
            } else {
                if (stream.isService) {
                    logDebug { "Stream is a service" }
                    realTimeStatus = NO_SLICING
                } else {

                    val anyServiceInStream = stream.othersOrEmpty.any { RogerConstants.LOCAL_SERVICE_ACCOUNTS.contains(it.id) }
                    if (anyServiceInStream) {
                        realTimeStatus = NO_SLICING
                    } else {
                        realTimeStatus = INITIAL_SLICING
                    }
                }
            }
        }

        // Reset RealTimeManager status
        RealTimeManager.resetState(stream)
    }


    //
    // PUBLIC METHODS
    //

    fun start() {
        // If already recording, give up
        if (isRecording) {
            return
        }

        // Some vars that we'll need during recording
        isRecording = true
        startTimestamp = SystemClock.elapsedRealtime()
        shouldStopTimestamp = startTimestamp + FOUR_HOUR_DURATION

        val usingAlternateOutput = PlaybackStateManager.usingAlternateOutput

        object : Thread() {
            override fun run() {
                setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

                // Sound source
                val audioSource = MediaRecorder.AudioSource.DEFAULT

                // Audio amplitude handling
                val audioAmplitude = AudioAmplitude()

                // Get an AudioRecord tailored for this device
                val audioRecord = buildAudioRecord(audioSource, sampleRateIndex)
                val sampleRate = SAMPLING_RATES[sampleRateIndex]

                logVerbose { "Min buffer size is $minBufferSize, Sampling rate: $sampleRate" }

                var appSilent = if (usingAlternateOutput) {
                    false
                } else {
                    true
                }

                if (AudioSystem.disableRecordingEffects) {
                    logDebug { "Will disable recording effects" }
                    disableAudioEffectsForced(audioRecord)
                }

                // This is the actual buffer to where we read PCM data
                // SampleRate[Hz] * 16bit * 2 Channels * X sec
                val entireBuffer = ShortArray(sampleRate * (16 / 16) * BUFFER_SECONDS)

                // We use this buffer to hold some audio in order to improve the result after silence detection
                val savedBufferArray = Array(BUFFER_STORE_SIZE) { entireBuffer }

                val savedBufferReadSizes = IntArray(BUFFER_STORE_SIZE)

                // This variable tells us if we filled the saved buffer at least once
                var loopedSavedBufferOnce = false

                // This number will automatically increment for each part, and later be added as a prefix to filename
                var recordingPart = 1

                // Initialize encoder
                val mp3Encoders = ArrayList<MP3Encoder>(10)
                mp3Encoders.add(MP3Encoder(sampleRate, entireBuffer.size, "Roger", audioComment(), AudioSystem.recordInStereo))

                val recordingPath = partFilePath(recordingPart)
                if (!mp3Encoders[recordingPart - 1].prepareFile(recordingPath)) {
                    // Could not generate the file
                    sendMessage(ERROR_CREATE_FILE)
                    isRecording = false
                    return
                }

                try {
                    try {
                        audioRecord.startRecording()
                    } catch (e: IllegalStateException) {
                        sendMessage(ERROR_REC_START)
                        isRecording = false
                        return
                    }

                    try {
                        sendMessage(STARTED)

                        var readSize: Int
                        var amplitude: Double?
                        var readCount = 0
                        val isStereo = AudioSystem.recordInStereo

                        while (isRecording) {
                            if (SystemClock.elapsedRealtime() > shouldStopTimestamp) {
                                logDebug { "Will stop recording next" }
                                isRecording = false
                            }

                            readSize = audioRecord.read(entireBuffer, 0, entireBuffer.size / 16)

                            if (readSize < 0) {
                                sendMessage(ERROR_AUDIO_RECORD)
                                // Try to lower sampling rate for next execution
                                if (sampleRateIndex < SAMPLING_RATES.size) {
                                    logVerbose { "Will lower the sampling rate for next execution." }
                                    PrefRepo.samplingRateIndex = sampleRateIndex + 1
                                }
                                break
                            } else if (readSize != 0) {
                                // If performance mode is down, don't show a recording visualizer
                                readCount++
                                if (readCount > 1) {
                                    // Get audio amplitude and broadcast to the app
                                    amplitude = audioAmplitude.getAmplitude(entireBuffer, readSize)
                                    if (amplitude == Double.POSITIVE_INFINITY || amplitude == Double.NEGATIVE_INFINITY) {
                                        if (isStereo) {
                                            totalAmplitudeFails++
                                            if (totalAmplitudeFails > MAX_AMPLITUDE_FAILURES) {
                                                sendMessage(ERROR_RECORD_STEREO)
                                                isRecording = false

                                                // Switch back to recording in mono the next time
                                                logError { "Recording on stereo failed" }
                                            }
                                        }
                                    } else {
                                        // Silence detection
                                        if (appSilent && amplitude > SILENCE_THRESHOLD) {
                                            logDebug { "!!!!reached audio threshold!!!!" }
                                            appSilent = false

                                            // Update timestamps
                                            startTimestamp = SystemClock.elapsedRealtime()
                                            shouldStopTimestamp = startTimestamp + FOUR_HOUR_DURATION

                                            // Encode any previously stored buffers to MP3 now.
                                            // Iterate trough the 1st part of storage
                                            logDebug { "pointer is at : $storedBufferPointer" }
                                            for (i in (storedBufferPointer - 1) downTo 0) {
                                                mp3Encoders[recordingPart - 1].encode(savedBufferArray[i], savedBufferReadSizes[i])
                                            }
                                            if (loopedSavedBufferOnce) {
                                                for (i in (BUFFER_STORE_SIZE - 1) downTo storedBufferPointer) {
                                                    mp3Encoders[recordingPart - 1].encode(savedBufferArray[i], savedBufferReadSizes[i])
                                                }
                                            }
                                        } else {
                                            // Save this piece of audio for later
                                            savedBufferArray[storedBufferPointer] = entireBuffer.clone()
                                            savedBufferReadSizes[storedBufferPointer] = readSize

                                            storedBufferPointer++
                                            if (storedBufferPointer > BUFFER_STORE_SIZE - 1) {
                                                storedBufferPointer = 0
                                                loopedSavedBufferOnce = true
                                            }
                                        }

                                        totalAmplitudeFails = 0
                                        if (PerformanceManager.performant) {
                                            postEvent(AudioAmplitudeEvent(amplitude, VisualizerType.RECORDING))
                                        }
                                    }
                                    readCount = 0
                                }

                                if (!appSilent) {
                                    // Encode to MP3 on-the-fly
                                    val encResult = mp3Encoders[recordingPart - 1].encode(entireBuffer.clone(), readSize)
                                    if (encResult < 0) {
                                        sendMessage(ERROR_AUDIO_ENCODE)
                                        break
                                    }
                                }

                            }

                            if (!appSilent) {
                                // Update realtime status if necessary
                                if (realTimeStatus == INITIAL_SLICING) {
                                    if (RealTimeManager.isListening) {
                                        realTimeStatus = RT_SLICING
                                        logVerbose { "Switched to Real Time Slicing :)" }
                                    }
                                }

                                // AUDIO - PARTITIONING happens here
                                val slicingTime = getSliceTime(realTimeStatus)
                                val elapsedTime = SystemClock.elapsedRealtime() - startTimestamp
                                val totalRecordingTime = accumulatedRecordingTime + slicingTime

                                if (elapsedTime > (totalRecordingTime)) {
                                    logDebug {
                                        "ElapsedT: $elapsedTime,TotalRecordingT: " +
                                                "$totalRecordingTime, AccumulatedRecordingT: " +
                                                "$accumulatedRecordingTime, " +
                                                "SlicingT: $slicingTime"
                                    }

                                    // Increase accumulated recording time properly
                                    accumulatedRecordingTime = elapsedTime

                                    // Send audio part
                                    sendRecordingPart(mp3Encoders[recordingPart - 1], recordingPart)

                                    // Create next audio part
                                    recordingPart++

                                    // Start a new encoder
                                    mp3Encoders.add(MP3Encoder(sampleRate, entireBuffer.size,
                                            "Roger", audioComment(), isStereo))

                                    if (!mp3Encoders[recordingPart - 1].prepareFile(partFilePath(recordingPart))) {
                                        // Could not generate the file
                                        sendMessage(ERROR_CREATE_FILE)
                                        isRecording = false
                                    }
                                }
                            }

                        }
                    } finally {
                        audioRecord.stop()
                        audioRecord.release()
                    }
                } finally {
                    RogerLame.close()
                    isRecording = false
                }

                // Recording stopped
                sendMessage(STOPPED)

                endRecording(recordingPart)

            }
        }.start()
    }

    fun stop() {
        // Stop recording in a second
        shouldStopTimestamp = SystemClock.elapsedRealtime() + 500
    }

    fun registerCallbackListener(listener: RecordingEventListener) {
        this.listener = listener
    }

    //
    // PRIVATE METHODS
    //

    /**
     * Current audio part file part
     */
    private fun partFilePath(partNum: Int): String {
        return "$partNum$baseFilePath"
    }

    /**
     * End the recording. It is up to AudioService to actually upload the last audio part!
     */
    private fun endRecording(partNum: Int) {
        if (listener != null) {
            var persistAudio = false
            val participant = stream.othersOrEmpty.firstOrNull()
            if (participant != null) {
                if (participant.id == RogerConstants.SHARE_ACCOUNT_ID) {
                    // Persist stuff sent to the share account
                    persistAudio = true
                    createChunkTokenAfter = true
                    logDebug { "using ShareAccount: will persist audio" }
                }
            }

            // This will call AudioService, so the recording state can be properly reset
            val file = File(appController().filesDir, "$PENDING_UPLOADS_DIR/${partFilePath(partNum)}")

            // Total time for this last element
            val duration = SystemClock.elapsedRealtime() - startTimestamp - (totalSentDuration)

            logDebug { "Last element duration was: $duration" }
            postEvent(RecordingFinishedBroadcastEvent(stream.id,
                    file, duration, createChunkTokenAfter, persistAudio))
        }
    }

    private fun audioComment(): String {
        return "Recorded on Android at ${SystemClock.elapsedRealtime()}"
    }

    /**
     * Send the current recording part to server
     */
    private fun sendRecordingPart(mp3Encoder: MP3Encoder, partNum: Int) {
        val closeFileResult = mp3Encoder.closeFile()
        if (closeFileResult < 0) {
            sendMessage(ERROR_AUDIO_ENCODE)
        } else {
            runOnUiThread {
                val file = File(appController().filesDir, "$PENDING_UPLOADS_DIR/${partFilePath(partNum)}")
                val partDuration = mp3Encoder.duration()
                logDebug { "Part duration: $partDuration" }
                totalSentDuration += partDuration
                SendAudioChunkRequest(stream.id, file, partDuration, persist = false).enqueueRequest()
            }
        }
    }


    private fun buildAudioRecord(audioSource: Int, index: Int): AudioRecord {
        val audioRecord: AudioRecord
        if (index >= SAMPLING_RATES.size) {
            logError { "Tried all the possible sampling rates, didn't find that worked!" }
            // Although it didn't work, try to move ahead with a lower one and hope for the best
            minBufferSize = 2400
            PrefRepo.samplingRateIndex = 1
            try {
                audioRecord = AudioRecord(
                        audioSource, CD_QUALITY_SR,
                        getRecordChannels(),
                        AudioFormat.ENCODING_PCM_16BIT, minBufferSize * 2)

            } catch (e: Exception) {
                logError(e) { "Not even the fallback worked. Really give up now." }
                throw Exception("Could not record audio")
            }
            return audioRecord
        }

        val localSamplingRate = SAMPLING_RATES[index]

        minBufferSize = AudioRecord.getMinBufferSize(
                localSamplingRate, getRecordChannels(),
                AudioFormat.ENCODING_PCM_16BIT)
        if (minBufferSize < 0) {
            //sendMessage(RecordingEventListener.AudioEvent.ERROR_GET_MIN_BUFFER_SIZE)

            // Re-test with a lower sampling rate
            return buildAudioRecord(audioSource, index + 1)
        }

        try {
            audioRecord = AudioRecord(
                    audioSource, localSamplingRate,
                    getRecordChannels(),
                    AudioFormat.ENCODING_PCM_16BIT, minBufferSize)

        } catch (e: Exception) {
            logWarn { "Sampling rate failed: $localSamplingRate . Will try lower value" }
            // Re-test with a lower sampling rate
            return buildAudioRecord(audioSource, index + 1)
        }

        // Humm, apparently this sampling rate work, persist it
        PrefRepo.samplingRateIndex = index

        return audioRecord
    }

    private fun getRecordChannels(): Int {
        if (AudioSystem.recordInStereo) {
            return AudioFormat.CHANNEL_IN_STEREO
        }
        return AudioFormat.CHANNEL_IN_MONO
    }

    private fun sendMessage(audioRecordEvent: RecordingEventListener.AudioRecordEvent) {
        if (listener != null) {
            runOnUiThread {
                listener?.recordingEventCallback(audioRecordEvent)
            }
        }
    }

    private fun disableAudioEffectsForced(audioRecord: AudioRecord) {
        try {
            val canceler = AcousticEchoCanceler.create(audioRecord.audioSessionId)
            canceler.enabled = false
            canceler.release()
        } catch (ignored: Exception) {
            logError(ignored)
        }
        try {
            val ns = NoiseSuppressor.create(audioRecord.audioSessionId)
            ns.enabled = false
            ns.release()
        } catch (ignored: Exception) {
            logError(ignored)
        }
    }

}
