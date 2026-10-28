package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean

class AmbientSoundEngine {

    private val sampleRate = 22050
    private var audioTrack: AudioTrack? = null
    private var generatorThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    var isRainActive = false
    var rainVolume = 0.6f

    var isCampfireActive = false
    var campfireVolume = 0.5f

    var isNatureActive = false
    var natureVolume = 0.5f

    var masterVolume = 0.8f

    fun start() {
        if (isRunning.get()) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize * 2, 4096)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        isRunning.set(true)
        audioTrack?.play()

        generatorThread = Thread({
            val random = Random()
            val chunk = ShortArray(1024)

            // Filter states
            var rainFilter = 0.0
            var campfireFilter = 0.0
            var natureFilter = 0.0

            var chirpPhase = 0.0
            var chirpTimer = 0

            while (isRunning.get()) {
                val anyActive = isRainActive || isCampfireActive || isNatureActive
                if (!anyActive) {
                    // Stream silent buffer to keep track alive or sleep briefly
                    chunk.fill(0)
                    audioTrack?.write(chunk, 0, chunk.size)
                    try {
                        Thread.sleep(30)
                    } catch (_: InterruptedException) {
                        break
                    }
                    continue
                }

                for (i in chunk.indices) {
                    var sampleSum = 0.0

                    // 1. Rain synthesis (low-pass filtered brown noise)
                    if (isRainActive) {
                        val white = (random.nextDouble() * 2.0 - 1.0)
                        rainFilter = (rainFilter * 0.92) + (white * 0.08)
                        sampleSum += rainFilter * rainVolume * 0.7
                    }

                    // 2. Campfire synthesis (crackle transients + low rumble)
                    if (isCampfireActive) {
                        val white = (random.nextDouble() * 2.0 - 1.0)
                        campfireFilter = (campfireFilter * 0.85) + (white * 0.15)
                        var crackle = 0.0
                        // Occasional pop/spark
                        if (random.nextInt(350) == 0) {
                            crackle = (random.nextDouble() * 2.0 - 1.0) * 1.5
                        }
                        sampleSum += (campfireFilter * 0.35 + crackle) * campfireVolume * 0.8
                    }

                    // 3. Nature synthesis (gentle breeze filter + harmonic chirp)
                    if (isNatureActive) {
                        val white = (random.nextDouble() * 2.0 - 1.0)
                        natureFilter = (natureFilter * 0.97) + (white * 0.03)

                        // Gentle periodic bird tone
                        chirpTimer++
                        if (chirpTimer > sampleRate * 4) { // every 4 seconds
                            if (chirpTimer < sampleRate * 4 + 800) {
                                chirpPhase += (2.0 * Math.PI * 2200.0) / sampleRate
                                sampleSum += Math.sin(chirpPhase) * 0.15 * natureVolume
                            } else {
                                chirpTimer = random.nextInt(sampleRate * 2)
                            }
                        }
                        sampleSum += natureFilter * natureVolume * 0.6
                    }

                    // Master gain & soft limiter
                    sampleSum *= masterVolume
                    val clamped = sampleSum.coerceIn(-1.0, 1.0)
                    chunk[i] = (clamped * 32767.0).toInt().toShort()
                }

                audioTrack?.write(chunk, 0, chunk.size)
            }
        }, "AmbientAudioGenerator").apply {
            priority = Thread.NORM_PRIORITY
            start()
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            generatorThread?.interrupt()
            generatorThread?.join(500)
        } catch (_: Exception) {}
        generatorThread = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
