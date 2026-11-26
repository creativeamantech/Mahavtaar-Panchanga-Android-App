package com.mahavtaar.panchanga.ui.localization

import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.NavtaraResult
import com.mahavtaar.panchanga.domain.model.PlanetPosition
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.TattvaElement

data class TithiSwaraDayRule(
    val dayNumber: Int,
    val tithiNumber: Int,
    val isShukla: Boolean,
    val nameEn: String,
    val nameHi: String,
    val pakshaEn: String,
    val pakshaHi: String,
    val sunriseNadi: SwaraNadi,
    val sunsetNadi: SwaraNadi
)

data class Star28NadiEntry(
    val number: Int,
    val nameEn: String,
    val nameHi: String,
    val rashiNum: Int,
    val startNadi: SwaraNadi,
    val endNadi: SwaraNadi
)

data class TattvaExplorerItem(
    val element: TattvaElement,
    val ratioMins: Int,
    val angulasEn: String,
    val angulasHi: String,
    val shapeColorEn: String,
    val shapeColorHi: String,
    val guidanceEn: String,
    val guidanceHi: String
)

data class NakshatraMetaInfo(
    val lordEn: String,
    val lordHi: String,
    val deityEn: String,
    val deityHi: String,
    val ganaEn: String,
    val ganaHi: String,
    val nadiEn: String,
    val nadiHi: String,
    val tattva: TattvaElement
)

data class PadaPurusharthaInfo(
    val pada: Int,
    val nameEn: String,
    val nameHi: String,
    val guidanceEn: String,
    val guidanceHi: String
)

data class GandantaAlertInfo(
    val isGandanta: Boolean,
    val isMoola: Boolean,
    val titleEn: String,
    val titleHi: String,
    val guidanceEn: String,
    val guidanceHi: String,
    val ritualEn: String,
    val ritualHi: String
)

data class TaraDasaRowInfo(
    val taraNumber: Int,
    val taraNameEn: String,
    val taraNameHi: String,
    val planetEn: String,
    val planetHi: String,
    val years: Int
)

data class TaraVedhaAnalysis(
    val hasOverride: Boolean,
    val summaryEn: String,
    val summaryHi: String,
    val adjustedScore: Int
)

object VedicSwaraNavtaraCatalog {

    private val tithi15En = listOf(
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
        "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Purnima"
    )
    private val tithi15Hi = listOf(
        "प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पञ्चमी",
        "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी",
        "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "पूर्णिमा"
    )

    val all30TithiSwaraRules: List<TithiSwaraDayRule> = (1..30).map { day ->
        val isShukla = day <= 15
        val tithiInPaksha = if (isShukla) day else day - 15
        val triad = (tithiInPaksha - 1) / 3
        val sunrise = if (isShukla) {
            if (triad % 2 == 0) SwaraNadi.IDA else SwaraNadi.PINGALA
        } else {
            if (triad % 2 == 0) SwaraNadi.PINGALA else SwaraNadi.IDA
        }
        val sunset = if (sunrise == SwaraNadi.IDA) SwaraNadi.PINGALA else SwaraNadi.IDA
        val nameEn = when (day) {
            15 -> "Purnima"
            30 -> "Amavasya"
            else -> tithi15En[tithiInPaksha - 1]
        }
        val nameHi = when (day) {
            15 -> "पूर्णिमा"
            30 -> "अमावस्या"
            else -> tithi15Hi[tithiInPaksha - 1]
        }
        TithiSwaraDayRule(
            dayNumber = day,
            tithiNumber = tithiInPaksha,
            isShukla = isShukla,
            nameEn = nameEn,
            nameHi = nameHi,
            pakshaEn = if (isShukla) "Śukla" else "Kṛṣṇa",
            pakshaHi = if (isShukla) "शुक्ल" else "कृष्ण",
            sunriseNadi = sunrise,
            sunsetNadi = sunset
        )
    }

    private val starStartNadi = mapOf(
        1 to SwaraNadi.PINGALA, 2 to SwaraNadi.PINGALA, 3 to SwaraNadi.PINGALA,
        4 to SwaraNadi.IDA, 5 to SwaraNadi.IDA,
        6 to SwaraNadi.PINGALA, 7 to SwaraNadi.PINGALA,
        8 to SwaraNadi.IDA, 9 to SwaraNadi.IDA,
        10 to SwaraNadi.PINGALA, 11 to SwaraNadi.PINGALA, 12 to SwaraNadi.PINGALA,
        13 to SwaraNadi.IDA, 14 to SwaraNadi.IDA,
        15 to SwaraNadi.PINGALA, 16 to SwaraNadi.PINGALA,
        17 to SwaraNadi.IDA, 18 to SwaraNadi.IDA,
        19 to SwaraNadi.PINGALA, 20 to SwaraNadi.PINGALA, 21 to SwaraNadi.PINGALA,
        22 to SwaraNadi.IDA, 23 to SwaraNadi.IDA,
        24 to SwaraNadi.PINGALA, 25 to SwaraNadi.PINGALA,
        26 to SwaraNadi.IDA, 27 to SwaraNadi.IDA,
        28 to SwaraNadi.IDA
    )

