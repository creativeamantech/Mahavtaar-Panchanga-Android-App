package com.mahavtaar.panchanga.ui.localization

import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.AngaSegment
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.domain.model.VratObservance
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Authoritative bilingual (Hindi Devanagari & English/IAST) localizer for all Vedic
 * astronomical entities, mapped 1:1 from `src/i18n.ts` while preserving canonical engine outputs.
 */
object VedicLocalizer {

    private val tithiHindi: Map<Int, String> = mapOf(
        1 to "शुक्ल प्रतिपदा", 2 to "शुक्ल द्वितीया", 3 to "शुक्ल तृतीया", 4 to "शुक्ल चतुर्थी",
        5 to "शुक्ल पञ्चमी", 6 to "शुक्ल षष्ठी", 7 to "शुक्ल सप्तमी", 8 to "शुक्ल अष्टमी",
        9 to "शुक्ल नवमी", 10 to "शुक्ल दशमी", 11 to "शुक्ल एकादशी", 12 to "शुक्ल द्वादशी",
        13 to "शुक्ल त्रयोदशी", 14 to "शुक्ल चतुर्दशी", 15 to "पूर्णिमा",
        16 to "कृष्ण प्रतिपदा", 17 to "कृष्ण द्वितीया", 18 to "कृष्ण तृतीया", 19 to "कृष्ण चतुर्थी",
        20 to "कृष्ण पञ्चमी", 21 to "कृष्ण षष्ठी", 22 to "कृष्ण सप्तमी", 23 to "कृष्ण अष्टमी",
        24 to "कृष्ण नवमी", 25 to "कृष्ण दशमी", 26 to "कृष्ण एकादशी", 27 to "कृष्ण द्वादशी",
        28 to "कृष्ण त्रयोदशी", 29 to "कृष्ण चतुर्दशी", 30 to "अमावस्या"
    )

    private val tithiShortHindi: Map<Int, String> = mapOf(
        1 to "प्रतिपदा", 2 to "द्वितीया", 3 to "तृतीया", 4 to "चतुर्थी", 5 to "पञ्चमी",
        6 to "षष्ठी", 7 to "सप्तमी", 8 to "अष्टमी", 9 to "नवमी", 10 to "दशमी",
        11 to "एकादशी", 12 to "द्वादशी", 13 to "त्रयोदशी", 14 to "चतुर्दशी", 15 to "पूर्णिमा",
        16 to "प्रतिपदा", 17 to "द्वितीया", 18 to "तृतीया", 19 to "चतुर्थी", 20 to "पञ्चमी",
        21 to "षष्ठी", 22 to "सप्तमी", 23 to "अष्टमी", 24 to "नवमी", 25 to "दशमी",
        26 to "एकादशी", 27 to "द्वादशी", 28 to "त्रयोदशी", 29 to "चतुर्दशी", 30 to "अमावस्या"
    )

    private val nakshatraHindi: Map<Int, String> = mapOf(
        1 to "अश्विनी", 2 to "भरणी", 3 to "कृत्तिका", 4 to "रोहिणी", 5 to "मृगशीर्षा",
        6 to "आर्द्रा", 7 to "पुनर्वसु", 8 to "पुष्य", 9 to "आश्लेषा", 10 to "मघा",
        11 to "पूर्व फाल्गुनी", 12 to "उत्तर फाल्गुनी", 13 to "हस्त", 14 to "चित्रा",
        15 to "स्वाती", 16 to "विशाखा", 17 to "अनुराधा", 18 to "ज्येष्ठा", 19 to "मूल",
        20 to "पूर्वाषाढ़ा", 21 to "उत्तराषाढ़ा", 22 to "श्रवण", 23 to "धनिष्ठा",
        24 to "शतभिषा", 25 to "पूर्व भाद्रपद", 26 to "उत्तर भाद्रपद", 27 to "रेवती"
    )

    private val nakshatraByCanonical: Map<String, String> = mapOf(
        "aśvinī" to "अश्विनी", "ashwini" to "अश्विनी",
        "bharaṇī" to "भरणी", "bharani" to "भरणी",
        "kṛttikā" to "कृत्तिका", "krittika" to "कृत्तिका",
        "rohiṇī" to "रोहिणी", "rohini" to "रोहिणी",
        "mṛgaśirā" to "मृगशीर्षा", "mrigashirsha" to "मृगशीर्षा",
        "ārdrā" to "आर्द्रा", "ardra" to "आर्द्रा",
        "punarvasū" to "पुनर्वसु", "punarvasu" to "पुनर्वसु",
        "puṣya" to "पुष्य", "pushya" to "पुष्य",
        "āśleṣā" to "आश्लेषा", "ashlesha" to "आश्लेषा",
        "maghā" to "मघा", "magha" to "मघा",
        "pūrvaphalgunī" to "पूर्व फाल्गुनी", "purva phalguni" to "पूर्व फाल्गुनी",
        "uttaraphalgunī" to "उत्तर फाल्गुनी", "uttara phalguni" to "उत्तर फाल्गुनी",
        "hasta" to "हस्त",
        "cittā" to "चित्रा", "chitra" to "चित्रा",
        "svāti" to "स्वाती", "swati" to "स्वाती",
        "viśākhā" to "विशाखा", "vishakha" to "विशाखा",
        "anurādhā" to "अनुराधा", "anuradha" to "अनुराधा",
        "jyeṣṭhā" to "ज्येष्ठा", "jyeshtha" to "ज्येष्ठा",
        "mūlā" to "मूल", "mula" to "मूल",
        "pūrvāṣāḍhā" to "पूर्वाषाढ़ा", "purva ashadha" to "पूर्वाषाढ़ा",
        "uttarāṣāḍhā" to "उत्तराषाढ़ा", "uttara ashadha" to "उत्तराषाढ़ा",
        "śravaṇā" to "श्रवण", "shravana" to "श्रवण",
        "dhaniṣṭhā" to "धनिष्ठा", "dhanishta" to "धनिष्ठा",
        "śatabhiṣā" to "शतभिषा", "shatabhisha" to "शतभिषा",
        "pūrvābhādrā" to "पूर्व भाद्रपद", "purva bhadrapada" to "पूर्व भाद्रपद",
        "uttarābhādrā" to "उत्तर भाद्रपद", "uttara bhadrapada" to "उत्तर भाद्रपद",
        "revatī" to "रेवती", "revati" to "रेवती"
    )

