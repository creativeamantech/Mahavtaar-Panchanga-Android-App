package com.mahavtaar.panchanga.data.audio

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.AlertDeliveryMode
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.BuiltinVedicTone
import com.mahavtaar.panchanga.domain.model.TuneSourceType
import com.mahavtaar.panchanga.domain.model.VoiceRecording
import com.mahavtaar.panchanga.domain.model.VoiceSourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates all Vedic notification audio playback, bilingual offline Android TTS (Hindi & English),
 * custom tune playback (Built-in WAV, System Ringtone, SAF Audio URI), personal voice recording (.m4a),
 * and Silent / DND / Quiet Hours / active-call checks.
 */
@Singleton
class VedicVoiceAudioController @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val playbackMutex = Mutex()
    private val ttsInitMutex = Mutex()

    @Volatile
    private var mediaPlayer: MediaPlayer? = null

    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var ttsReady: Boolean = false

    private var activeRecorder: MediaRecorder? = null
    private var activeRecordingFile: File? = null
    private var activeRecordingName: String = ""
    private var activeRecordingStartMs: Long = 0L

    private val notificationAudioAttrs: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val mediaSpeechAudioAttrs: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    /**
     * Resolves AudioAttributes and legacy stream type.
     * Uses USAGE_MEDIA / STREAM_MUSIC for explicit in-app previews or when the notification
     * stream is independently muted while media volume is audible (after DND checks already pass).
     */
    private fun resolveAudioConfig(
        isInAppPreview: Boolean,
        respectSilentAndDnd: Boolean = true
    ): Pair<AudioAttributes, Int> {
        val am = audioManager
        val notifVol = runCatching { am?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: 1 }.getOrDefault(1)
        val mediaVol = runCatching { am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 1 }.getOrDefault(1)
        return if (isInAppPreview || (!respectSilentAndDnd && notifVol == 0 && mediaVol > 0)) {
            mediaSpeechAudioAttrs to AudioManager.STREAM_MUSIC
        } else {
            notificationAudioAttrs to AudioManager.STREAM_NOTIFICATION
        }
    }

    /** Returns true if Android Do Not Disturb (Priority / Alarms Only / Total Silence) is active. */
    fun isDeviceInDndInterruptionMode(): Boolean {
        val filter = notificationManager?.currentInterruptionFilter ?: return false
        return filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
            filter == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
            filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }

    /** Returns true if audio/voice should be suppressed due to App DND, Timed Mute, Silent, Vibrate, OS DND, or active call. */
    fun shouldSuppressForDeviceAudioState(settings: AlertAudioSettings, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (settings.isManualDndActive(nowMs)) return true
        val am = audioManager
        if (am != null) {
            val mode = am.mode
            if (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_RINGTONE) {
                return true
            }
            if (!settings.respectSilentAndDnd) return false
            if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return true
        } else if (!settings.respectSilentAndDnd) {
            return false
        }
        return isDeviceInDndInterruptionMode()
    }

    fun stopAllPlayback() {
        runCatching {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        mediaPlayer = null
        runCatching { tts?.stop() }
    }

    fun playTunePreview(
        tuneSource: TuneSourceType,
        tuneUriOrId: String,
        onCompleted: () -> Unit = {}
    ) {
        stopAllPlayback()
        val (attrs, _) = resolveAudioConfig(isInAppPreview = true)
        val focusReq = requestTransientFocus(attrs)
        val player = buildPlayerForTune(tuneSource, tuneUriOrId, attrs)
        if (player == null) {
            abandonFocus(focusReq)
            onCompleted()
            return
        }
        mediaPlayer = player
        player.setOnCompletionListener {
            runCatching { it.release() }
            if (mediaPlayer === it) mediaPlayer = null
            abandonFocus(focusReq)
            onCompleted()
        }
        player.setOnErrorListener { mp, _, _ ->
            runCatching { mp.release() }
            if (mediaPlayer === mp) mediaPlayer = null
            abandonFocus(focusReq)
            onCompleted()
            true
        }
        runCatching { player.start() }.onFailure {
            runCatching { player.release() }
            mediaPlayer = null
            abandonFocus(focusReq)
            onCompleted()
        }
    }

    fun playVoiceRecording(filePath: String, onCompleted: () -> Unit = {}) {
        stopAllPlayback()
        val file = File(filePath)
        if (!file.exists()) {
            onCompleted()
            return
        }
        val (attrs, _) = resolveAudioConfig(isInAppPreview = true)
        val focusReq = requestTransientFocus(attrs)
        val player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(file.absolutePath)
                prepare()
            }
        }.getOrNull()
        if (player == null) {
            abandonFocus(focusReq)
            onCompleted()
            return
        }
        mediaPlayer = player
        player.setOnCompletionListener {
            runCatching { it.release() }
            if (mediaPlayer === it) mediaPlayer = null
            abandonFocus(focusReq)
            onCompleted()
        }
        player.setOnErrorListener { mp, _, _ ->
            runCatching { mp.release() }
            if (mediaPlayer === mp) mediaPlayer = null
            abandonFocus(focusReq)
            onCompleted()
            true
        }
        runCatching { player.start() }.onFailure {
            runCatching { player.release() }
            mediaPlayer = null
            abandonFocus(focusReq)
            onCompleted()
        }
    }

    suspend fun speakBlocking(
        text: String,
        language: AppLanguage,
        speechRate: Float,
        pitch: Float,
        fallbackText: String? = null,
        isInAppPreview: Boolean = false,
        respectSilentAndDnd: Boolean = true
    ) {
        if (text.isBlank() && fallbackText.isNullOrBlank()) return
        val primaryEngine = ensureTtsReady() ?: return
        val (attrs, streamType) = resolveAudioConfig(isInAppPreview, respectSilentAndDnd)
        val focusReq = requestTransientFocus(attrs)

        try {
            val spokeFirstAttempt = speakOnceWithEngine(
                engine = primaryEngine,
                primaryText = text,
                fallbackText = fallbackText,
                language = language,
                speechRate = speechRate,
                pitch = pitch,
                attrs = attrs,
                streamType = streamType
            )
            if (!spokeFirstAttempt) {
                // Re-initialize once if a stale binder returned TextToSpeech.ERROR
                ttsReady = false
                runCatching { primaryEngine.shutdown() }
                if (tts === primaryEngine) tts = null
                val reinitEngine = ensureTtsReady()
                if (reinitEngine != null) {
                    speakOnceWithEngine(
                        engine = reinitEngine,
                        primaryText = text,
                        fallbackText = fallbackText,
                        language = language,
                        speechRate = speechRate,
                        pitch = pitch,
                        attrs = attrs,
                        streamType = streamType
                    )
                }
            }
        } finally {
            abandonFocus(focusReq)
        }
    }

    private suspend fun speakOnceWithEngine(
        engine: TextToSpeech,
        primaryText: String,
        fallbackText: String?,
        language: AppLanguage,
        speechRate: Float,
        pitch: Float,
        attrs: AudioAttributes,
        streamType: Int
    ): Boolean {
        val done = CompletableDeferred<Boolean>()
        val utteranceId = "vedic_${UUID.randomUUID()}"

        val queueStatus = withContext(Dispatchers.Main) {
            val textToSpeak = selectLocaleAndText(
                engine = engine,
                primaryText = primaryText,
                fallbackText = fallbackText,
                language = language
            )
            if (textToSpeak.isBlank()) return@withContext TextToSpeech.ERROR

            engine.setAudioAttributes(attrs)
            engine.setSpeechRate(speechRate.coerceIn(0.6f, 1.6f))
            engine.setPitch(pitch.coerceIn(0.7f, 1.4f))
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) {
                    if (id == utteranceId && !done.isCompleted) done.complete(true)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    if (id == utteranceId && !done.isCompleted) done.complete(false)
                }
                override fun onError(id: String?, errorCode: Int) {
                    if (id == utteranceId && !done.isCompleted) done.complete(false)
                }
            })
            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, streamType)
            }
            engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }

        if (queueStatus != TextToSpeech.SUCCESS) {
            return false
        }
        return withTimeoutOrNull(14_000L) { done.await() } ?: true
    }

    private fun selectLocaleAndText(
        engine: TextToSpeech,
        primaryText: String,
        fallbackText: String?,
        language: AppLanguage
    ): String {
        fun tryLocales(locales: List<Locale>): Boolean {
            for (loc in locales) {
                val res = runCatching { engine.setLanguage(loc) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                if (res >= TextToSpeech.LANG_AVAILABLE) return true
            }
            return false
        }

        val englishLocales = listOf(
            Locale.forLanguageTag("en-IN"),
            Locale.US,
            Locale.ENGLISH,
            Locale.getDefault()
        )
        val hindiLocales = listOf(
            Locale.forLanguageTag("hi-IN"),
            Locale.forLanguageTag("hi")
        )

        return if (language == AppLanguage.HINDI) {
            if (primaryText.isNotBlank() && tryLocales(hindiLocales)) {
                primaryText
            } else if (!fallbackText.isNullOrBlank()) {
                tryLocales(englishLocales)
                fallbackText
            } else {
                tryLocales(listOf(Locale.getDefault()))
                primaryText
            }
        } else {
            val chosenText = primaryText.ifBlank { fallbackText.orEmpty() }
            tryLocales(englishLocales)
            chosenText
        }
    }

    /**
     * Full sequential alert playback: Tune (capped at 5s) -> Bilingual TTS OR Custom Voice Clip.
     * Serialized via [playbackMutex] so simultaneous transition alerts play back-to-back cleanly.
     */
    suspend fun playFullAlertSequence(
        deliveryMode: AlertDeliveryMode,
        tuneSource: TuneSourceType,
        tuneUriOrId: String,
        voiceSource: VoiceSourceType,
        customRecordingPath: String?,
        spokenText: String,
        language: AppLanguage,
        audioSettings: AlertAudioSettings,
        ignoreSilentCheck: Boolean = false,
        fallbackText: String? = null,
        isInAppPreview: Boolean = ignoreSilentCheck
    ) {
        if (deliveryMode == AlertDeliveryMode.SILENT_BANNER) return
        if (!ignoreSilentCheck && shouldSuppressForDeviceAudioState(audioSettings)) return

        playbackMutex.withLock {
            val (attrs, _) = resolveAudioConfig(isInAppPreview, audioSettings.respectSilentAndDnd)

            // Step 1: Play Tune if enabled
            if (deliveryMode == AlertDeliveryMode.TUNE_AND_VOICE || deliveryMode == AlertDeliveryMode.TUNE_ONLY) {
                playTuneBlocking(tuneSource, tuneUriOrId, maxDurationMs = 5_000L, audioAttrs = attrs)
            }

            // Step 2: Speak voice or play custom recording if enabled
            if (deliveryMode == AlertDeliveryMode.TUNE_AND_VOICE || deliveryMode == AlertDeliveryMode.VOICE_ONLY) {
                if (voiceSource == VoiceSourceType.CUSTOM_RECORDING && !customRecordingPath.isNullOrBlank() && File(customRecordingPath).exists()) {
                    playFileBlocking(customRecordingPath, maxDurationMs = 15_000L, audioAttrs = attrs)
                } else {
                    speakBlocking(
                        text = spokenText,
                        language = language,
                        speechRate = audioSettings.ttsSpeechRate,
                        pitch = audioSettings.ttsPitch,
                        fallbackText = fallbackText,
                        isInAppPreview = isInAppPreview,
                        respectSilentAndDnd = audioSettings.respectSilentAndDnd
                    )
                }
            }
        }
    }

    /**
     * Plays the built-in Soft Chime safely through [playbackMutex], respecting Silent / DND / in-call checks.
     */
    suspend fun playSoftChimeIfAllowed(audioSettings: AlertAudioSettings) {
        if (shouldSuppressForDeviceAudioState(audioSettings)) return
        playbackMutex.withLock {
            val (attrs, _) = resolveAudioConfig(
                isInAppPreview = false,
                respectSilentAndDnd = audioSettings.respectSilentAndDnd
            )
            playTuneBlocking(
                tuneSource = TuneSourceType.BUILTIN_VEDIC,
                tuneUriOrId = BuiltinVedicTone.SOFT_CHIME.id,
                maxDurationMs = 3_500L,
                audioAttrs = attrs
            )
        }
    }

    private suspend fun playTuneBlocking(
        tuneSource: TuneSourceType,
        tuneUriOrId: String,
        maxDurationMs: Long,
        audioAttrs: AudioAttributes = notificationAudioAttrs
    ) {
        val done = CompletableDeferred<Unit>()
        val focusReq = requestTransientFocus(audioAttrs)
        val player = withContext(Dispatchers.IO) { buildPlayerForTune(tuneSource, tuneUriOrId, audioAttrs) }
        if (player == null) {
            abandonFocus(focusReq)
            return
        }
        mediaPlayer = player
        player.setOnCompletionListener {
            if (!done.isCompleted) done.complete(Unit)
        }
        player.setOnErrorListener { _, _, _ ->
            if (!done.isCompleted) done.complete(Unit)
            true
        }
        runCatching { player.start() }.onFailure {
            if (!done.isCompleted) done.complete(Unit)
        }
        withTimeoutOrNull(maxDurationMs) { done.await() }
        runCatching {
            if (player.isPlaying) player.stop()
            player.release()
        }
        if (mediaPlayer === player) mediaPlayer = null
        abandonFocus(focusReq)
    }

    private suspend fun playFileBlocking(
        filePath: String,
        maxDurationMs: Long,
        audioAttrs: AudioAttributes = notificationAudioAttrs
    ) {
        val done = CompletableDeferred<Unit>()
        val focusReq = requestTransientFocus(audioAttrs)
        val player = withContext(Dispatchers.IO) {
            runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(audioAttrs)
                    setDataSource(filePath)
                    prepare()
                }
            }.getOrNull()
        }
        if (player == null) {
            abandonFocus(focusReq)
            return
        }
        mediaPlayer = player
        player.setOnCompletionListener {
            if (!done.isCompleted) done.complete(Unit)
        }
        player.setOnErrorListener { _, _, _ ->
            if (!done.isCompleted) done.complete(Unit)
            true
        }
        runCatching { player.start() }.onFailure {
            if (!done.isCompleted) done.complete(Unit)
        }
        withTimeoutOrNull(maxDurationMs) { done.await() }
        runCatching {
            if (player.isPlaying) player.stop()
            player.release()
        }
        if (mediaPlayer === player) mediaPlayer = null
        abandonFocus(focusReq)
    }

    private fun buildPlayerForTune(
        tuneSource: TuneSourceType,
        tuneUriOrId: String,
        audioAttrs: AudioAttributes = notificationAudioAttrs
    ): MediaPlayer? {
        return runCatching {
            MediaPlayer().apply {
                setAudioAttributes(audioAttrs)
                when (tuneSource) {
                    TuneSourceType.BUILTIN_VEDIC -> {
                        val tone = BuiltinVedicTone.fromId(tuneUriOrId)
                        val wavFile = VedicToneSynthesizer.getOrCreateToneFile(context, tone)
                        setDataSource(wavFile.absolutePath)
                    }
                    TuneSourceType.SYSTEM_RINGTONE,
                    TuneSourceType.CUSTOM_AUDIO_URI -> {
                        val uri = Uri.parse(tuneUriOrId)
                        setDataSource(context, uri)
                    }
                }
                prepare()
            }
        }.getOrElse {
            // Fallback gracefully to Temple Ghanti if external URI became unavailable
            runCatching {
                val wav = VedicToneSynthesizer.getOrCreateToneFile(context, BuiltinVedicTone.TEMPLE_GHANTI)
                MediaPlayer().apply {
                    setAudioAttributes(audioAttrs)
                    setDataSource(wav.absolutePath)
                    prepare()
                }
            }.getOrNull()
        }
    }

    private suspend fun ensureTtsReady(): TextToSpeech? = ttsInitMutex.withLock {
        tts?.takeIf { ttsReady }?.let { return@withLock it }
        val existing = tts
        if (existing != null && !ttsReady) {
            runCatching { existing.shutdown() }
            tts = null
        }

        val ready = CompletableDeferred<TextToSpeech?>()
        withContext(Dispatchers.Main) {
            var syncInitStatus: Int? = null
            var createdRef: TextToSpeech? = null
            createdRef = runCatching {
                TextToSpeech(context.applicationContext) { status ->
                    val instance = createdRef
                    if (instance == null) {
                        syncInitStatus = status
                    } else if (status == TextToSpeech.SUCCESS) {
                        tts = instance
                        ttsReady = true
                        if (!ready.isCompleted) ready.complete(instance)
                    } else {
                        ttsReady = false
                        if (!ready.isCompleted) ready.complete(null)
                    }
                }
            }.getOrNull()

            if (createdRef == null) {
                if (!ready.isCompleted) ready.complete(null)
            } else if (syncInitStatus != null) {
                if (syncInitStatus == TextToSpeech.SUCCESS) {
                    tts = createdRef
                    ttsReady = true
                    if (!ready.isCompleted) ready.complete(createdRef)
                } else {
                    runCatching { createdRef.shutdown() }
                    ttsReady = false
                    if (!ready.isCompleted) ready.complete(null)
                }
            }
        }

        val bound = withTimeoutOrNull(7_000L) { ready.await() }
        if (bound == null) {
            runCatching { tts?.shutdown() }
            tts = null
            ttsReady = false
        }
        bound
    }

    // ---- Personal Voice Recording (.m4a in app-private storage) ----

    fun startVoiceRecording(displayName: String): Result<String> = runCatching {
        stopAllPlayback()
        cancelVoiceRecording()

        val id = "voice_${UUID.randomUUID()}"
        val dir = File(context.filesDir, "voice_clips").apply { mkdirs() }
        val outFile = File(dir, "$id.m4a")

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setMaxDuration(25_000) // Cap custom voice clips at 25 seconds
            setOutputFile(outFile.absolutePath)
            prepare()
            start()
        }

        activeRecorder = recorder
        activeRecordingFile = outFile
        activeRecordingName = displayName.trim().ifEmpty { "My Vedic Voice" }
        activeRecordingStartMs = System.currentTimeMillis()
        id
    }

    fun stopVoiceRecording(): Result<VoiceRecording> = runCatching {
        val recorder = activeRecorder ?: error("No active recording")
        val file = activeRecordingFile ?: error("Missing output file")
        val duration = (System.currentTimeMillis() - activeRecordingStartMs).coerceAtLeast(400L)
        val name = activeRecordingName

        runCatching { recorder.stop() }
        runCatching { recorder.release() }
        activeRecorder = null
        activeRecordingFile = null

        val id = file.nameWithoutExtension
        VoiceRecording(
            id = id,
            displayName = name,
            filePath = file.absolutePath,
            durationMs = duration,
            createdAtMs = System.currentTimeMillis()
        )
    }

    fun cancelVoiceRecording() {
        runCatching { activeRecorder?.stop() }
        runCatching { activeRecorder?.release() }
        activeRecorder = null
        runCatching { activeRecordingFile?.delete() }
        activeRecordingFile = null
    }

    fun deleteVoiceFile(filePath: String) {
        runCatching { File(filePath).delete() }
    }

    private fun requestTransientFocus(attrs: AudioAttributes = notificationAudioAttrs): AudioFocusRequest {
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs)
            .setAcceptsDelayedFocusGain(false)
            .build()
        runCatching { audioManager?.requestAudioFocus(req) }
        return req
    }

    private fun abandonFocus(req: AudioFocusRequest) {
        runCatching { audioManager?.abandonAudioFocusRequest(req) }
    }
}