    private val starRashiMap = mapOf(
        1 to 1, 2 to 1, 3 to 1, 4 to 2, 5 to 2, 6 to 3, 7 to 3, 8 to 4, 9 to 4,
        10 to 5, 11 to 5, 12 to 5, 13 to 6, 14 to 6, 15 to 7, 16 to 7, 17 to 8, 18 to 8,
        19 to 9, 20 to 9, 21 to 9, 22 to 10, 23 to 10, 24 to 11, 25 to 11, 26 to 12, 27 to 12, 28 to 10
    )

    val all28StarNadiRules: List<Star28NadiEntry> = (1..28).map { n ->
        val start = starStartNadi[n] ?: SwaraNadi.PINGALA
        val end = if (start == SwaraNadi.IDA) SwaraNadi.PINGALA else SwaraNadi.IDA
        val nameEn = if (n == 28) "Abhijit" else VedicLocalizer.nakshatraNameByNumber(n, AppLanguage.ENGLISH)
        val nameHi = if (n == 28) "अभिजित्" else VedicLocalizer.nakshatraNameByNumber(n, AppLanguage.HINDI)
        Star28NadiEntry(
            number = n,
            nameEn = nameEn,
            nameHi = nameHi,
            rashiNum = starRashiMap[n] ?: 1,
            startNadi = start,
            endNadi = end
        )
    }

    fun swaraSloka(nadi: SwaraNadi, lang: AppLanguage): Pair<String, String> = when (nadi) {
        SwaraNadi.IDA -> "इडायां शान्तिकर्माणि स्थिरकर्माणि कारयेत्।" to
            if (lang == AppLanguage.HINDI) {
                "इड़ा (चन्द्र नाड़ी / बायाँ स्वर) में शान्ति, पोषण, यात्रा, विवाह और स्थिर शुभ कर्म सिद्धि प्रदान करते हैं।"
            } else {
                "During Iḍā (Left / Lunar breath), perform peaceful, constructive, and enduring auspicious acts for lasting harmony."
            }
        SwaraNadi.PINGALA -> "पिङ्गलायां चरं कर्म क्रूरं चोग्रं विधीयते।" to
            if (lang == AppLanguage.HINDI) {
                "पिङ्गला (सूर्य नाड़ी / दायाँ स्वर) में साहसिक, पाचन-सम्बन्धी, बलप्रद, वाद-विवाद व गतिशील कार्य करने चाहिए।"
            } else {
                "During Piṅgalā (Right / Solar breath), undertake dynamic, athletic, intellectual, and digestion-intensive pursuits."
            }
        SwaraNadi.SUSHUMNA -> "सुषुम्णायां स्थिरे चित्ते ध्यानयोगं समाचरेत्।" to
            if (lang == AppLanguage.HINDI) {
                "सुषुम्णा (मध्य स्वर) सक्रिय होने पर सांसारिक आरम्भ त्यागकर केवल ध्यान, प्राणायाम और ईश्वर चिन्तन करें।"
            } else {
                "When Suṣumnā flows through both nostrils, pause worldly negotiations and turn inward to meditation and japa."
            }
    }

