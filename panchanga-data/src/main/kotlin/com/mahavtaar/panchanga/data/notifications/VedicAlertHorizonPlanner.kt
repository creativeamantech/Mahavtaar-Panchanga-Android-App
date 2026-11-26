package com.mahavtaar.panchanga.data.notifications

import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.core.VedicHindiNames
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.NotificationRule
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.ScheduledAlertItem
import com.mahavtaar.panchanga.domain.model.ScheduledAlertStatus
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.domain.model.TimingInterval
import com.mahavtaar.panchanga.domain.model.VedicAlertCategory
import com.mahavtaar.panchanga.domain.model.VedicAlertEventType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Computes exact upcoming alert trigger instances across the next 36 hours from
 * authentic Drik Ganita snapshots, 24-Hora Sub-Tattvas, and Shiva Svarodaya Nadi windows.
 *
 * Notifications strictly separate `Start` and `End` timestamps (with Date + Seconds),
 * use one consistent simple Nadi name without conflicting Surya/Chandra Swara aliases,
 * and keep spoken voice announcements concise without clock times or unsolicited advice.
 */
object VedicAlertHorizonPlanner {

    private data class CandidateEvent(
        val keySuffix: String,
        val eventType: VedicAlertEventType,
        val eventStartTimeMs: Long,
        val eventEndTimeMs: Long? = null,
        val allowSimultaneousMultiple: Boolean = false,
        val titleEn: String,
        val titleHi: String,
        val liveBodyEn: String,
        val liveBodyHi: String,
        val advanceBodyEn: (Int) -> String,
        val advanceBodyHi: (Int) -> String,
        val liveSpeechEn: String,
        val liveSpeechHi: String,
        val advanceSpeechEn: (Int) -> String,
        val advanceSpeechHi: (Int) -> String
    )

