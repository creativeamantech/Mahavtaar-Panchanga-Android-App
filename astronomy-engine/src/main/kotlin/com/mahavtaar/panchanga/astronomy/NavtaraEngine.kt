package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.domain.model.NavtaraParihara
import com.mahavtaar.panchanga.domain.model.NavtaraResult

/**
 * Pure Kotlin port of `src/lib/navtaraEngine.ts` computing the 9-Tara Chakra
 * (Janma, Sampat, Vipat, Kshema, Pratyak, Sadhaka, Vadha/Naidhana, Mitra, Parama Mitra),
 * Paryaya cycle intensity, and classical Parihara remedies.
 */
object NavtaraEngine {

    private data class TaraDef(
        val nameEn: String,
        val nameHi: String,
        val natureEn: String,
        val natureHi: String,
        val isAuspicious: Boolean,
        val isNeutral: Boolean,
        val score: Int,
        val descEn: String,
        val descHi: String
    )

    private val taraDefs = mapOf(
        1 to TaraDef(
            nameEn = "Janma",
            nameHi = "जन्म तारा",
            natureEn = "Neutral / Mixed",
            natureHi = "मध्यम / मिश्रित",
            isAuspicious = false,
            isNeutral = true,
            score = 50,
            descEn = "Influences physical vitality, body, and self. Exercise mindful care during intense transits.",
            descHi = "शरीर, स्वास्थ्य और आत्मबल को प्रभावित करता है। अशुभ गोचर में स्वास्थ्य का विशेष ध्यान रखें।"
        ),
        2 to TaraDef(
            nameEn = "Sampat",
            nameHi = "सम्पत् तारा",
            natureEn = "Highly Auspicious",
            natureHi = "अत्यन्त शुभ",
            isAuspicious = true,
            isNeutral = false,
            score = 90,
            descEn = "Brings wealth, financial gains, prosperity, and material well-being.",
            descHi = "धन-धान्य, समृद्धि, आर्थिक लाभ एवं भौतिक सुख-सुविधाओं की प्राप्ति होती है।"
        ),
        3 to TaraDef(
            nameEn = "Vipat",
            nameHi = "विपत् तारा",
            natureEn = "Inauspicious",
            natureHi = "अशुभ (सावधानी)",
            isAuspicious = false,
            isNeutral = false,
            score = 20,
            descEn = "May bring unexpected hurdles, financial risk, or sudden delays. Avoid risky ventures.",
            descHi = "आकस्मिक बाधा, धन-हानि या अवरोध का संकेत। जोखिम भरे निवेश और बड़े अनुबंध से बचें।"
        ),
        4 to TaraDef(
            nameEn = "Kshema",
            nameHi = "क्षेम तारा",
            natureEn = "Auspicious",
            natureHi = "शुभ",
            isAuspicious = true,
            isNeutral = false,
            score = 80,
            descEn = "Grants protection, stability, peace of mind, and general well-being.",
            descHi = "सुरक्षा, कुशलता, मानसिक शान्ति और सभी कार्यों में स्थिरता प्रदान करता है।"
        ),
        5 to TaraDef(
            nameEn = "Pratyak",
            nameHi = "प्रत्यरि तारा",
            natureEn = "Inauspicious",
            natureHi = "अशुभ (विरोध)",
            isAuspicious = false,
            isNeutral = false,
            score = 30,
            descEn = "Creates resistance, delays, or disagreements in endeavors. Patience is advised.",
            descHi = "कार्यों में विलम्ब, विरोध या मतभेद उत्पन्न हो सकते हैं। धैर्य और संयम बनाए रखें।"
        ),
        6 to TaraDef(
            nameEn = "Sadhak",
            nameHi = "साधक तारा",
            natureEn = "Highly Auspicious",
            natureHi = "सर्वोत्तम शुभ",
            isAuspicious = true,
            isNeutral = false,
            score = 100,
            descEn = "Bestows accomplishment of goals, success in endeavours, and spiritual progress.",
            descHi = "समस्त मनोरथ सिद्धि, कार्य सफलता और आध्यात्मिक उन्नति के लिए सर्वश्रेष्ठ तारा।"
        ),
        7 to TaraDef(
            nameEn = "Vadha / Naidhana",
            nameHi = "वध / नैधन तारा",
            natureEn = "Severely Inauspicious",
            natureHi = "अति अशुभ",
            isAuspicious = false,
            isNeutral = false,
            score = 5,
            descEn = "Indicates heavy obstruction, stress, or hazards. Postpone major initiatives.",
            descHi = "अत्यधिक बाधा, कष्ट या संकट का सूचक। किसी भी महत्वपूर्ण नए कार्य को टालना श्रेयस्कर है।"
        ),
        8 to TaraDef(
            nameEn = "Mitra",
            nameHi = "मित्र तारा",
            natureEn = "Auspicious",
            natureHi = "शुभ",
            isAuspicious = true,
            isNeutral = false,
            score = 75,
            descEn = "Brings friendly support, harmony, collaboration, and favorable outcomes.",
            descHi = "मित्रों का सहयोग, प्रसन्नता, सौहार्द एवं अनुकूल परिस्थितियाँ प्राप्त होती हैं।"
        ),
        9 to TaraDef(
            nameEn = "Parama Mitra",
            nameHi = "अति-मित्र तारा",
            natureEn = "Highly Auspicious",
            natureHi = "अत्यन्त शुभ",
            isAuspicious = true,
            isNeutral = false,
            score = 95,
            descEn = "Bestows deep alliances, major breakthroughs, and joyful fulfillment of desires.",
            descHi = "गहन सहयोग, बड़ी सफलता और सभी शुभ इच्छाओं की पूर्ति का महायोग।"
        )
    )

