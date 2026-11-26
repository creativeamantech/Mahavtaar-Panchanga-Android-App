package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.GocharPlanetEffect
import com.mahavtaar.panchanga.domain.model.GocharReport
import com.mahavtaar.panchanga.domain.model.KundliDoshaSummary
import com.mahavtaar.panchanga.domain.model.KundliHouseEntry
import com.mahavtaar.panchanga.domain.model.KundliPlanetEntry
import com.mahavtaar.panchanga.domain.model.KundliYogaSummary
import io.github.cosinekitty.astronomy.Time
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.floor

/**
 * Classical 13 Parashari & Phaladeepika Yogas with structured match/failure evidence & cancellations,
 * plus exact Gochara transit + Vedha + Sade Sati calculator matching `RuleMetadata.ts` and `gocharaEngine.ts`.
 */
object KundliRulesAndGocharEngine {

    private val planetSpecs = listOf(
        Triple("sun", "Sun", "सूर्य"),
        Triple("moon", "Moon", "चन्द्र"),
        Triple("mars", "Mars", "मङ्गल"),
        Triple("mercury", "Mercury", "बुध"),
        Triple("jupiter", "Jupiter", "बृहस्पति"),
        Triple("venus", "Venus", "शुक्र"),
        Triple("saturn", "Saturn", "शनि"),
        Triple("rahu", "Rahu", "राहु"),
        Triple("ketu", "Ketu", "केतु")
    )

    private val favorableHouses = mapOf(
        "sun" to setOf(3, 6, 10, 11),
        "moon" to setOf(1, 3, 6, 7, 10, 11),
        "mars" to setOf(3, 6, 11),
        "mercury" to setOf(2, 4, 6, 8, 10, 11),
        "jupiter" to setOf(2, 5, 7, 9, 11),
        "venus" to setOf(1, 2, 3, 4, 5, 8, 9, 11, 12),
        "saturn" to setOf(3, 6, 11),
        "rahu" to setOf(3, 6, 11),
        "ketu" to setOf(3, 6, 11)
    )

    private val vedhaPairs = mapOf(
        "sun" to mapOf(3 to 9, 6 to 12, 10 to 4, 11 to 5),
        "moon" to mapOf(1 to 5, 3 to 9, 6 to 12, 7 to 2, 10 to 4, 11 to 8),
        "mars" to mapOf(3 to 12, 6 to 9, 11 to 5),
        "mercury" to mapOf(2 to 5, 4 to 3, 6 to 9, 8 to 1, 10 to 8, 11 to 12),
        "jupiter" to mapOf(2 to 12, 5 to 4, 7 to 3, 9 to 10, 11 to 8),
        "venus" to mapOf(1 to 8, 2 to 7, 3 to 1, 4 to 10, 5 to 9, 8 to 5, 9 to 11, 11 to 6, 12 to 3),
        "saturn" to mapOf(3 to 12, 6 to 9, 11 to 5)
    )

    private val signLordsId = listOf(
        "mars", "venus", "mercury", "moon", "sun", "mercury",
        "venus", "mars", "jupiter", "saturn", "saturn", "jupiter"
    )

    private val exaltSignByPlanet = mapOf(
        "sun" to 0, "moon" to 1, "mars" to 9, "mercury" to 5,
        "jupiter" to 3, "venus" to 11, "saturn" to 6
    )

    fun computeGocharForDate(
        natalMoonSignIndex: Int,
        natalLagnaSignIndex: Int,
        transitDate: LocalDate,
        ayanamsha: CoordinateSelection
    ): GocharReport {
        val ms = transitDate.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return computeGochar(natalMoonSignIndex, natalLagnaSignIndex, ayanamsha, ms, transitDate.toString())
    }

