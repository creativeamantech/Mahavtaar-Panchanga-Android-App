package com.mahavtaar.panchanga.ui.localization

import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.VratObservance
import java.util.Locale

data class VedicGlossaryEntry(
    val id: String,
    val termEn: String,
    val termHi: String,
    val categoryEn: String,
    val categoryHi: String,
    val definitionEn: String,
    val definitionHi: String,
    val usageTipEn: String,
    val usageTipHi: String
)

data class VaaraAttributes(
    val deity: String,
    val ruler: String,
    val color: String,
    val metal: String
)

object VedicGlossaryCatalog {

    val entries: List<VedicGlossaryEntry> = listOf(
        VedicGlossaryEntry(
            id = "tithi",
            termEn = "Tithi (Lunar Day)",
            termHi = "तिथि (चन्द्र दिवस)",
            categoryEn = "Five Angas",
            categoryHi = "पञ्चाङ्ग",
            definitionEn = "Each Tithi equals 12° of angular separation between Moon and Sun sidereal longitudes, spanning 30 Tithis across Śukla and Kṛṣṇa Pakṣas.",
            definitionHi = "सूर्य और चन्द्रमा के बीच प्रत्येक १२° के कोणीय अन्तर से एक तिथि बनती है। शुक्ल एवं कृष्ण पक्ष मिलाकर कुल ३० तिथियाँ होती हैं।",
            usageTipEn = "Avoid Riktā Tithis (4, 9, 14) for new auspicious ventures; prefer Pūrṇā and Bhadrā Tithis.",
            usageTipHi = "शुभ आरम्भ के लिए रिक्ता (४, ९, १४) तिथियाँ टालें तथा नन्दा, भद्रा और पूर्णा तिथियाँ चुनें।"
        ),
        VedicGlossaryEntry(
            id = "nakshatra",
            termEn = "Nakṣatra (Lunar Mansion)",
            termHi = "नक्षत्र (तारा मण्डल)",
            categoryEn = "Five Angas",
            categoryHi = "पञ्चाङ्ग",
            definitionEn = "One of 27 equal 13°20' divisions of the sidereal zodiac traversed by the Moon, each subdivided into 4 Padas of 3°20'.",
            definitionHi = "भचक्र के ३६०° के २७ बराबर १३°२०' खण्डों में से एक, जिसे चन्द्रमा पार करता है। प्रत्येक नक्षत्र के ४ चरण (३°२०') होते हैं।",
            usageTipEn = "Compare daily transit Nakṣatra against birth star via Navtārā Cakra for daily alignment.",
            usageTipHi = "दैनिक अनुकूलता के लिए गोचर नक्षत्र की तुलना जन्म नक्षत्र से नवतारा चक्र द्वारा करें।"
        ),
        VedicGlossaryEntry(
            id = "yoga",
            termEn = "Nitya Yoga",
            termHi = "नित्य योग",
            categoryEn = "Five Angas",
            categoryHi = "पञ्चाङ्ग",
            definitionEn = "Sum of sidereal longitudes of Sun and Moon divided into 27 segments of 13°20' each, governing overall daily harmony.",
            definitionHi = "सूर्य एवं चन्द्र के भोगांशों का योग जिसे १३°२०' के २७ भागों में बाँटा गया है। यह दिन की समग्र ऊर्जा दर्शाता है।",
            usageTipEn = "Siddha, Śubha, and Amṛta Yogas amplify auspicious actions while Vyatīpāta and Vaidhṛti favor meditation.",
            usageTipHi = "सिद्ध, शुभ और अमृत योग कार्य सिद्धि देते हैं, जबकि व्यतीपात व वैधृति ध्यान व जप के लिए श्रेष्ठ हैं।"
        ),
        VedicGlossaryEntry(
            id = "karana",
            termEn = "Karaṇa (Half-Tithi)",
            termHi = "करण (अर्ध-तिथि)",
            categoryEn = "Five Angas",
            categoryHi = "पञ्चाङ्ग",
            definitionEn = "Half of a Tithi (6° elongation). Seven movable and four fixed Karaṇas cycle through the 60 half-Tithis of a lunar month.",
            definitionHi = "तिथि का आधा भाग (६° अन्तर)। ७ चर और ४ स्थिर करण एक चन्द्र मास के ६० अर्ध-भागों में क्रमशः चलते हैं।",
            usageTipEn = "Viṣṭi (Bhadra) Karaṇa requires caution for mundane starts, whereas Bava, Bālava, and Kaulava favor constructive tasks.",
            usageTipHi = "विष्टि (भद्रा) करण में नवीन शुभ कार्य वर्जित माने जाते हैं; बव, बालव व कौलव उत्तम हैं।"
        ),
        VedicGlossaryEntry(
            id = "vara",
            termEn = "Vāra (Vedic Weekday)",
            termHi = "वार (वैदिक दिवस)",
            categoryEn = "Five Angas",
            categoryHi = "पञ्चाङ्ग",
            definitionEn = "Runs strictly from local Sunrise to the next local Sunrise—not midnight—governed by its ruling Graha (Sun through Saturn).",
            definitionHi = "वैदिक वार मध्यरात्रि से नहीं बल्कि स्थानीय सूर्योदय से अगले सूर्योदय तक चलता है, जिसके स्वामी सूर्य से शनि तक सात ग्रह हैं।",
            usageTipEn = "The first Hora after Sunrise always belongs to the day's Vāra lord.",
            usageTipHi = "सूर्योदय के ठीक बाद की पहली होरा सदैव उसी दिन के वार स्वामी की होती है।"
        ),
        VedicGlossaryEntry(
            id = "rahu_kala",
            termEn = "Rāhu Kāla",
            termHi = "राहु काल",
            categoryEn = "Inauspicious Kāla",
            categoryHi = "त्याज्य काल",
            definitionEn = "A daily 1/8th division of daytime (approx. 90 minutes) ruled by shadow node Rāhu, reserved for spiritual discipline rather than material starts.",
            definitionHi = "दिनमान के ८ बराबर भागों में से राहु शासित लगभग ९० मिनट का खण्ड, जिसमें नवीन शुभ कार्य आरम्भ नहीं किए जाते।",
            usageTipEn = "Ongoing work already started before Rāhu Kāla may continue without interruption.",
            usageTipHi = "राहु काल से पूर्व आरम्भ हो चुके नियमित कार्य बिना बाधा के जारी रखे जा सकते हैं।"
        ),
        VedicGlossaryEntry(
            id = "abhijit",
            termEn = "Abhijit Muhūrta",
            termHi = "अभिजित मुहूर्त",
            categoryEn = "Auspicious Muhūrta",
            categoryHi = "शुभ मुहूर्त",
            definitionEn = "The 8th Muhūrta centered on local solar noon (~48 minutes), praised in classics as able to overcome minor daily doshas.",
            definitionHi = "स्थानीय मध्याह्न के दोनों ओर फैला ८वाँ मुहूर्त (लगभग ४८ मिनट), जो अनेक सामान्य दोषों का शमन कर विजय दिलाता है।",
            usageTipEn = "Traditionally avoided only when overlapping Wednesday's caution span if extreme precision is sought.",
            usageTipHi = "सफलता और महत्त्वपूर्ण निर्णयों के लिए मध्याह्न का यह काल सर्वाधिक उत्तम माना जाता है।"
        ),
        VedicGlossaryEntry(
            id = "yamaganda",
            termEn = "Yamagaṇḍa & Gulika",
            termHi = "यमगण्ड एवं गुलिक काल",
            categoryEn = "Inauspicious Kāla",
            categoryHi = "त्याज्य काल",
            definitionEn = "Two 1/8th daytime divisions ruled by Yama and Saturn's upagraha Gulika, avoided for commencing journeys or ceremonies.",
            definitionHi = "यम और शनि के उपग्रह गुलिक द्वारा शासित दिनमान के दो अष्टमांश खण्ड, जिन्हें यात्रा या माङ्गलिक आरम्भ हेतु टाला जाता है।",
            usageTipEn = "Good for clearing debts, introspection, or administrative clean-up rather than fresh launches.",
            usageTipHi = "ऋण मुक्ति, आत्म-मन्थन एवं पुराने कार्यों के समापन हेतु उपयुक्त, नवीन आरम्भ से बचें।"
        ),
        VedicGlossaryEntry(
            id = "amrit_kalam",
            termEn = "Amṛta Kālam & Brahma Muhūrta",
            termHi = "अमृत काल एवं ब्रह्म मुहूर्त",
            categoryEn = "Auspicious Muhūrta",
            categoryHi = "शुभ मुहूर्त",
            definitionEn = "Brahma Muhūrta spans ~96 to ~48 minutes before sunrise for meditation; Amṛta Kālam is the nectarine window derived from the active Nakṣatra.",
            definitionHi = "सूर्योदय से ९६ से ४८ मिनट पूर्व का समय ब्रह्म मुहूर्त तथा सक्रिय नक्षत्र के अनुसार दिन का सर्वाधिक शुभ अंश अमृत काल कहलाता है।",
            usageTipEn = "Ideal for sādhana, study, mantra initiation, and sacred vows.",
            usageTipHi = "साधना, अध्ययन, मन्त्र जप और संकल्प लेने के लिए सर्वोत्तम समय।"
        ),
        VedicGlossaryEntry(
            id = "choghadiya",
            termEn = "Gauri Choghadiya",
            termHi = "गौरी चौघड़िया",
            categoryEn = "Muhūrta System",
            categoryHi = "मुहूर्त प्रणाली",
            definitionEn = "Divides both Day and Night into 8 equal parts named Amṛt, Śubha, Lābha, Chara (favorable) and Udvega, Kāla, Roga (unfavorable).",
            definitionHi = "दिन व रात्रि को ८-८ बराबर भागों में बाँटकर अमृत, शुभ, लाभ, चर (शुभ) तथा उद्वेग, काल, रोग (अशुभ) चौघड़िया निर्धारित किए जाते हैं।",
            usageTipEn = "Chara (Moon) is favored for travel; Lābha (Mercury) for commerce; Amṛt/Śubha for ceremonies.",
            usageTipHi = "यात्रा हेतु चर, व्यापार हेतु लाभ तथा पूजन व शुभ कार्यों हेतु अमृत व शुभ चौघड़िया चुनें।"
        ),
        VedicGlossaryEntry(
            id = "hora",
            termEn = "Planetary Hora",
            termHi = "ग्रह होरा",
            categoryEn = "Micro-Timing",
            categoryHi = "सूक्ष्म काल",
            definitionEn = "Divides local Day into 12 equal Sunrise-to-Sunset Horas and Night into 12 Sunset-to-Sunrise Horas following the Chaldean speed order.",
            definitionHi = "दिन को सूर्योदय से सूर्यास्त तक १२ तथा रात्रि को १२ होराओं में बाँटा गया है, जो शनि-गुरु-मङ्गल-सूर्य-शुक्र-बुध-चन्द्र क्रम से चलती हैं।",
            usageTipEn = "Jupiter & Venus Horas favor auspicious commitments; Mars suits bold physical effort; Saturn favors discipline.",
            usageTipHi = "गुरु व शुक्र होरा माङ्गलिक कार्यों के लिए, मङ्गल साहसिक कार्य हेतु तथा शनि स्थिर व दीर्घकालिक कार्य हेतु श्रेष्ठ हैं।"
        ),
        VedicGlossaryEntry(
            id = "swara",
            termEn = "Swara Yoga (Śiva Svarodaya)",
            termHi = "स्वर योग (शिव स्वरोदय)",
            categoryEn = "Prāṇic Rhythm",
            categoryHi = "प्राण विज्ञान",
            definitionEn = "Correlates alternating lunar (Iḍā / Left / Cool) and solar (Piṅgalā / Right / Warm) nostril breath dominance with Pakṣa and weekday cycles.",
            definitionHi = "पक्ष एवं वार के आधार पर चन्द्र नाड़ी (इड़ा / बायाँ स्वर / शीतल) और सूर्य नाड़ी (पिङ्गला / दायाँ स्वर / उष्ण) के स्वाभाविक प्रवाह का विज्ञान।",
            usageTipEn = "Wait for Svarodaya synchronization before major departures, learning, or decisive conversations.",
            usageTipHi = "महत्त्वपूर्ण यात्रा, विद्या आरम्भ या संवाद से पूर्व सक्रिय स्वर की अनुकूलता अवश्य जाँचें।"
        ),
        VedicGlossaryEntry(
            id = "tattva",
            termEn = "Pañca Mahābhūta Tattva",
            termHi = "पञ्च महाभूत तत्त्व",
            categoryEn = "Prāṇic Rhythm",
            categoryHi = "प्राण विज्ञान",
            definitionEn = "Each 60-minute cycle cycles through Ākāśa, Vāyu, Agni, Pṛthvī, and Jala in traditional Swara proportions (6/12/18/24/12 or weighted bands).",
            definitionHi = "प्रत्येक चक्र में आकाश, वायु, अग्नि, पृथ्वी और जल तत्त्वों का क्रमशः उदय होता है, जो कार्य की प्रकृति और परिणाम को प्रभावित करता है।",
            usageTipEn = "Pṛthvī & Jala support enduring creation; Agni & Vāyu bring rapid motion; Ākāśa serves contemplation.",
            usageTipHi = "पृथ्वी व जल तत्त्व स्थिर निर्माण व समृद्धि के लिए, अग्नि व वायु गति के लिए और आकाश ध्यान के लिए श्रेष्ठ हैं।"
        ),
        VedicGlossaryEntry(
            id = "panchaka_bhadra",
            termEn = "Pañcaka & Diśā Śūla",
            termHi = "पञ्चक एवं दिशा शूल",
            categoryEn = "Travel & Safety",
            categoryHi = "यात्रा व शुभाशुभ",
            definitionEn = "Pañcaka occurs when Moon transits Dhaniṣṭhā (later half) to Revatī; Diśā Śūla marks the direction unfavored on a given weekday.",
            definitionHi = "धनिष्ठा के उत्तरार्ध से रेवती तक चन्द्र गोचर पञ्चक कहलाता है तथा वार के अनुसार यात्रा की वर्जित दिशा को दिशा शूल कहते हैं।",
            usageTipEn = "Traditional parihāra (remedy) allows necessary travel after consuming a small auspicious offering before departing.",
            usageTipHi = "आवश्यक यात्रा से पूर्व वार के अनुसार शास्त्रीय परिहार (जैसे दही, पान, गुड़ या धनिया ग्रहण) कर प्रस्थान करें।"
        )
    )