    private val yogaHindi: Map<Int, String> = mapOf(
        1 to "विष्कम्भ", 2 to "प्रीति", 3 to "आयुष्मान्", 4 to "सौभाग्य", 5 to "शोभन",
        6 to "अतिगण्ड", 7 to "सुकर्मा", 8 to "धृति", 9 to "शूल", 10 to "गण्ड",
        11 to "वृद्धि", 12 to "ध्रुव", 13 to "व्याघात", 14 to "हर्षण", 15 to "वज्र",
        16 to "सिद्धि", 17 to "व्यतीपात", 18 to "वरीयान्", 19 to "परिघ", 20 to "शिव",
        21 to "सिद्ध", 22 to "साध्य", 23 to "शुभ", 24 to "शुक्ल", 25 to "ब्रह्म",
        26 to "ऐन्द्र", 27 to "वैधृति"
    )

    private val yogaByCanonical: Map<String, String> = mapOf(
        "viṣkambha" to "विष्कम्भ", "prīti" to "प्रीति", "āyuṣmān" to "आयुष्मान्",
        "saubhāgya" to "सौभाग्य", "śobhana" to "शोभन", "atigaṇḍa" to "अतिगण्ड",
        "sukarmā" to "सुकर्मा", "dhṛti" to "धृति", "śūla" to "शूल", "gaṇḍa" to "गण्ड",
        "vṛddhi" to "वृद्धि", "dhruva" to "ध्रुव", "vyāghāta" to "व्याघात",
        "harṣaṇa" to "हर्षण", "vajra" to "वज्र", "siddhi" to "सिद्धि",
        "vyatīpāta" to "व्यतीपात", "varīyān" to "वरीयान्", "parigha" to "परिघ",
        "śiva" to "शिव", "siddha" to "सिद्ध", "sādhya" to "साध्य",
        "śubha" to "शुभ", "śukla" to "शुक्ल", "brahma" to "ब्रह्म",
        "aindra" to "ऐन्द्र", "vaidhṛti" to "वैधृति"
    )

    private val karanaByNameHindi: Map<String, String> = mapOf(
        "kiṃstughna" to "किंस्तुघ्न", "kimstughna" to "किंस्तुघ्न",
        "bava" to "बव", "bālava" to "बालव", "balava" to "बालव",
        "kaulava" to "कौलव", "taitila" to "तैतिल",
        "garaja" to "गर", "gara" to "गर",
        "vaṇija" to "वणिज", "vanija" to "वणिज",
        "viṣṭi" to "विष्टि (भद्रा)", "visti" to "विष्टि (भद्रा)",
        "śakuni" to "शकुनि", "sakuni" to "शकुनि",
        "catuṣpāda" to "चतुष्पाद", "catuspada" to "चतुष्पाद",
        "nāgava" to "नाग", "naga" to "नाग"
    )

    private val vaaraHindiByWeekday: Map<Int, String> = mapOf(
        0 to "रविवार", 1 to "सोमवार", 2 to "मंगलवार", 3 to "बुधवार",
        4 to "गुरुवार", 5 to "शुक्रवार", 6 to "शनिवार"
    )

    private val vaaraHindiByName: Map<String, String> = mapOf(
        "ravivāra" to "रविवार", "sunday" to "रविवार", "sun" to "रवि",
        "somavāra" to "सोमवार", "monday" to "सोमवार", "mon" to "सोम",
        "maṅgalavāra" to "मंगलवार", "tuesday" to "मंगलवार", "tue" to "मंगल",
        "budhavāra" to "बुधवार", "wednesday" to "बुधवार", "wed" to "बुध",
        "guruvāra" to "गुरुवार", "thursday" to "गुरुवार", "thu" to "गुरु",
        "śukravāra" to "शुक्रवार", "friday" to "शुक्रवार", "fri" to "शुक्र",
        "śanivāra" to "शनिवार", "saturday" to "शनिवार", "sat" to "शनि"
    )

    private val masaHindiByNum: Map<Int, String> = mapOf(
        1 to "चैत्र", 2 to "वैशाख", 3 to "ज्येष्ठ", 4 to "आषाढ़",
        5 to "श्रावण", 6 to "भाद्रपद", 7 to "आश्विन", 8 to "कार्तिक",
        9 to "मार्गशीर्ष", 10 to "पौष", 11 to "माघ", 12 to "फाल्गुन"
    )

    private val masaHindiByName: Map<String, String> = mapOf(
        "caitra" to "चैत्र", "chaitra" to "चैत्र",
        "vaiśākha" to "वैशाख", "vaishakha" to "वैशाख",
        "jyeṣṭha" to "ज्येष्ठ", "jyeshtha" to "ज्येष्ठ",
        "āṣāḍha" to "आषाढ़", "ashadha" to "आषाढ़",
        "śrāvaṇa" to "श्रावण", "shravana" to "श्रावण",
        "bhādrapada" to "भाद्रपद", "bhadrapada" to "भाद्रपद",
        "āśvina" to "आश्विन", "ashvina" to "आश्विन",
        "kārtika" to "कार्तिक", "kartika" to "कार्तिक",
        "mārgaśīrṣa" to "मार्गशीर्ष", "margashirsha" to "मार्गशीर्ष",
        "puṣya" to "पौष", "pauṣa" to "पौष", "pausha" to "पौष",
        "māgha" to "माघ", "magha" to "माघ",
        "phālguṇa" to "फाल्गुन", "phalguna" to "फाल्गुन"
    )

    private val rtuHindi: Map<String, String> = mapOf(
        "vasanta" to "वसन्त ऋतु", "grīṣma" to "ग्रीष्म ऋतु", "varṣā" to "वर्षा ऋतु",
        "śarad" to "शरद ऋतु", "hemanta" to "हेमन्त ऋतु", "śiśira" to "शिशिर ऋतु"
    )