    fun calculate(birthNakshatraIndex: Int, targetNakshatraIndex: Int): NavtaraResult {
        val birth = birthNakshatraIndex.coerceIn(1, 27)
        val target = targetNakshatraIndex.coerceIn(1, 27)
        val distance = ((target - birth + 27) % 27) + 1
        val rawMod = distance % 9
        val taraNumber = if (rawMod == 0) 9 else rawMod
        val paryaya = when {
            distance <= 9 -> 1
            distance <= 18 -> 2
            else -> 3
        }

        val def = taraDefs.getValue(taraNumber)
        var score = def.score
        var descEn = def.descEn
        var descHi = def.descHi

        if (paryaya == 1 && (taraNumber == 3 || taraNumber == 7)) {
            score = (score - 10).coerceAtLeast(0)
            descEn += " (Cycle 1: peak intensity.)"
            descHi += " (प्रथम पर्याय: सर्वाधिक प्रभाव।)"
        } else if (paryaya >= 2 && (taraNumber == 3 || taraNumber == 5 || taraNumber == 7)) {
            score = (score + 15).coerceAtMost(100)
            descEn += " (Cycle $paryaya: softened intensity.)"
            descHi += " (पर्याय $paryaya: अशुभ प्रभाव अपेक्षाकृत कम।)"
        }

        val (intensityEn, intensityHi) = when {
            taraNumber in setOf(2, 4, 6, 8, 9) -> "Favorable" to "अनुकूल"
            paryaya == 1 -> "High" to "उच्च"
            paryaya == 2 -> "Moderate" to "मध्यम"
            taraNumber == 7 -> "High" to "उच्च"
            else -> "Mild" to "सामान्य"
        }

        val parihara = when (taraNumber) {
            1 -> NavtaraParihara(
                deityEn = "Lord Vishnu",
                deityHi = "भगवान श्री विष्णु",
                mantraEn = "Vishnu Sahasranama",
                mantraHi = "विष्णु सहस्रनाम पाठ",
                donationEn = "Green Gram (Moong), Vegetables, or Ghee",
                donationHi = "हरी मूँग, सब्ज़ी या घी का दान",
                avoidEn = listOf("Unnecessary physical exhaustion"),
                avoidHi = listOf("अनावश्यक शारीरिक श्रम से बचें")
            )
            3 -> NavtaraParihara(
                deityEn = "Lord Ganesha",
                deityHi = "भगवान श्री गणेश",
                mantraEn = "Ganapati Atharvashirsha or Gayatri Mantra",
                mantraHi = "गणपति अथर्वशीर्ष अथवा गायत्री मंत्र",
                donationEn = "Jaggery (Gud), Gold, or Copper",
                donationHi = "गुड़, स्वर्ण या ताम्र का दान",
                avoidEn = listOf("High-risk financial investments", "Major binding contracts"),
                avoidHi = listOf("नए जोखिमपूर्ण निवेश", "महत्वपूर्ण अनुबंध / समझौते")
            )
            5 -> NavtaraParihara(
                deityEn = "Goddess Durga",
                deityHi = "माँ दुर्गा",
                mantraEn = "Durga Saptashati or Chandi Patha",
                mantraHi = "दुर्गा सप्तशती अथवा चण्डी पाठ",
                donationEn = "Salt, Wheat Grain, or Sesame Seeds (Til)",
                donationHi = "नमक, गेहूँ या तिल का दान",
                avoidEn = listOf("Confrontations & disputes", "Critical negotiations"),
                avoidHi = listOf("वाद-विवाद एवं मुक़दमेबाज़ी", "महत्वपूर्ण वार्ताओं से बचें")
            )
            7 -> NavtaraParihara(
                deityEn = "Lord Shiva",
                deityHi = "भगवान शिव",
                mantraEn = "Mahamrityunjaya Mantra or Rudra Abhisheka",
                mantraHi = "महामृत्युंजय मंत्र अथवा रुद्राभिषेक",
                donationEn = "Sesame Oil, Black Sesame, or Dark Cloth",
                donationHi = "तिल का तेल, काला तिल या वस्त्र दान",
                avoidEn = listOf("Rash travel or driving", "Elective risky procedures"),
                avoidHi = listOf("तीव्र गति से वाहन चलाना", "अनावश्यक जोखिमपूर्ण कार्य")
            )
            else -> null
        }

        return NavtaraResult(
            birthNakshatraIndex = birth,
            targetNakshatraIndex = target,
            distance = distance,
            paryaya = paryaya,
            taraNumber = taraNumber,
            taraNameEn = def.nameEn,
            taraNameHi = def.nameHi,
            natureEn = def.natureEn,
            natureHi = def.natureHi,
            isAuspicious = def.isAuspicious,
            isNeutral = def.isNeutral,
            score = score,
            descriptionEn = descEn,
            descriptionHi = descHi,
            intensityLevelEn = intensityEn,
            intensityLevelHi = intensityHi,
            parihara = parihara
        )
    }

