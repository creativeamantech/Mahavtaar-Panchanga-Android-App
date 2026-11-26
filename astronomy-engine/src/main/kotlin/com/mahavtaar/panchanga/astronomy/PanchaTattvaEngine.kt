package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.DailyKaranaTattvaItem
import com.mahavtaar.panchanga.domain.model.KaranaMasterRuleItem
import com.mahavtaar.panchanga.domain.model.KaranaTattvaCycleReport
import com.mahavtaar.panchanga.domain.model.KaranaTattvaInfo
import com.mahavtaar.panchanga.domain.model.KaranaTattvaMaasOverlapItem
import com.mahavtaar.panchanga.domain.model.KaranaTattvaPeriodItem
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.PanchaTattvaReport
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.domain.model.TattvaHarmonyWindow
import com.mahavtaar.panchanga.domain.model.TithiMasterRuleItem
import com.mahavtaar.panchanga.domain.model.TithiTattvaInfo
import com.mahavtaar.panchanga.domain.model.TithiTattvaPeriodItem
import com.mahavtaar.panchanga.domain.model.VaraMasterRuleItem
import com.mahavtaar.panchanga.domain.model.VaraTattvaInfo
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.searchMoonPhase
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure Kotlin Pancha Tattva synthesizer matching `varaTattvaEngine.ts`, `tithiTattvaEngine.ts`,
 * `tithiSwaraEngine.ts`, and `karanaTattvaEngine.ts`, enhanced with:
 * - Complete 7-Vara, 30-Tithi (Shukla 1..15 & Krishna 1..15), and 11-Karana Master Reference Tables
 * - Tithi Start & End Tattva/Nadi panels + 60-minute alternating Nadi
 * - Daily Half-Tithi Karana Tattva Table with Paksha, Tithi, Nadi, Tattva, and Bhadra/Vishti caution
 * - 6-Month Macro Chaitra–Sharad Karana Tattva Cycle with Previous/Current/Next cycle switcher
 * - 4-Pillar Tattva Alignment Report (Peak Harmony Hours across all 120 daily Sub-Tattva windows)
 */
object PanchaTattvaEngine {

    private val cycleElements = listOf(
        TattvaElement.AKASH,
        TattvaElement.VAYU,
        TattvaElement.TEJAS,
        TattvaElement.PRITHVI,
        TattvaElement.JALA
    )
    private val cycleWeights = listOf(1, 2, 3, 5, 4)