    private val samvatsaraHindi: Map<String, String> = mapOf(
        "akṣaya" to "अक्षय", "prabhava" to "प्रभव", "vibhava" to "विभव", "śukla" to "शुक्ल",
        "pramoda" to "प्रमोद", "prajāpati" to "प्रजापति", "āṅgirasa" to "आङ्गिरस", "śrīmukha" to "श्रीमुख",
        "bhāva" to "भाव", "yuva" to "युव", "dhātrī" to "धाता", "īśvara" to "ईश्वर",
        "bahudhānya" to "बहुधान्य", "pramādhi" to "प्रमाथी", "vikrama" to "विक्रम", "vṛṣa" to "वृष",
        "citrabhānu" to "चित्रभानु", "svabhānu" to "सुभानु", "tāraṇa" to "तारण", "pārthiva" to "पार्थिव",
        "vyaya" to "व्यय", "sarvajit" to "सर्वजित्", "sarvadhārī" to "सर्वधारी", "virodhī" to "विरोधी",
        "vikṛti" to "विकृति", "khara" to "खर", "nandana" to "नन्दन", "vijaya" to "विजय",
        "jaya" to "जय", "manmatha" to "मन्मथ", "durmukhī" to "दुर्मुखी", "hevilambī" to "हेविलम्बी",
        "vilambī" to "विलम्बी", "vikārī" to "विकारी", "śārvarī" to "शार्वरी", "plava" to "प्लव",
        "śubhakṛt" to "शुभकृत्", "śobhakṛt" to "शोभकृत्", "krodhī" to "क्रोधी", "viśvāvasu" to "विश्वावसु",
        "parābhava" to "पराभव", "plavaṅga" to "प्लवङ्ग", "kīlaka" to "कीलक", "saumya" to "सौम्य",
        "sādhāraṇa" to "साधारण", "virodhikṛti" to "विरोधिकृत्", "paridhāvī" to "परिधावी",
        "pramādīca" to "प्रमादीच", "ānanda" to "आनन्द", "rākṣasa" to "राक्षस", "nala" to "नल",
        "piṅgala" to "पिङ्गल", "kalāyukti" to "कालयुक्त", "siddhārthī" to "सिद्धार्थी", "raudra" to "रौद्र",
        "durmati" to "दुर्मति", "dundubhi" to "दुन्दुभि", "rudhirodgārī" to "रुधिरोद्गारी",
        "raktākṣī" to "रक्ताक्षी", "krodhana" to "क्रोधन"
    )

    private val rasiHindiByNum: Map<Int, String> = mapOf(
        1 to "मेष", 2 to "वृषभ", 3 to "मिथुन", 4 to "कर्क",
        5 to "सिंह", 6 to "कन्या", 7 to "तुला", 8 to "वृश्चिक",
        9 to "धनु", 10 to "मकर", 11 to "कुम्भ", 12 to "मीन"
    )

    private val rasiHindiByName: Map<String, String> = mapOf(
        "meṣa" to "मेष", "mesha" to "मेष", "aries" to "मेष",
        "vṛṣabha" to "वृषभ", "vrishabha" to "वृषभ", "taurus" to "वृषभ",
        "mithuna" to "मिथुन", "gemini" to "मिथुन",
        "karkāṭaka" to "कर्क", "karkaṭa" to "कर्क", "karka" to "कर्क", "cancer" to "कर्क",
        "siṃha" to "सिंह", "siṁha" to "सिंह", "simha" to "सिंह", "leo" to "सिंह",
        "kanyā" to "कन्या", "kanya" to "कन्या", "virgo" to "कन्या",
        "tulā" to "तुला", "tula" to "तुला", "libra" to "तुला",
        "vṛścika" to "वृश्चिक", "vrishchika" to "वृश्चिक", "scorpio" to "वृश्चिक",
        "dhanus" to "धनु", "dhanu" to "धनु", "sagittarius" to "धनु",
        "makara" to "मकर", "capricorn" to "मकर",
        "kumbha" to "कुम्भ", "aquarius" to "कुम्भ",
        "mīna" to "मीन", "meena" to "मीन", "pisces" to "मीन"
    )

    private val planetHindiByKey: Map<String, String> = mapOf(
        "sun" to "सूर्य", "surya" to "सूर्य",
        "moon" to "चन्द्र", "chandra" to "चन्द्र",
        "mars" to "मंगल", "mangala" to "मंगल",
        "mercury" to "बुध", "budha" to "बुध",
        "jupiter" to "बृहस्पति (गुरु)", "guru" to "बृहस्पति (गुरु)",
        "venus" to "शुक्र", "shukra" to "शुक्र",
        "saturn" to "शनि", "shani" to "शनि",
        "rahu" to "राहु",
        "ketu" to "केतु"
    )

    private val planetShortHindiByKey: Map<String, String> = mapOf(
        "sun" to "सूर्य", "moon" to "चन्द्र", "mars" to "मंगल", "mercury" to "बुध",
        "jupiter" to "गुरु", "venus" to "शुक्र", "saturn" to "शनि", "rahu" to "राहु", "ketu" to "केतु"
    )

    private val gregorianMonthsHindi = listOf(
        "जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून",
        "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
    )

    private val gregorianMonthsShortHindi = listOf(
        "जन", "फ़र", "मार्च", "अप्रैल", "मई", "जून",
        "जुलाई", "अग", "सित", "अक्टू", "नव", "दिस"
    )

    private val weekdaysShortHindi = listOf("रवि", "सोम", "मंगल", "बुध", "गुरु", "शुक्र", "शनि")
    private val weekdaysShortEnglish = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    fun tithiName(number: Int, fallback: String, lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) tithiHindi[number] ?: fallback else fallback

    fun tithiShortName(number: Int, fallback: String, lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) {
            (tithiShortHindi[number] ?: fallback).take(7)
        } else {
            fallback.substringAfterLast(" ").take(6)
        }

    fun nakshatraName(number: Int, fallback: String, lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) nakshatraHindi[number] ?: nakshatraByCanonical[fallback.lowercase()] ?: fallback
        else fallback