    fun generateMatrix(birthNakshatraIndex: Int): List<NavtaraResult> =
        (1..27).map { target -> calculate(birthNakshatraIndex, target) }

    data class NakshatraMeta(
        val lordEn: String,
        val lordHi: String,
        val deityEn: String,
        val deityHi: String,
        val ganaEn: String,
        val ganaHi: String,
        val nadiEn: String,
        val nadiHi: String,
        val tattvaEn: String,
        val tattvaHi: String
    )

    data class PadaPurushartha(
        val pada: Int,
        val purusharthaEn: String,
        val purusharthaHi: String,
        val focusEn: String,
        val focusHi: String
    )

    data class GandantaReport(
        val isGandanta: Boolean,
        val isMoolaNakshatra: Boolean,
        val titleEn: String,
        val titleHi: String,
        val guidanceEn: String,
        val guidanceHi: String,
        val ritualEn: String,
        val ritualHi: String
    )

    data class TaraDasaRow(
        val taraNumber: Int,
        val taraNameEn: String,
        val taraNameHi: String,
        val planetEn: String,
        val planetHi: String,
        val years: Int
    )

    private val nakLordsOrder = listOf(
        "Ketu" to "केतु", "Venus" to "शुक्र", "Sun" to "सूर्य", "Moon" to "चन्द्र",
        "Mars" to "मङ्गल", "Rahu" to "राहु", "Jupiter" to "बृहस्पति", "Saturn" to "शनि", "Mercury" to "बुध"
    )
    private val nakDeities = listOf(
        "Ashwini Kumaras" to "अश्विनी कुमार", "Yama" to "यम", "Agni" to "अग्नि", "Brahma / Prajapati" to "प्रजापति ब्रह्मा",
        "Soma (Chandra)" to "चन्द्र (सोम)", "Rudra" to "रुद्र", "Aditi" to "अदिति", "Brihaspati" to "बृहस्पति",
        "Sarpa (Nagas)" to "सर्प (नाग)", "Pitris" to "पितृगण", "Bhaga" to "भग", "Aryaman" to "अर्यमा",
        "Surya (Savita)" to "सविता (सूर्य)", "Vishwakarma" to "त्वष्टा / विश्वकर्मा", "Vayu" to "वायु", "Indragni" to "इन्द्राग्नि",
        "Mitra" to "मित्र", "Indra" to "इन्द्र", "Nirrti" to "निरृति", "Apah" to "आपः (जल)",
        "Vishvedevas" to "विश्वेदेव", "Vishnu" to "भगवान विष्णु", "Ashta Vasus" to "अष्ट वसु", "Varuna" to "वरुण",
        "Aja Ekapada" to "अज एकपाद", "Ahirbudhnya" to "अहिर्बुध्न्य", "Pushan" to "पूषा"
    )
    private val dashaYearsByPlanet = mapOf(
        "Sun" to 6, "Moon" to 10, "Mars" to 7, "Rahu" to 18, "Jupiter" to 16,
        "Saturn" to 19, "Mercury" to 17, "Ketu" to 7, "Venus" to 20
    )