    fun vaaraAttributes(weekday: Int, lang: AppLanguage): VaaraAttributes = when (weekday % 7) {
        0 -> if (lang == AppLanguage.HINDI) {
            VaaraAttributes("सूर्य नारायण", "सूर्य", "लाल / केसरिया", "ताँबा")
        } else {
            VaaraAttributes("Sūrya Nārāyaṇa", "Sun", "Ruby / Saffron", "Copper")
        }
        1 -> if (lang == AppLanguage.HINDI) {
            VaaraAttributes("भगवान शिव", "चन्द्रमा", "श्वेत / मोती", "चाँदी")
        } else {
            VaaraAttributes("Lord Śiva", "Moon", "White / Silver", "Silver")
        }
        2 -> if (lang == AppLanguage.HINDI) {
            VaaraAttributes("श्री हनुमान जी", "मङ्गल", "सिन्दूरी लाल", "ताँबा / मूंगा")
        } else {
            VaaraAttributes("Lord Hanumān / Kārttikeya", "Mars", "Scarlet Red", "Copper")
        }
        3 -> if (lang == AppLanguage.HINDI) {
            VaaraAttributes("श्री गणेश / विष्णु", "बुध", "हरा / पन्ना", "काँसा")
        } else {
            VaaraAttributes("Lord Gaṇeśa / Viṣṇu", "Mercury", "Emerald Green", "Bronze")
        }
        4 -> if (lang == AppLanguage.HINDI) {
            VaaraAttributes("श्री बृहस्पति / दत्तात्रेय", "गुरु", "पीला / स्वर्ण", "स्वर्ण / पीतल")
        } else {
            VaaraAttributes("Bṛhaspati / Dattātreya", "Jupiter", "Golden Yellow", "Gold / Brass")
        }
        5 -> if (lang == AppLanguage.HINDI) {
            VaaraAttributes("माँ लक्ष्मी / देवी", "शुक्र", "श्वेत / गुलाबी", "चाँदी / प्लेटिनम")
        } else {
            VaaraAttributes("Mahālakṣmī / Devī", "Venus", "Ivory / Rose", "Silver")
        }
        else -> if (lang == AppLanguage.HINDI) {
            VaaraAttributes("शनि देव / भैरव", "शनि", "नीला / काला", "लोहा")
        } else {
            VaaraAttributes("Śani Deva / Hanumān", "Saturn", "Indigo / Navy", "Iron")
        }
    }