    fun nakshatraByName(name: String, lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) nakshatraByCanonical[name.lowercase().trim()] ?: name else name

    fun yogaName(number: Int, fallback: String, lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) yogaHindi[number] ?: yogaByCanonical[fallback.lowercase()] ?: fallback
        else fallback

    fun yogaByName(name: String, lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) yogaByCanonical[name.lowercase().trim()] ?: name else name

    fun karanaName(name: String, lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) karanaByNameHindi[name.lowercase().trim()] ?: name else name

    fun vaaraName(weekday: Int, fallback: String, lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) vaaraHindiByWeekday[weekday] ?: vaaraHindiByName[fallback.lowercase()] ?: fallback
        else fallback

    fun vaaraByName(name: String, lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) vaaraHindiByName[name.lowercase().trim()] ?: name else name

    fun masaName(masaNumber: Int, isAdhika: Boolean, isKshaya: Boolean, fallback: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return fallback
        val base = masaHindiByNum[masaNumber] ?: masaNameByText(fallback, lang)
        return when {
            isAdhika && !base.startsWith("अधिक") -> "अधिक $base"
            isKshaya && !base.startsWith("क्षय") -> "क्षय $base"
            else -> base
        }
    }

    fun masaNameByText(rawMasa: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return rawMasa
        val lower = rawMasa.lowercase().trim()
        val isAdhika = lower.startsWith("adhika ")
        val isKshaya = lower.startsWith("kṣaya ") || lower.startsWith("kshaya ")
        val clean = lower.removePrefix("adhika ").removePrefix("kṣaya ").removePrefix("kshaya ").trim()
        val base = masaHindiByName[clean] ?: rawMasa
        return when {
            isAdhika -> "अधिक $base"
            isKshaya -> "क्षय $base"
            else -> base
        }
    }

    fun pakshaName(paksha: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return paksha
        val lower = paksha.lowercase()
        return when {
            lower.contains("śukla") || lower.contains("shukla") -> "शुक्ल"
            lower.contains("kṛṣṇa") || lower.contains("krishna") -> "कृष्ण"
            lower.contains("full") -> "पूर्णिमा"
            lower.contains("no moon") -> "अमावस्या"
            else -> paksha
        }
    }

    fun rtuName(rtu: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return rtu
        val lower = rtu.lowercase()
        return rtuHindi.entries.firstOrNull { lower.contains(it.key) }?.value ?: rtu
    }

    fun ayanaName(ayana: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return ayana
        val lower = ayana.lowercase()
        return when {
            lower.contains("uttar") -> "उत्तरायण"
            lower.contains("dakṣ") || lower.contains("daksh") -> "दक्षिणायन"
            else -> ayana
        }
    }

    fun samvatsaraName(samvatsara: String, lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) samvatsaraHindi[samvatsara.lowercase().trim()] ?: samvatsara
        else samvatsara

    fun rasiName(rasiNumber: Int?, fallback: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return fallback.replaceFirstChar { it.uppercase() }
        if (rasiNumber != null && rasiNumber in 1..12) return rasiHindiByNum[rasiNumber] ?: fallback
        return rasiHindiByName[fallback.lowercase().trim()] ?: fallback
    }

    fun rasiByName(name: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return name.replaceFirstChar { it.uppercase() }
        return rasiHindiByName[name.lowercase().trim()] ?: name
    }

    fun planetName(planetIdOrName: String, fallback: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return fallback
        val key = planetIdOrName.lowercase().trim()
        return planetHindiByKey[key] ?: planetHindiByKey[fallback.lowercase().trim()] ?: fallback
    }

    fun horaRulerName(ruler: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return ruler
        return planetShortHindiByKey[ruler.lowercase().trim()] ?: ruler
    }

    fun choghadiyaName(name: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return name
        return when (name.lowercase().trim()) {
            "amrita", "amṛta" -> "अमृत"
            "shubha", "śubha" -> "शुभ"
            "labha", "lābha" -> "लाभ"
            "chara", "cala" -> "चर (चल)"
            "udvega" -> "उद्वेग"
            "kala", "kāla" -> "काल"
            "roga" -> "रोग"
            else -> name
        }
    }

    fun muhurtaOrPeriodName(name: String?, lang: AppLanguage): String {
        val raw = name ?: return if (lang == AppLanguage.HINDI) "अवधि" else "Period"
        if (lang == AppLanguage.ENGLISH) return raw
        val lower = raw.lowercase()
        return when {
            lower.contains("brahma") -> "ब्रह्म मुहूर्त"
            lower.contains("abhijit") -> "अभिजित् मुहूर्त"
            lower.contains("amrita") || lower.contains("amṛta") -> "अमृत काल"
            lower.contains("rahu") || lower.contains("rāhu") -> "राहु काल"
            lower.contains("yamag") -> "यमगण्ड"
            lower.contains("gulika") -> "गुलिक काल"
            lower.contains("durmuh") -> "दुर्मुहूर्त"
            lower.contains("varjyam") -> "वर्ज्यम्"
            else -> choghadiyaName(raw, lang)
        }
    }

    fun tattvaName(element: TattvaElement, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "${element.sanskritName} (${element.displayName})"
        AppLanguage.HINDI -> when (element) {
            TattvaElement.PRITHVI -> "पृथ्वी तत्व"
            TattvaElement.JALA -> "जल तत्व"
            TattvaElement.TEJAS -> "अग्नि (तेजस्) तत्व"
            TattvaElement.VAYU -> "वायु तत्व"
            TattvaElement.AKASH -> "आकाश तत्व"
        }
    }

    fun tattvaQuality(element: TattvaElement, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> element.quality
        AppLanguage.HINDI -> when (element) {
            TattvaElement.PRITHVI -> "स्थिर सफलता एवं समृद्धि"
            TattvaElement.JALA -> "प्रवाहमान लाभ एवं सुख"
            TattvaElement.TEJAS -> "उच्च ऊर्जा / सावधानी आवश्यक"
            TattvaElement.VAYU -> "गतिशीलता एवं परिवर्तन"
            TattvaElement.AKASH -> "आध्यात्मिक चिन्तन एवं ध्यान"
        }
    }