    fun getNakshatraMeta(nakshatraIndex: Int): NakshatraMeta {
        val idx = nakshatraIndex.coerceIn(1, 27) - 1
        val (lordEn, lordHi) = nakLordsOrder[idx % 9]
        val (deityEn, deityHi) = nakDeities[idx]
        val ganaMod = idx % 3
        val (ganaEn, ganaHi) = when (ganaMod) {
            0 -> "Deva" to "देव गण"
            1 -> "Manushya" to "मनुष्य गण"
            else -> "Rakshasa" to "राक्षस गण"
        }
        val (nadiEn, nadiHi) = when (idx % 3) {
            0 -> "Adi (Vata)" to "आदि नाड़ी"
            1 -> "Madhya (Pitta)" to "मध्य नाड़ी"
            else -> "Antya (Kapha)" to "अन्त्य नाड़ी"
        }
        val (tEn, tHi) = when {
            idx < 6 -> "Prithvi" to "पृथ्वी"
            idx < 12 -> "Jala" to "जल"
            idx < 18 -> "Agni" to "अग्नि"
            idx < 23 -> "Vayu" to "वायु"
            else -> "Akasha" to "आकाश"
        }
        return NakshatraMeta(lordEn, lordHi, deityEn, deityHi, ganaEn, ganaHi, nadiEn, nadiHi, tEn, tHi)
    }

    fun getPadaPurushartha(pada: Int): PadaPurushartha = when (pada.coerceIn(1, 4)) {
        1 -> PadaPurushartha(1, "Dharma Pada", "धर्म पद", "Vital direction, ethical clarity & spiritual purpose", "धर्म, सन्मार्ग, नेतृत्व व आध्यात्मिक उद्देश्य")
        2 -> PadaPurushartha(2, "Artha Pada", "अर्थ पद", "Resource building, practical enterprise & stability", "धनार्जन, व्यावहारिक निर्माण व समृद्धि")
        3 -> PadaPurushartha(3, "Kama Pada", "काम पद", "Harmony, creativity, desires & alliances", "सौहार्द, कला, अभिव्यक्ति व मनोकामना")
        else -> PadaPurushartha(4, "Moksha Pada", "मोक्ष पद", "Deep wisdom, healing & transcendent release", "गहन विवेक, शान्ति व आत्मिक मुक्ति")
    }