    fun computeGochar(
        natalMoonSignIndex: Int,
        natalLagnaSignIndex: Int,
        ayanamsha: CoordinateSelection,
        transitMs: Long,
        dateIso: String = java.time.Instant.ofEpochMilli(transitMs).atZone(ZoneOffset.UTC).toLocalDate().toString()
    ): GocharReport {
        val t = Time.fromMillisecondsSince1970(transitMs)
        val tNext = Time.fromMillisecondsSince1970(transitMs + 86_400_000L)

        val positions = planetSpecs.map { (id, en, hi) ->
            val lon = AyanamshaAndSolvers.getBodySiderealLongitude(id, t, ayanamsha)
            val sign = floor(lon / 30.0).toInt().coerceIn(0, 11)
            val deg = lon % 30.0
            val isRetro = when (id) {
                "rahu", "ketu" -> true
                "sun", "moon" -> false
                else -> {
                    val lonNext = AyanamshaAndSolvers.getBodySiderealLongitude(id, tNext, ayanamsha)
                    var d = lonNext - lon
                    if (d > 180.0) d -= 360.0
                    if (d < -180.0) d += 360.0
                    d < 0.0
                }
            }
            Quad(id, en, hi, sign, deg, isRetro)
        }

        val effects = positions.map { p ->
            val houseMoon = ((p.sign - natalMoonSignIndex + 12) % 12) + 1
            val houseLagna = ((p.sign - natalLagnaSignIndex + 12) % 12) + 1
            val rawFav = houseMoon in (favorableHouses[p.id] ?: emptySet())
            var hasVedha = false
            var vedhaEn: String? = null
            var vedhaHi: String? = null

            if (rawFav) {
                val obsHouse = vedhaPairs[p.id]?.get(houseMoon)
                if (obsHouse != null) {
                    val obsSign = (natalMoonSignIndex + obsHouse - 1) % 12
                    val blocker = positions.firstOrNull { it.id != p.id && it.id != "sun" && it.sign == obsSign }
                    if (blocker != null) {
                        hasVedha = true
                        vedhaEn = blocker.en
                        vedhaHi = blocker.hi
                    }
                }
            }

            val effectiveFav = rawFav && !hasVedha
            val verdictEn = when {
                rawFav && hasVedha -> "Benefic House $houseMoon (Obstructed by Vedha from $vedhaEn)"
                rawFav -> "Favorable transit in House $houseMoon from Moon"
                else -> "Challenging transit in House $houseMoon — patience advised"
            }
            val verdictHi = when {
                rawFav && hasVedha -> "चन्द्र से ${houseMoon}वें भाव में शुभ ($vedhaHi द्वारा वेध प्रभावित)"
                rawFav -> "चन्द्र राशि से ${houseMoon}वें भाव में शुभ एवं फलदायी गोचर"
                else -> "चन्द्र राशि से ${houseMoon}वें भाव में मध्यम / संघर्षपूर्ण गोचर"
            }

            GocharPlanetEffect(
                planetId = p.id,
                nameEn = p.en,
                nameHi = p.hi,
                transitSignIndex = p.sign,
                houseFromMoon = houseMoon,
                isFavorable = effectiveFav,
                verdictEn = verdictEn,
                verdictHi = verdictHi,
                transitDegreeInSign = p.deg,
                houseFromLagna = houseLagna,
                isRetrograde = p.isRetro,
                hasVedha = hasVedha,
                vedhaPlanetNameEn = vedhaEn,
                vedhaPlanetNameHi = vedhaHi
            )
        }

        val satHouse = effects.firstOrNull { it.planetId == "saturn" }?.houseFromMoon ?: 1
        val (inSade, pEn, pHi, dEn, dHi) = when (satHouse) {
            12 -> Quint(true, "Sade Sati — Rising Phase (12th)", "प्रथम चरण (उदय मान)",
                "Saturn transits the 12th from Moon. Watch expenses and travel discipline.",
                "शनि जन्म चन्द्रमा से १२वें भाव में गोचररत हैं। आर्थिक योजना और व्यय पर नियंत्रण रखें।")
            1 -> Quint(true, "Sade Sati — Peak Phase (Over Moon)", "द्वितीय चरण (शिखर काल)",
                "Saturn transits over Natal Moon. Cultivate meditation, calm focus & Hanuman Chalisa.",
                "शनि जन्म चन्द्रमा पर गोचररत हैं। मानसिक धैर्य, ध्यान एवं हनुमान चालीसा का पाठ हितकर है।")
            2 -> Quint(true, "Sade Sati — Setting Phase (2nd)", "तृतीय चरण (अस्त मान)",
                "Saturn transits the 2nd from Moon. Guard speech and consolidate savings.",
                "शनि जन्म चन्द्रमा से द्वितीय भाव में गोचररत हैं। वाणी संयम व संचित धन सुरक्षा आवश्यक है।")
            4 -> Quint(false, "Kantaka Shani — 4th Dhaiya", "कंटक शनि (चतुर्थ ढैय्या)",
                "Saturn in 4th from Moon highlights home harmony and mother's health care.",
                "शनि चतुर्थ भाव में गोचर कर रहे हैं। माता के स्वास्थ्य व गृह शांति का ध्यान रखें।")
            8 -> Quint(false, "Ashtama Shani — 8th Dhaiya", "अष्टम शनि (अष्टम ढैय्या)",
                "Saturn in 8th from Moon advises travel caution and regular Shani Stotra recitation.",
                "शनि अष्टम भाव में गोचररत हैं। वाहन सावधानी व नित्य शनि स्तोत्र पाठ करें।")
            else -> Quint(false, "No Sade Sati / Dhaiya", "साढ़े साती प्रभाव नहीं",
                "Saturn is outside Sade Sati and Dhaiya zones (House $satHouse from Moon).",
                "वर्तमान में शनि साढ़े साती या ढैय्या का प्रभाव नहीं है (शनि ${satHouse}वें भाव में)।")
        }

        return GocharReport(
            transitDateIso = dateIso,
            natalMoonSignIndex = natalMoonSignIndex,
            natalLagnaSignIndex = natalLagnaSignIndex,
            transits = effects,
            inSadeSati = inSade,
            sadeSatiPhaseEn = pEn,
            sadeSatiPhaseHi = pHi,
            sadeSatiDescEn = dEn,
            sadeSatiDescHi = dHi
        )
    }