    fun rasiNadiBadge(rasiOneBased: Int, lang: AppLanguage): Pair<String, SwaraNadi> {
        val normalized = ((rasiOneBased - 1) % 12 + 12) % 12 + 1
        val isOdd = normalized % 2 == 1
        return if (isOdd) {
            val label = if (lang == AppLanguage.HINDI) "पिङ्गला (विषम)" else "Piṅgalā (Odd)"
            label to SwaraNadi.PINGALA
        } else {
            val label = if (lang == AppLanguage.HINDI) "इड़ा (सम)" else "Iḍā (Even)"
            label to SwaraNadi.IDA
        }
    }

    fun formatGhatiPala(sunriseMs: Long, nowMs: Long, lang: AppLanguage): String {
        val daySpanMs = 86_400_000L
        val elapsed = ((nowMs - sunriseMs) % daySpanMs + daySpanMs) % daySpanMs
        val totalVighatis = elapsed / 400L // 1 Vighati = 0.4s, 60 Vighatis = 1 Pala = 24s
        val totalPalas = elapsed / 24_000L
        val ghati = (totalPalas / 60L).toInt()
        val pala = (totalPalas % 60L).toInt()
        val vighati = (totalVighatis % 60L).toInt()
        return if (lang == AppLanguage.HINDI) {
            String.format(Locale.US, "%02d घटी %02d पल", ghati, pala)
        } else {
            String.format(Locale.US, "%02d Gh %02d Pa (%02d)", ghati, pala, vighati)
        }
    }