    fun compute(
        snapshot: PanchangaSnapshot,
        dailyHoras: DailyHoras,
        nowMs: Long,
        cycleOffset: Int = 0
    ): PanchaTattvaReport {
        val varaInfo = varaTattva(snapshot.weekday)

        val dayTithiTattvas = snapshot.tithi.map { seg ->
            val rawNum = seg.number.coerceIn(1, 30)
            val isShukla = rawNum <= 15
            val pakshaTithi = if (isShukla) rawNum else (rawNum - 15).coerceIn(1, 15)
            val pakshaLabel = if (isShukla) "Śukla" else "Kṛṣṇa"
            val el = tithiElementFor(rawNum)
            val (grpEn, grpHi) = tithiGroupNameFor(pakshaTithi)
            val startNadi = seg.swaraInfo?.startNadi ?: tithiStartNadiFor(isShukla, pakshaTithi)
            val endNadi = seg.swaraInfo?.endNadi ?: oppositeNadi(startNadi)
            val s = seg.startTimeMs ?: Long.MIN_VALUE
            val e = seg.endTimeMs ?: Long.MAX_VALUE
            val altNadi = computeAlternatingNadi(startNadi, seg.startTimeMs, nowMs)
            TithiTattvaPeriodItem(
                number = rawNum,
                tithiInPaksha = pakshaTithi,
                name = seg.name,
                paksha = pakshaLabel,
                groupNameEn = grpEn,
                groupNameHi = grpHi,
                element = el,
                endElement = el,
                nadi = startNadi,
                endNadi = endNadi,
                alternatingNadi = altNadi,
                starts = seg.starts,
                ends = seg.ends,
                startTimeMs = seg.startTimeMs,
                endTimeMs = seg.endTimeMs,
                isActive = nowMs in s until e
            )
        }

        val activeTithiPeriod = dayTithiTattvas.firstOrNull { it.isActive }
            ?: dayTithiTattvas.firstOrNull {
                val s = it.startTimeMs ?: Long.MIN_VALUE
                val e = it.endTimeMs ?: Long.MAX_VALUE
                nowMs in s..e
            }
            ?: dayTithiTattvas.firstOrNull()

        val rawTithiNum = activeTithiPeriod?.number ?: 1
        val isShuklaActive = rawTithiNum <= 15
        val pakshaTithiActive = if (isShuklaActive) rawTithiNum else (rawTithiNum - 15).coerceIn(1, 15)
        val tithiElement = activeTithiPeriod?.element ?: tithiElementFor(rawTithiNum)
        val tithiStartNadi = activeTithiPeriod?.nadi ?: tithiStartNadiFor(isShuklaActive, pakshaTithiActive)
        val tithiEndNadi = activeTithiPeriod?.endNadi ?: oppositeNadi(tithiStartNadi)
        val tithiAltNadi = activeTithiPeriod?.alternatingNadi
            ?: computeAlternatingNadi(tithiStartNadi, activeTithiPeriod?.startTimeMs, nowMs)

        val tithiInfo = TithiTattvaInfo(
            paksha = if (isShuklaActive) "Śukla" else "Kṛṣṇa",
            tithiNumber = rawTithiNum,
            tithiInPaksha = pakshaTithiActive,
            tithiName = activeTithiPeriod?.name ?: "Pratipadā",
            element = tithiElement,
            nadi = tithiStartNadi,
            endNadi = tithiEndNadi,
            alternatingNadi = tithiAltNadi,
            starts = activeTithiPeriod?.starts,
            ends = activeTithiPeriod?.ends,
            startTimeMs = activeTithiPeriod?.startTimeMs,
            endTimeMs = activeTithiPeriod?.endTimeMs
        )

        val dayKaranaTattvas = snapshot.karana.mapIndexed { idx, seg ->
            val kName = seg.name
            val kElement = karanaElementFor(kName)
            val s = seg.startTimeMs ?: Long.MIN_VALUE
            val e = seg.endTimeMs ?: Long.MAX_VALUE
            val midMs = when {
                seg.startTimeMs != null && seg.endTimeMs != null -> (seg.startTimeMs!! + seg.endTimeMs!!) / 2L
                seg.startTimeMs != null -> seg.startTimeMs!! + 3_600_000L
                seg.endTimeMs != null -> seg.endTimeMs!! - 3_600_000L
                else -> nowMs
            }
            val matchingTithi = dayTithiTattvas.firstOrNull { t ->
                val ts = t.startTimeMs ?: Long.MIN_VALUE
                val te = t.endTimeMs ?: Long.MAX_VALUE
                midMs in ts..te
            } ?: activeTithiPeriod ?: dayTithiTattvas.firstOrNull()

            val kNadi = karanaNadiFor(kName, matchingTithi?.nadi ?: tithiStartNadi, idx)
            val isBhadra = isBhadraKarana(kName)
            DailyKaranaTattvaItem(
                number = seg.number,
                karanaName = kName,
                paksha = matchingTithi?.paksha ?: tithiInfo.paksha,
                tithiNumber = matchingTithi?.tithiInPaksha ?: tithiInfo.tithiInPaksha,
                tithiName = matchingTithi?.name ?: tithiInfo.tithiName,
                element = kElement,
                nadi = kNadi,
                starts = seg.starts,
                ends = seg.ends,
                startTimeMs = seg.startTimeMs,
                endTimeMs = seg.endTimeMs,
                isBhadraCaution = isBhadra,
                isActive = nowMs in s until e
            )
        }

        val activeKaranaItem = dayKaranaTattvas.firstOrNull { it.isActive }
            ?: dayKaranaTattvas.firstOrNull {
                val s = it.startTimeMs ?: Long.MIN_VALUE
                val e = it.endTimeMs ?: Long.MAX_VALUE
                nowMs in s..e
            }
            ?: dayKaranaTattvas.firstOrNull()

        val karanaName = activeKaranaItem?.karanaName ?: "Bava"
        val karanaElement = activeKaranaItem?.element ?: karanaElementFor(karanaName)
        val karanaNadi = activeKaranaItem?.nadi ?: tithiStartNadi
        val karanaInfo = KaranaTattvaInfo(
            karanaName = karanaName,
            element = karanaElement,
            nadi = karanaNadi,
            starts = activeKaranaItem?.starts,
            ends = activeKaranaItem?.ends,
            isBhadraCaution = activeKaranaItem?.isBhadraCaution ?: isBhadraKarana(karanaName)
        )

        val activeHora = dailyHoras.activeHora ?: dailyHoras.horas.firstOrNull()
        val activeHoraTattva = dailyHoras.activeTattva ?: activeHora?.tattvas?.firstOrNull()

        val karanaCycle = runCatching {
            computeKaranaTattvaCycle(
                targetDateMs = nowMs,
                ayanamsaKey = snapshot.ayanamshaKey,
                cycleOffset = cycleOffset
            )
        }.getOrNull()

        val macroElement = karanaCycle?.activePeriod?.element ?: karanaInfo.element

        val counts = mutableMapOf<TattvaElement, Int>()
        listOfNotNull(
            varaInfo.element,
            tithiInfo.element,
            macroElement,
            karanaInfo.element,
            activeHoraTattva?.element,
            activeHoraTattva?.element
        ).forEach { el -> counts[el] = (counts[el] ?: 0) + 1 }

        val dominant = counts.maxByOrNull { it.value }?.key ?: TattvaElement.PRITHVI
        val (guidanceEn, guidanceHi) = elementGuidance(dominant)

        // Compute 4-Pillar Tattva Alignment Windows across all 24 Horas × 5 Sub-Tattvas
        val harmonyWindows = computeHarmonyWindows(
            dailyHoras = dailyHoras,
            varaElement = varaInfo.element,
            dayTithiTattvas = dayTithiTattvas,
            defaultTithiElement = tithiInfo.element,
            dayKaranaTattvas = dayKaranaTattvas,
            macroKaranaElement = macroElement,
            nowMs = nowMs
        )

        return PanchaTattvaReport(
            varaTattva = varaInfo,
            tithiTattva = tithiInfo,
            karanaTattva = karanaInfo,
            activeHoraRuler = activeHora?.ruler,
            activeHoraTattva = activeHoraTattva,
            dominantElement = dominant,
            guidanceEn = guidanceEn,
            guidanceHi = guidanceHi,
            dayTithiTattvas = dayTithiTattvas,
            dayKaranaTattvas = dayKaranaTattvas,
            karanaCycle = karanaCycle,
            harmonyWindows = harmonyWindows,
            varaMasterTable = buildVaraMasterTable(snapshot.weekday),
            tithiMasterTable = buildTithiMasterTable(rawTithiNum),
            karanaMasterTable = buildKaranaMasterTable(karanaName)
        )
    }

