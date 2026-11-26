package com.mahavtaar.panchanga.core

/**
 * Shared Hindi Devanagari lookup tables for Panchanga segments, planets, and observances
 * usable across both data/notification planners and UI modules.
 */
object VedicHindiNames {

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

    private val nakshatraHindi: Map<Int, String> = mapOf(
        1 to "अश्विनी", 2 to "भरणी", 3 to "कृत्तिका", 4 to "रोहिणी", 5 to "मृगशीर्षा",
        6 to "आर्द्रा", 7 to "पुनर्वसु", 8 to "पुष्य", 9 to "आश्लेषा", 10 to "मघा",
        11 to "पूर्व फाल्गुनी", 12 to "उत्तर फाल्गुनी", 13 to "हस्त", 14 to "चित्रा",
        15 to "स्वाती", 16 to "विशाखा", 17 to "अनुराधा", 18 to "ज्येष्ठा", 19 to "मूल",
        20 to "पूर्वाषाढ़ा", 21 to "उत्तराषाढ़ा", 22 to "श्रवण", 23 to "धनिष्ठा",
        24 to "शतभिषा", 25 to "पूर्व भाद्रपद", 26 to "उत्तर भाद्रपद", 27 to "रेवती"
    )

    private val yogaHindi: Map<Int, String> = mapOf(
        1 to "विष्कम्भ", 2 to "प्रीति", 3 to "आयुष्मान्", 4 to "सौभाग्य", 5 to "शोभन",
        6 to "अतिगण्ड", 7 to "सुकर्मा", 8 to "धृति", 9 to "शूल", 10 to "गण्ड",
        11 to "वृद्धि", 12 to "ध्रुव", 13 to "व्याघात", 14 to "हर्षण", 15 to "वज्र",
        16 to "सिद्धि", 17 to "व्यतीपात", 18 to "वरीयान्", 19 to "परिघ", 20 to "शिव",
        21 to "सिद्ध", 22 to "साध्य", 23 to "शुभ", 24 to "शुक्ल", 25 to "ब्रह्म",
        26 to "ऐन्द्र", 27 to "वैधृति"
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

    fun tithiHi(number: Int, fallback: String): String = tithiHindi[number] ?: fallback

    fun nakshatraHi(number: Int, fallback: String): String = nakshatraHindi[number] ?: fallback

    fun yogaHi(number: Int, fallback: String): String = yogaHindi[number] ?: fallback

    fun karanaHi(name: String): String = karanaByNameHindi[name.lowercase().trim()] ?: name

    fun vaaraHi(weekday: Int, fallback: String): String = vaaraHindiByWeekday[weekday] ?: fallback

    fun observanceHi(id: String, fallbackTitle: String): String {
        val lowerId = id.lowercase()
        return when {
            lowerId.startsWith("ekadashi") -> "एकादशी व्रत"
            lowerId == "purnima-12" -> "फाल्गुन पूर्णिमा"
            lowerId == "purnima-4" -> "गुरु पूर्णिमा"
            lowerId == "purnima-5" -> "श्रावण पूर्णिमा"
            lowerId == "purnima-8" -> "कार्तिक पूर्णिमा"
            lowerId.startsWith("purnima") -> "पूर्णिमा व्रत"
            lowerId == "amavasya-7" -> "दीपावली अमावस्या"
            lowerId == "amavasya-11" -> "मौनी अमावस्या"
            lowerId == "amavasya-6" -> "सर्वपितृ अमावस्या"
            lowerId.startsWith("amavasya") -> "अमावस्या"
            lowerId.startsWith("pradosha") -> "प्रदोष व्रत"
            lowerId.startsWith("chaturthi") && fallbackTitle.contains("Saṅkaṣṭī", true) -> "संकष्टी चतुर्थी"
            lowerId.startsWith("chaturthi") -> "चतुर्थी व्रत"
            lowerId.startsWith("shivaratri") && fallbackTitle.contains("Mahā", true) -> "महाशिवरात्रि"
            lowerId.startsWith("shivaratri") -> "मासिक शिवरात्रि"
            lowerId == "chaitra-navaratri" -> "चैत्र नवरात्रि"
            lowerId == "rama-navami" -> "श्री राम नवमी"
            lowerId == "krishna-janmashtami" -> "श्री कृष्ण जन्माष्टमी"
            lowerId == "vijayadashami" -> "विजयादशमी"
            lowerId == "vasant-panchami" -> "वसन्त पञ्चमी"
            else -> fallbackTitle
        }
    }
}