    fun planHorizon(
        snapshotsWithHoras: List<Pair<PanchangaSnapshot, DailyHoras>>,
        rules: List<NotificationRule>,
        audioSettings: AlertAudioSettings,
        nowMs: Long,
        horizonHours: Int = 36
    ): List<ScheduledAlertItem> {
        if (!audioSettings.masterEnabled) return emptyList()
        val enabledByType = rules.filter { it.isEnabled }.associateBy { it.eventType }
        if (enabledByType.isEmpty()) return emptyList()

        val horizonEndMs = nowMs + horizonHours * 3_600_000L
        val results = mutableListOf<ScheduledAlertItem>()
        val seenIds = HashSet<String>()
        val seenTriggerSignatures = HashSet<String>()

        snapshotsWithHoras.forEach { (snap, dailyHoras) ->
            val zone = TimeFormatters.safeZoneId(snap.timezone)
            val candidates = collectCandidatesForDay(snap, dailyHoras, enabledByType.keys)

            candidates.forEach { c ->
                val rule = enabledByType[c.eventType] ?: return@forEach

                // 1) Advance warning (leadTimeMinutes > 0)
                if (rule.leadTimeMinutes > 0) {
                    val triggerMs = c.eventStartTimeMs - rule.leadTimeMinutes * 60_000L
                    if (triggerMs in (nowMs + 2_000L)..horizonEndMs) {
                        val triggerZoned = Instant.ofEpochMilli(triggerMs).atZone(zone)
                        val minOfDay = triggerZoned.hour * 60 + triggerZoned.minute
                        if (!audioSettings.isWithinQuietHours(minOfDay) && !audioSettings.isManualDndActive(triggerMs)) {
                            val id = "${c.eventType.name}_${c.keySuffix}_pre_${rule.leadTimeMinutes}"
                            val triggerBucket = triggerMs / 30_000L
                            val sig = if (c.allowSimultaneousMultiple) {
                                "${c.eventType.name}_pre_${c.keySuffix}_$triggerBucket"
                            } else {
                                "${c.eventType.name}_pre_$triggerBucket"
                            }
                            if (seenIds.add(id) && seenTriggerSignatures.add(sig)) {
                                val speechEn = rule.customEnglishTemplate.trim().takeIf { it.isNotEmpty() }
                                    ?.replace("{minutes}", rule.leadTimeMinutes.toString())
                                    ?: c.advanceSpeechEn(rule.leadTimeMinutes)
                                val speechHi = rule.customHindiTemplate.trim().takeIf { it.isNotEmpty() }
                                    ?.replace("{minutes}", rule.leadTimeMinutes.toString())
                                    ?: c.advanceSpeechHi(rule.leadTimeMinutes)
                                results += ScheduledAlertItem(
                                    id = id,
                                    requestCode = stableRequestCode(sig),
                                    eventType = c.eventType,
                                    triggerTimeMs = triggerMs,
                                    eventStartTimeMs = c.eventStartTimeMs,
                                    eventEndTimeMs = c.eventEndTimeMs,
                                    isAdvanceWarning = true,
                                    leadTimeMinutes = rule.leadTimeMinutes,
                                    titleEn = "Starts in ${rule.leadTimeMinutes}m · ${c.titleEn}",
                                    titleHi = "${rule.leadTimeMinutes} मिनट बाकी · ${c.titleHi}",
                                    bodyEn = c.advanceBodyEn(rule.leadTimeMinutes),
                                    bodyHi = c.advanceBodyHi(rule.leadTimeMinutes),
                                    speechEn = speechEn,
                                    speechHi = speechHi,
                                    deliveryMode = rule.deliveryMode,
                                    tuneSource = rule.tuneSource,
                                    tuneUriOrId = rule.tuneUriOrId,
                                    voiceSource = rule.voiceSource,
                                    voiceRecordingId = rule.voiceRecordingId,
                                    status = ScheduledAlertStatus.SCHEDULED,
                                    updatedAtMs = nowMs
                                )
                            }
                        }
                    }
                }

                // 2) Exact transition start alert
                if (rule.notifyAtStart) {
                    val triggerMs = c.eventStartTimeMs
                    if (triggerMs in (nowMs + 2_000L)..horizonEndMs) {
                        val triggerZoned = Instant.ofEpochMilli(triggerMs).atZone(zone)
                        val minOfDay = triggerZoned.hour * 60 + triggerZoned.minute
                        if (!audioSettings.isWithinQuietHours(minOfDay) && !audioSettings.isManualDndActive(triggerMs)) {
                            val id = "${c.eventType.name}_${c.keySuffix}_start"
                            val triggerBucket = triggerMs / 30_000L
                            val sig = if (c.allowSimultaneousMultiple) {
                                "${c.eventType.name}_start_${c.keySuffix}_$triggerBucket"
                            } else {
                                "${c.eventType.name}_start_$triggerBucket"
                            }
                            if (seenIds.add(id) && seenTriggerSignatures.add(sig)) {
                                val speechEn = rule.customEnglishTemplate.trim().takeIf { it.isNotEmpty() }
                                    ?: c.liveSpeechEn
                                val speechHi = rule.customHindiTemplate.trim().takeIf { it.isNotEmpty() }
                                    ?: c.liveSpeechHi
                                results += ScheduledAlertItem(
                                    id = id,
                                    requestCode = stableRequestCode(sig),
                                    eventType = c.eventType,
                                    triggerTimeMs = triggerMs,
                                    eventStartTimeMs = c.eventStartTimeMs,
                                    eventEndTimeMs = c.eventEndTimeMs,
                                    isAdvanceWarning = false,
                                    leadTimeMinutes = 0,
                                    titleEn = c.titleEn,
                                    titleHi = c.titleHi,
                                    bodyEn = c.liveBodyEn,
                                    bodyHi = c.liveBodyHi,
                                    speechEn = speechEn,
                                    speechHi = speechHi,
                                    deliveryMode = rule.deliveryMode,
                                    tuneSource = rule.tuneSource,
                                    tuneUriOrId = rule.tuneUriOrId,
                                    voiceSource = rule.voiceSource,
                                    voiceRecordingId = rule.voiceRecordingId,
                                    status = ScheduledAlertStatus.SCHEDULED,
                                    updatedAtMs = nowMs
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cap to Android AlarmManager safe quota (up to 96 upcoming alarms in 36h)
        return results.sortedBy { it.triggerTimeMs }.take(96)
    }

    fun buildSampleTestItem(
        rule: NotificationRule,
        nowMs: Long,
        timezone: String = TimeZone.getDefault().id
    ): ScheduledAlertItem {
        val sampleEndMs = nowMs + 48L * 60_000L
        val titleEn: String
        val titleHi: String
        val metaEn: String?
        val metaHi: String?
        val speechEnDefault: String
        val speechHiDefault: String
        val hasDuration: Boolean

        when (rule.eventType) {
            VedicAlertEventType.SWARA_SUNRISE_NADI,
            VedicAlertEventType.SWARA_MIDDAY_NADI,
            VedicAlertEventType.SWARA_SUNSET_NADI,
            VedicAlertEventType.SWARA_MOONRISE_NADI,
            VedicAlertEventType.SWARA_MOONSET_NADI,
            VedicAlertEventType.SWARA_TITHI_BOUNDARY,
            VedicAlertEventType.SWARA_NAKSHATRA_BOUNDARY -> {
                titleEn = rule.eventType.titleEn
                titleHi = rule.eventType.titleHi
                metaEn = "Nadi: ${nadiLabelEn(SwaraNadi.IDA)} · Tattva: ${tattvaEn(TattvaElement.PRITHVI)}"
                metaHi = "नाड़ी: ${nadiLabelHi(SwaraNadi.IDA)} · तत्व: ${tattvaHi(TattvaElement.PRITHVI)}"
                speechEnDefault = "${rule.eventType.titleEn}, ${nadiSpeechEn(SwaraNadi.IDA)} with ${tattvaEn(TattvaElement.PRITHVI)} has started."
                speechHiDefault = "${rule.eventType.titleHi}, ${nadiSpeechHi(SwaraNadi.IDA)} और ${tattvaHi(TattvaElement.PRITHVI)} शुरू हो गई है।"
                hasDuration = true
            }
            VedicAlertEventType.HORA_SUB_TATTVA,
            VedicAlertEventType.VARA_TATTVA,
            VedicAlertEventType.TITHI_TATTVA -> {
                titleEn = "${tattvaEn(TattvaElement.TEJAS)} · Sun Hora"
                titleHi = "${tattvaHi(TattvaElement.TEJAS)} · सूर्य होरा"
                metaEn = "Nadi: ${nadiLabelEn(SwaraNadi.PINGALA)} · Tattva: ${tattvaEn(TattvaElement.TEJAS)}"
                metaHi = "नाड़ी: ${nadiLabelHi(SwaraNadi.PINGALA)} · तत्व: ${tattvaHi(TattvaElement.TEJAS)}"
                speechEnDefault = "${tattvaEn(TattvaElement.TEJAS)} with ${nadiSpeechEn(SwaraNadi.PINGALA)} has started."
                speechHiDefault = "${tattvaHi(TattvaElement.TEJAS)} और ${nadiSpeechHi(SwaraNadi.PINGALA)} शुरू हो गई है।"
                hasDuration = true
            }
            VedicAlertEventType.PLANETARY_HORA -> {
                titleEn = "Jupiter Hora"
                titleHi = "गुरु होरा"
                metaEn = "Nadi: ${nadiLabelEn(SwaraNadi.IDA)} · Tattva: ${tattvaEn(TattvaElement.PRITHVI)}"
                metaHi = "नाड़ी: ${nadiLabelHi(SwaraNadi.IDA)} · तत्व: ${tattvaHi(TattvaElement.PRITHVI)}"
                speechEnDefault = "Jupiter Hora with ${nadiSpeechEn(SwaraNadi.IDA)} and ${tattvaEn(TattvaElement.PRITHVI)} has started."
                speechHiDefault = "गुरु होरा में ${nadiSpeechHi(SwaraNadi.IDA)} और ${tattvaHi(TattvaElement.PRITHVI)} शुरू हो गया है।"
                hasDuration = true
            }
            VedicAlertEventType.SUNRISE,
            VedicAlertEventType.SUNSET,
            VedicAlertEventType.MOONRISE,
            VedicAlertEventType.MOONSET -> {
                titleEn = rule.eventType.titleEn
                titleHi = rule.eventType.titleHi
                metaEn = "Nadi: ${nadiLabelEn(SwaraNadi.IDA)}"
                metaHi = "नाड़ी: ${nadiLabelHi(SwaraNadi.IDA)}"
                speechEnDefault = "${rule.eventType.titleEn} with ${nadiSpeechEn(SwaraNadi.IDA)} has occurred."
                speechHiDefault = "${rule.eventType.titleHi} और ${nadiSpeechHi(SwaraNadi.IDA)} शुरू हो गई है।"
                hasDuration = false
            }
            else -> {
                titleEn = rule.eventType.titleEn
                titleHi = rule.eventType.titleHi
                metaEn = if (rule.eventType.category == VedicAlertCategory.MUHURTA) {
                    "Nadi: ${nadiLabelEn(SwaraNadi.IDA)}"
                } else null
                metaHi = if (rule.eventType.category == VedicAlertCategory.MUHURTA) {
                    "नाड़ी: ${nadiLabelHi(SwaraNadi.IDA)}"
                } else null
                speechEnDefault = "${rule.eventType.titleEn} has started."
                speechHiDefault = "${rule.eventType.titleHi} शुरू हो गया है।"
                hasDuration = true
            }
        }

        val bodyEn = if (hasDuration) {
            formatRangeBody(
                timezone = timezone,
                isHindi = false,
                startMs = nowMs,
                endMs = sampleEndMs,
                prefixLine = null,
                metaLine = metaEn
            )
        } else {
            formatPointBody(
                timezone = timezone,
                isHindi = false,
                eventMs = nowMs,
                prefixLine = null,
                metaLine = metaEn
            )
        }

        val bodyHi = if (hasDuration) {
            formatRangeBody(
                timezone = timezone,
                isHindi = true,
                startMs = nowMs,
                endMs = sampleEndMs,
                prefixLine = null,
                metaLine = metaHi
            )
        } else {
            formatPointBody(
                timezone = timezone,
                isHindi = true,
                eventMs = nowMs,
                prefixLine = null,
                metaLine = metaHi
            )
        }

        return ScheduledAlertItem(
            id = "test_${rule.eventType.name}_$nowMs",
            requestCode = stableRequestCode("test_${rule.eventType.name}"),
            eventType = rule.eventType,
            triggerTimeMs = nowMs,
            eventStartTimeMs = nowMs,
            eventEndTimeMs = if (hasDuration) sampleEndMs else null,
            isAdvanceWarning = false,
            leadTimeMinutes = rule.leadTimeMinutes,
            titleEn = titleEn,
            titleHi = titleHi,
            bodyEn = bodyEn,
            bodyHi = bodyHi,
            speechEn = rule.customEnglishTemplate.trim().ifEmpty { speechEnDefault },
            speechHi = rule.customHindiTemplate.trim().ifEmpty { speechHiDefault },
            deliveryMode = rule.deliveryMode,
            tuneSource = rule.tuneSource,
            tuneUriOrId = rule.tuneUriOrId,
            voiceSource = rule.voiceSource,
            voiceRecordingId = rule.voiceRecordingId,
            status = ScheduledAlertStatus.DELIVERED,
            updatedAtMs = nowMs
        )
    }

    private fun collectCandidatesForDay(
        snap: PanchangaSnapshot,
        dailyHoras: DailyHoras,
        enabledTypes: Set<VedicAlertEventType>
    ): List<CandidateEvent> {
        val list = mutableListOf<CandidateEvent>()
        val dateKey = snap.dateIso
        val tz = snap.timezone

        fun activeNadiAt(epochMs: Long): SwaraNadi? =
            dailyHoras.horas.firstOrNull { epochMs in it.startTimeMs until it.endTimeMs }?.nadi

        fun activeTattvaAt(epochMs: Long): TattvaElement? =
            dailyHoras.horas
                .asSequence()
                .flatMap { it.tattvas.asSequence() }
                .firstOrNull { epochMs in it.startTimeMs until it.endTimeMs }
                ?.element

        // 1. Hora Sub-Tattva Transitions (every 5 sub-tattvas in all 24 horas)
        if (VedicAlertEventType.HORA_SUB_TATTVA in enabledTypes) {
            dailyHoras.horas.forEach { hora ->
                val rulerEn = hora.ruler
                val rulerHi = horaRulerHi(hora.ruler)
                val nEn = nadiLabelEn(hora.nadi)
                val nHi = nadiLabelHi(hora.nadi)
                val nSpeechEn = nadiSpeechEn(hora.nadi)
                val nSpeechHi = nadiSpeechHi(hora.nadi)
                hora.tattvas.forEachIndexed { idx, tp ->
                    val tEn = tattvaEn(tp.element)
                    val tHi = tattvaHi(tp.element)
                    val metaEn = "Nadi: $nEn · Tattva: $tEn"
                    val metaHi = "नाड़ी: $nHi · तत्व: $tHi"
                    list += CandidateEvent(
                        keySuffix = "${dateKey}_h${hora.index}_t$idx",
                        eventType = VedicAlertEventType.HORA_SUB_TATTVA,
                        eventStartTimeMs = tp.startTimeMs,
                        eventEndTimeMs = tp.endTimeMs,
                        titleEn = "$tEn · $rulerEn Hora",
                        titleHi = "$tHi · $rulerHi होरा",
                        liveBodyEn = formatRangeBody(tz, false, tp.startTimeMs, tp.endTimeMs, null, metaEn),
                        liveBodyHi = formatRangeBody(tz, true, tp.startTimeMs, tp.endTimeMs, null, metaHi),
                        advanceBodyEn = { m -> formatRangeBody(tz, false, tp.startTimeMs, tp.endTimeMs, "Starts in $m minutes", metaEn) },
                        advanceBodyHi = { m -> formatRangeBody(tz, true, tp.startTimeMs, tp.endTimeMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                        liveSpeechEn = "$tEn in $rulerEn Hora with $nSpeechEn has started.",
                        liveSpeechHi = "$rulerHi होरा में $tHi और $nSpeechHi शुरू हो गई है।",
                        advanceSpeechEn = { m -> "$m minutes left for $tEn in $rulerEn Hora with $nSpeechEn." },
                        advanceSpeechHi = { m -> "$rulerHi होरा में $tHi और $nSpeechHi शुरू होने में $m मिनट बाकी हैं।" }
                    )
                }
            }
        }

        // 2. Daily Vara Tattva (at Sunrise until next Sunrise)
        if (VedicAlertEventType.VARA_TATTVA in enabledTypes) {
            val (vEn, vHi) = weekdayTattva(snap.weekday)
            val vaaraHi = VedicHindiNames.vaaraHi(snap.weekday, snap.vaara)
            val srNadi = snap.swaraYoga.sunriseSwara
            val nEn = nadiLabelEn(srNadi)
            val nHi = nadiLabelHi(srNadi)
            val nSpeechEn = nadiSpeechEn(srNadi)
            val nSpeechHi = nadiSpeechHi(srNadi)
            val metaEn = "Nadi: $nEn · Tattva: $vEn"
            val metaHi = "नाड़ी: $nHi · तत्व: $vHi"
            list += CandidateEvent(
                keySuffix = "${dateKey}_vara_tattva",
                eventType = VedicAlertEventType.VARA_TATTVA,
                eventStartTimeMs = snap.sunriseMs,
                eventEndTimeMs = snap.nextSunriseMs,
                titleEn = "${snap.vaara} Vara Tattva: $vEn",
                titleHi = "$vaaraHi वार तत्व: $vHi",
                liveBodyEn = formatRangeBody(tz, false, snap.sunriseMs, snap.nextSunriseMs, null, metaEn),
                liveBodyHi = formatRangeBody(tz, true, snap.sunriseMs, snap.nextSunriseMs, null, metaHi),
                advanceBodyEn = { m -> formatRangeBody(tz, false, snap.sunriseMs, snap.nextSunriseMs, "Starts in $m minutes", metaEn) },
                advanceBodyHi = { m -> formatRangeBody(tz, true, snap.sunriseMs, snap.nextSunriseMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                liveSpeechEn = "${snap.vaara} $vEn with $nSpeechEn has started.",
                liveSpeechHi = "$vaaraHi का $vHi और $nSpeechHi शुरू हो गया है।",
                advanceSpeechEn = { m -> "$m minutes left for ${snap.vaara} $vEn with $nSpeechEn." },
                advanceSpeechHi = { m -> "$vaaraHi का $vHi शुरू होने में $m मिनट बाकी हैं।" }
            )
        }

        // 3. Tithi Tattva Shift (at each Tithi transition)
        if (VedicAlertEventType.TITHI_TATTVA in enabledTypes) {
            snap.tithi.forEach { seg ->
                val startMs = seg.startTimeMs ?: return@forEach
                val (ttEn, ttHi) = tithiTattva(seg.number)
                val tithiHi = VedicHindiNames.tithiHi(seg.number, seg.name)
                val nadi = seg.swaraInfo?.startNadi ?: activeNadiAt(startMs)
                val metaEn = buildMetaLineEn(nadi, ttEn)
                val metaHi = buildMetaLineHi(nadi, ttHi)
                val nSuffixEn = nadi?.let { " with ${nadiSpeechEn(it)}" }.orEmpty()
                val nSuffixHi = nadi?.let { " और ${nadiSpeechHi(it)}" }.orEmpty()
                val afterEndEn = "After ${TimeFormatters.formatLocalizedDateTimeWithSeconds(snap.nextSunriseMs, tz, false)}"
                val afterEndHi = "${TimeFormatters.formatLocalizedDateTimeWithSeconds(snap.nextSunriseMs, tz, true)} के बाद"
                list += CandidateEvent(
                    keySuffix = "${dateKey}_tithi_tattva_${seg.number}",
                    eventType = VedicAlertEventType.TITHI_TATTVA,
                    eventStartTimeMs = startMs,
                    eventEndTimeMs = seg.endTimeMs,
                    titleEn = "Tithi Tattva: $ttEn (${seg.name})",
                    titleHi = "तिथि तत्व: $ttHi ($tithiHi)",
                    liveBodyEn = formatRangeBody(tz, false, startMs, seg.endTimeMs, null, metaEn, afterEndEn),
                    liveBodyHi = formatRangeBody(tz, true, startMs, seg.endTimeMs, null, metaHi, afterEndHi),
                    advanceBodyEn = { m -> formatRangeBody(tz, false, startMs, seg.endTimeMs, "Starts in $m minutes", metaEn, afterEndEn) },
                    advanceBodyHi = { m -> formatRangeBody(tz, true, startMs, seg.endTimeMs, "शुरू होने में $m मिनट बाकी", metaHi, afterEndHi) },
                    liveSpeechEn = "${seg.name} $ttEn$nSuffixEn has started.",
                    liveSpeechHi = "$tithiHi का $ttHi$nSuffixHi शुरू हो गया है।",
                    advanceSpeechEn = { m -> "$m minutes left for ${seg.name} $ttEn$nSuffixEn." },
                    advanceSpeechHi = { m -> "$tithiHi का $ttHi$nSuffixHi शुरू होने में $m मिनट बाकी हैं।" }
                )
            }
        }

        // 4. Swara Nadi Celestial Windows (Sunrise = 1st Hora, Midday = 7th Hora, Sunset = 12th Hora)
        val swara = snap.swaraYoga
        val dayHoras = dailyHoras.horas.filter { it.isDay }

        if (VedicAlertEventType.SWARA_SUNRISE_NADI in enabledTypes) {
            val firstHora = dayHoras.getOrNull(0)
            val startMs = swara.sunriseWindow.startTimeMs ?: firstHora?.startTimeMs ?: snap.sunriseMs
            val endMs = swara.sunriseWindow.endTimeMs ?: firstHora?.endTimeMs
            val tattva = firstHora?.tattvas?.firstOrNull()?.element
            val nadi = swara.sunriseSwara
            val metaEn = buildMetaLineEn(nadi, tattva?.let(::tattvaEn))
            val metaHi = buildMetaLineHi(nadi, tattva?.let(::tattvaHi))
            val tSpeechEn = tattva?.let { " with ${tattvaEn(it)}" }.orEmpty()
            val tSpeechHi = tattva?.let { " और ${tattvaHi(it)}" }.orEmpty()
            list += CandidateEvent(
                keySuffix = "${dateKey}_swara_sunrise",
                eventType = VedicAlertEventType.SWARA_SUNRISE_NADI,
                eventStartTimeMs = startMs,
                eventEndTimeMs = endMs,
                titleEn = "Sunrise · ${nadiLabelEn(nadi)}",
                titleHi = "सूर्योदय · ${nadiLabelHi(nadi)}",
                liveBodyEn = formatRangeBody(tz, false, startMs, endMs, null, metaEn),
                liveBodyHi = formatRangeBody(tz, true, startMs, endMs, null, metaHi),
                advanceBodyEn = { m -> formatRangeBody(tz, false, startMs, endMs, "Starts in $m minutes", metaEn) },
                advanceBodyHi = { m -> formatRangeBody(tz, true, startMs, endMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                liveSpeechEn = "Sunrise ${nadiSpeechEn(nadi)}$tSpeechEn has started.",
                liveSpeechHi = "सूर्योदय ${nadiSpeechHi(nadi)}$tSpeechHi शुरू हो गई है।",
                advanceSpeechEn = { m -> "$m minutes left for Sunrise ${nadiSpeechEn(nadi)}$tSpeechEn." },
                advanceSpeechHi = { m -> "सूर्योदय ${nadiSpeechHi(nadi)}$tSpeechHi शुरू होने में $m मिनट बाकी हैं।" }
            )
        }

        if (VedicAlertEventType.SWARA_MIDDAY_NADI in enabledTypes) {
            val seventhHora = dayHoras.getOrNull(6)
            val startMs = swara.middayWindow.startTimeMs ?: seventhHora?.startTimeMs
                ?: (snap.sunriseMs + (snap.sunsetMs - snap.sunriseMs) / 2L)
            val endMs = swara.middayWindow.endTimeMs ?: seventhHora?.endTimeMs
            val tattva = seventhHora?.tattvas?.firstOrNull()?.element
            val nadi = swara.middayWindow.nadi
            val metaEn = buildMetaLineEn(nadi, tattva?.let(::tattvaEn))
            val metaHi = buildMetaLineHi(nadi, tattva?.let(::tattvaHi))
            val tSpeechEn = tattva?.let { " with ${tattvaEn(it)}" }.orEmpty()
            val tSpeechHi = tattva?.let { " और ${tattvaHi(it)}" }.orEmpty()
            list += CandidateEvent(
                keySuffix = "${dateKey}_swara_midday",
                eventType = VedicAlertEventType.SWARA_MIDDAY_NADI,
                eventStartTimeMs = startMs,
                eventEndTimeMs = endMs,
                titleEn = "Midday · ${nadiLabelEn(nadi)}",
                titleHi = "मध्याह्न · ${nadiLabelHi(nadi)}",
                liveBodyEn = formatRangeBody(tz, false, startMs, endMs, null, metaEn),
                liveBodyHi = formatRangeBody(tz, true, startMs, endMs, null, metaHi),
                advanceBodyEn = { m -> formatRangeBody(tz, false, startMs, endMs, "Starts in $m minutes", metaEn) },
                advanceBodyHi = { m -> formatRangeBody(tz, true, startMs, endMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                liveSpeechEn = "Midday ${nadiSpeechEn(nadi)}$tSpeechEn has started.",
                liveSpeechHi = "मध्याह्न ${nadiSpeechHi(nadi)}$tSpeechHi शुरू हो गई है।",
                advanceSpeechEn = { m -> "$m minutes left for Midday ${nadiSpeechEn(nadi)}$tSpeechEn." },
                advanceSpeechHi = { m -> "मध्याह्न ${nadiSpeechHi(nadi)}$tSpeechHi शुरू होने में $m मिनट बाकी हैं।" }
            )
        }

        // Sunset Swara: single alert at 12th Day Hora window start (matching 1st & 7th Hora behavior)
        if (VedicAlertEventType.SWARA_SUNSET_NADI in enabledTypes) {
            val twelfthHora = dayHoras.getOrNull(11)
            val startMs = swara.sunsetWindow.startTimeMs ?: twelfthHora?.startTimeMs
                ?: (snap.sunsetMs - ((snap.sunsetMs - snap.sunriseMs) / 12L))
            val endMs = swara.sunsetWindow.endTimeMs ?: twelfthHora?.endTimeMs ?: snap.sunsetMs
            val tattva = twelfthHora?.tattvas?.firstOrNull()?.element
            val nadi = swara.sunsetSwara
            val metaEn = buildMetaLineEn(nadi, tattva?.let(::tattvaEn))
            val metaHi = buildMetaLineHi(nadi, tattva?.let(::tattvaHi))
            val tSpeechEn = tattva?.let { " with ${tattvaEn(it)}" }.orEmpty()
            val tSpeechHi = tattva?.let { " और ${tattvaHi(it)}" }.orEmpty()
            list += CandidateEvent(
                keySuffix = "${dateKey}_swara_sunset_window",
                eventType = VedicAlertEventType.SWARA_SUNSET_NADI,
                eventStartTimeMs = startMs,
                eventEndTimeMs = endMs,
                titleEn = "Sunset · ${nadiLabelEn(nadi)}",
                titleHi = "सूर्यास्त · ${nadiLabelHi(nadi)}",
                liveBodyEn = formatRangeBody(tz, false, startMs, endMs, null, metaEn),
                liveBodyHi = formatRangeBody(tz, true, startMs, endMs, null, metaHi),
                advanceBodyEn = { m -> formatRangeBody(tz, false, startMs, endMs, "Starts in $m minutes", metaEn) },
                advanceBodyHi = { m -> formatRangeBody(tz, true, startMs, endMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                liveSpeechEn = "Sunset ${nadiSpeechEn(nadi)}$tSpeechEn has started.",
                liveSpeechHi = "सूर्यास्त ${nadiSpeechHi(nadi)}$tSpeechHi शुरू हो गई है।",
                advanceSpeechEn = { m -> "$m minutes left for Sunset ${nadiSpeechEn(nadi)}$tSpeechEn." },
                advanceSpeechHi = { m -> "सूर्यास्त ${nadiSpeechHi(nadi)}$tSpeechHi शुरू होने में $m मिनट बाकी हैं।" }
            )
        }

        val moonriseForSwara = snap.moonrise
        if (VedicAlertEventType.SWARA_MOONRISE_NADI in enabledTypes && (swara.moonriseWindow?.startTimeMs != null || snap.moonriseMs != null || moonriseForSwara != null)) {
            val startMs = swara.moonriseWindow?.startTimeMs
                ?: snap.moonriseMs
                ?: moonriseForSwara?.let { parseLocalTimeOnDate(snap.dateIso, it, tz) }
            if (startMs != null) {
                val endMs = swara.moonriseWindow?.endTimeMs ?: (startMs + 3_600_000L)
                val tattva = activeTattvaAt(startMs)
                val nadi = swara.moonriseSwara
                val metaEn = buildMetaLineEn(nadi, tattva?.let(::tattvaEn))
                val metaHi = buildMetaLineHi(nadi, tattva?.let(::tattvaHi))
                val tSpeechEn = tattva?.let { " with ${tattvaEn(it)}" }.orEmpty()
                val tSpeechHi = tattva?.let { " और ${tattvaHi(it)}" }.orEmpty()
                list += CandidateEvent(
                    keySuffix = "${dateKey}_swara_moonrise",
                    eventType = VedicAlertEventType.SWARA_MOONRISE_NADI,
                    eventStartTimeMs = startMs,
                    eventEndTimeMs = endMs,
                    titleEn = "Moonrise · ${nadiLabelEn(nadi)}",
                    titleHi = "चन्द्रोदय · ${nadiLabelHi(nadi)}",
                    liveBodyEn = formatRangeBody(tz, false, startMs, endMs, null, metaEn),
                    liveBodyHi = formatRangeBody(tz, true, startMs, endMs, null, metaHi),
                    advanceBodyEn = { m -> formatRangeBody(tz, false, startMs, endMs, "Starts in $m minutes", metaEn) },
                    advanceBodyHi = { m -> formatRangeBody(tz, true, startMs, endMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                    liveSpeechEn = "Moonrise ${nadiSpeechEn(nadi)}$tSpeechEn has started.",
                    liveSpeechHi = "चन्द्रोदय ${nadiSpeechHi(nadi)}$tSpeechHi शुरू हो गई है।",
                    advanceSpeechEn = { m -> "$m minutes left for Moonrise ${nadiSpeechEn(nadi)}$tSpeechEn." },
                    advanceSpeechHi = { m -> "चन्द्रोदय ${nadiSpeechHi(nadi)}$tSpeechHi शुरू होने में $m मिनट बाकी हैं।" }
                )
            }
        }

        // Moonset Swara: single alert 1 hour before Moonset (matching Swara window start)
        val moonsetForSwara = snap.moonset
        if (VedicAlertEventType.SWARA_MOONSET_NADI in enabledTypes && (swara.moonsetWindow?.startTimeMs != null || snap.moonsetMs != null || moonsetForSwara != null)) {
            val moonsetEndMs = swara.moonsetWindow?.endTimeMs
                ?: snap.moonsetMs
                ?: moonsetForSwara?.let { parseLocalTimeOnDate(snap.dateIso, it, tz) }
            val startMs = swara.moonsetWindow?.startTimeMs ?: moonsetEndMs?.minus(3_600_000L)
            if (startMs != null) {
                val endMs = moonsetEndMs ?: (startMs + 3_600_000L)
                val tattva = activeTattvaAt(startMs)
                val nadi = swara.moonsetSwara
                val metaEn = buildMetaLineEn(nadi, tattva?.let(::tattvaEn))
                val metaHi = buildMetaLineHi(nadi, tattva?.let(::tattvaHi))
                val tSpeechEn = tattva?.let { " with ${tattvaEn(it)}" }.orEmpty()
                val tSpeechHi = tattva?.let { " और ${tattvaHi(it)}" }.orEmpty()
                list += CandidateEvent(
                    keySuffix = "${dateKey}_swara_moonset_window",
                    eventType = VedicAlertEventType.SWARA_MOONSET_NADI,
                    eventStartTimeMs = startMs,
                    eventEndTimeMs = endMs,
                    titleEn = "Moonset · ${nadiLabelEn(nadi)}",
                    titleHi = "चन्द्रास्त · ${nadiLabelHi(nadi)}",
                    liveBodyEn = formatRangeBody(tz, false, startMs, endMs, null, metaEn),
                    liveBodyHi = formatRangeBody(tz, true, startMs, endMs, null, metaHi),
                    advanceBodyEn = { m -> formatRangeBody(tz, false, startMs, endMs, "Starts in $m minutes", metaEn) },
                    advanceBodyHi = { m -> formatRangeBody(tz, true, startMs, endMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                    liveSpeechEn = "Moonset ${nadiSpeechEn(nadi)}$tSpeechEn has started.",
                    liveSpeechHi = "चन्द्रास्त ${nadiSpeechHi(nadi)}$tSpeechHi शुरू हो गई है।",
                    advanceSpeechEn = { m -> "$m minutes left for Moonset ${nadiSpeechEn(nadi)}$tSpeechEn." },
                    advanceSpeechHi = { m -> "चन्द्रास्त ${nadiSpeechHi(nadi)}$tSpeechHi शुरू होने में $m मिनट बाकी हैं।" }
                )
            }
        }

        if (VedicAlertEventType.SWARA_TITHI_BOUNDARY in enabledTypes) {
            snap.tithi.forEach { seg ->
                val tithiHi = VedicHindiNames.tithiHi(seg.number, seg.name)
                val (ttEn, ttHi) = tithiTattva(seg.number)
                val startEvt = seg.swaraInfo?.startEvent
                if (startEvt != null) {
                    val metaEn = buildMetaLineEn(startEvt.nadi, ttEn)
                    val metaHi = buildMetaLineHi(startEvt.nadi, ttHi)
                    list += CandidateEvent(
                        keySuffix = "${dateKey}_swara_tithi_start_${seg.number}",
                        eventType = VedicAlertEventType.SWARA_TITHI_BOUNDARY,
                        eventStartTimeMs = startEvt.startTimeMs,
                        eventEndTimeMs = startEvt.endTimeMs,
                        titleEn = "${seg.name} Start · ${nadiLabelEn(startEvt.nadi)}",
                        titleHi = "$tithiHi आरम्भ · ${nadiLabelHi(startEvt.nadi)}",
                        liveBodyEn = formatRangeBody(tz, false, startEvt.startTimeMs, startEvt.endTimeMs, null, metaEn),
                        liveBodyHi = formatRangeBody(tz, true, startEvt.startTimeMs, startEvt.endTimeMs, null, metaHi),
                        advanceBodyEn = { m -> formatRangeBody(tz, false, startEvt.startTimeMs, startEvt.endTimeMs, "Starts in $m minutes", metaEn) },
                        advanceBodyHi = { m -> formatRangeBody(tz, true, startEvt.startTimeMs, startEvt.endTimeMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                        liveSpeechEn = "${seg.name} ${nadiSpeechEn(startEvt.nadi)} with $ttEn has started.",
                        liveSpeechHi = "$tithiHi की ${nadiSpeechHi(startEvt.nadi)} और $ttHi शुरू हो गई है।",
                        advanceSpeechEn = { m -> "$m minutes left for ${seg.name} ${nadiSpeechEn(startEvt.nadi)} with $ttEn." },
                        advanceSpeechHi = { m -> "$tithiHi की ${nadiSpeechHi(startEvt.nadi)} शुरू होने में $m मिनट बाकी हैं।" }
                    )
                }
                val endEvt = seg.swaraInfo?.endEvent
                if (endEvt != null) {
                    val metaEn = buildMetaLineEn(endEvt.nadi, ttEn)
                    val metaHi = buildMetaLineHi(endEvt.nadi, ttHi)
                    list += CandidateEvent(
                        keySuffix = "${dateKey}_swara_tithi_end_${seg.number}",
                        eventType = VedicAlertEventType.SWARA_TITHI_BOUNDARY,
                        eventStartTimeMs = endEvt.startTimeMs,
                        eventEndTimeMs = endEvt.endTimeMs,
                        titleEn = "${seg.name} Closing · ${nadiLabelEn(endEvt.nadi)}",
                        titleHi = "$tithiHi समाप्ति · ${nadiLabelHi(endEvt.nadi)}",
                        liveBodyEn = formatRangeBody(tz, false, endEvt.startTimeMs, endEvt.endTimeMs, null, metaEn),
                        liveBodyHi = formatRangeBody(tz, true, endEvt.startTimeMs, endEvt.endTimeMs, null, metaHi),
                        advanceBodyEn = { m -> formatRangeBody(tz, false, endEvt.startTimeMs, endEvt.endTimeMs, "Starts in $m minutes", metaEn) },
                        advanceBodyHi = { m -> formatRangeBody(tz, true, endEvt.startTimeMs, endEvt.endTimeMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                        liveSpeechEn = "${seg.name} closing ${nadiSpeechEn(endEvt.nadi)} with $ttEn has started.",
                        liveSpeechHi = "$tithiHi समाप्ति की ${nadiSpeechHi(endEvt.nadi)} और $ttHi शुरू हो गई है।",
                        advanceSpeechEn = { m -> "$m minutes left for ${seg.name} closing ${nadiSpeechEn(endEvt.nadi)}." },
                        advanceSpeechHi = { m -> "$tithiHi समाप्ति की ${nadiSpeechHi(endEvt.nadi)} शुरू होने में $m मिनट बाकी हैं।" }
                    )
                }
            }
        }

        if (VedicAlertEventType.SWARA_NAKSHATRA_BOUNDARY in enabledTypes) {
            snap.nakshatra.forEach { seg ->
                val nakHi = VedicHindiNames.nakshatraHi(seg.number, seg.name)
                val startEvt = seg.swaraInfo?.startEvent
                if (startEvt != null) {
                    val tattva = activeTattvaAt(startEvt.startTimeMs)
                    val metaEn = buildMetaLineEn(startEvt.nadi, tattva?.let(::tattvaEn))
                    val metaHi = buildMetaLineHi(startEvt.nadi, tattva?.let(::tattvaHi))
                    val tSpeechEn = tattva?.let { " with ${tattvaEn(it)}" }.orEmpty()
                    val tSpeechHi = tattva?.let { " और ${tattvaHi(it)}" }.orEmpty()
                    list += CandidateEvent(
                        keySuffix = "${dateKey}_swara_nak_start_${seg.number}",
                        eventType = VedicAlertEventType.SWARA_NAKSHATRA_BOUNDARY,
                        eventStartTimeMs = startEvt.startTimeMs,
                        eventEndTimeMs = startEvt.endTimeMs,
                        titleEn = "${seg.name} Start · ${nadiLabelEn(startEvt.nadi)}",
                        titleHi = "$nakHi आरम्भ · ${nadiLabelHi(startEvt.nadi)}",
                        liveBodyEn = formatRangeBody(tz, false, startEvt.startTimeMs, startEvt.endTimeMs, null, metaEn),
                        liveBodyHi = formatRangeBody(tz, true, startEvt.startTimeMs, startEvt.endTimeMs, null, metaHi),
                        advanceBodyEn = { m -> formatRangeBody(tz, false, startEvt.startTimeMs, startEvt.endTimeMs, "Starts in $m minutes", metaEn) },
                        advanceBodyHi = { m -> formatRangeBody(tz, true, startEvt.startTimeMs, startEvt.endTimeMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                        liveSpeechEn = "${seg.name} Nakshatra ${nadiSpeechEn(startEvt.nadi)}$tSpeechEn has started.",
                        liveSpeechHi = "$nakHi नक्षत्र की ${nadiSpeechHi(startEvt.nadi)}$tSpeechHi शुरू हो गई है।",
                        advanceSpeechEn = { m -> "$m minutes left for ${seg.name} Nakshatra ${nadiSpeechEn(startEvt.nadi)}$tSpeechEn." },
                        advanceSpeechHi = { m -> "$nakHi नक्षत्र की ${nadiSpeechHi(startEvt.nadi)}$tSpeechHi शुरू होने में $m मिनट बाकी हैं।" }
                    )
                }
                val endEvt = seg.swaraInfo?.endEvent
                if (endEvt != null) {
                    val tattva = activeTattvaAt(endEvt.startTimeMs)
                    val metaEn = buildMetaLineEn(endEvt.nadi, tattva?.let(::tattvaEn))
                    val metaHi = buildMetaLineHi(endEvt.nadi, tattva?.let(::tattvaHi))
                    val tSpeechEn = tattva?.let { " with ${tattvaEn(it)}" }.orEmpty()
                    val tSpeechHi = tattva?.let { " और ${tattvaHi(it)}" }.orEmpty()
                    list += CandidateEvent(
                        keySuffix = "${dateKey}_swara_nak_end_${seg.number}",
                        eventType = VedicAlertEventType.SWARA_NAKSHATRA_BOUNDARY,
                        eventStartTimeMs = endEvt.startTimeMs,
                        eventEndTimeMs = endEvt.endTimeMs,
                        titleEn = "${seg.name} Closing · ${nadiLabelEn(endEvt.nadi)}",
                        titleHi = "$nakHi समाप्ति · ${nadiLabelHi(endEvt.nadi)}",
                        liveBodyEn = formatRangeBody(tz, false, endEvt.startTimeMs, endEvt.endTimeMs, null, metaEn),
                        liveBodyHi = formatRangeBody(tz, true, endEvt.startTimeMs, endEvt.endTimeMs, null, metaHi),
                        advanceBodyEn = { m -> formatRangeBody(tz, false, endEvt.startTimeMs, endEvt.endTimeMs, "Starts in $m minutes", metaEn) },
                        advanceBodyHi = { m -> formatRangeBody(tz, true, endEvt.startTimeMs, endEvt.endTimeMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                        liveSpeechEn = "${seg.name} Nakshatra closing ${nadiSpeechEn(endEvt.nadi)}$tSpeechEn has started.",
                        liveSpeechHi = "$nakHi नक्षत्र समाप्ति की ${nadiSpeechHi(endEvt.nadi)}$tSpeechHi शुरू हो गई है।",
                        advanceSpeechEn = { m -> "$m minutes left for ${seg.name} Nakshatra closing ${nadiSpeechEn(endEvt.nadi)}$tSpeechEn." },
                        advanceSpeechHi = { m -> "$nakHi नक्षत्र समाप्ति की ${nadiSpeechHi(endEvt.nadi)}$tSpeechHi शुरू होने में $m मिनट बाकी हैं।" }
                    )
                }
            }
        }

        // 5. Planetary Horas
        if (VedicAlertEventType.PLANETARY_HORA in enabledTypes) {
            dailyHoras.horas.forEach { hora ->
                val rulerEn = hora.ruler
                val rulerHi = horaRulerHi(hora.ruler)
                val firstTattva = hora.tattvas.firstOrNull()?.element
                val metaEn = buildMetaLineEn(hora.nadi, firstTattva?.let(::tattvaEn))
                val metaHi = buildMetaLineHi(hora.nadi, firstTattva?.let(::tattvaHi))
                val tSpeechEn = firstTattva?.let { " and ${tattvaEn(it)}" }.orEmpty()
                val tSpeechHi = firstTattva?.let { " और ${tattvaHi(it)}" }.orEmpty()
                list += CandidateEvent(
                    keySuffix = "${dateKey}_hora_${hora.index}",
                    eventType = VedicAlertEventType.PLANETARY_HORA,
                    eventStartTimeMs = hora.startTimeMs,
                    eventEndTimeMs = hora.endTimeMs,
                    titleEn = "$rulerEn Hora",
                    titleHi = "$rulerHi होरा",
                    liveBodyEn = formatRangeBody(tz, false, hora.startTimeMs, hora.endTimeMs, null, metaEn),
                    liveBodyHi = formatRangeBody(tz, true, hora.startTimeMs, hora.endTimeMs, null, metaHi),
                    advanceBodyEn = { m -> formatRangeBody(tz, false, hora.startTimeMs, hora.endTimeMs, "Starts in $m minutes", metaEn) },
                    advanceBodyHi = { m -> formatRangeBody(tz, true, hora.startTimeMs, hora.endTimeMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                    liveSpeechEn = "$rulerEn Hora with ${nadiSpeechEn(hora.nadi)}$tSpeechEn has started.",
                    liveSpeechHi = "$rulerHi होरा में ${nadiSpeechHi(hora.nadi)}$tSpeechHi शुरू हो गया है।",
                    advanceSpeechEn = { m -> "$m minutes left for $rulerEn Hora with ${nadiSpeechEn(hora.nadi)}$tSpeechEn." },
                    advanceSpeechHi = { m -> "$rulerHi होरा में ${nadiSpeechHi(hora.nadi)}$tSpeechHi शुरू होने में $m मिनट बाकी हैं।" }
                )
            }
        }

        // 6. Muhurtas & Choghadiya
        fun addTimingAlert(
            type: VedicAlertEventType,
            interval: TimingInterval,
            titleEn: String,
            titleHi: String,
            speechNameEn: String,
            speechNameHi: String
        ) {
            if (type !in enabledTypes) return
            val startMs = interval.startTimeMs ?: return
            val endMs = interval.endTimeMs
            val nadi = activeNadiAt(startMs)
            val tattva = activeTattvaAt(startMs)
            val metaEn = buildMetaLineEn(nadi, tattva?.let(::tattvaEn))
            val metaHi = buildMetaLineHi(nadi, tattva?.let(::tattvaHi))
            val nSpeechEn = nadi?.let { " with ${nadiSpeechEn(it)}" }.orEmpty()
            val tSpeechEn = tattva?.let { " and ${tattvaEn(it)}" }.orEmpty()
            val nSpeechHi = nadi?.let { " (${nadiSpeechHi(it)}" + (tattva?.let { t -> ", ${tattvaHi(t)}" } ?: "") + ")" }.orEmpty()
            list += CandidateEvent(
                keySuffix = "${dateKey}_${type.name.lowercase()}",
                eventType = type,
                eventStartTimeMs = startMs,
                eventEndTimeMs = endMs,
                titleEn = titleEn,
                titleHi = titleHi,
                liveBodyEn = formatRangeBody(tz, false, startMs, endMs, null, metaEn),
                liveBodyHi = formatRangeBody(tz, true, startMs, endMs, null, metaHi),
                advanceBodyEn = { m -> formatRangeBody(tz, false, startMs, endMs, "Starts in $m minutes", metaEn) },
                advanceBodyHi = { m -> formatRangeBody(tz, true, startMs, endMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                liveSpeechEn = "$speechNameEn$nSpeechEn$tSpeechEn has started.",
                liveSpeechHi = "$speechNameHi$nSpeechHi शुरू हो गया है।",
                advanceSpeechEn = { m -> "$m minutes left for $speechNameEn$nSpeechEn$tSpeechEn." },
                advanceSpeechHi = { m -> "$speechNameHi शुरू होने में $m मिनट बाकी हैं।" }
            )
        }

        addTimingAlert(VedicAlertEventType.BRAHMA_MUHURTA, snap.brahmaMuhurta, "Brahma Muhūrta", "ब्रह्म मुहूर्त", "Brahma Muhurta", "ब्रह्म मुहूर्त")
        addTimingAlert(VedicAlertEventType.ABHIJIT_MUHURTA, snap.abhijitMuhurta, "Abhijit Muhūrta", "अभिजित मुहूर्त", "Abhijit Muhurta", "अभिजित मुहूर्त")
        addTimingAlert(VedicAlertEventType.RAHU_KALA, snap.rahuKala, "Rāhu Kāla", "राहु काल", "Rahu Kala", "राहु काल")
        addTimingAlert(VedicAlertEventType.YAMAGANDA, snap.yamaganda, "Yamagaṇḍa Kāla", "यमगण्ड काल", "Yamaganda Kala", "यमगण्ड काल")
        addTimingAlert(VedicAlertEventType.GULIKA_KALA, snap.gulikaKala, "Gulika Kāla", "गुलिक काल", "Gulika Kala", "गुलिक काल")

        if (VedicAlertEventType.AUSPICIOUS_CHOGHADIYA in enabledTypes) {
            (snap.gauriChoghadiyaDay + snap.gauriChoghadiyaNight)
                .filter { it.isAuspicious && it.startTimeMs != null }
                .forEachIndexed { idx, chog ->
                    val startMs = chog.startTimeMs!!
                    val endMs = chog.endTimeMs
                    val nameEn = chog.name ?: "Auspicious Choghadiya"
                    val nameHi = choghadiyaHi(nameEn)
                    val nadi = activeNadiAt(startMs)
                    val tattva = activeTattvaAt(startMs)
                    val metaEn = buildMetaLineEn(nadi, tattva?.let(::tattvaEn))
                    val metaHi = buildMetaLineHi(nadi, tattva?.let(::tattvaHi))
                    val nSpeechEn = nadi?.let { " with ${nadiSpeechEn(it)}" }.orEmpty()
                    val tSpeechEn = tattva?.let { " and ${tattvaEn(it)}" }.orEmpty()
                    val ntSpeechHi = when {
                        nadi != null && tattva != null -> " में ${nadiSpeechHi(nadi)} और ${tattvaHi(tattva)}"
                        nadi != null -> " में ${nadiSpeechHi(nadi)}"
                        tattva != null -> " में ${tattvaHi(tattva)}"
                        else -> ""
                    }
                    list += CandidateEvent(
                        keySuffix = "${dateKey}_chog_$idx",
                        eventType = VedicAlertEventType.AUSPICIOUS_CHOGHADIYA,
                        eventStartTimeMs = startMs,
                        eventEndTimeMs = endMs,
                        titleEn = "$nameEn Choghadiya",
                        titleHi = "$nameHi चौघड़िया",
                        liveBodyEn = formatRangeBody(tz, false, startMs, endMs, null, metaEn),
                        liveBodyHi = formatRangeBody(tz, true, startMs, endMs, null, metaHi),
                        advanceBodyEn = { m -> formatRangeBody(tz, false, startMs, endMs, "Starts in $m minutes", metaEn) },
                        advanceBodyHi = { m -> formatRangeBody(tz, true, startMs, endMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                        liveSpeechEn = "$nameEn Choghadiya$nSpeechEn$tSpeechEn has started.",
                        liveSpeechHi = "$nameHi चौघड़िया$ntSpeechHi शुरू हो गया है।",
                        advanceSpeechEn = { m -> "$m minutes left for $nameEn Choghadiya$nSpeechEn$tSpeechEn." },
                        advanceSpeechHi = { m -> "$nameHi चौघड़िया शुरू होने में $m मिनट बाकी हैं।" }
                    )
                }
        }

        // 7. Sun, Moon & Five Angas
        if (VedicAlertEventType.SUNRISE in enabledTypes) {
            val srNadi = swara.sunriseSwara
            val tattva = dayHoras.getOrNull(0)?.tattvas?.firstOrNull()?.element ?: activeTattvaAt(snap.sunriseMs)
            val metaEn = buildMetaLineEn(srNadi, tattva?.let(::tattvaEn))
            val metaHi = buildMetaLineHi(srNadi, tattva?.let(::tattvaHi))
            val tSpeechEn = tattva?.let { " and ${tattvaEn(it)}" }.orEmpty()
            val tSpeechHi = tattva?.let { " और ${tattvaHi(it)}" }.orEmpty()
            list += CandidateEvent(
                keySuffix = "${dateKey}_sunrise",
                eventType = VedicAlertEventType.SUNRISE,
                eventStartTimeMs = snap.sunriseMs,
                titleEn = "Local Sunrise · ${snap.city}",
                titleHi = "स्थानीय सूर्योदय · ${snap.city}",
                liveBodyEn = formatPointBody(tz, false, snap.sunriseMs, null, metaEn),
                liveBodyHi = formatPointBody(tz, true, snap.sunriseMs, null, metaHi),
                advanceBodyEn = { m -> formatPointBody(tz, false, snap.sunriseMs, "Starts in $m minutes", metaEn) },
                advanceBodyHi = { m -> formatPointBody(tz, true, snap.sunriseMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                liveSpeechEn = "Sunrise with ${nadiSpeechEn(srNadi)}$tSpeechEn has started.",
                liveSpeechHi = "सूर्योदय ${nadiSpeechHi(srNadi)}$tSpeechHi शुरू हो गई है।",
                advanceSpeechEn = { m -> "$m minutes left for Sunrise with ${nadiSpeechEn(srNadi)}$tSpeechEn." },
                advanceSpeechHi = { m -> "सूर्योदय होने में $m मिनट बाकी हैं।" }
            )
        }

        if (VedicAlertEventType.SUNSET in enabledTypes) {
            val ssNadi = swara.sunsetSwara
            val tattva = activeTattvaAt(snap.sunsetMs)
            val metaEn = buildMetaLineEn(ssNadi, tattva?.let(::tattvaEn))
            val metaHi = buildMetaLineHi(ssNadi, tattva?.let(::tattvaHi))
            val tSpeechEn = tattva?.let { " and ${tattvaEn(it)}" }.orEmpty()
            val tSpeechHi = tattva?.let { " और ${tattvaHi(it)}" }.orEmpty()
            list += CandidateEvent(
                keySuffix = "${dateKey}_sunset",
                eventType = VedicAlertEventType.SUNSET,
                eventStartTimeMs = snap.sunsetMs,
                titleEn = "Local Sunset · ${snap.city}",
                titleHi = "स्थानीय सूर्यास्त · ${snap.city}",
                liveBodyEn = formatPointBody(tz, false, snap.sunsetMs, null, metaEn),
                liveBodyHi = formatPointBody(tz, true, snap.sunsetMs, null, metaHi),
                advanceBodyEn = { m -> formatPointBody(tz, false, snap.sunsetMs, "Starts in $m minutes", metaEn) },
                advanceBodyHi = { m -> formatPointBody(tz, true, snap.sunsetMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                liveSpeechEn = "Sunset with ${nadiSpeechEn(ssNadi)}$tSpeechEn has started.",
                liveSpeechHi = "सूर्यास्त ${nadiSpeechHi(ssNadi)}$tSpeechHi शुरू हो गई है।",
                advanceSpeechEn = { m -> "$m minutes left for Sunset." },
                advanceSpeechHi = { m -> "सूर्यास्त होने में $m मिनट बाकी हैं।" }
            )
        }

        val moonriseTime = snap.moonrise
        if (VedicAlertEventType.MOONRISE in enabledTypes && (snap.moonriseMs != null || moonriseTime != null)) {
            val moonriseMs = snap.moonriseMs ?: moonriseTime?.let { parseLocalTimeOnDate(snap.dateIso, it, tz) }
            if (moonriseMs != null) {
                val mrNadi = swara.moonriseSwara
                val tattva = activeTattvaAt(moonriseMs)
                val metaEn = buildMetaLineEn(mrNadi, tattva?.let(::tattvaEn))
                val metaHi = buildMetaLineHi(mrNadi, tattva?.let(::tattvaHi))
                val tSpeechEn = tattva?.let { " and ${tattvaEn(it)}" }.orEmpty()
                val tSpeechHi = tattva?.let { " और ${tattvaHi(it)}" }.orEmpty()
                list += CandidateEvent(
                    keySuffix = "${dateKey}_moonrise",
                    eventType = VedicAlertEventType.MOONRISE,
                    eventStartTimeMs = moonriseMs,
                    titleEn = "Local Moonrise · ${snap.city}",
                    titleHi = "स्थानीय चन्द्रोदय · ${snap.city}",
                    liveBodyEn = formatPointBody(tz, false, moonriseMs, null, metaEn),
                    liveBodyHi = formatPointBody(tz, true, moonriseMs, null, metaHi),
                    advanceBodyEn = { m -> formatPointBody(tz, false, moonriseMs, "Starts in $m minutes", metaEn) },
                    advanceBodyHi = { m -> formatPointBody(tz, true, moonriseMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                    liveSpeechEn = "Moonrise with ${nadiSpeechEn(mrNadi)}$tSpeechEn has started.",
                    liveSpeechHi = "चन्द्रोदय ${nadiSpeechHi(mrNadi)}$tSpeechHi शुरू हो गई है।",
                    advanceSpeechEn = { m -> "$m minutes left for Moonrise." },
                    advanceSpeechHi = { m -> "चन्द्रोदय होने में $m मिनट बाकी हैं।" }
                )
            }
        }

        val moonsetTime = snap.moonset
        if (VedicAlertEventType.MOONSET in enabledTypes && (snap.moonsetMs != null || moonsetTime != null)) {
            val moonsetMs = snap.moonsetMs ?: moonsetTime?.let { parseLocalTimeOnDate(snap.dateIso, it, tz) }
            if (moonsetMs != null) {
                val msNadi = swara.moonsetSwara
                val tattva = activeTattvaAt(moonsetMs)
                val metaEn = buildMetaLineEn(msNadi, tattva?.let(::tattvaEn))
                val metaHi = buildMetaLineHi(msNadi, tattva?.let(::tattvaHi))
                val tSpeechEn = tattva?.let { " and ${tattvaEn(it)}" }.orEmpty()
                val tSpeechHi = tattva?.let { " और ${tattvaHi(it)}" }.orEmpty()
                list += CandidateEvent(
                    keySuffix = "${dateKey}_moonset",
                    eventType = VedicAlertEventType.MOONSET,
                    eventStartTimeMs = moonsetMs,
                    titleEn = "Local Moonset · ${snap.city}",
                    titleHi = "स्थानीय चन्द्रास्त · ${snap.city}",
                    liveBodyEn = formatPointBody(tz, false, moonsetMs, null, metaEn),
                    liveBodyHi = formatPointBody(tz, true, moonsetMs, null, metaHi),
                    advanceBodyEn = { m -> formatPointBody(tz, false, moonsetMs, "Starts in $m minutes", metaEn) },
                    advanceBodyHi = { m -> formatPointBody(tz, true, moonsetMs, "शुरू होने में $m मिनट बाकी", metaHi) },
                    liveSpeechEn = "Moonset with ${nadiSpeechEn(msNadi)}$tSpeechEn has started.",
                    liveSpeechHi = "चन्द्रास्त ${nadiSpeechHi(msNadi)}$tSpeechHi शुरू हो गई है।",
                    advanceSpeechEn = { m -> "$m minutes left for Moonset." },
                    advanceSpeechHi = { m -> "चन्द्रास्त होने में $m मिनट बाकी हैं।" }
                )
            }
        }

        val afterEndEn = "After ${TimeFormatters.formatLocalizedDateTimeWithSeconds(snap.nextSunriseMs, tz, false)}"
        val afterEndHi = "${TimeFormatters.formatLocalizedDateTimeWithSeconds(snap.nextSunriseMs, tz, true)} के बाद"

        if (VedicAlertEventType.TITHI_CHANGE in enabledTypes) {
            snap.tithi.forEach { seg ->
                val ms = seg.startTimeMs ?: return@forEach
                val tithiHi = VedicHindiNames.tithiHi(seg.number, seg.name)
                val (ttEn, ttHi) = tithiTattva(seg.number)
                val nadi = seg.swaraInfo?.startNadi ?: activeNadiAt(ms)
                val metaEn = buildMetaLineEn(nadi, ttEn)
                val metaHi = buildMetaLineHi(nadi, ttHi)
                val nSpeechEn = nadi?.let { " with ${nadiSpeechEn(it)}" }.orEmpty()
                val nSpeechHi = nadi?.let { " और ${nadiSpeechHi(it)}" }.orEmpty()
                list += CandidateEvent(
                    keySuffix = "${dateKey}_tithi_${seg.number}",
                    eventType = VedicAlertEventType.TITHI_CHANGE,
                    eventStartTimeMs = ms,
                    eventEndTimeMs = seg.endTimeMs,
                    titleEn = "Tithi: ${seg.name}",
                    titleHi = "तिथि: $tithiHi",
                    liveBodyEn = formatRangeBody(tz, false, ms, seg.endTimeMs, null, metaEn, afterEndEn),
                    liveBodyHi = formatRangeBody(tz, true, ms, seg.endTimeMs, null, metaHi, afterEndHi),
                    advanceBodyEn = { m -> formatRangeBody(tz, false, ms, seg.endTimeMs, "Starts in $m minutes", metaEn, afterEndEn) },
                    advanceBodyHi = { m -> formatRangeBody(tz, true, ms, seg.endTimeMs, "शुरू होने में $m मिनट बाकी", metaHi, afterEndHi) },
                    liveSpeechEn = "${seg.name} Tithi$nSpeechEn has started.",
                    liveSpeechHi = "$tithiHi तिथि$nSpeechHi शुरू हो गई है।",
                    advanceSpeechEn = { m -> "$m minutes left for ${seg.name} Tithi$nSpeechEn." },
                    advanceSpeechHi = { m -> "$tithiHi तिथि शुरू होने में $m मिनट बाकी हैं।" }
                )
            }
        }

        if (VedicAlertEventType.NAKSHATRA_CHANGE in enabledTypes) {
            snap.nakshatra.forEach { seg ->
                val ms = seg.startTimeMs ?: return@forEach
                val nakHi = VedicHindiNames.nakshatraHi(seg.number, seg.name)
                val nadi = seg.swaraInfo?.startNadi ?: activeNadiAt(ms)
                val tattva = activeTattvaAt(ms)
                val metaEn = buildMetaLineEn(nadi, tattva?.let(::tattvaEn))
                val metaHi = buildMetaLineHi(nadi, tattva?.let(::tattvaHi))
                val nSpeechEn = nadi?.let { " with ${nadiSpeechEn(it)}" }.orEmpty()
                val nSpeechHi = nadi?.let { " और ${nadiSpeechHi(it)}" }.orEmpty()
                list += CandidateEvent(
                    keySuffix = "${dateKey}_nak_${seg.number}",
                    eventType = VedicAlertEventType.NAKSHATRA_CHANGE,
                    eventStartTimeMs = ms,
                    eventEndTimeMs = seg.endTimeMs,
                    titleEn = "Nakshatra: ${seg.name}",
                    titleHi = "नक्षत्र: $nakHi",
                    liveBodyEn = formatRangeBody(tz, false, ms, seg.endTimeMs, null, metaEn, afterEndEn),
                    liveBodyHi = formatRangeBody(tz, true, ms, seg.endTimeMs, null, metaHi, afterEndHi),
                    advanceBodyEn = { m -> formatRangeBody(tz, false, ms, seg.endTimeMs, "Starts in $m minutes", metaEn, afterEndEn) },
                    advanceBodyHi = { m -> formatRangeBody(tz, true, ms, seg.endTimeMs, "शुरू होने में $m मिनट बाकी", metaHi, afterEndHi) },
                    liveSpeechEn = "${seg.name} Nakshatra$nSpeechEn has started.",
                    liveSpeechHi = "$nakHi नक्षत्र$nSpeechHi शुरू हो गया है।",
                    advanceSpeechEn = { m -> "$m minutes left for ${seg.name} Nakshatra$nSpeechEn." },
                    advanceSpeechHi = { m -> "$nakHi नक्षत्र शुरू होने में $m मिनट बाकी हैं।" }
                )
            }
        }

        if (VedicAlertEventType.YOGA_CHANGE in enabledTypes) {
            snap.yoga.forEach { seg ->
                val ms = seg.startTimeMs ?: return@forEach
                val yogaHi = VedicHindiNames.yogaHi(seg.number, seg.name)
                val nadi = activeNadiAt(ms)
                val tattva = activeTattvaAt(ms)
                val metaEn = buildMetaLineEn(nadi, tattva?.let(::tattvaEn))
                val metaHi = buildMetaLineHi(nadi, tattva?.let(::tattvaHi))
                list += CandidateEvent(
                    keySuffix = "${dateKey}_yoga_${seg.number}",
                    eventType = VedicAlertEventType.YOGA_CHANGE,
                    eventStartTimeMs = ms,
                    eventEndTimeMs = seg.endTimeMs,
                    titleEn = "Yoga: ${seg.name}",
                    titleHi = "योग: $yogaHi",
                    liveBodyEn = formatRangeBody(tz, false, ms, seg.endTimeMs, null, metaEn, afterEndEn),
                    liveBodyHi = formatRangeBody(tz, true, ms, seg.endTimeMs, null, metaHi, afterEndHi),
                    advanceBodyEn = { m -> formatRangeBody(tz, false, ms, seg.endTimeMs, "Starts in $m minutes", metaEn, afterEndEn) },
                    advanceBodyHi = { m -> formatRangeBody(tz, true, ms, seg.endTimeMs, "शुरू होने में $m मिनट बाकी", metaHi, afterEndHi) },
                    liveSpeechEn = "${seg.name} Yoga has started.",
                    liveSpeechHi = "$yogaHi योग शुरू हो गया है।",
                    advanceSpeechEn = { m -> "$m minutes left for ${seg.name} Yoga." },
                    advanceSpeechHi = { m -> "$yogaHi योग शुरू होने में $m मिनट बाकी हैं।" }
                )
            }
        }

        if (VedicAlertEventType.KARANA_CHANGE in enabledTypes) {
            snap.karana.forEach { seg ->
                val ms = seg.startTimeMs ?: return@forEach
                val karanaHi = VedicHindiNames.karanaHi(seg.name)
                val nadi = activeNadiAt(ms)
                val tattva = activeTattvaAt(ms)
                val metaEn = buildMetaLineEn(nadi, tattva?.let(::tattvaEn))
                val metaHi = buildMetaLineHi(nadi, tattva?.let(::tattvaHi))
                list += CandidateEvent(
                    keySuffix = "${dateKey}_karana_${seg.number}",
                    eventType = VedicAlertEventType.KARANA_CHANGE,
                    eventStartTimeMs = ms,
                    eventEndTimeMs = seg.endTimeMs,
                    titleEn = "Karana: ${seg.name}",
                    titleHi = "करण: $karanaHi",
                    liveBodyEn = formatRangeBody(tz, false, ms, seg.endTimeMs, null, metaEn, afterEndEn),
                    liveBodyHi = formatRangeBody(tz, true, ms, seg.endTimeMs, null, metaHi, afterEndHi),
                    advanceBodyEn = { m -> formatRangeBody(tz, false, ms, seg.endTimeMs, "Starts in $m minutes", metaEn, afterEndEn) },
                    advanceBodyHi = { m -> formatRangeBody(tz, true, ms, seg.endTimeMs, "शुरू होने में $m मिनट बाकी", metaHi, afterEndHi) },
                    liveSpeechEn = "${seg.name} Karana has started.",
                    liveSpeechHi = "$karanaHi करण शुरू हो गया है।",
                    advanceSpeechEn = { m -> "$m minutes left for ${seg.name} Karana." },
                    advanceSpeechHi = { m -> "$karanaHi करण शुरू होने में $m मिनट बाकी हैं।" }
                )
            }
        }

        // 8. Vratas & Major Festivals (scheduled at local Sunrise + 30 min; day runs Sunrise to Next Sunrise)
        val morningVratTimeMs = snap.sunriseMs + 30L * 60_000L
        snap.observances.forEach { obs ->
            val isFestival = obs.category.equals("festival", ignoreCase = true)
            val targetType = if (isFestival) VedicAlertEventType.MAJOR_FESTIVAL else VedicAlertEventType.VRAT_OBSERVANCE
            if (targetType in enabledTypes) {
                val obsHi = VedicHindiNames.observanceHi(obs.id, obs.title)
                list += CandidateEvent(
                    keySuffix = "${dateKey}_obs_${obs.id}",
                    eventType = targetType,
                    eventStartTimeMs = morningVratTimeMs,
                    eventEndTimeMs = snap.nextSunriseMs,
                    allowSimultaneousMultiple = true,
                    titleEn = obs.title,
                    titleHi = obsHi,
                    liveBodyEn = formatRangeBody(tz, false, snap.sunriseMs, snap.nextSunriseMs, null, null),
                    liveBodyHi = formatRangeBody(tz, true, snap.sunriseMs, snap.nextSunriseMs, null, null),
                    advanceBodyEn = { m -> formatRangeBody(tz, false, snap.sunriseMs, snap.nextSunriseMs, "Reminder in $m minutes", null) },
                    advanceBodyHi = { m -> formatRangeBody(tz, true, snap.sunriseMs, snap.nextSunriseMs, "सूचना में $m मिनट बाकी", null) },
                    liveSpeechEn = "Today is ${obs.title}.",
                    liveSpeechHi = "आज $obsHi है।",
                    advanceSpeechEn = { m -> "$m minutes left for ${obs.title} reminder." },
                    advanceSpeechHi = { m -> "$obsHi की सूचना में $m मिनट बाकी हैं।" }
                )
            }
        }

        return list
    }

    private fun formatRangeBody(
        timezone: String,
        isHindi: Boolean,
        startMs: Long,
        endMs: Long?,
        prefixLine: String?,
        metaLine: String?,
        fallbackEndText: String? = null
    ): String {
        val startStr = TimeFormatters.formatLocalizedDateTimeWithSeconds(startMs, timezone, isHindi)
        val endStr = endMs?.let { TimeFormatters.formatLocalizedDateTimeWithSeconds(it, timezone, isHindi) }
            ?: fallbackEndText
        val lines = mutableListOf<String>()
        if (!prefixLine.isNullOrBlank()) lines += prefixLine
        if (!metaLine.isNullOrBlank()) lines += metaLine
        if (isHindi) {
            lines += "आरम्भ: $startStr"
            if (!endStr.isNullOrBlank()) lines += "समाप्ति: $endStr"
        } else {
            lines += "Start: $startStr"
            if (!endStr.isNullOrBlank()) lines += "End: $endStr"
        }
        return lines.joinToString("\n")
    }

    private fun formatPointBody(
        timezone: String,
        isHindi: Boolean,
        eventMs: Long,
        prefixLine: String?,
        metaLine: String?
    ): String {
        val timeStr = TimeFormatters.formatLocalizedDateTimeWithSeconds(eventMs, timezone, isHindi)
        val lines = mutableListOf<String>()
        if (!prefixLine.isNullOrBlank()) lines += prefixLine
        if (!metaLine.isNullOrBlank()) lines += metaLine
        lines += if (isHindi) "समय: $timeStr" else "Time: $timeStr"
        return lines.joinToString("\n")
    }

    private fun buildMetaLineEn(nadi: SwaraNadi?, tattvaLabel: String?): String? {
        val parts = mutableListOf<String>()
        if (nadi != null) parts += "Nadi: ${nadiLabelEn(nadi)}"
        if (!tattvaLabel.isNullOrBlank()) parts += "Tattva: $tattvaLabel"
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private fun buildMetaLineHi(nadi: SwaraNadi?, tattvaLabel: String?): String? {
        val parts = mutableListOf<String>()
        if (nadi != null) parts += "नाड़ी: ${nadiLabelHi(nadi)}"
        if (!tattvaLabel.isNullOrBlank()) parts += "तत्व: $tattvaLabel"
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private fun parseLocalTimeOnDate(dateIso: String, timeStr: String, timezone: String): Long? = runCatching {
        val cleaned = timeStr.trim().uppercase(Locale.US)
        if (cleaned.isEmpty() || cleaned == "—" || cleaned == "-") return@runCatching null
        val date = LocalDate.parse(dateIso)
        val zone = TimeFormatters.safeZoneId(timezone)
        val startOfDay = date.atStartOfDay(zone)

        // Handle 24-hour HH:mm:ss or HH:mm (including Vedic 24+ hour notation like 25:15:30)
        if (!cleaned.contains("AM") && !cleaned.contains("PM")) {
            val parts = cleaned.split(":")
            if (parts.size >= 2) {
                val hours = parts[0].trim().toLongOrNull() ?: return@runCatching null
                val minutes = parts[1].trim().toLongOrNull() ?: return@runCatching null
                val seconds = parts.getOrNull(2)?.trim()?.takeWhile { it.isDigit() }?.toLongOrNull() ?: 0L
                return@runCatching startOfDay
                    .plusHours(hours)
                    .plusMinutes(minutes)
                    .plusSeconds(seconds)
                    .toInstant()
                    .toEpochMilli()
            }
        }

        val patterns = listOf("hh:mm:ss a", "h:mm:ss a", "hh:mm a", "h:mm a", "HH:mm:ss", "HH:mm")
        var parsedTime: LocalTime? = null
        for (pattern in patterns) {
            parsedTime = runCatching {
                LocalTime.parse(cleaned, DateTimeFormatter.ofPattern(pattern, Locale.US))
            }.getOrNull()
            if (parsedTime != null) break
        }
        val finalTime = parsedTime ?: return@runCatching null
        ZonedDateTime.of(date, finalTime, zone).toInstant().toEpochMilli()
    }.getOrNull()

    private fun stableRequestCode(id: String): Int = 10_000 + (abs(id.hashCode()) % 80_000)

    private fun tattvaEn(element: TattvaElement): String = when (element) {
        TattvaElement.PRITHVI -> "Prithvi (Earth)"
        TattvaElement.JALA -> "Jala (Water)"
        TattvaElement.TEJAS -> "Agni (Fire)"
        TattvaElement.VAYU -> "Vayu (Air)"
        TattvaElement.AKASH -> "Akasha (Space)"
    }

    private fun tattvaHi(element: TattvaElement): String = when (element) {
        TattvaElement.PRITHVI -> "पृथ्वी तत्व"
        TattvaElement.JALA -> "जल तत्व"
        TattvaElement.TEJAS -> "अग्नि तत्व"
        TattvaElement.VAYU -> "वायु तत्व"
        TattvaElement.AKASH -> "आकाश तत्व"
    }

    private fun nadiLabelEn(nadi: SwaraNadi): String = when (nadi) {
        SwaraNadi.IDA -> "Ida Nadi (Left)"
        SwaraNadi.PINGALA -> "Pingala Nadi (Right)"
        SwaraNadi.SUSHUMNA -> "Sushumna Nadi (Both)"
    }

    private fun nadiLabelHi(nadi: SwaraNadi): String = when (nadi) {
        SwaraNadi.IDA -> "इड़ा नाड़ी (बायाँ)"
        SwaraNadi.PINGALA -> "पिंगला नाड़ी (दायाँ)"
        SwaraNadi.SUSHUMNA -> "सुषुम्णा नाड़ी (मध्य)"
    }

    private fun nadiSpeechEn(nadi: SwaraNadi): String = when (nadi) {
        SwaraNadi.IDA -> "Ida Nadi"
        SwaraNadi.PINGALA -> "Pingala Nadi"
        SwaraNadi.SUSHUMNA -> "Sushumna Nadi"
    }

    private fun nadiSpeechHi(nadi: SwaraNadi): String = when (nadi) {
        SwaraNadi.IDA -> "इड़ा नाड़ी"
        SwaraNadi.PINGALA -> "पिंगला नाड़ी"
        SwaraNadi.SUSHUMNA -> "सुषुम्णा नाड़ी"
    }

    private fun horaRulerHi(ruler: String): String = when (ruler.lowercase(Locale.US)) {
        "sun", "surya" -> "सूर्य"
        "moon", "chandra" -> "चन्द्र"
        "mars", "mangal" -> "मंगल"
        "mercury", "budha" -> "बुध"
        "jupiter", "guru" -> "गुरु"
        "venus", "shukra" -> "शुक्र"
        "saturn", "shani" -> "शनि"
        else -> ruler
    }

    private fun choghadiyaHi(name: String): String = when {
        name.contains("Amrit", true) -> "अमृत"
        name.contains("Shubh", true) -> "शुभ"
        name.contains("Labh", true) -> "लाभ"
        name.contains("Char", true) -> "चर"
        else -> name
    }

    private fun weekdayTattva(weekday: Int): Pair<String, String> = when (weekday) {
        0 -> "Agni (Fire) Tattva" to "अग्नि तत्व"
        1 -> "Jala (Water) Tattva" to "जल तत्व"
        2 -> "Agni (Fire) Tattva" to "अग्नि तत्व"
        3 -> "Prithvi (Earth) Tattva" to "पृथ्वी तत्व"
        4 -> "Akasha (Space) Tattva" to "आकाश तत्व"
        5 -> "Jala (Water) Tattva" to "जल तत्व"
        6 -> "Vayu (Air) Tattva" to "वायु तत्व"
        else -> "Prithvi (Earth) Tattva" to "पृथ्वी तत्व"
    }

    private fun tithiTattva(tithiNumber: Int): Pair<String, String> {
        val idx = ((tithiNumber - 1) % 15) + 1
        return when (idx) {
            1, 6, 11 -> "Agni (Fire) Tattva" to "अग्नि तत्व"
            2, 7, 12 -> "Prithvi (Earth) Tattva" to "पृथ्वी तत्व"
            3, 8, 13 -> "Akasha (Space) Tattva" to "आकाश तत्व"
            4, 9, 14 -> "Jala (Water) Tattva" to "जल तत्व"
            else -> "Vayu (Air) Tattva" to "वायु तत्व"
        }
    }
}