    fun computeDoshas(
        planets: List<KundliPlanetEntry>,
        gocharReport: GocharReport
    ): KundliDoshaSummary {
        val mars = planets.first { it.id == "mars" }
        val isManglik = mars.houseD1 in setOf(1, 2, 4, 7, 8, 12)
        val rahuLon = planets.first { it.id == "rahu" }.longitude
        val ketuLon = planets.first { it.id == "ketu" }.longitude
        val others = planets.filter { it.id !in setOf("rahu", "ketu") }
        val allOneSide = others.all { AyanamshaAndSolvers.normalizeDegrees(it.longitude - rahuLon) < 180.0 } ||
            others.all { AyanamshaAndSolvers.normalizeDegrees(it.longitude - ketuLon) < 180.0 }
        val hasDhaiya = gocharReport.sadeSatiPhaseEn.contains("Dhaiya")
        return KundliDoshaSummary(
            isManglik = isManglik,
            manglikLabelEn = if (isManglik) "Manglik (Mars in House ${mars.houseD1})" else "Non-Manglik (Mars in House ${mars.houseD1})",
            manglikLabelHi = if (isManglik) "मांगलिक दोष उपस्थित (मङ्गल ${mars.houseD1}वें भाव में)" else "मांगलिक दोष नहीं है (मङ्गल ${mars.houseD1}वें भाव में)",
            isKalsarpa = allOneSide,
            kalsarpaLabelEn = if (allOneSide) "Kalsarpa Yoga present (all planets hemmed by Rahu–Ketu)" else "No Kalsarpa Dosha",
            kalsarpaLabelHi = if (allOneSide) "कालसर्प योग उपस्थित (सभी ग्रह राहु-केतु अक्ष में)" else "कालसर्प दोष नहीं है",
            sadeSatiLabelEn = gocharReport.sadeSatiPhaseEn,
            sadeSatiLabelHi = gocharReport.sadeSatiPhaseHi,
            sadeSatiActive = gocharReport.inSadeSati || hasDhaiya
        )
    }