    fun checkGandanta(nakshatraIndex: Int, pada: Int): GandantaReport {
        val isMoola = nakshatraIndex in setOf(1, 9, 10, 18, 19, 27)
        val isJunctionGandanta = (nakshatraIndex in setOf(9, 18, 27) && pada == 4) ||
            (nakshatraIndex in setOf(1, 10, 19) && pada == 1)
        return if (isJunctionGandanta) {
            GandantaReport(
                isGandanta = true,
                isMoolaNakshatra = true,
                titleEn = "Gandanta Sandhi (${SanskritNamesCatalog.nakshatras[nakshatraIndex]} Pada $pada)",
                titleHi = "गण्डान्त संधि उपस्थित (${SanskritNamesCatalog.nakshatras[nakshatraIndex]} चरण $pada)",
                guidanceEn = "Water–Fire junction boundary: Moola Shanti (27-well water & Pancha-Pallava Kalasha) confers immense protection.",
                guidanceHi = "जल–अग्नि राशि संधि पर जन्म: मूल शान्ति (पंच-पल्लव कलश पूजन एवं तिल-घृत हवन) से पूर्ण रक्षा व शुभ फल प्राप्त होता है।",
                ritualEn = "Kalasha Puja with 5 leaves (Gular, Vata, Pipal, Mango, Neem) & 108 sesame-ghee ahutis",
                ritualHi = "गूलर, वट, पीपल, आम, नीम पल्लव सहित कलश पूजन व १०८ तिल-घृत आहुति"
            )
        } else if (isMoola) {
            GandantaReport(
                isGandanta = false,
                isMoolaNakshatra = true,
                titleEn = "Nakshatra Moola Group (${SanskritNamesCatalog.nakshatras[nakshatraIndex]})",
                titleHi = "गण्डमूल नक्षत्र वर्ग (${SanskritNamesCatalog.nakshatras[nakshatraIndex]})",
                guidanceEn = "Mercury/Ketu junction star (non-sandhi pada); light Nakshatra deity worship on every 27th-day cycle brings blessings.",
                guidanceHi = "गण्डमूल वर्ग का नक्षत्र (संधि चरण से बाहर); प्रति २७ दिन पर नक्षत्र देवता के पूजन से श्रेष्ठ लाभ।",
                ritualEn = "Recite Vishnu Sahasranama or Rudrashtakam on lunar star return",
                ritualHi = "नक्षत्र आवृत्ति पर विष्णु सहस्रनाम या रुद्राभिषेक"
            )
        } else {
            GandantaReport(
                isGandanta = false,
                isMoolaNakshatra = false,
                titleEn = "Free from Gandanta Dosha",
                titleHi = "गण्डान्त दोष से मुक्त",
                guidanceEn = "No Water–Fire junction stress present on this birth star and pada.",
                guidanceHi = "जन्म नक्षत्र एवं चरण गण्डान्त संधि से पूर्णतः सुरक्षित है।",
                ritualEn = "",
                ritualHi = ""
            )
        }
    }

    fun computeTaraDasa(strongestPlanetEn: String): List<TaraDasaRow> {
        val seq = listOf("Sun" to "सूर्य", "Moon" to "चन्द्र", "Mars" to "मङ्गल", "Rahu" to "राहु",
            "Jupiter" to "बृहस्पति", "Saturn" to "शनि", "Mercury" to "बुध", "Ketu" to "केतु", "Venus" to "शुक्र")
        val startIdx = seq.indexOfFirst { it.first.equals(strongestPlanetEn, ignoreCase = true) }.coerceAtLeast(0)
        return (0 until 9).map { i ->
            val p = seq[(startIdx + i) % 9]
            val def = taraDefs.getValue(i + 1)
            TaraDasaRow(
                taraNumber = i + 1,
                taraNameEn = def.nameEn,
                taraNameHi = def.nameHi,
                planetEn = p.first,
                planetHi = p.second,
                years = dashaYearsByPlanet[p.first] ?: 10
            )
        }
    }
}
