package com.example.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object SoundFX {
    private const val SAMPLE_RATE = 22050
    private var isMuted = false

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun getMuteStatus(): Boolean = isMuted

    fun playJump() {
        if (isMuted) return
        GlobalScope.launch(Dispatchers.Default) {
            playSound(320f, 650f, 100, isSquare = true)
        }
    }

    fun playPoint() {
        if (isMuted) return
        GlobalScope.launch(Dispatchers.Default) {
            playSound(600f, 1200f, 130, isSquare = false)
        }
    }

    fun playHit() {
        if (isMuted) return
        GlobalScope.launch(Dispatchers.Default) {
            playSound(280f, 80f, 350, isSquare = true)
        }
    }

    private fun playSound(startFreq: Float, endFreq: Float, durationMs: Int, isSquare: Boolean) {
        try {
            val numSamples = durationMs * SAMPLE_RATE / 1000
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                // Linear slide in frequency
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                val angle = 2.0 * Math.PI * currentFreq * (i.toDouble() / SAMPLE_RATE)
                
                // Synth shape (Square for retro chirp, Sine for mellow coin chime)
                val value = if (isSquare) {
                    val raw = Math.sin(angle)
                    if (raw > 0) 0.18 else -0.18 // Softened volume multiplier
                } else {
                    Math.sin(angle) * 0.35 // Volume scaled
                }
                
                // Fade-out envelope to remove clicky clicks at the end of sounds
                val envelope = if (progress > 0.8) (1.0 - progress) / 0.2 else 1.0
                samples[i] = (value * Short.MAX_VALUE * envelope).toInt().toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            
            Thread.sleep(durationMs.toLong() + 30)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
