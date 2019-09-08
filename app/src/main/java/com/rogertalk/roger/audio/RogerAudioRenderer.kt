package com.rogertalk.roger.audio

import android.media.MediaCodec
import android.os.Build
import android.os.Handler
import com.google.android.exoplayer.*
import com.google.android.exoplayer.audio.AudioCapabilities
import com.google.android.exoplayer.drm.DrmSessionManager
import com.google.android.exoplayer.drm.ExoMediaCrypto
import com.google.android.exoplayer.util.MimeTypes
import com.rogertalk.roger.event.broadcasts.audio.AudioAmplitudeEvent
import com.rogertalk.roger.models.data.VisualizerType
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logError
import org.greenrobot.eventbus.EventBus
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RogerAudioRenderer(source: SampleSource, mediaCodecSelector: MediaCodecSelector,
                         drmSessionManager: DrmSessionManager<ExoMediaCrypto>?, playClearSamplesWithoutKeys: Boolean,
                         eventHandler: Handler, eventListener: EventListener, audioCapabilities: AudioCapabilities,
                         streamType: Int) :
        MediaCodecAudioTrackRenderer(source, mediaCodecSelector,
                drmSessionManager, playClearSamplesWithoutKeys,
                eventHandler, eventListener, audioCapabilities,
                streamType) {


    var innerPcmEncoding = 2

    private var lastSeenBufferIndex = -1


    //
    // OVERRIDE METHODS
    //

    override fun onInputFormatChanged(holder: MediaFormatHolder?) {
        super.onInputFormatChanged(holder)
        if (holder != null) {
            innerPcmEncoding = if (MimeTypes.AUDIO_RAW == holder.format.mimeType)
                holder.format.pcmEncoding
            else
                C.ENCODING_PCM_16BIT
        }
    }

    override fun processOutputBuffer(positionUs: Long, elapsedRealtimeUs: Long, codec: MediaCodec?, buffer: ByteBuffer?, bufferInfo: MediaCodec.BufferInfo?, bufferIndex: Int, shouldSkip: Boolean): Boolean {
        if (bufferIndex == lastSeenBufferIndex) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferInfo, bufferIndex, shouldSkip);
        } else {
            lastSeenBufferIndex = bufferIndex
        }

        if (buffer != null && bufferInfo != null) {
            try {
                if (innerPcmEncoding == 2) {
                    val bytesToRead: Int

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        buffer.position(0)
                        bytesToRead = bufferInfo.size / 2
                    } else {
                        bytesToRead = buffer.remaining() / 2
                    }

                    var sum = 0.0
                    val dataCopy = buffer.duplicate()
                    val shortArray = ShortArray(bytesToRead)
                    dataCopy.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray)
                    val readSize = shortArray.size

                    // Get the root mean square of all the readings
                    for (anArray in shortArray) {
                        sum += (anArray * anArray).toDouble()
                    }

                    val amplitude = Math.sqrt(sum / readSize)
                    logDebug { "Amplitude: $amplitude, sum $sum, readSize: $readSize" }
                    if (amplitude > 0) {
                        // TODO : This amplitude calculation still needs improvement
                        // Get the decibel level from the raw audio amplitude
                        val decibels = 20 * Math.log10(amplitude / java.lang.Byte.MAX_VALUE) - 50
                        //Log.d("Roger", "Decibels: $decibels, Amplitude: $amplitude")
                        EventBus.getDefault().post(AudioAmplitudeEvent(decibels, VisualizerType.PLAYBACK))
                    }
                } else {
                    logError(Exception("PCM Encoding is: " + innerPcmEncoding))
                }
            } catch (e: Exception) {
                //Log.e("Roger", "oops");
            }

        }

        return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferInfo, bufferIndex, shouldSkip)
    }
}