    val tattvaExplorerItems: List<TattvaExplorerItem> = listOf(
        TattvaExplorerItem(
            element = TattvaElement.VAYU,
            ratioMins = 8,
            angulasEn = "8 Aṅgulas · Oblique flow",
            angulasHi = "८ अङ्गुल · तिर्यक् प्रवाह",
            shapeColorEn = "Blue / Hexagonal Sphere",
            shapeColorHi = "नील वर्ण / षट्कोण मण्डल",
            guidanceEn = "Favors travel, messaging, swift changes, and mental agility; avoid heavy structural anchors.",
            guidanceHi = "यात्रा, सन्देश, त्वरित गति और परिवर्तन हेतु श्रेष्ठ; स्थायी भूमि-निर्माण टालें।"
        ),
        TattvaExplorerItem(
            element = TattvaElement.TEJAS,
            ratioMins = 12,
            angulasEn = "4 Aṅgulas · Upward flow",
            angulasHi = "४ अङ्गुल · ऊर्ध्व प्रवाह",
            shapeColorEn = "Crimson Red / Triangle",
            shapeColorHi = "रक्त वर्ण / त्रिकोण मण्डल",
            guidanceEn = "Sharpens command, metallurgy, valor, and digestion; avoid delicate peacemaking or cooling rites.",
            guidanceHi = "तेज, पराक्रम, यन्त्र व पाचन हेतु बलप्रद; कोमल सन्धि-वार्ता टालें।"
        ),
        TattvaExplorerItem(
            element = TattvaElement.PRITHVI,
            ratioMins = 20,
            angulasEn = "12 Aṅgulas · Central flow",
            angulasHi = "१२ अङ्गुल · मध्य स्थिर प्रवाह",
            shapeColorEn = "Golden Yellow / Square",
            shapeColorHi = "पीत वर्ण / चतुष्कोण मण्डल",
            guidanceEn = "Most auspicious for long-term prosperity, vows, foundations, marriages, and wealth creation.",
            guidanceHi = "दीर्घकालिक सिद्धि, नींव, विवाह, व्यापार और स्थिर समृद्धि हेतु सर्वोत्तम।"
        ),
        TattvaExplorerItem(
            element = TattvaElement.JALA,
            ratioMins = 16,
            angulasEn = "16 Aṅgulas · Downward flow",
            angulasHi = "१६ अङ्गुल · अधोगामी प्रवाह",
            shapeColorEn = "Moon White / Crescent",
            shapeColorHi = "श्वेत वर्ण / अर्धचन्द्र मण्डल",
            guidanceEn = "Brings smooth completion, healing, creative grace, fluid commerce, and reconciliation.",
            guidanceHi = "मधुर पूर्णता, चिकित्सा, कला, जल-कार्य एवं पारिवारिक सौहार्द हेतु उत्तम।"
        ),
        TattvaExplorerItem(
            element = TattvaElement.AKASH,
            ratioMins = 4,
            angulasEn = "All-pervading subtle flow",
            angulasHi = "सर्वव्यापी सूक्ष्म प्रवाह",
            shapeColorEn = "Smoky Violet / Dotted Void",
            shapeColorHi = "धूम्र वर्ण / बिन्दु मण्डल",
            guidanceEn = "Reserved for silent meditation, mantra sādhana, and contemplation; avoid material beginnings.",
            guidanceHi = "केवल ध्यान, मन्त्र साधना व मौन के लिए श्रेष्ठ; लौकिक आरम्भ वर्जित।"
        )
    )

    private val nakLordsOrder = listOf(
        "Ketu" to "केतु", "Venus" to "शुक्र", "Sun" to "सूर्य", "Moon" to "चन्द्र",
        "Mars" to "मङ्गल", "Rahu" to "राहु", "Jupiter" to "गुरु", "Saturn" to "शनि", "Mercury" to "बुध"
    )

    private val nakDeities = listOf(
        "Aśvinī Kumāras" to "अश्विनी कुमार", "Yama" to "यम", "Agni" to "अग्नि", "Prajāpati Brahmā" to "प्रजापति ब्रह्मा",
        "Soma (Candra)" to "चन्द्र (सोम)", "Rudra" to "रुद्र", "Aditi" to "अदिति", "Bṛhaspati" to "बृहस्पति",
        "Sarpa (Nāgas)" to "सर्प (नाग)", "Pitṛs" to "पितृगण", "Bhaga" to "भग", "Aryaman" to "अर्यमा",
        "Savitr (Sūrya)" to "सविता (सूर्य)", "Tvaṣṭā / Viśvakarmā" to "विश्वकर्मा", "Vāyu" to "वायु", "Indrāgni" to "इन्द्राग्नि",
        "Mitra" to "मित्र", "Indra" to "इन्द्र", "Nirṛti" to "निरृति", "Āpaḥ (Waters)" to "आपः (जल)",
        "Viśvedevās" to "विश्वेदेव", "Viṣṇu" to "भगवान विष्णु", "Aṣṭa Vasus" to "अष्ट वसु", "Varuṇa" to "वरुण",
        "Aja Ekapāda" to "अज एकपाद", "Ahirbudhnya" to "अहिर्बुध्न्य", "Pūṣan" to "पूषा"
    )