    private fun computeHarmonyWindows(
        dailyHoras: DailyHoras,
        varaElement: TattvaElement,
        dayTithiTattvas: List<TithiTattvaPeriodItem>,
        defaultTithiElement: TattvaElement,
        dayKaranaTattvas: List<DailyKaranaTattvaItem>,
        macroKaranaElement: TattvaElement,
        nowMs: Long
    ): List<TattvaHarmonyWindow> {
        val windows = mutableListOf<TattvaHarmonyWindow>()
        dailyHoras.horas.forEach { hora ->
            hora.tattvas.forEach { sub ->
                val mid = (sub.startTimeMs + sub.endTimeMs) / 2L
                val tithiElAtSlot = dayTithiTattvas.firstOrNull { t ->
                    val s = t.startTimeMs ?: Long.MIN_VALUE
                    val e = t.endTimeMs ?: Long.MAX_VALUE
                    mid in s..e
                }?.element ?: defaultTithiElement

                val dailyKaranaElAtSlot = dayKaranaTattvas.firstOrNull { k ->
                    val s = k.startTimeMs ?: Long.MIN_VALUE
                    val e = k.endTimeMs ?: Long.MAX_VALUE
                    mid in s..e
                }?.element

                val matchedEn = mutableListOf("Hora Tattva")
                val matchedHi = mutableListOf("होरा तत्व")

                if (sub.element == varaElement) {
                    matchedEn += "Vara Tattva"
                    matchedHi += "वार तत्व"
                }
                if (sub.element == tithiElAtSlot) {
                    matchedEn += "Tithi Tattva"
                    matchedHi += "तिथि तत्व"
                }
                if (sub.element == macroKaranaElement || sub.element == dailyKaranaElAtSlot) {
                    matchedEn += "Karana Tattva"
                    matchedHi += "करण तत्व"
                }

                if (matchedEn.size >= 2) {
                    windows += TattvaHarmonyWindow(
                        horaIndex = hora.index,
                        horaRuler = hora.ruler,
                        isDay = hora.isDay,
                        element = sub.element,
                        nadi = hora.nadi,
                        startTime = sub.startTime,
                        endTime = sub.endTime,
                        startTimeMs = sub.startTimeMs,
                        endTimeMs = sub.endTimeMs,
                        durationMins = sub.durationMins,
                        matchedPillarsCount = matchedEn.size,
                        matchedPillarsEn = matchedEn.joinToString(" + "),
                        matchedPillarsHi = matchedHi.joinToString(" + "),
                        isActive = nowMs in sub.startTimeMs until sub.endTimeMs
                    )
                }
            }
        }
        return windows.sortedWith(
            compareByDescending<TattvaHarmonyWindow> { it.isActive }
                .thenByDescending { it.matchedPillarsCount }
                .thenBy { it.startTimeMs }
        )
    }

    private fun computeAlternatingNadi(
        startNadi: SwaraNadi,
        startTimeMs: Long?,
        nowMs: Long
    ): SwaraNadi {
        if (startNadi == SwaraNadi.SUSHUMNA) return SwaraNadi.SUSHUMNA
        val baseStart = startTimeMs ?: return startNadi
        if (nowMs <= baseStart) return startNadi
        val elapsedHours = ((nowMs - baseStart) / 3_600_000L).coerceAtLeast(0L)
        return if (elapsedHours % 2L == 0L) startNadi else oppositeNadi(startNadi)
    }

    private fun oppositeNadi(nadi: SwaraNadi): SwaraNadi = when (nadi) {
        SwaraNadi.IDA -> SwaraNadi.PINGALA
        SwaraNadi.PINGALA -> SwaraNadi.IDA
        SwaraNadi.SUSHUMNA -> SwaraNadi.SUSHUMNA
    }

    private fun tithiStartNadiFor(isShukla: Boolean, pakshaTithi: Int): SwaraNadi {
        val triadIndex = ((pakshaTithi.coerceIn(1, 15) - 1) / 3) // 0: 1-3, 1: 4-6, 2: 7-9, 3: 10-12, 4: 13-15
        val isEvenTriad = triadIndex % 2 == 0
        return if (isShukla) {
            if (isEvenTriad) SwaraNadi.IDA else SwaraNadi.PINGALA
        } else {
            if (isEvenTriad) SwaraNadi.PINGALA else SwaraNadi.IDA
        }
    }

    private fun tithiGroupNameFor(pakshaTithi: Int): Pair<String, String> = when (pakshaTithi) {
        1, 6, 11 -> "Nandā (Joy)" to "नन्दा (आनन्द)"
        2, 7, 12 -> "Bhadrā (Auspicious)" to "भद्रा (कल्याण)"
        3, 8, 13 -> "Jayā (Victory)" to "जया (विजय)"
        4, 9, 14 -> "Riktā (Empty/Caution)" to "रिक्ता (संयम)"
        else -> "Pūrṇā (Fulfillment)" to "पूर्णा (पूर्णता)"
    }