    fun nadiShortLabel(nadi: SwaraNadi, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> nadi.label
        AppLanguage.HINDI -> when (nadi) {
            SwaraNadi.IDA -> "इड़ा"
            SwaraNadi.PINGALA -> "पिंगला"
            SwaraNadi.SUSHUMNA -> "सुषुम्णा"
        }
    }

    fun nadiBadgeLabel(nadi: SwaraNadi, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> when (nadi) {
            SwaraNadi.IDA -> "Ida · Left"
            SwaraNadi.PINGALA -> "Pingala · Right"
            SwaraNadi.SUSHUMNA -> "Sushumna · Both"
        }
        AppLanguage.HINDI -> when (nadi) {
            SwaraNadi.IDA -> "इड़ा · बायाँ"
            SwaraNadi.PINGALA -> "पिंगला · दायाँ"
            SwaraNadi.SUSHUMNA -> "सुषुम्णा · मध्य"
        }
    }

    fun nadiFullTitle(nadi: SwaraNadi, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> when (nadi) {
            SwaraNadi.IDA -> "Ida Nadi · Left Nostril (Lunar)"
            SwaraNadi.PINGALA -> "Pingala Nadi · Right Nostril (Solar)"
            SwaraNadi.SUSHUMNA -> "Sushumna Nadi · Both Nostrils (Central)"
        }
        AppLanguage.HINDI -> nadi.sanskritName
    }

    fun swaraWindowLabel(label: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return label
        return when {
            label.contains("Sunrise", true) -> "सूर्योदय स्वर"
            label.contains("Midday", true) -> "मध्याह्न स्वर"
            label.contains("Sunset", true) -> "सूर्यास्त स्वर"
            label.contains("Moonrise", true) -> "चन्द्रोदय स्वर"
            label.contains("Moonset", true) -> "चन्द्रास्त स्वर"
            else -> label
        }
    }

    fun swaraWindowRule(label: String, fallback: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return fallback
        return when {
            label.contains("Sunrise", true) -> "प्रथम (१ली) दिन होरा के समान समय (सूर्योदय से प्रथम होरा तक)"
            label.contains("Midday", true) -> "सप्तम (७वीं) दिन होरा के समान समय (सूर्योदय के समान स्वर)"
            label.contains("Sunset", true) -> "द्वादश (१२वीं) दिन होरा के समान समय (सूर्यास्त तक)"
            label.contains("Moonrise", true) -> "चन्द्रोदय से १ घंटे तक (सूर्योदय के विपरीत स्वर)"
            label.contains("Moonset", true) -> "चन्द्रास्त से १ घंटा पूर्व (सूर्यास्त के विपरीत स्वर)"
            else -> fallback
        }
    }

    fun swaraEnergySummary(nadi: SwaraNadi, fallback: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return fallback
        return when (nadi) {
            SwaraNadi.IDA -> "शीतल, सौम्य, शांत एवं ग्रहणात्मक ऊर्जा (जल व पृथ्वी तत्व)"
            SwaraNadi.PINGALA -> "उष्ण, ऊर्जावान, सक्रिय एवं तेजस्वी ऊर्जा (अग्नि व वायु तत्व)"
            SwaraNadi.SUSHUMNA -> "संतुलित, योगिक, ध्यानमय एवं आध्यात्मिक अवस्था (आकाश तत्व)"
        }
    }

    fun swaraRulingDeities(nadi: SwaraNadi, fallback: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return fallback
        return when (nadi) {
            SwaraNadi.IDA -> "चन्द्र, बृहस्पति, शुक्र एवं बुध"
            SwaraNadi.PINGALA -> "सूर्य, मंगल, शनि एवं राहु"
            SwaraNadi.SUSHUMNA -> "भगवान शिव एवं पराशक्ति"
        }
    }

    fun swaraAuspiciousWorks(nadi: SwaraNadi, fallback: List<String>, lang: AppLanguage): List<String> {
        if (lang == AppLanguage.ENGLISH) return fallback
        return when (nadi) {
            SwaraNadi.IDA -> listOf(
                "यात्रा या लंबी दूरी के प्रवास का शुभारम्भ",
                "गृह प्रवेश एवं नवीन निर्माण कार्य",
                "वस्त्र, आभूषण अथवा वाहन क्रय करना",
                "विवाह, सगाई एवं शांतिपूर्ण मांगलिक संस्कार",
                "कला, संगीत, ध्यान एवं साहित्य का अध्ययन",
                "औषधि सेवन अथवा चिकित्सा आरम्भ करना"
            )
            SwaraNadi.PINGALA -> listOf(
                "भोजन ग्रहण एवं पाचन (प्रबल जठराग्नि)",
                "व्यायाम, खेलकूद एवं योगासन अभ्यास",
                "वाद-विवाद, परीक्षा, प्रतियोगिता एवं संवाद",
                "साहसिक एवं ऊर्जावान कार्यों का आरम्भ",
                "स्नान, जल यात्रा एवं वाहन संचालन",
                "यंत्र, धातु अथवा उपकरण क्रय करना"
            )
            SwaraNadi.SUSHUMNA -> listOf(
                "ध्यान एवं आध्यात्मिक चिन्तन",
                "प्राणायाम एवं कुण्डलिनी साधना",
                "मंत्र जप एवं शास्त्र स्वाध्याय",
                "आंतरिक शांति एवं वैराग्य हेतु प्रार्थना"
            )
        }
    }

    fun swaraInauspiciousWorks(nadi: SwaraNadi, fallback: List<String>, lang: AppLanguage): List<String> {
        if (lang == AppLanguage.ENGLISH) return fallback
        return when (nadi) {
            SwaraNadi.IDA -> listOf(
                "गरिष्ठ भोजन (मंद जठराग्नि)",
                "कठोर शारीरिक श्रम या उग्र विवाद",
                "जोखिम भरे कानूनी विवाद या संघर्ष"
            )
            SwaraNadi.PINGALA -> listOf(
                "लंबी शांत यात्रा या गृह प्रवेश",
                "कोमल शांति समझौते या सौम्य संकल्प",
                "अत्यधिक शीतल पेय पदार्थों का सेवन"
            )
            SwaraNadi.SUSHUMNA -> listOf(
                "सभी सांसारिक एवं व्यावसायिक लेन-देन",
                "यात्रा, क्रय-विक्रय अथवा मुकदमेबाज़ी"
            )
        }
    }