    fun formatLiveCountdown(diffMs: Long, lang: AppLanguage): String {
        if (diffMs <= 0L) return if (lang == AppLanguage.HINDI) "० से" else "0s"
        val totalSec = diffMs / 1000L
        val days = totalSec / 86400L
        val hours = (totalSec % 86400L) / 3600L
        val mins = (totalSec % 3600L) / 60L
        val secs = totalSec % 60L
        return if (lang == AppLanguage.HINDI) {
            when {
                days > 0L -> "${days} दि ${hours} घं ${mins} मि"
                hours > 0L -> String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
                else -> String.format(Locale.US, "%02d मि %02d से", mins, secs)
            }
        } else {
            when {
                days > 0L -> "${days}d ${hours}h ${mins}m"
                hours > 0L -> String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
                else -> String.format(Locale.US, "%02dm %02ds", mins, secs)
            }
        }
    }

    fun horaPlanetaryFocus(planetId: String, lang: AppLanguage): Pair<String, String> = when (planetId.lowercase()) {
        "sun" -> if (lang == AppLanguage.HINDI) {
            "नेतृत्व, सरकारी कार्य, पिता/अधिकारी भेंट, पदभार" to "दीर्घकालिक साझेदारी विवाद"
        } else {
            "Leadership, government liaison, authority, vitality" to "Ego clashes or secret disputes"
        }
        "moon" -> if (lang == AppLanguage.HINDI) {
            "यात्रा, जलीय कार्य, मातृ सेवा, काव्य व रचनात्मक चिन्तन" to "कठोर वित्तीय सौदे"
        } else {
            "Travel, creative flow, caregiving, agriculture, intuition" to "Rigid legal confrontations"
        }
        "mars" -> if (lang == AppLanguage.HINDI) {
            "व्यायाम, भूमि/निर्माण, साहसिक निर्णय, यन्त्र कार्य" to "शान्ति वार्ता एवं विवाह सम्बन्ध"
        } else {
            "Athletics, engineering, property work, decisive action" to "Delicate diplomacy or matrimony"
        }
        "mercury" -> if (lang == AppLanguage.HINDI) {
            "अध्ययन, लेखन, लेखा-जोखा, व्यापार एवं संवाद" to "अत्यधिक भावनात्मक निर्णय"
        } else {
            "Study, writing, trade, accounting, contracts, coding" to "Impulsive physical risks"
        }
        "jupiter" -> if (lang == AppLanguage.HINDI) {
            "माङ्गलिक कार्य, विवाह, शिक्षा, दान, आध्यात्मिक दीक्षा" to "अहङ्कार एवं अनैतिक कार्य"
        } else {
            "Weddings, education, dharma, investments, mentorship" to "Harsh disputes or shortcuts"
        }
        "venus" -> if (lang == AppLanguage.HINDI) {
            "कला, संगीत, वस्त्र-आभूषण, मैत्री एवं गृह सज्जा" to "कठोर तप या शुष्क विवाद"
        } else {
            "Arts, music, harmony, home beauty, joyous gatherings" to "Harsh confrontations"
        }
        else -> if (lang == AppLanguage.HINDI) {
            "दीर्घकालिक नियोजन, सेवा कार्य, लोहा/भूमि एवं ध्यान" to "शीघ्र परिणाम की अपेक्षा वाले नए आरम्भ"
        } else {
            "Long-term discipline, service, structural planning, patience" to "Rushed launches & celebrations"
        }
    }