    private fun tithiElementFor(rawTithiNum: Int): TattvaElement {
        val pakshaTithi = if (rawTithiNum > 15) rawTithiNum - 15 else rawTithiNum.coerceIn(1, 15)
        return when (pakshaTithi) {
            1, 2, 3, 14, 15 -> TattvaElement.PRITHVI
            4, 5, 6, 7 -> TattvaElement.JALA
            8 -> TattvaElement.AKASH
            9, 10 -> TattvaElement.VAYU
            11, 12, 13 -> TattvaElement.TEJAS
            else -> TattvaElement.PRITHVI
        }
    }

    private fun karanaElementFor(karanaName: String): TattvaElement = when {
        karanaName.contains("Bava", true) || karanaName.contains("Kaulava", true) || karanaName.contains("Kimstughna", true) || karanaName.contains("Kiṃstughna", true) -> TattvaElement.PRITHVI
        karanaName.contains("Balava", true) || karanaName.contains("Bālava", true) || karanaName.contains("Gara", true) || karanaName.contains("Garaja", true) || karanaName.contains("Chatushpada", true) || karanaName.contains("Catuṣpāda", true) -> TattvaElement.JALA
        karanaName.contains("Taitila", true) || karanaName.contains("Vishti", true) || karanaName.contains("Viṣṭi", true) || karanaName.contains("Bhadra", true) -> TattvaElement.TEJAS
        karanaName.contains("Vanija", true) || karanaName.contains("Vaṇija", true) || karanaName.contains("Shakuni", true) || karanaName.contains("Śakuni", true) -> TattvaElement.VAYU
        else -> TattvaElement.AKASH
    }

    private fun isBhadraKarana(karanaName: String): Boolean =
        karanaName.contains("Vishti", true) || karanaName.contains("Viṣṭi", true) || karanaName.contains("Bhadra", true)

    private fun karanaNadiFor(karanaName: String, tithiStartNadi: SwaraNadi, halfIndex: Int): SwaraNadi {
        if (isBhadraKarana(karanaName)) return SwaraNadi.SUSHUMNA
        return if (halfIndex % 2 == 0) tithiStartNadi else oppositeNadi(tithiStartNadi)
    }

    /**
     * Exact port of `karanaTattvaEngine.ts`: 6-month Pratipada-to-Pratipada cycle
     * weighted 1:2:3:5:4 across Akasha, Vayu, Agni, Prithvi, Jala with lunar Maas overlaps.
     * Supports `cycleOffset` (-1 = Previous 6-month cycle, 0 = Current cycle, +1 = Next 6-month cycle).
     */
    fun computeKaranaTattvaCycle(
        targetDateMs: Long,
        ayanamsaKey: CoordinateSelection,
        cycleOffset: Int = 0
    ): KaranaTattvaCycleReport? {
        fun masaAtNm(nmTime: Time): Pair<Int, String> {
            val ayanDeg = AyanamshaAndSolvers.calculateAyanamsha(nmTime, ayanamsaKey)
            val sunSid = AyanamshaAndSolvers.getSiderealLon(AyanamshaAndSolvers.getTropicalSunLon(nmTime), ayanDeg)
            val rasi = floor(sunSid / 30.0).toInt().coerceIn(0, 11)
            val amantaMasaNum = ((rasi + 2) % 12).let { if (it == 0) 12 else it }
            val name = SanskritNamesCatalog.masas[amantaMasaNum] ?: "Caitra"
            return amantaMasaNum to name
        }

        val shiftedTargetMs = targetDateMs + (cycleOffset.coerceIn(-2, 2) * 177L * 86_400_000L)
        var cursorMs = shiftedTargetMs
        var startBoundaryMs: Long? = null
        var startMasaNum = 1
        for (i in 0 until 15) {
            val nm = searchMoonPhase(0.0, Time.fromMillisecondsSince1970(cursorMs - 32L * 86_400_000L), 33.0) ?: break
            val (masaNum, _) = masaAtNm(nm)
            if (masaNum == 1 || masaNum == 7) {
                var earliest = nm
                var checkMs = nm.toMillisecondsSince1970() - 5L * 86_400_000L
                for (j in 0 until 2) {
                    val prev = searchMoonPhase(0.0, Time.fromMillisecondsSince1970(checkMs - 32L * 86_400_000L), 33.0) ?: break
                    if (masaAtNm(prev).first == masaNum) {
                        earliest = prev
                        checkMs = prev.toMillisecondsSince1970() - 5L * 86_400_000L
                    } else break
                }
                startBoundaryMs = earliest.toMillisecondsSince1970()
                startMasaNum = masaNum
                break
            }
            cursorMs = nm.toMillisecondsSince1970() - 5L * 86_400_000L
        }

        val cStart = startBoundaryMs ?: return null
        val targetMasa = if (startMasaNum == 1) 7 else 1
        var forwardMs = cStart + 5L * 86_400_000L
        var endBoundaryMs: Long? = null
        for (i in 0 until 15) {
            val nm = searchMoonPhase(0.0, Time.fromMillisecondsSince1970(forwardMs), 33.0) ?: break
            if (masaAtNm(nm).first == targetMasa) {
                endBoundaryMs = nm.toMillisecondsSince1970()
                break
            }
            forwardMs = nm.toMillisecondsSince1970() + 5L * 86_400_000L
        }
        val cEnd = endBoundaryMs ?: return null
        val totalDur = (cEnd - cStart).coerceAtLeast(1L)
        val totalDays = (totalDur / 86_400_000L).toInt()
        val totalRemHours = ((totalDur % 86_400_000L) / 3_600_000L).toInt()
        val cycleType = if (startMasaNum == 1) "CHAITRA_TO_SHARAD" else "SHARAD_TO_CHAITRA"

        val periods = mutableListOf<KaranaTattvaPeriodItem>()
        var periodStart = cStart
        for (i in 0 until 5) {
            val w = cycleWeights[i]
            val dur = if (i == 4) (cEnd - periodStart) else (totalDur * w / 15L)
            val periodEnd = periodStart + dur
            val pDays = (dur / 86_400_000L).toInt()
            val pHours = ((dur % 86_400_000L) / 3_600_000L).toInt()
            val overlaps = computeMaasOverlaps(cycleElements[i], periodStart, periodEnd, dur, ayanamsaKey)
            val isPeriodActive = targetDateMs in periodStart until periodEnd
            periods += KaranaTattvaPeriodItem(
                element = cycleElements[i],
                weight = w,
                percentage = (w * 100.0) / 15.0,
                startTimeMs = periodStart,
                endTimeMs = periodEnd,
                durationDays = pDays,
                durationHours = pHours,
                isActive = isPeriodActive,
                maasOverlaps = overlaps
            )
            periodStart = periodEnd
        }

        val active = periods.firstOrNull { it.isActive } ?: periods.firstOrNull()
        return KaranaTattvaCycleReport(
            cycleType = cycleType,
            cycleOffset = cycleOffset,
            startTimeMs = cStart,
            endTimeMs = cEnd,
            totalDurationDays = totalDays,
            totalDurationHours = totalRemHours,
            activePeriod = active,
            periods = periods
        )
    }

