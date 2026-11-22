package com.example.game.simulation

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * Lightweight zero-asset procedural sound synthesizer for tactile GTA feedback:
 * car horn, police siren chirp, cash/mission fanfare, nitro burst, and door latch.
 * Fails gracefully if audio hardware is unavailable in headless/emulator test containers.
 */
class VallartaSoundEngine {
    @Volatile var muted: Boolean = false

    fun playHorn() = playToneSequence(listOf(340f to 160, 420f to 180), volume = 0.22f, dualFreq = 425f)
    fun playDoorLatch() = playToneSequence(listOf(180f to 45, 290f to 55), volume = 0.20f)
    fun playNitroBurst() = playToneSequence(listOf(190f to 90, 320f to 120, 480f to 140), volume = 0.18f)
    fun playPoliceChirp() = playToneSequence(listOf(640f to 110, 920f to 130, 640f to 110), volume = 0.16f)
    fun playMissionComplete() = playToneSequence(listOf(523f to 110, 659f to 110, 784f to 130, 1046f to 260), volume = 0.24f)
    fun playPunch() = playToneSequence(listOf(140f to 65, 90f to 75), volume = 0.22f)
    fun playJump() = playToneSequence(listOf(220f to 55, 360f to 70), volume = 0.16f)
    fun playClimbGrab() = playToneSequence(listOf(170f to 40, 260f to 50), volume = 0.15f)

    private fun playToneSequence(notes: List<Pair<Float, Int>>, volume: Float, dualFreq: Float = 0f) {
        if (muted) return
        thread(isDaemon = true, name = "VallartaSfx") {
            var track: AudioTrack? = null
            try {
                val sampleRate = 22050
                val totalSamples = notes.sumOf { (it.second * sampleRate) / 1000 }
                val pcm = ShortArray(totalSamples)
                var cursor = 0
                for ((freq, ms) in notes) {
                    val count = (ms * sampleRate) / 1000
                    for (i in 0 until count) {
                        val t = i.toFloat() / sampleRate
                        val env = when {
                            i < 120 -> i / 120f
                            i > count - 200 -> ((count - i).coerceAtLeast(0)) / 200f
                            else -> 1f
                        }
                        var wave = sin(2.0 * PI * freq * t)
                        if (dualFreq > 0f) {
                            wave = (wave + sin(2.0 * PI * dualFreq * t)) * 0.5
                        }
                        pcm[cursor++] = (wave * env * volume * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(pcm.size * 2)

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(notes.sumOf { it.second }.toLong() + 60L)
            } catch (_: Throwable) {
                // Ignore audio unavailability
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {
                }
            }
        }
    }
}