    fun observanceSignificance(obs: VratObservance, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) {
            return when (obs.category) {
                "ekadashi" -> "Observed on the 11th lunar Tithi dedicated to Lord Viṣṇu for inner purification, steadiness of mind, and spiritual upliftment."
                "purnima" -> "Full Moon culmination honoring Śrī Satyanārāyaṇa, sacred bathing, charity, and gratitude."
                "amavasya" -> "New Moon conjunction reserved for Pitṛ tarpaṇa, ancestral remembrance, and quiet contemplation."
                "pradosham" -> "Twilight Trayodaśī observance when Lord Śiva and Devī Pārvatī shower grace upon devotees."
                "chaturthi" -> "4th lunar Tithi honoring Lord Gaṇeśa to remove obstacles and grant clarity of intellect."
                "shivaratri" -> "Kṛṣṇa Caturdaśī vigil celebrating stillness and Śiva consciousness through the night."
                else -> "Major Vedic festival marking sacred seasonal, solar, or divine celebration."
            }
        }
        return when (obs.category) {
            "ekadashi" -> "भगवान श्री महाविष्णु को समर्पित एकादशी व्रत चित्त शुद्धि, संयम एवं आध्यात्मिक उन्नति का सर्वोत्तम साधन है।"
            "purnima" -> "पूर्ण चन्द्र की यह पावन तिथि श्री सत्यनारायण कथा, पवित्र स्नान, दान एवं कृतज्ञता हेतु समर्पित है।"
            "amavasya" -> "दर्श अमावस्या पूर्वजों के स्मरण, पितृ तर्पण, दान और मौन आत्म-चिन्तन का विशेष पर्व है।"
            "pradosham" -> "त्रयोदशी के सूर्यास्त प्रदोष काल में भगवान शिव एवं माता पार्वती की आराधना कष्ट निवारक मानी जाती है।"
            "chaturthi" -> "विघ्नहर्ता श्री गणेश की आराधना से बुद्धि शुद्धि, सिद्धि और कार्यों में निर्विघ्न सफलता प्राप्त होती है।"
            "shivaratri" -> "कृष्ण चतुर्दशी रात्रि जागरण एवं अभिषेक द्वारा शिव तत्व में लीन होने का महापर्व है।"
            else -> "सनातन धर्म का पावन ऋतु एवं देव आराधना उत्सव।"
        }
    }

    fun observancePractices(obs: VratObservance, lang: AppLanguage): List<String> {
        if (lang == AppLanguage.ENGLISH) {
            return when (obs.category) {
                "ekadashi" -> listOf(
                    "Morning saṅkalpa with tulasi & Viṣṇu Sahasranāma",
                    "Sattvic fruit/fasting observance; avoid grains & rice",
                    "Break fast (Pāraṇa) on Dvādaśī during proper morning window"
                )
                "purnima" -> listOf(
                    "Perform Śrī Satyanārāyaṇa Pūjā or read Kathā",
                    "Offer arghya to Moon after moonrise",
                    "Practice charitable giving of food or white garments"
                )
                "amavasya" -> listOf(
                    "Offer water and black sesame (Pitṛ Tarpaṇa) at noon",
                    "Light a sesame oil lamp in the evening",
                    "Feed cows, birds, or those in need"
                )
                "pradosham" -> listOf(
                    "Bathe before sunset and worship during Pradosha dusk window",
                    "Offer Bilva leaves and Abhiṣeka to Śiva Liṅga",
                    "Chant Om Namaḥ Śivāya or Mahāmṛtyuñjaya Mantra"
                )
                "chaturthi" -> listOf(
                    "Offer Dūrvā grass, red hibiscus, and Modaka to Gaṇeśa",
                    "Recite Saṅkaṭanāśana Gaṇeśa Stotram",
                    "If Saṅkaṣṭī, offer Moon arghya before breaking fast"
                )
                "shivaratri" -> listOf(
                    "Observe four-Prahara night worship & Rudrābhiṣeka",
                    "Offer Bilva patra, sandalwood, and pure water",
                    "Maintain night vigil in silent japa"
                )
                else -> listOf(
                    "Morning purification bath & Dīpa lighting",
                    "Chant traditional stotras of the presiding deity",
                    "Share festive prasāda & charity with family and community"
                )
            }
        }
        return when (obs.category) {
            "ekadashi" -> listOf(
                "प्रातः स्नान कर तुलसी दल अर्पण एवं विष्णु सहस्रनाम पाठ",
                "अन्न व चावल का त्याग कर सात्त्विक फलाहार अथवा उपवास",
                "द्वादशी तिथि के शुभ पारण काल में व्रत का पारण"
            )
            "purnima" -> listOf(
                "श्री सत्यनारायण भगवान की पूजा एवं कथा श्रवण",
                "चन्द्रोदय के पश्चात् चन्द्रदेव को अर्घ्य अर्पण",
                "अन्न, वस्त्र एवं यथाशक्ति दान-पुण्य"
            )
            "amavasya" -> listOf(
                "मध्याह्न में तिल व जल से पितृ तर्पण",
                "सन्ध्या समय तिल के तेल का दीप प्रज्वलन",
                "गाय, पक्षियों एवं जरूरतमन्दों को भोजन दान"
            )
            "pradosham" -> listOf(
                "सूर्यास्त से पूर्व स्नान कर प्रदोष काल में शिव पूजन",
                "शिवलिङ्ग पर बिल्वपत्र एवं जल/दुग्ध अभिषेक",
                "ॐ नमः शिवाय एवं पञ्चाक्षर मन्त्र जप"
            )
            "chaturthi" -> listOf(
                "दूर्वा दल, सिन्दूर एवं मोदक के साथ श्री गणेश पूजन",
                "संकटनाशन गणेश स्तोत्र व अथर्वशीर्ष पाठ",
                "संकष्टी में रात्रि चन्द्रोदय दर्शन व अर्घ्य उपरान्त पारण"
            )
            "shivaratri" -> listOf(
                "रात्रि के चारों प्रहर में रुद्राभिषेक व शिव ध्यान",
                "बिल्वपत्र, चन्दन एवं भस्म अर्पण",
                "रात्रि जागरण एवं महामृत्युञ्जय जप"
            )
            else -> listOf(
                "प्रातः स्नान, दीप प्रज्वलन एवं संकल्प",
                "अधिष्ठाता देव के स्तोत्र व आरती का गान",
                "परिवार संग प्रसाद वितरण एवं सत्कर्म"
            )
        }
    }
}