    private fun computeMaasOverlaps(
        element: TattvaElement,
        pStart: Long,
        pEnd: Long,
        pDurationMs: Long,
        ayanamsaKey: CoordinateSelection
    ): List<KaranaTattvaMaasOverlapItem> {
        val overlaps = mutableListOf<KaranaTattvaMaasOverlapItem>()
        var curNm = searchMoonPhase(0.0, Time.fromMillisecondsSince1970(pStart - 32L * 86_400_000L), 33.0) ?: return overlaps
        var curMs = curNm.toMillisecondsSince1970()
        var searchMs = curMs + 5L * 86_400_000L

        fun masaData(nm: Time): Pair<Int, String> {
            val ayanDeg = AyanamshaAndSolvers.calculateAyanamsha(nm, ayanamsaKey)
            val sunSid = AyanamshaAndSolvers.getSiderealLon(AyanamshaAndSolvers.getTropicalSunLon(nm), ayanDeg)
            val rasi = floor(sunSid / 30.0).toInt().coerceIn(0, 11)
            val num = ((rasi + 2) % 12).let { if (it == 0) 12 else it }
            return num to (SanskritNamesCatalog.masas[num] ?: "Caitra")
        }

        while (curMs < pEnd) {
            val nextNm = searchMoonPhase(0.0, Time.fromMillisecondsSince1970(searchMs), 33.0) ?: break
            val nextMs = nextNm.toMillisecondsSince1970()
            val ovStart = max(pStart, curMs)
            val ovEnd = min(pEnd, nextMs)
            if (ovStart < ovEnd) {
                val (mNum, mName) = masaData(curNm)
                val (nextNum, _) = masaData(nextNm)
                val isAdhika = mNum == nextNum
                val ovDur = ovEnd - ovStart
                overlaps += KaranaTattvaMaasOverlapItem(
                    element = element,
                    maasName = if (isAdhika) "Adhika $mName" else mName,
                    isAdhika = isAdhika,
                    startTimeMs = ovStart,
                    endTimeMs = ovEnd,
                    overlapPercent = ((ovDur * 100.0) / pDurationMs.coerceAtLeast(1L)).roundToInt()
                )
            }
            curNm = nextNm
            curMs = nextMs
            searchMs = curMs + 5L * 86_400_000L
        }
        return overlaps
    }

