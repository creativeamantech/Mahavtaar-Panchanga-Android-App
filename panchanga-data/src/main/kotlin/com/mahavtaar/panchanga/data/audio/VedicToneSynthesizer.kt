package com.mahavtaar.panchanga.data.audio

import android.content.Context
import com.mahavtaar.panchanga.domain.model.BuiltinVedicTone
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedurally synthesizes crisp, authentic sacred Vedic tones as 16-bit 44.1kHz PCM WAV files
 * in app-private storage so built-in notification tunes work 100% offline with zero APK bloat.
 */
object VedicToneSynthesizer {

    private const val SAMPLE_RATE = 44_100

    fun getOrCreateToneFile(context: Context, tone: BuiltinVedicTone): File {
        val dir = File(context.filesDir, "vedic_tones").apply { mkdirs() }
        val target = File(dir, "${tone.id}_v1.wav")
        if (target.exists() && target.length() > 1024L) return target

        val samples = when (tone) {
            BuiltinVedicTone.TEMPLE_GHANTI -> synthTempleGhanti()
            BuiltinVedicTone.SHANKH_RESONANCE -> synthShankhResonance()
            BuiltinVedicTone.OM_TANPURA_DRONE -> synthOmTanpura()
            BuiltinVedicTone.SINGING_BOWL -> synthSingingBowl()
            BuiltinVedicTone.SOFT_CHIME -> synthSoftChime()
        }
        writeWavFile(target, samples)
        return target
    }

    private fun synthTempleGhanti(): ShortArray {
        val durationSec = 2.6
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val attack = (t / 0.006).coerceAtMost(1.0)
            val env1 = exp(-1.7 * t)
            val env2 = exp(-3.4 * t)
            val env3 = exp(-5.8 * t)
            val wave = 0.50 * sin(2.0 * PI * 528.0 * t) * env1 +
                0.28 * sin(2.0 * PI * 1056.0 * t) * env2 +
                0.14 * sin(2.0 * PI * 1589.0 * t) * env2 +
                0.08 * sin(2.0 * PI * 2124.0 * t) * env3
            out[i] = (wave * attack * 26000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthShankhResonance(): ShortArray {
        val durationSec = 2.8
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val attack = (t / 0.35).coerceAtMost(1.0)
            val release = ((durationSec - t) / 0.55).coerceIn(0.0, 1.0)
            val vibrato = 1.0 + 0.008 * sin(2.0 * PI * 5.2 * t)
            val base = 293.66 * vibrato
            val wave = 0.55 * sin(2.0 * PI * base * t) +
                0.26 * sin(2.0 * PI * base * 2.0 * t) +
                0.12 * sin(2.0 * PI * base * 3.0 * t) +
                0.05 * sin(2.0 * PI * base * 4.0 * t)
            out[i] = (wave * attack * release * 24000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthOmTanpura(): ShortArray {
        val durationSec = 3.0
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val attack = (t / 0.45).coerceAtMost(1.0)
            val release = ((durationSec - t) / 0.75).coerceIn(0.0, 1.0)
            val f0 = 136.1 // Classic Om Sa pitch
            val wave = 0.42 * sin(2.0 * PI * f0 * t) +
                0.30 * sin(2.0 * PI * (f0 * 1.5) * t) +
                0.20 * sin(2.0 * PI * (f0 * 2.0) * t) +
                0.08 * sin(2.0 * PI * (f0 * 3.0) * t)
            out[i] = (wave * attack * release * 24000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthSingingBowl(): ShortArray {
        val durationSec = 2.8
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val attack = (t / 0.04).coerceAtMost(1.0)
            val env = exp(-1.1 * t)
            val shimmer = 0.5 + 0.5 * sin(2.0 * PI * 3.2 * t)
            val wave = 0.50 * sin(2.0 * PI * 432.0 * t) +
                0.22 * sin(2.0 * PI * 435.2 * t) +
                0.18 * sin(2.0 * PI * 864.0 * t) * (0.6 + 0.4 * shimmer)
            out[i] = (wave * attack * env * 25000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthSoftChime(): ShortArray {
        val durationSec = 2.1
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(count)
        val notes = listOf(0.0 to 523.25, 0.32 to 659.25, 0.64 to 783.99)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            var sum = 0.0
            for ((start, freq) in notes) {
                val dt = t - start
                if (dt >= 0.0) {
                    val attack = (dt / 0.01).coerceAtMost(1.0)
                    val env = exp(-2.8 * dt)
                    sum += (0.55 * sin(2.0 * PI * freq * dt) + 0.20 * sin(2.0 * PI * freq * 2.0 * dt)) * attack * env
                }
            }
            out[i] = (sum * 22000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun writeWavFile(file: File, pcm: ShortArray) {
        val byteDataLen = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + byteDataLen)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM chunk size
            putShort(1) // PCM format
            putShort(1) // Mono
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(byteDataLen)
        }
        val body = ByteBuffer.allocate(byteDataLen).order(ByteOrder.LITTLE_ENDIAN)
        pcm.forEach { body.putShort(it) }

        FileOutputStream(file).use { fos ->
            fos.write(header.array())
            fos.write(body.array())
        }
    }
}