    fun evaluateAllCanonicalYogas(
        planets: List<KundliPlanetEntry>,
        housesD1: List<KundliHouseEntry>,
        moon: KundliPlanetEntry
    ): List<KundliYogaSummary> {
        val pMap = planets.associateBy { it.id }
        val sun = pMap.getValue("sun")
        val mars = pMap.getValue("mars")
        val merc = pMap.getValue("mercury")
        val jup = pMap.getValue("jupiter")
        val ven = pMap.getValue("venus")
        val sat = pMap.getValue("saturn")
        val rahu = pMap.getValue("rahu")
        val kendras = setOf(1, 4, 7, 10)
        val nonDusthana = setOf(1, 2, 4, 5, 7, 9, 10, 11)
        val trika = setOf(6, 8, 12)
        val kendraTrikona = setOf(1, 4, 5, 7, 9, 10)

        fun houseLord(h: Int): KundliPlanetEntry {
            val s = housesD1[(h - 1).coerceIn(0, 11)].signIndex
            return pMap.getValue(signLordsId[s])
        }

        fun orb(a: KundliPlanetEntry, b: KundliPlanetEntry): Double {
            var d = abs(a.longitude - b.longitude)
            if (d > 180.0) d = 360.0 - d
            return d
        }

        fun mutAspect(a: KundliPlanetEntry, b: KundliPlanetEntry): Boolean =
            ((a.houseD1 - b.houseD1 + 12) % 12 == 6)

        val out = mutableListOf<KundliYogaSummary>()

        // 1-5: Pancha Mahapurusha Yogas
        data class MpSpec(val id: String, val p: KundliPlanetEntry, val en: String, val hi: String, val descEn: String, val descHi: String, val signs: Set<Int>, val src: String, val cancOrb: Double?)
        listOf(
            MpSpec("ruchaka", mars, "Ruchaka Mahapurusha Yoga", "रुचक महापुरुष योग",
                "Mars in Kendra (1,4,7,10) in own/exaltation sign bestows courage & leadership.",
                "मंगल केंद्र भाव में स्व/उच्च राशि में हो तो अदम्य पराक्रम व नेतृत्व देता है।", setOf(0, 7, 9), "BPHS 75.1–5", 3.0),
            MpSpec("bhadra", merc, "Bhadra Mahapurusha Yoga", "भद्र महापुरुष योग",
                "Mercury in Kendra in Gemini/Virgo grants intellect, eloquence & trade skill.",
                "बुध केंद्र में मिथुन/कन्या राशि में हो तो प्रखर मेधा व वाक्पटुता देता है।", setOf(2, 5), "BPHS 75.21–25", 2.0),
            MpSpec("hamsa", jup, "Hamsa Mahapurusha Yoga", "हंस महापुरुष योग",
                "Jupiter in Kendra in Cancer/Sagittarius/Pisces grants wisdom & righteousness.",
                "गुरु केंद्र में कर्क/धनु/मीन में हो तो सात्विक ज्ञान व पूजनीय प्रतिष्ठा देता है।", setOf(3, 8, 11), "BPHS 75.6–10", null),
            MpSpec("malavya", ven, "Malavya Mahapurusha Yoga", "मालव्य महापुरुष योग",
                "Venus in Kendra in Taurus/Libra/Pisces grants artistic grace & luxury.",
                "शुक्र केंद्र में वृषभ/तुला/मीन में हो तो वैभव व कलात्मक सौंदर्य देता है।", setOf(1, 6, 11), "BPHS 75.11–15", null),
            MpSpec("shasha", sat, "Shasha Mahapurusha Yoga", "शश महापुरुष योग",
                "Saturn in Kendra in Libra/Capricorn/Aquarius grants enduring authority.",
                "शनि केंद्र में तुला/मकर/कुम्भ में हो तो संगठन क्षमता व दीर्घ प्रभुत्व देता है।", setOf(6, 9, 10), "BPHS 75.16–20", null)
        ).forEach { sp ->
            val c1 = sp.p.houseD1 in kendras
            val c2 = sp.p.signIndex in sp.signs
            val present = c1 && c2
            val canc = sp.cancOrb != null && present && orb(sp.p, sun) <= sp.cancOrb
            out += buildYoga(
                id = sp.id, cat = "Mahapurusha", en = sp.en, hi = sp.hi, descEn = sp.descEn, descHi = sp.descHi,
                srcEn = sp.src, isAusp = true, present = present, mitigated = canc,
                matchedEn = buildList {
                    if (c1) add("${sp.p.nameEn} occupies Kendra House ${sp.p.houseD1}")
                    if (c2) add("${sp.p.nameEn} is in strong own/exalted sign")
                },
                matchedHi = buildList {
                    if (c1) add("${sp.p.nameHi} केंद्र भाव (${sp.p.houseD1}) में स्थित")
                    if (c2) add("${sp.p.nameHi} स्वराशि या उच्च राशि में स्थित")
                },
                failedEn = buildList {
                    if (!c1) add("${sp.p.nameEn} in House ${sp.p.houseD1} (outside Kendra 1,4,7,10)")
                    if (!c2) add("${sp.p.nameEn} not in own/exalted sign")
                },
                failedHi = buildList {
                    if (!c1) add("${sp.p.nameHi} ${sp.p.houseD1}वें भाव में (केंद्र से बाहर)")
                    if (!c2) add("${sp.p.nameHi} स्व/उच्च राशि में नहीं है")
                },
                cancEn = if (canc) listOf("Tightly combust within ${sp.cancOrb}° of Sun") else emptyList(),
                cancHi = if (canc) listOf("सूर्य से ${sp.cancOrb}° के भीतर अत्यंत अस्त") else emptyList()
            )
        }

        // 6: Gajakesari Yoga
        val jupFromMoon = ((jup.signIndex - moon.signIndex + 12) % 12) + 1
        val gkPresent = jupFromMoon in kendras
        val gkMit = gkPresent && (jup.dignityEn == "Debilitated" || moon.dignityEn == "Debilitated")
        out += buildYoga(
            id = "gajakesari_yoga", cat = "Chandra / Raja",
            en = "Gajakesari Yoga", hi = "गजकेसरी योग",
            descEn = "Jupiter in Kendra (1,4,7,10) from Moon bestows wisdom, fame, and moral triumph.",
            descHi = "गुरु चन्द्रमा से केंद्र (१,४,७,१०) में हो तो अक्षुण्ण यश, ज्ञान व समृद्धि देता है।",
            srcEn = "BPHS 36.3–4; Phaladeepika 6.14", isAusp = true, present = gkPresent, mitigated = gkMit,
            matchedEn = if (gkPresent) listOf("Jupiter is in House $jupFromMoon from Natal Moon") else emptyList(),
            matchedHi = if (gkPresent) listOf("बृहस्पति जन्म चन्द्रमा से ${jupFromMoon}वें केंद्र भाव में") else emptyList(),
            failedEn = if (!gkPresent) listOf("Jupiter is in House $jupFromMoon from Moon (needs 1,4,7,10)") else emptyList(),
            failedHi = if (!gkPresent) listOf("बृहस्पति चन्द्र से ${jupFromMoon}वें भाव में (केंद्र आवश्यक)") else emptyList(),
            cancEn = if (gkMit) listOf("Jupiter or Moon in debilitation softens full potential") else emptyList(),
            cancHi = if (gkMit) listOf("गुरु या चन्द्र नीच राशि में होने से प्रभाव कुछ कम") else emptyList()
        )

        // 7: Budhaditya Yoga
        val baPresent = sun.houseD1 == merc.houseD1
        val baMit = baPresent && orb(sun, merc) <= 3.0
        out += buildYoga(
            id = "budhaditya_yoga", cat = "Surya",
            en = "Budhaditya Yoga", hi = "बुधादित्य योग",
            descEn = "Sun and Mercury in the same house sharpen intellect, administration & speech.",
            descHi = "सूर्य और बुध की युति तीक्ष्ण बुद्धि, वाक्पटुता एवं प्रशासनिक प्रतिष्ठा देती है।",
            srcEn = "Saravali 31.1", isAusp = true, present = baPresent, mitigated = baMit,
            matchedEn = if (baPresent) listOf("Sun and Mercury conjoined in House ${sun.houseD1}") else emptyList(),
            matchedHi = if (baPresent) listOf("सूर्य और बुध ${sun.houseD1}वें भाव में युत") else emptyList(),
            failedEn = if (!baPresent) listOf("Sun (H${sun.houseD1}) and Mercury (H${merc.houseD1}) in separate houses") else emptyList(),
            failedHi = if (!baPresent) listOf("सूर्य (${sun.houseD1}) और बुध (${merc.houseD1}) अलग भावों में") else emptyList(),
            cancEn = if (baMit) listOf("Mercury deeply combust within 3° of Sun") else emptyList(),
            cancHi = if (baMit) listOf("बुध सूर्य के ३° के भीतर अत्यंत अस्त") else emptyList()
        )

        // 8: Chandra-Mangala Yoga
        val cmConj = moon.houseD1 == mars.houseD1
        val cmAsp = mutAspect(moon, mars)
        out += buildYoga(
            id = "chandra_mangala_yoga", cat = "Chandra / Dhana",
            en = "Chandra-Mangala Yoga", hi = "चन्द्र-मंगल योग",
            descEn = "Moon and Mars conjoined or mutually aspecting energize enterprise and wealth.",
            descHi = "चन्द्र-मंगल युति या समसप्तक दृष्टि व्यापारिक दक्षता व धनार्जन बढ़ाती है।",
            srcEn = "Saravali 15.20", isAusp = true, present = cmConj || cmAsp, mitigated = false,
            matchedEn = buildList {
                if (cmConj) add("Moon & Mars conjoined in House ${moon.houseD1}")
                if (cmAsp) add("Moon & Mars in mutual 7th aspect")
            },
            matchedHi = buildList {
                if (cmConj) add("चन्द्र व मंगल ${moon.houseD1}वें भाव में युत")
                if (cmAsp) add("चन्द्र व मंगल परस्पर पूर्ण ७वीं दृष्टि में")
            },
            failedEn = if (!cmConj && !cmAsp) listOf("No conjunction or 7th-house opposition between Moon & Mars") else emptyList(),
            failedHi = if (!cmConj && !cmAsp) listOf("चन्द्र और मंगल में युति या समसप्तक सम्बन्ध नहीं") else emptyList(),
            cancEn = emptyList(), cancHi = emptyList()
        )

        // 9: Kendra-Trikona & Dharma-Karmadhipati Raja Yogas
        val klIds = setOf(1, 4, 7, 10).map { houseLord(it).id }
        val tlIds = setOf(1, 5, 9).map { houseLord(it).id }
        var ktPair: Pair<String, String>? = null
        for (kl in klIds) for (tl in tlIds) {
            if (kl != tl && pMap.getValue(kl).houseD1 == pMap.getValue(tl).houseD1) {
                ktPair = kl to tl
                break
            }
        }
        out += buildYoga(
            id = "kendra_trikona_raja", cat = "Raja",
            en = "Kendra-Trikona Raja Yoga", hi = "केन्द्र-त्रिकोण राजयोग",
            descEn = "Conjunction between a Kendra lord and Trikona lord confers high status and power.",
            descHi = "केंद्रेश व त्रिकोणेश की युति समाज में उच्च पद, अधिकार एवं सम्मान देती है।",
            srcEn = "BPHS 34.1–16", isAusp = true, present = ktPair != null, mitigated = false,
            matchedEn = if (ktPair != null) listOf("${pMap.getValue(ktPair.first).nameEn} and ${pMap.getValue(ktPair.second).nameEn} conjoined in House ${pMap.getValue(ktPair.first).houseD1}") else emptyList(),
            matchedHi = if (ktPair != null) listOf("${pMap.getValue(ktPair.first).nameHi} व ${pMap.getValue(ktPair.second).nameHi} की ${pMap.getValue(ktPair.first).houseD1}वें भाव में युति") else emptyList(),
            failedEn = if (ktPair == null) listOf("No Kendra and Trikona lords occupy the same house") else emptyList(),
            failedHi = if (ktPair == null) listOf("किसी केंद्रेश और त्रिकोणेश की युति नहीं") else emptyList(),
            cancEn = emptyList(), cancHi = emptyList()
        )

        val l9 = houseLord(9)
        val l10 = houseLord(10)
        val dkPresent = l9.id == l10.id || l9.houseD1 == l10.houseD1 || mutAspect(l9, l10) ||
            (l9.signIndex == housesD1[9].signIndex && l10.signIndex == housesD1[8].signIndex)
        out += buildYoga(
            id = "dharma_karmadhipati", cat = "Raja / DharmaKarma",
            en = "Dharma-Karmadhipati Raja Yoga", hi = "धर्म-कर्माधिपति राजयोग",
            descEn = "9th and 10th lords connected by conjunction, aspect, or exchange bestow supreme Raja Yoga.",
            descHi = "नवमेश व दशमेश का सम्बन्ध जीवन में महान अधिकार, धर्मनिष्ठ यश व कर्म सिद्धि देता है।",
            srcEn = "BPHS 34.17–20", isAusp = true, present = dkPresent, mitigated = false,
            matchedEn = if (dkPresent) listOf("9th Lord (${l9.nameEn}) & 10th Lord (${l10.nameEn}) connected in House ${l9.houseD1}") else emptyList(),
            matchedHi = if (dkPresent) listOf("नवमेश (${l9.nameHi}) व दशमेश (${l10.nameHi}) शुभ सम्बन्ध में") else emptyList(),
            failedEn = if (!dkPresent) listOf("9th Lord (${l9.nameEn} H${l9.houseD1}) & 10th Lord (${l10.nameEn} H${l10.houseD1}) disconnected") else emptyList(),
            failedHi = if (!dkPresent) listOf("नवमेश (${l9.nameHi}) व दशमेश (${l10.nameHi}) में प्रत्यक्ष सम्बन्ध नहीं") else emptyList(),
            cancEn = emptyList(), cancHi = emptyList()
        )

        // 10-11: Dhana Yogas (2-11 & 5-9)
        val l2 = houseLord(2); val l11 = houseLord(11)
        val d211 = (l2.houseD1 in nonDusthana && l11.houseD1 in nonDusthana) && (l2.houseD1 == l11.houseD1 || mutAspect(l2, l11))
        out += buildYoga(
            id = "dhana_2_11", cat = "Dhana",
            en = "Dhana Yoga (2nd & 11th Lords)", hi = "धन योग (द्वितीय-एकादशेश)",
            descEn = "2nd and 11th lords connected in non-dusthana houses indicate sustained financial prosperity.",
            descHi = "धनेश व लाभेश का शुभ सम्बन्ध प्रचुर धन-संपत्ति एवं नियमित आय प्रदान करता है।",
            srcEn = "BPHS 37.1–10", isAusp = true, present = d211, mitigated = false,
            matchedEn = if (d211) listOf("2nd Lord (${l2.nameEn}) & 11th Lord (${l11.nameEn}) aligned auspiciously") else emptyList(),
            matchedHi = if (d211) listOf("द्वितीयेश (${l2.nameHi}) व एकादशेश (${l11.nameHi}) शुभ भाव में सम्बद्ध") else emptyList(),
            failedEn = if (!d211) listOf("2nd Lord (H${l2.houseD1}) & 11th Lord (H${l11.houseD1}) not conjoined/opposite in auspicious houses") else emptyList(),
            failedHi = if (!d211) listOf("धनेश व लाभेश शुभ भाव में परस्पर सम्बद्ध नहीं") else emptyList(),
            cancEn = emptyList(), cancHi = emptyList()
        )

        val l5 = houseLord(5)
        val d59 = l5.houseD1 in kendraTrikona && l9.houseD1 in kendraTrikona
        out += buildYoga(
            id = "dhana_5_9", cat = "Dhana / Spiritual",
            en = "Lakshmi Dhana Yoga (5th & 9th)", hi = "लक्ष्मी धन योग (पंचम-नवमेश)",
            descEn = "Both 5th and 9th lords in Kendra or Trikona unlock Purva Punya fortune.",
            descHi = "पंचमेश व नवमेश दोनों केंद्र/त्रिकोण में हों तो पूर्वपुण्य से सहज भाग्योदय होता है।",
            srcEn = "BPHS 37.11–15", isAusp = true, present = d59, mitigated = false,
            matchedEn = if (d59) listOf("5th Lord (${l5.nameEn} H${l5.houseD1}) & 9th Lord (${l9.nameEn} H${l9.houseD1}) in Kendra/Trikona") else emptyList(),
            matchedHi = if (d59) listOf("पंचमेश (${l5.nameHi}) व नवमेश (${l9.nameHi}) केंद्र/त्रिकोण में स्थित") else emptyList(),
            failedEn = if (!d59) listOf("5th Lord H${l5.houseD1} or 9th Lord H${l9.houseD1} outside Kendra/Trikona") else emptyList(),
            failedHi = if (!d59) listOf("पंचमेश या नवमेश केंद्र/त्रिकोण से बाहर हैं") else emptyList(),
            cancEn = emptyList(), cancHi = emptyList()
        )

        // 12: Viparita Raja Yogas (Harsha / Sarala / Vimala)
        listOf(
            Triple(6, "Harsha Viparita Raja Yoga", "हर्ष विपरीत राजयोग"),
            Triple(8, "Sarala Viparita Raja Yoga", "सरल विपरीत राजयोग"),
            Triple(12, "Vimala Viparita Raja Yoga", "विमल विपरीत राजयोग")
        ).forEach { (h, vEn, vHi) ->
            val lord = houseLord(h)
            val vPresent = lord.houseD1 in trika
            out += buildYoga(
                id = "viparita_$h", cat = "Viparita / Raja",
                en = vEn, hi = vHi,
                descEn = "${h}th lord placed in Trika houses (6,8,12) converts adversity into sudden breakthrough.",
                descHi = "${h}वें भाव के स्वामी ६, ८ या १२वें भाव में हों तो संकटों से निकलकर अप्रत्याशित सफलता देते हैं।",
                srcEn = "Phaladeepika 6.57–65", isAusp = true, present = vPresent, mitigated = false,
                matchedEn = if (vPresent) listOf("${h}th Lord (${lord.nameEn}) in Dusthana House ${lord.houseD1}") else emptyList(),
                matchedHi = if (vPresent) listOf("${h}वें स्वामी (${lord.nameHi}) त्रिक भाव (${lord.houseD1}) में स्थित") else emptyList(),
                failedEn = if (!vPresent) listOf("${h}th Lord (${lord.nameEn}) in House ${lord.houseD1} (outside 6,8,12)") else emptyList(),
                failedHi = if (!vPresent) listOf("${h}वें स्वामी त्रिक भाव (६, ८, १२) में नहीं हैं") else emptyList(),
                cancEn = emptyList(), cancHi = emptyList()
            )
        }

        // 13: Neechabhanga Raja Yoga
        val debilPlanets = planets.filter { it.dignityEn == "Debilitated" }
        val nbMatchedEn = mutableListOf<String>()
        val nbMatchedHi = mutableListOf<String>()
        debilPlanets.forEach { dp ->
            val dispId = signLordsId[dp.signIndex]
            val disp = pMap[dispId]
            val exSign = exaltSignByPlanet[dp.id]
            val exLord = exSign?.let { pMap[signLordsId[it]] }
            if (disp != null && (disp.houseD1 in kendras || ((disp.signIndex - moon.signIndex + 12) % 12) + 1 in kendras)) {
                nbMatchedEn += "${dp.nameEn} debilitation cancelled (dispositor ${disp.nameEn} in Kendra)"
                nbMatchedHi += "${dp.nameHi} का नीचभंग (नीचेश्वर ${disp.nameHi} केंद्र में)"
            }
            if (exLord != null && (exLord.houseD1 in kendras || ((exLord.signIndex - moon.signIndex + 12) % 12) + 1 in kendras)) {
                nbMatchedEn += "${dp.nameEn} debilitation cancelled (exaltation lord ${exLord.nameEn} in Kendra)"
                nbMatchedHi += "${dp.nameHi} का नीचभंग (उच्चेश ${exLord.nameHi} केंद्र में)"
            }
            val exConj = planets.firstOrNull { it.houseD1 == dp.houseD1 && it.dignityEn == "Exalted" }
            if (exConj != null) {
                nbMatchedEn += "${dp.nameEn} conjoined exalted ${exConj.nameEn} in House ${dp.houseD1}"
                nbMatchedHi += "${dp.nameHi} उच्च ${exConj.nameHi} के साथ युत"
            }
        }
        out += buildYoga(
            id = "neechabhanga_raja", cat = "Neechabhanga / Raja",
            en = "Neechabhanga Raja Yoga", hi = "नीचभंग राजयोग",
            descEn = "Debilitated planet gains cancellation via Kendra dispositor, exaltation lord, or exalted conjunction.",
            descHi = "नीच ग्रह का नीचत्व नीचेश्वर, उच्चेश अथवा उच्च ग्रह की युति से भंग होकर राजयोग देता है।",
            srcEn = "BPHS 40.1–11", isAusp = true, present = nbMatchedEn.isNotEmpty(), mitigated = false,
            matchedEn = nbMatchedEn, matchedHi = nbMatchedHi,
            failedEn = if (nbMatchedEn.isEmpty()) listOf(if (debilPlanets.isEmpty()) "No debilitated planets in D1" else "Debilitation cancellation criteria not met") else emptyList(),
            failedHi = if (nbMatchedHi.isEmpty()) listOf(if (debilPlanets.isEmpty()) "कुण्डली में कोई नीच ग्रह नहीं" else "नीचभंग की शर्तें पूर्ण नहीं") else emptyList(),
            cancEn = emptyList(), cancHi = emptyList()
        )

        // 14: Guru-Chandal Yoga
        val gcPresent = jup.houseD1 == rahu.houseD1
        val gcMit = gcPresent && jup.isExaltedOrOwn
        out += buildYoga(
            id = "guru_chandal_yoga", cat = "Dosha / Arishta",
            en = "Guru-Chandal Yoga", hi = "गुरु-चांडाल योग",
            descEn = "Jupiter-Rahu conjunction advises ethical discernment and honoring traditional teachers.",
            descHi = "गुरु-राहु युति आध्यात्मिक विवेक, गुरु-निष्ठा एवं सात्विक आचरण की आवश्यकता दर्शाती है।",
            srcEn = "Jataka Parijata Ch. 6", isAusp = false, present = gcPresent, mitigated = gcMit,
            matchedEn = if (gcPresent) listOf("Jupiter and Rahu conjoined in House ${jup.houseD1}") else emptyList(),
            matchedHi = if (gcPresent) listOf("गुरु व राहु ${jup.houseD1}वें भाव में युत") else emptyList(),
            failedEn = if (!gcPresent) listOf("Jupiter (H${jup.houseD1}) and Rahu (H${rahu.houseD1}) in different houses") else emptyList(),
            failedHi = if (!gcPresent) listOf("गुरु और राहु एक भाव में युत नहीं हैं") else emptyList(),
            cancEn = if (gcMit) listOf("Jupiter in own/exalted sign strongly mitigates Dosha") else emptyList(),
            cancHi = if (gcMit) listOf("गुरु के स्व/उच्च राशि में होने से दोष का परिहार") else emptyList()
        )

        return out
    }

    private fun buildYoga(
        id: String, cat: String, en: String, hi: String, descEn: String, descHi: String,
        srcEn: String, isAusp: Boolean, present: Boolean, mitigated: Boolean,
        matchedEn: List<String>, matchedHi: List<String>,
        failedEn: List<String>, failedHi: List<String>,
        cancEn: List<String>, cancHi: List<String>
    ) = KundliYogaSummary(
        nameEn = en, nameHi = hi, descriptionEn = descEn, descriptionHi = descHi,
        isAuspicious = isAusp, id = id, category = cat, isPresent = present, isMitigated = mitigated,
        sourceEn = srcEn, sourceHi = srcEn,
        matchedConditionsEn = matchedEn, matchedConditionsHi = matchedHi,
        failedConditionsEn = failedEn, failedConditionsHi = failedHi,
        cancellationsEn = cancEn, cancellationsHi = cancHi
    )

    private data class Quad(val id: String, val en: String, val hi: String, val sign: Int, val deg: Double, val isRetro: Boolean)
    private data class Quint(val inSade: Boolean, val pEn: String, val pHi: String, val dEn: String, val dHi: String)
}