    private fun buildVaraMasterTable(todayWeekday: Int): List<VaraMasterRuleItem> = listOf(
        VaraMasterRuleItem(
            weekday = 1,
            dayNameEn = "Monday (Somavāra)",
            dayNameHi = "सोमवार (चन्द्र)",
            rulerEn = "Moon",
            rulerHi = "चन्द्र",
            element = TattvaElement.JALA,
            elementLabelEn = "Water (Jala)",
            elementLabelHi = "जल तत्व",
            nadi = SwaraNadi.IDA,
            isToday = todayWeekday == 1
        ),
        VaraMasterRuleItem(
            weekday = 2,
            dayNameEn = "Tuesday (Maṅgalavāra)",
            dayNameHi = "मंगलवार (मंगल)",
            rulerEn = "Mars",
            rulerHi = "मंगल",
            element = TattvaElement.TEJAS,
            elementLabelEn = "Fire (Agni)",
            elementLabelHi = "अग्नि तत्व",
            nadi = SwaraNadi.IDA,
            isToday = todayWeekday == 2
        ),
        VaraMasterRuleItem(
            weekday = 3,
            dayNameEn = "Wednesday (Budhavāra)",
            dayNameHi = "बुधवार (बुध)",
            rulerEn = "Mercury",
            rulerHi = "बुध",
            element = TattvaElement.PRITHVI,
            elementLabelEn = "Earth (Pṛthvī)",
            elementLabelHi = "पृथ्वी तत्व",
            nadi = SwaraNadi.IDA,
            isToday = todayWeekday == 3
        ),
        VaraMasterRuleItem(
            weekday = 4,
            dayNameEn = "Thursday (Guruvāra)",
            dayNameHi = "गुरुवार (बृहस्पति)",
            rulerEn = "Jupiter",
            rulerHi = "गुरु",
            element = TattvaElement.AKASH,
            elementLabelEn = "Space then Air (Ākāśa / Vāyu)",
            elementLabelHi = "आकाश एवं वायु तत्व",
            nadi = SwaraNadi.SUSHUMNA,
            isToday = todayWeekday == 4
        ),
        VaraMasterRuleItem(
            weekday = 5,
            dayNameEn = "Friday (Śukravāra)",
            dayNameHi = "शुक्रवार (शुक्र)",
            rulerEn = "Venus",
            rulerHi = "शुक्र",
            element = TattvaElement.TEJAS,
            elementLabelEn = "Fire (Agni)",
            elementLabelHi = "अग्नि तत्व",
            nadi = SwaraNadi.PINGALA,
            isToday = todayWeekday == 5
        ),
        VaraMasterRuleItem(
            weekday = 6,
            dayNameEn = "Saturday (Śanivāra)",
            dayNameHi = "शनिवार (शनि)",
            rulerEn = "Saturn",
            rulerHi = "शनि",
            element = TattvaElement.JALA,
            elementLabelEn = "Water (Jala)",
            elementLabelHi = "जल तत्व",
            nadi = SwaraNadi.PINGALA,
            isToday = todayWeekday == 6
        ),
        VaraMasterRuleItem(
            weekday = 0,
            dayNameEn = "Sunday (Ravivāra)",
            dayNameHi = "रविवार (सूर्य)",
            rulerEn = "Sun",
            rulerHi = "सूर्य",
            element = TattvaElement.PRITHVI,
            elementLabelEn = "Earth (Pṛthvī)",
            elementLabelHi = "पृथ्वी तत्व",
            nadi = SwaraNadi.PINGALA,
            isToday = todayWeekday == 0
        )
    )

    private val tithiNamesEn = listOf(
        "Pratipadā", "Dvitīyā", "Tṛtīyā", "Caturthī", "Pañcamī",
        "Ṣaṣṭhī", "Saptamī", "Aṣṭamī", "Navamī", "Daśamī",
        "Ekādaśī", "Dvādaśī", "Trayodaśī", "Caturdaśī", "Pūrṇimā / Amāvasyā"
    )

    private val tithiNamesHi = listOf(
        "प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पंचमी",
        "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी",
        "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "पूर्णिमा / अमावस्या"
    )

    private fun buildTithiMasterTable(activeRawTithiNumber: Int): List<TithiMasterRuleItem> {
        return (1..30).map { rawNum ->
            val isShukla = rawNum <= 15
            val pakshaTithi = if (isShukla) rawNum else rawNum - 15
            val nameEn = if (rawNum == 15) "Pūrṇimā" else if (rawNum == 30) "Amāvasyā" else tithiNamesEn[pakshaTithi - 1]
            val nameHi = if (rawNum == 15) "पूर्णिमा" else if (rawNum == 30) "अमावस्या" else tithiNamesHi[pakshaTithi - 1]
            val (grpEn, grpHi) = tithiGroupNameFor(pakshaTithi)
            val el = tithiElementFor(rawNum)
            val startNadi = tithiStartNadiFor(isShukla, pakshaTithi)
            val endNadi = oppositeNadi(startNadi)
            TithiMasterRuleItem(
                rawNumber = rawNum,
                tithiInPaksha = pakshaTithi,
                pakshaEn = if (isShukla) "Śukla Pakṣa" else "Kṛṣṇa Pakṣa",
                pakshaHi = if (isShukla) "शुक्ल पक्ष" else "कृष्ण पक्ष",
                tithiNameEn = nameEn,
                tithiNameHi = nameHi,
                groupNameEn = grpEn,
                groupNameHi = grpHi,
                element = el,
                startNadi = startNadi,
                endNadi = endNadi,
                isActiveToday = rawNum == activeRawTithiNumber
            )
        }
    }