    fun monthSystemShort(system: MonthSystem, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> if (system == MonthSystem.AMANTA) "Amānta" else "Pūrṇimānta"
        AppLanguage.HINDI -> if (system == MonthSystem.AMANTA) "अमान्त" else "पूर्णिमान्त"
    }

    fun ayanamshaLabel(coord: CoordinateSelection, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> coord.displayLabel
        AppLanguage.HINDI -> when (coord) {
            CoordinateSelection.CITRA -> "चित्रा पक्ष (लाहिड़ी)"
            CoordinateSelection.REVATI -> "रेवती (उषा-शशि)"
            CoordinateSelection.ROHINI -> "रोहिणी"
            CoordinateSelection.PUSHYA -> "पुष्य"
            CoordinateSelection.MULA -> "मूल"
            CoordinateSelection.KRISHNAMURTI -> "कृष्णमूर्ति (KP)"
            CoordinateSelection.RAMAN -> "बी.वी. रमन"
            CoordinateSelection.TROPICAL -> "सायन (ट्रॉपिकल)"
        }
    }

    fun coordinateBannerLabel(coord: CoordinateSelection, fallback: String, lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) ayanamshaLabel(coord, lang) else fallback

    fun lunarPhaseName(phaseName: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return phaseName
        val lower = phaseName.lowercase()
        return when {
            lower.contains("new moon") -> "अमावस्या (नव चन्द्र)"
            lower.contains("full moon") -> "पूर्णिमा (पूर्ण चन्द्र)"
            lower.contains("first quarter") -> "शुक्ल अष्टमी (अर्ध चन्द्र)"
            lower.contains("last quarter") -> "कृष्ण अष्टमी (अर्ध चन्द्र)"
            lower.contains("waxing crescent") -> "शुक्ल वर्धमान चन्द्र"
            lower.contains("waxing gibbous") -> "शुक्ल पूर्णोन्मुख चन्द्र"
            lower.contains("waning gibbous") -> "कृष्ण क्षीयमाण चन्द्र"
            lower.contains("waning crescent") -> "कृष्ण अवसान चन्द्र"
            else -> phaseName
        }
    }

    fun observanceTitle(obs: VratObservance, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return obs.title
        val id = obs.id.lowercase()
        return when {
            id.startsWith("ekadashi") -> "श्री हरि एकादशी व्रत"
            id == "purnima-12" -> "होली / फाल्गुन पूर्णिमा"
            id == "purnima-4" -> "गुरु पूर्णिमा (व्यास पूर्णिमा)"
            id == "purnima-5" -> "रक्षा बंधन / श्रावण पूर्णिमा"
            id == "purnima-8" -> "कार्तिक पूर्णिमा (देव दीपावली)"
            id.startsWith("purnima") -> "पूर्णिमा (सत्यनारायण व्रत)"
            id == "amavasya-7" -> "दीपावली / लक्ष्मी पूजा (अमावस्या)"
            id == "amavasya-11" -> "मौनी अमावस्या"
            id == "amavasya-6" -> "सर्वपितृ अमावस्या"
            id.startsWith("amavasya") -> "अमावस्या (पितृ तर्पण)"
            id.startsWith("pradosha") -> "प्रदोष व्रत"
            id.startsWith("chaturthi") && obs.title.contains("Gaṇeśa", true) -> "गणेश चतुर्थी (विनायक महोत्सव)"
            id.startsWith("chaturthi") && obs.title.contains("Saṅkaṣṭī", true) -> "संकष्टी चतुर्थी व्रत"
            id.startsWith("chaturthi") -> "विनायक चतुर्थी"
            id.startsWith("shivaratri") && obs.title.contains("Mahā", true) -> "मासिक / महाशिवरात्रि"
            id.startsWith("shivaratri") -> "मासिक शिवरात्रि"
            id == "chaitra-navaratri" -> "चैत्र नवरात्रि एवं हिन्दू नववर्ष"
            id == "rama-navami" -> "श्री राम नवमी"
            id == "krishna-janmashtami" -> "श्री कृष्ण जन्माष्टमी"
            id == "vijayadashami" -> "विजयादशमी (दशहरा)"
            id == "vasant-panchami" -> "वसन्त पञ्चमी (सरस्वती पूजा)"
            else -> obs.title
        }
    }

    fun observanceSubtitle(obs: VratObservance, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return obs.subtitle
        return when (obs.category) {
            "ekadashi" -> "भगवान महाविष्णु को समर्पित परम पावन उपवास"
            "purnima" -> "श्री सत्यनारायण पूजा, पावन स्नान एवं दान का दिवस"
            "amavasya" -> "पितृ तर्पण एवं आत्म-चिन्तन का पावन दिन"
            "pradosham" -> "त्रयोदशी प्रदोष काल में भगवान शिव की आराधना"
            "chaturthi" -> "विघ्नहर्ता श्री गणेश को समर्पित पावन व्रत"
            "shivaratri" -> "कृष्ण चतुर्दशी रात्रि में भगवान शिव की पूजा"
            else -> "पावन वैदिक पर्व एवं शुभ उत्सव"
        }
    }

    fun observanceDeity(obs: VratObservance, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return obs.deity
        return when (obs.category) {
            "ekadashi" -> "भगवान विष्णु / श्री कृष्ण"
            "purnima" -> "श्री सत्यनारायण एवं चन्द्र देव"
            "amavasya" -> "पितृ गण एवं सूर्य देव"
            "pradosham" -> "भगवान सदाशिव एवं माता पार्वती"
            "chaturthi" -> "भगवान श्री गणेश"
            "shivaratri" -> "भगवान महादेव"
            else -> when (obs.id) {
                "chaitra-navaratri" -> "माँ दुर्गा"
                "rama-navami" -> "भगवान श्री राम"
                "krishna-janmashtami" -> "भगवान श्री कृष्ण"
                "vijayadashami" -> "श्री राम एवं माँ अपराजिता"
                "vasant-panchami" -> "माँ सरस्वती"
                else -> obs.deity
            }
        }
    }