    fun nakshatraMeta(nakshatraIndex: Int): NakshatraMetaInfo {
        val idx = nakshatraIndex.coerceIn(1, 27) - 1
        val (lordEn, lordHi) = nakLordsOrder[idx % 9]
        val (deityEn, deityHi) = nakDeities[idx]
        val (ganaEn, ganaHi) = when (idx % 3) {
            0 -> "Deva" to "देव गण"
            1 -> "Manuṣya" to "मनुष्य गण"
            else -> "Rākṣasa" to "राक्षस गण"
        }
        val (nadiEn, nadiHi) = when (idx % 3) {
            0 -> "Ādi (Vāta)" to "आदि नाड़ी"
            1 -> "Madhya (Pitta)" to "मध्य नाड़ी"
            else -> "Antya (Kapha)" to "अन्त्य नाड़ी"
        }
        val tattva = when {
            idx < 6 -> TattvaElement.PRITHVI
            idx < 12 -> TattvaElement.JALA
            idx < 18 -> TattvaElement.TEJAS
            idx < 23 -> TattvaElement.VAYU
            else -> TattvaElement.AKASH
        }
        return NakshatraMetaInfo(lordEn, lordHi, deityEn, deityHi, ganaEn, ganaHi, nadiEn, nadiHi, tattva)
    }

    fun padaPurushartha(pada: Int): PadaPurusharthaInfo = when (pada.coerceIn(1, 4)) {
        1 -> PadaPurusharthaInfo(1, "Dharma", "धर्म पद", "Noble intent, righteous direction & vitality", "धर्म, सन्मार्ग, नैतिक स्पष्टता व तेज")
        2 -> PadaPurusharthaInfo(2, "Artha", "अर्थ पद", "Resourcefulness, enterprise & material grounding", "अर्थ सिद्धि, व्यावहारिक निर्माण व समृद्धि")
        3 -> PadaPurusharthaInfo(3, "Kāma", "काम पद", "Creativity, joy, alliances & dialogue", "कला, सौहार्द, संवाद व मनोकामना पूर्ति")
        else -> PadaPurusharthaInfo(4, "Mokṣa", "मोक्ष पद", "Inner clarity, healing & spiritual depth", "गहन विवेक, चिकित्सा व आध्यात्मिक शान्ति")
    }

    fun checkGandanta(nakshatraIndex: Int, pada: Int): GandantaAlertInfo {
        val isMoola = nakshatraIndex in setOf(1, 9, 10, 18, 19, 27)
        val isJunction = (nakshatraIndex in setOf(9, 18, 27) && pada == 4) ||
            (nakshatraIndex in setOf(1, 10, 19) && pada == 1)
        val nakEn = VedicLocalizer.nakshatraNameByNumber(nakshatraIndex, AppLanguage.ENGLISH)
        val nakHi = VedicLocalizer.nakshatraNameByNumber(nakshatraIndex, AppLanguage.HINDI)
        return when {
            isJunction -> GandantaAlertInfo(
                isGandanta = true,
                isMoola = true,
                titleEn = "Gaṇḍānta Sandhi ($nakEn Pada $pada)",
                titleHi = "गण्डान्त सन्धि उपस्थित ($nakHi चरण $pada)",
                guidanceEn = "Water–Fire zodiacal junction: Mūla Śānti with Pañca-Pallava Kalaśa protects health, mind & fortune.",
                guidanceHi = "जल–अग्नि राशि सन्धि पर स्थित: पञ्च-पल्लव कलश सहित मूल शान्ति अत्यन्त रक्षात्मक एवं शुभकारी है।",
                ritualEn = "Kalasha Puja with 5 sacred leaves & 108 sesame-ghee āhutis",
                ritualHi = "गूलर, वट, पीपल, आम व नीम पल्लव कलश पूजन व १०८ तिल-घृत आहुति"
            )
            isMoola -> GandantaAlertInfo(
                isGandanta = false,
                isMoola = true,
                titleEn = "Gaṇḍamūla Star Group ($nakEn)",
                titleHi = "गण्डमूल नक्षत्र वर्ग ($nakHi)",
                guidanceEn = "Mercury/Ketu junction star (outside Sandhi Pada); offer brief lamp & deity mantra on monthly star return.",
                guidanceHi = "गण्डमूल नक्षत्र वर्ग (सन्धि चरण से बाहर); प्रति २७ दिन पर नक्षत्र देवता के स्मरण व दीपदान से शुभ फल।",
                ritualEn = "Recite Viṣṇu Sahasranāma or Rudrāṣṭakam on lunar star return",
                ritualHi = "नक्षत्र आवृत्ति पर विष्णु सहस्रनाम या रुद्राभिषेक"
            )
            else -> GandantaAlertInfo(
                isGandanta = false,
                isMoola = false,
                titleEn = "Free from Gaṇḍānta Doṣa",
                titleHi = "गण्डान्त दोष से मुक्त",
                guidanceEn = "No Water–Fire junction strain affects this birth star and pada.",
                guidanceHi = "जन्म नक्षत्र एवं चरण जल-अग्नि सन्धि से पूर्णतः सुरक्षित है।",
                ritualEn = "",
                ritualHi = ""
            )
        }
    }