    private fun buildKaranaMasterTable(activeKaranaName: String): List<KaranaMasterRuleItem> = listOf(
        KaranaMasterRuleItem(
            number = 1,
            nameEn = "Bava",
            nameHi = "बव",
            categoryEn = "Chara (Movable)",
            categoryHi = "चर करण",
            rulerEn = "Indra / Sun",
            rulerHi = "इन्द्र / सूर्य",
            element = TattvaElement.PRITHVI,
            defaultNadi = SwaraNadi.PINGALA,
            effectEn = "Auspicious for health, construction, beginnings, and steady progress.",
            effectHi = "स्वास्थ्य, निर्माण, शुभ आरम्भ और स्थायी प्रगति के लिए उत्तम।",
            isBhadraCaution = false,
            isActiveToday = activeKaranaName.contains("Bava", true)
        ),
        KaranaMasterRuleItem(
            number = 2,
            nameEn = "Bālava",
            nameHi = "बालव",
            categoryEn = "Chara (Movable)",
            categoryHi = "चर करण",
            rulerEn = "Brahmā / Moon",
            rulerHi = "ब्रह्मा / चन्द्र",
            element = TattvaElement.JALA,
            defaultNadi = SwaraNadi.IDA,
            effectEn = "Favorable for learning, sacred rites, charity, and social harmony.",
            effectHi = "अध्ययन, धार्मिक संस्कार, दान और सामाजिक सौहार्द के लिए शुभ।",
            isBhadraCaution = false,
            isActiveToday = activeKaranaName.contains("Balava", true) || activeKaranaName.contains("Bālava", true)
        ),
        KaranaMasterRuleItem(
            number = 3,
            nameEn = "Kaulava",
            nameHi = "कौलव",
            categoryEn = "Chara (Movable)",
            categoryHi = "चर करण",
            rulerEn = "Mitra / Mars",
            rulerHi = "मित्र / मंगल",
            element = TattvaElement.PRITHVI,
            defaultNadi = SwaraNadi.IDA,
            effectEn = "Best for friendship, marriage alliances, diplomacy, and agreements.",
            effectHi = "मित्रता, विवाह सम्बन्ध, समझौते और साझेदारी के लिए श्रेष्ठ।",
            isBhadraCaution = false,
            isActiveToday = activeKaranaName.contains("Kaulava", true)
        ),
        KaranaMasterRuleItem(
            number = 4,
            nameEn = "Taitila",
            nameHi = "तैतिल",
            categoryEn = "Chara (Movable)",
            categoryHi = "चर करण",
            rulerEn = "Aryamā / Mercury",
            rulerHi = "अर्यमा / बुध",
            element = TattvaElement.TEJAS,
            defaultNadi = SwaraNadi.PINGALA,
            effectEn = "Supports wealth, housing, trade, leadership, and state affairs.",
            effectHi = "धन-सम्पत्ति, गृह-कार्य, व्यापार और नेतृत्व के लिए अनुकूल।",
            isBhadraCaution = false,
            isActiveToday = activeKaranaName.contains("Taitila", true)
        ),
        KaranaMasterRuleItem(
            number = 5,
            nameEn = "Gara (Garaja)",
            nameHi = "गर (गरज)",
            categoryEn = "Chara (Movable)",
            categoryHi = "चर करण",
            rulerEn = "Bhūmi / Jupiter",
            rulerHi = "भूमि / गुरु",
            element = TattvaElement.JALA,
            defaultNadi = SwaraNadi.IDA,
            effectEn = "Ideal for agriculture, cultivation, nourishment, and housewarming.",
            effectHi = "कृषि, भूमि कर्म, पोषण और गृह-प्रवेश आदि के लिए शुभ।",
            isBhadraCaution = false,
            isActiveToday = activeKaranaName.contains("Gara", true)
        ),
        KaranaMasterRuleItem(
            number = 6,
            nameEn = "Vaṇija",
            nameHi = "वणिज",
            categoryEn = "Chara (Movable)",
            categoryHi = "चर करण",
            rulerEn = "Lakṣmī / Venus",
            rulerHi = "लक्ष्मी / शुक्र",
            element = TattvaElement.VAYU,
            defaultNadi = SwaraNadi.IDA,
            effectEn = "Excellent for commerce, banking, buying/selling, and travel.",
            effectHi = "व्यापार, वाणिज्य, क्रय-विक्रय और आर्थिक लेन-देन के लिए उत्तम।",
            isBhadraCaution = false,
            isActiveToday = activeKaranaName.contains("Vanija", true) || activeKaranaName.contains("Vaṇija", true)
        ),
        KaranaMasterRuleItem(
            number = 7,
            nameEn = "Viṣṭi (Bhadrā)",
            nameHi = "विष्टि (भद्रा)",
            categoryEn = "Chara (Caution)",
            categoryHi = "चर करण (भद्रा त्याज्य)",
            rulerEn = "Yama / Saturn",
            rulerHi = "यम / शनि",
            element = TattvaElement.TEJAS,
            defaultNadi = SwaraNadi.SUSHUMNA,
            effectEn = "Avoid new auspicious beginnings; suitable only for fierce/defensive tasks or sadhana.",
            effectHi = "शुभ व मांगलिक कार्यों में त्याज्य; केवल उग्र कर्म, शत्रु-निवारण व तांत्रिक साधना हेतु।",
            isBhadraCaution = true,
            isActiveToday = isBhadraKarana(activeKaranaName)
        ),
        KaranaMasterRuleItem(
            number = 8,
            nameEn = "Śakuni",
            nameHi = "शकुनि",
            categoryEn = "Sthira (Fixed · Kṛṣṇa 14 2nd half)",
            categoryHi = "स्थिर करण (कृष्ण १४ उत्तरार्ध)",
            rulerEn = "Kālī / Rahu",
            rulerHi = "काली / राहु",
            element = TattvaElement.VAYU,
            defaultNadi = SwaraNadi.PINGALA,
            effectEn = "Favors medicine, mantra remedies, healing herbs, and quiet reflection.",
            effectHi = "औषधि निर्माण, मंत्र-उपाय, चिकित्सा और एकान्त चिन्तन के लिए उपयुक्त।",
            isBhadraCaution = false,
            isActiveToday = activeKaranaName.contains("Shakuni", true) || activeKaranaName.contains("Śakuni", true)
        ),
        KaranaMasterRuleItem(
            number = 9,
            nameEn = "Catuṣpāda",
            nameHi = "चतुष्पाद",
            categoryEn = "Sthira (Fixed · Amāvasyā 1st half)",
            categoryHi = "स्थिर करण (अमावस्या पूर्वार्ध)",
            rulerEn = "Vṛṣabha / Ancestors",
            rulerHi = "वृषभ / पितृ",
            element = TattvaElement.JALA,
            defaultNadi = SwaraNadi.IDA,
            effectEn = "Suited for cattle care, Pitri Tarpan, ancestral rites, and charity.",
            effectHi = "गौ-सेवा, पितृ तर्पण, श्राद्ध कर्म और दान-पुण्य के लिए अनुकूल।",
            isBhadraCaution = false,
            isActiveToday = activeKaranaName.contains("Chatushpada", true) || activeKaranaName.contains("Catuṣpāda", true)
        ),
        KaranaMasterRuleItem(
            number = 10,
            nameEn = "Nāga",
            nameHi = "नाग",
            categoryEn = "Sthira (Fixed · Amāvasyā 2nd half)",
            categoryHi = "स्थिर करण (अमावस्या उत्तरार्ध)",
            rulerEn = "Nāga / Ketu",
            rulerHi = "नाग / केतु",
            element = TattvaElement.AKASH,
            defaultNadi = SwaraNadi.SUSHUMNA,
            effectEn = "Best reserved for meditation, Kundalini sadhana, and silence.",
            effectHi = "ध्यान, कुण्डलिनी साधना, मौन और आध्यात्मिक जप के लिए उपयुक्त।",
            isBhadraCaution = false,
            isActiveToday = activeKaranaName.contains("Naga", true) || activeKaranaName.contains("Nāga", true)
        ),
        KaranaMasterRuleItem(
            number = 11,
            nameEn = "Kiṃstughna",
            nameHi = "किंस्तुघ्न",
            categoryEn = "Sthira (Fixed · Śukla 1 1st half)",
            categoryHi = "स्थिर करण (शुक्ल १ पूर्वार्ध)",
            rulerEn = "Kubera / Maruts",
            rulerHi = "कुबेर / मरुत्",
            element = TattvaElement.PRITHVI,
            defaultNadi = SwaraNadi.IDA,
            effectEn = "Auspicious for prosperity, purification, and fresh lunar-month vows.",
            effectHi = "सौभाग्य, शुद्धि, समृद्धि और नए चन्द्र-मास के शुभ संकल्पों हेतु उत्तम।",
            isBhadraCaution = false,
            isActiveToday = activeKaranaName.contains("Kimstughna", true) || activeKaranaName.contains("Kiṃstughna", true)
        )
    )