    fun transitRelativeText(relativeText: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return relativeText
        val lower = relativeText.lowercase().trim()
        return when {
            lower == "today" -> "आज"
            lower == "tomorrow" -> "कल"
            lower.startsWith("in ") && lower.endsWith(" days") -> {
                val n = lower.removePrefix("in ").removeSuffix(" days").trim()
                "$n दिन में"
            }
            lower.startsWith("in ") && lower.endsWith(" day") -> {
                val n = lower.removePrefix("in ").removeSuffix(" day").trim()
                "$n दिन में"
            }
            else -> relativeText
        }
    }

    fun transitTypeLabel(type: String, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> type.replaceFirstChar { it.uppercase() }
        AppLanguage.HINDI -> if (type.equals("rasi", true)) "राशि गोचर" else "नक्षत्र गोचर"
    }

    fun transitValue(type: String, value: String, sanskritFallback: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return value
        return if (type.equals("rasi", true)) {
            rasiByName(value, lang).takeIf { it != value } ?: sanskritFallback.ifBlank { value }
        } else {
            nakshatraByName(value, lang).takeIf { it != value } ?: sanskritFallback.ifBlank { value }
        }
    }

    fun formatDurationRemaining(remainingMs: Long, lang: AppLanguage): String {
        if (remainingMs <= 0L) return if (lang == AppLanguage.HINDI) "० मि" else "0m"
        val totalSeconds = remainingMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (lang == AppLanguage.HINDI) {
            when {
                hours > 0L -> "${hours} घं ${minutes} मि"
                minutes > 0L -> "${minutes} मि ${seconds} से"
                else -> "${seconds} से"
            }
        } else {
            when {
                hours > 0L -> "${hours}h ${minutes}m"
                minutes > 0L -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }
    }

    fun formatDurationHoursMinutes(durationMs: Long, lang: AppLanguage): String {
        val totalMinutes = (durationMs.coerceAtLeast(0L)) / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (lang == AppLanguage.HINDI) "${hours} घं ${minutes} मि" else "${hours}h ${minutes}m"
    }

    fun formatHeaderDate(date: LocalDate, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) {
            return date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.US))
        }
        val dayIdx = date.dayOfWeek.value % 7 // Sun = 0 .. Sat = 6
        val wd = weekdaysShortHindi[dayIdx]
        val mon = gregorianMonthsShortHindi[date.monthValue - 1]
        return "$wd, ${date.dayOfMonth} $mon ${date.year}"
    }

    fun formatMonthYear(yearMonth: YearMonth, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) {
            return yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
        }
        return "${gregorianMonthsHindi[yearMonth.monthValue - 1]} ${yearMonth.year}"
    }

    fun weekdayShortLabels(lang: AppLanguage): List<String> =
        if (lang == AppLanguage.HINDI) weekdaysShortHindi else weekdaysShortEnglish

    private val nakshatraEnglish = listOf(
        "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashirsha", "Ardra",
        "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
        "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
        "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha",
        "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
    )

    private val rasiEnglish = listOf(
        "Aries (Meṣa)", "Taurus (Vṛṣabha)", "Gemini (Mithuna)", "Cancer (Karka)",
        "Leo (Siṃha)", "Virgo (Kanyā)", "Libra (Tulā)", "Scorpio (Vṛścika)",
        "Sagittarius (Dhanu)", "Capricorn (Makara)", "Aquarius (Kumbha)", "Pisces (Mīna)"
    )

    private val rasiShortEnglish = listOf(
        "Ari", "Tau", "Gem", "Can", "Leo", "Vir", "Lib", "Sco", "Sag", "Cap", "Aqu", "Pis"
    )

    private val gregorianMonthsEnglish = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun nakshatraNameByNumber(number: Int, lang: AppLanguage): String {
        val idx = number.coerceIn(1, 27)
        return if (lang == AppLanguage.HINDI) nakshatraHindi[idx] ?: nakshatraEnglish[idx - 1]
        else nakshatraEnglish[idx - 1]
    }

    fun rasiNameByZeroIndex(signIndex: Int, lang: AppLanguage): String {
        val idx = ((signIndex % 12) + 12) % 12
        return if (lang == AppLanguage.HINDI) rasiHindiByNum[idx + 1] ?: rasiEnglish[idx]
        else rasiEnglish[idx]
    }

    fun rasiShortByZeroIndex(signIndex: Int, lang: AppLanguage): String {
        val idx = ((signIndex % 12) + 12) % 12
        return if (lang == AppLanguage.HINDI) rasiHindiByNum[idx + 1] ?: rasiShortEnglish[idx]
        else rasiShortEnglish[idx]
    }

    fun gregorianMonthName(monthValue: Int, lang: AppLanguage): String {
        val idx = (monthValue - 1).coerceIn(0, 11)
        return if (lang == AppLanguage.HINDI) gregorianMonthsHindi[idx] else gregorianMonthsEnglish[idx]
    }

    fun formatDateTimeWithSeconds(epochMs: Long, timeZoneId: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) {
            return TimeFormatters.formatDateTimeWithSeconds(epochMs, timeZoneId)
        }
        val zdt = Instant.ofEpochMilli(epochMs).atZone(TimeFormatters.safeZoneId(timeZoneId))
        val mon = gregorianMonthsShortHindi[zdt.monthValue - 1]
        return "${zdt.dayOfMonth} $mon, ${TimeFormatters.format12HourHMS(epochMs, timeZoneId)}"
    }

    fun formatDateTimeRangeWithSeconds(startMs: Long, endMs: Long, timeZoneId: String, lang: AppLanguage): String =
        "${formatDateTimeWithSeconds(startMs, timeZoneId, lang)} – ${formatDateTimeWithSeconds(endMs, timeZoneId, lang)}"

    fun formatAngaStart(segment: AngaSegment, sunriseMs: Long, timeZoneId: String, lang: AppLanguage): String {
        segment.startTimeMs?.let { return formatDateTimeWithSeconds(it, timeZoneId, lang) }
        val sr = formatDateTimeWithSeconds(sunriseMs, timeZoneId, lang)
        return if (lang == AppLanguage.HINDI) "$sr से पूर्व" else "Before $sr"
    }

    fun formatAngaEnd(segment: AngaSegment, nextSunriseMs: Long, timeZoneId: String, lang: AppLanguage): String {
        segment.endTimeMs?.let { return formatDateTimeWithSeconds(it, timeZoneId, lang) }
        val nsr = formatDateTimeWithSeconds(nextSunriseMs, timeZoneId, lang)
        return if (lang == AppLanguage.HINDI) "$nsr के बाद जारी" else "After $nsr"
    }

    fun simpleNowIntervalSentence(
        title: String,
        isCaution: Boolean,
        endMs: Long?,
        nowMs: Long,
        lang: AppLanguage
    ): String {
        val remText = endMs?.let { (it - nowMs).takeIf { rem -> rem > 0L } }?.let { formatDurationRemaining(it, lang) }
        return if (lang == AppLanguage.HINDI) {
            val timeClause = if (remText != null) " ($remText शेष)" else ""
            val tip = if (isCaution) "इसके समाप्त होने तक नया शुभ काम न शुरू करें।" else "यह अच्छे और शुभ काम के लिए बढ़िया समय है।"
            "अभी $title चल रहा है$timeClause। $tip"
        } else {
            val timeClause = if (remText != null) " ($remText left)" else ""
            val tip = if (isCaution) "Wait until it ends before starting new important work." else "Good time for calm, learning, or positive tasks."
            "Right now: $title is running$timeClause. $tip"
        }
    }

    fun simpleNowClearWindowSentence(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Right now: No Rahu Kala or caution period is running. Normal daily tasks are safe."
        AppLanguage.HINDI -> "अभी कोई राहु काल या अशुभ समय नहीं चल रहा है। रोज़मर्रा के सामान्य काम सुरक्षित हैं।"
    }

    fun simpleNowNadiSentence(nadi: SwaraNadi, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> when (nadi) {
            SwaraNadi.IDA -> "Right now: Ida (Left nostril) is flowing — calm, cool energy for study, kindness, and steady work."
            SwaraNadi.PINGALA -> "Right now: Pingala (Right nostril) is flowing — warm, active energy for meals, sports, and lively tasks."
            SwaraNadi.SUSHUMNA -> "Right now: Sushumna (Both nostrils) is flowing — best for quiet prayer, breathing, and peaceful focus."
        }
        AppLanguage.HINDI -> when (nadi) {
            SwaraNadi.IDA -> "अभी इड़ा (बायाँ स्वर) चल रहा है — शांत और शीतल ऊर्जा पढ़ाई, शांति व अच्छे कामों के लिए उत्तम है।"
            SwaraNadi.PINGALA -> "अभी पिंगला (दायाँ स्वर) चल रहा है — ऊर्जावान समय भोजन, खेल, व्यायाम व सक्रिय कामों के लिए अच्छा है।"
            SwaraNadi.SUSHUMNA -> "अभी सुषुम्णा (दोनों स्वर) चल रहे हैं — यह समय शांत प्रार्थना, प्राणायाम और ध्यान के लिए सबसे उत्तम है।"
        }
    }

    fun simpleNowTattvaSentence(element: TattvaElement, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> when (element) {
            TattvaElement.PRITHVI -> "Earth Tattva right now: Calm and steady — great for finishing homework and careful work."
            TattvaElement.JALA -> "Water Tattva right now: Smooth and gentle — good for friendly talks, learning, and happy moments."
            TattvaElement.TEJAS -> "Fire Tattva right now: Very high energy — stay patient, avoid anger, and act carefully."
            TattvaElement.VAYU -> "Air Tattva right now: Fast and active — helpful for movement, travel, and quick chores."
            TattvaElement.AKASH -> "Space Tattva right now: Quiet and peaceful — best for prayer, rest, and thinking clearly."
        }
        AppLanguage.HINDI -> when (element) {
            TattvaElement.PRITHVI -> "अभी पृथ्वी तत्व चल रहा है: शांत और स्थिर समय — पढ़ाई व जरूरी काम पूरा करने के लिए उत्तम है।"
            TattvaElement.JALA -> "अभी जल तत्व चल रहा है: सहज और मधुर समय — अच्छी बातचीत, सीखने और खुशी से काम करने के लिए बढ़िया है।"
            TattvaElement.TEJAS -> "अभी अग्नि तत्व चल रहा है: ऊर्जा बहुत तेज़ है — गुस्सा न करें और हर काम सावधानी से करें।"
            TattvaElement.VAYU -> "अभी वायु तत्व चल रहा है: तेज़ और गतिशील समय — खेल, यात्रा और चलते-फिरते कामों के लिए ठीक है।"
            TattvaElement.AKASH -> "अभी आकाश तत्व चल रहा है: शांत और गहरा समय — प्रार्थना, आराम और ध्यान लगाने के लिए सबसे अच्छा है।"
        }
    }

    fun simpleNowHoraSentence(
        ruler: String,
        nadi: SwaraNadi,
        tattva: TattvaElement?,
        lang: AppLanguage
    ): String {
        val rulerLabel = horaRulerName(ruler, lang)
        val nadiLabel = nadiBadgeLabel(nadi, lang)
        val tattvaLabel = tattva?.let { tattvaName(it, lang) }
        return if (lang == AppLanguage.HINDI) {
            val tPart = if (tattvaLabel != null) " और $tattvaLabel" else ""
            "अभी $rulerLabel होरा चल रही है — साथ में $nadiLabel$tPart सक्रिय है।"
        } else {
            val tPart = if (tattvaLabel != null) " and $tattvaLabel" else ""
            "Right now: $rulerLabel Hora is running with $nadiLabel$tPart active."
        }
    }
}