    private val dashaOrder = listOf(
        Triple("Ketu", "केतु", 7), Triple("Venus", "शुक्र", 20), Triple("Sun", "सूर्य", 6),
        Triple("Moon", "चन्द्र", 10), Triple("Mars", "मङ्गल", 7), Triple("Rahu", "राहु", 18),
        Triple("Jupiter", "बृहस्पति", 16), Triple("Saturn", "शनि", 19), Triple("Mercury", "बुध", 17)
    )

    private val taraNames = listOf(
        "Janma" to "जन्म", "Sampat" to "सम्पत्", "Vipat" to "विपत्",
        "Kṣema" to "क्षेम", "Pratyak" to "प्रत्यरि", "Sādhaka" to "साधक",
        "Vadha" to "वध", "Mitra" to "मित्र", "Parama Mitra" to "अति-मित्र"
    )

    fun computeTaraDasa(birthNakshatraIndex: Int): List<TaraDasaRowInfo> {
        val startPlanetIdx = (birthNakshatraIndex.coerceIn(1, 27) - 1) % 9
        return (0 until 9).map { offset ->
            val p = dashaOrder[(startPlanetIdx + offset) % 9]
            val tara = taraNames[offset]
            TaraDasaRowInfo(
                taraNumber = offset + 1,
                taraNameEn = tara.first,
                taraNameHi = tara.second,
                planetEn = p.first,
                planetHi = p.second,
                years = p.third
            )
        }
    }

    fun evaluateTaraGocharaVedha(
        todayResult: NavtaraResult,
        planets: List<PlanetPosition>
    ): TaraVedhaAnalysis {
        val jupiter = planets.firstOrNull { it.id.equals("jupiter", true) }
        val saturn = planets.firstOrNull { it.id.equals("saturn", true) }
        val rahu = planets.firstOrNull { it.id.equals("rahu", true) }
        val jupDist = jupiter?.let { ((it.nakshatraNumber - todayResult.birthNakshatraIndex + 27) % 27) + 1 }
        val satDist = saturn?.let { ((it.nakshatraNumber - todayResult.birthNakshatraIndex + 27) % 27) + 1 }
        val rahuDist = rahu?.let { ((it.nakshatraNumber - todayResult.birthNakshatraIndex + 27) % 27) + 1 }

        val jupiterMitigates = jupDist != null && (jupDist % 9 in setOf(2, 4, 6, 8, 0)) && !todayResult.isAuspicious
        val maleficVedha = (satDist != null && satDist % 9 == 7) || (rahuDist != null && rahuDist % 9 == 3)

        return when {
            jupiterMitigates -> {
                val adj = (todayResult.score + 18).coerceAtMost(85)
                TaraVedhaAnalysis(
                    hasOverride = true,
                    summaryEn = "Guru (Jupiter) transits a benefic Tara (#${jupiter!!.nakshatraNumber}), soothing lunar Tārā friction (+18 uplift).",
                    summaryHi = "गुरु का शुभ तारा में गोचर (#${jupiter!!.nakshatraNumber}) दैनिक तारा दोष को शान्त कर रहा है (+१८ बल वृद्धि)।",
                    adjustedScore = adj
                )
            }
            maleficVedha && todayResult.isAuspicious -> {
                val adj = (todayResult.score - 15).coerceAtLeast(35)
                TaraVedhaAnalysis(
                    hasOverride = true,
                    summaryEn = "Śani/Rāhu Gochara casts subtle Vedha over today's benefic Tārā; verify fine details before signing commitments.",
                    summaryHi = "शनि/राहु गोचर से आंशिक वेध प्रभाव उपस्थित; बड़े निर्णयों में सावधानीपूर्वक जाँच करें।",
                    adjustedScore = adj
                )
            }
            else -> TaraVedhaAnalysis(
                hasOverride = false,
                summaryEn = "Clean Tārā expression: No slow-planet Vedha override interferes with today's lunar Nāḍī.",
                summaryHi = "स्पष्ट तारा प्रभाव: आज किसी धीमे ग्रह का वेध बाधक नहीं है।",
                adjustedScore = todayResult.score
            )
        }
    }
}
