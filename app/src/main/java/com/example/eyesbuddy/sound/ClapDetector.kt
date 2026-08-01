package com.example.eyesbuddy.sound

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * OPTIONAL feature, off by default. Only reads raw amplitude from the mic
 * buffer to detect a loud sound (e.g. a clap) - it never writes audio to
 * disk or transmits it anywhere. Caller must have already been granted
 * RECORD_AUDIO before calling start().
 */
class ClapDetector(private val context: Context) {

    private val _loudSoundPulse = MutableStateFlow(0L)
    val loudSoundPulse: StateFlow<Long> = _loudSoundPulse

    private var job: Job? = null
    private var audioRecord: AudioRecord? = null

    private val sampleRate = 16000
    private val loudnessThreshold = 9000 // tune to taste; raw PCM16 amplitude
    private val cooldownMs = 1200L

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun start(scope: CoroutineScope) {
        if (!hasPermission()) return
        stop()

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer
        )
        audioRecord = record

        job = scope.launch(Dispatchers.Default) {
            val buffer = ShortArray(minBuffer)
            var lastTrigger = 0L
            record.startRecording()
            while (isActive) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    var peak = 0
                    for (i in 0 until read) {
                        val v = abs(buffer[i].toInt())
                        if (v > peak) peak = v
                    }
                    val now = System.currentTimeMillis()
                    if (peak > loudnessThreshold && now - lastTrigger > cooldownMs) {
                        lastTrigger = now
                        _loudSoundPulse.value = _loudSoundPulse.value + 1
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        audioRecord?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) { /* not recording */ }
            it.release()
        }
        audioRecord = null
    }
}