    private fun varaTattva(weekday: Int): VaraTattvaInfo = when (weekday) {
        1 -> VaraTattvaInfo(1, TattvaElement.JALA, "Water / Jala", "जल तत्व", SwaraNadi.IDA)
        2 -> VaraTattvaInfo(2, TattvaElement.TEJAS, "Fire / Agni", "अग्नि तत्व", SwaraNadi.IDA)
        3 -> VaraTattvaInfo(3, TattvaElement.PRITHVI, "Earth / Prithvi", "पृथ्वी तत्व", SwaraNadi.IDA)
        4 -> VaraTattvaInfo(4, TattvaElement.AKASH, "Space then Air", "आकाश एवं वायु तत्व", SwaraNadi.SUSHUMNA)
        5 -> VaraTattvaInfo(5, TattvaElement.TEJAS, "Fire / Agni", "अग्नि तत्व", SwaraNadi.PINGALA)
        6 -> VaraTattvaInfo(6, TattvaElement.JALA, "Water / Jala", "जल तत्व", SwaraNadi.PINGALA)
        else -> VaraTattvaInfo(0, TattvaElement.PRITHVI, "Earth / Prithvi", "पृथ्वी तत्व", SwaraNadi.PINGALA)
    }

    private fun elementGuidance(element: TattvaElement): Pair<String, String> = when (element) {
        TattvaElement.PRITHVI ->
            "Prithvi (Earth) predominates: ideal for long-term foundations, property, financial agreements, steady work, and lasting commitments." to
                "पृथ्वी तत्व प्रधान है: स्थायी कार्य, भूमि-भवन, व्यापारिक अनुबंध, निवेश और दीर्घकालिक संकल्पों के लिए अति उत्तम समय।"
        TattvaElement.JALA ->
            "Jala (Water) predominates: favors harmonious relationships, creativity, healing, hospitality, travel, and smooth financial flow." to
                "जल तत्व प्रधान है: सौहार्द, कला, चिकित्सा, यात्रा, नए सम्बन्ध और सुगम आर्थिक लेन-देन के लिए श्रेष्ठ समय।"
        TattvaElement.TEJAS ->
            "Agni/Tejas (Fire) predominates: channels high willpower, discipline, leadership, and overcoming hurdles—avoid anger or impulsive disputes." to
                "अग्नि (तेजस्) तत्व प्रधान है: साहस, नेतृत्व, साधना व कठिन कार्यों में सफलता मिलती है; क्रोध और जल्दबाज़ी से बचें।"
        TattvaElement.VAYU ->
            "Vayu (Air) predominates: supports movement, communication, study, networking, and dynamic short-term initiatives." to
                "वायु तत्व प्रधान है: यात्रा, संवाद, अध्ययन, संचार और गतिशील कार्यों के लिए अनुकूल समय।"
        TattvaElement.AKASH ->
            "Akasha (Space) predominates: supreme for meditation, mantra japa, yoga, spiritual contemplation, and inner clarity; avoid purely materialistic haste." to
                "आकाश तत्व प्रधान है: ध्यान, मंत्र-जप, योग, आध्यात्मिक चिन्तन एवं आत्मिक शान्ति के लिए सर्वोत्तम समय।"
    }
}
