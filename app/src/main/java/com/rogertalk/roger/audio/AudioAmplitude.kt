package com.rogertalk.roger.audio

/**
 * Audio amplitude is the max volume of an audio frame
 */
class AudioAmplitude {

    fun getAmplitude(audioData: ShortArray, readSize: Int): Double {
        var sum = 0.0
        if (readSize <= 0) {
            return sum
        }

        // Get the root mean square of all the readings
        for (i in 0..readSize) {
            sum += audioData[i] * audioData[i]
        }

        val amplitude = Math.sqrt(sum / readSize.toDouble())
        // Get the decibel level from the raw audio amplitude
        val decibels = 20 * Math.log10(amplitude / Short.MAX_VALUE)
        return decibels
    }

    fun getAmplitude(audioData: ByteArray) : Double {
        var sum = 0.0
        val readSize = audioData.size

        // Get the root mean square of all the readings
        for (i in 0 until readSize) {
            sum += audioData[i] * audioData[i]
        }

        val amplitude = Math.sqrt(sum / readSize.toDouble())
        // Get the decibel level from the raw audio amplitude
        val decibels = 20 * Math.log10(amplitude / Byte.MAX_VALUE)
        return decibels
    }

}