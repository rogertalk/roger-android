package com.rogertalk.roger.audio
/**
 * What a lame name for a class!
 */
object RogerLame {

    /**
     * Initialize LAME.

     * @param inSampleRate
     * *            input sample rate in Hz.
     * *
     * @param outChannel
     * *            number of channels in input stream.
     * *
     * @param outSampleRate
     * *            output sample rate in Hz.
     * *
     * @param outBitrate
     * *            brate compression ratio in KHz.
     */
    fun init(inSampleRate: Int, outChannel: Int,
             outSampleRate: Int, outBitrate: Int) {
        init(inSampleRate, outChannel, outSampleRate, outBitrate, 7, "", "")
    }

    /**
     * Initialize LAME.

     * @param inSampleRate
     * *            input sample rate in Hz.
     * *
     * @param outChannel
     * *            number of channels in input stream.
     * *
     * @param outSampleRate
     * *            output sample rate in Hz.
     * *
     * @param outBitrate
     * *            brate compression ratio in KHz.
     * *
     * @param quality
     * *            quality=0..9. 0=best (very slow). 9=worst.
     * *            recommended:
     * *            2 near-best quality, not too slow
     * *            5 good quality, fast
     * *            7 ok quality, really fast
     */

    external fun init(inSampleRate: Int, outChannel: Int,
                      outSampleRate: Int, outBitrate: Int, quality: Int, title: String, comment: String)

    /**
     * Encode buffer to mp3.

     * @param buffer_l
     * *            PCM data for left channel.
     * *
     * @param buffer_r
     * *            PCM data for right channel.
     * *
     * @param samples
     * *            number of samples per channel.
     * *
     * @param mp3buf
     * *            result encoded MP3 stream. You must specified
     * *            "7200 + (1.25 * buffer_l.length)" length array.
     * *
     * @return number of bytes output in mp3buf. Can be 0.
     * *         -1: mp3buf was too small
     * *         -2: malloc() problem
     * *         -3: lame_init_params() not called
     * *         -4: psycho acoustic problems
     */
    external fun encode(buffer_l: ShortArray, buffer_r: ShortArray,
               samples: Int, mp3buf: ByteArray): Int

    /**
     * Flush LAME buffer.

     * @param mp3buf
     * *            result encoded MP3 stream. You must specified at least 7200
     * *            bytes.
     * *
     * @return number of bytes output to mp3buf. Can be 0.
     */
    external fun flush(mp3buf: ByteArray): Int

    /**
     * Close LAME.
     */
    external fun close()

}
