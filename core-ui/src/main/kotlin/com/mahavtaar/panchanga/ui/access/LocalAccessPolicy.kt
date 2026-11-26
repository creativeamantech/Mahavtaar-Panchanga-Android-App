package com.mahavtaar.panchanga.ui.access

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.mahavtaar.panchanga.domain.model.AccessPolicy
import com.mahavtaar.panchanga.domain.model.AccessStatus
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.PermissionCategory
import com.mahavtaar.panchanga.domain.model.UserRole

val LocalAccessPolicy = staticCompositionLocalOf { AccessPolicy.MINIMAL_EVENTS }

@Composable
fun ProvideAccessPolicy(
    policy: AccessPolicy,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAccessPolicy provides policy, content = content)
}

object AccessPermissionCatalog {

    fun roleLabel(role: UserRole, lang: AppLanguage): String = when (role) {
        UserRole.ADMIN -> if (lang == AppLanguage.HINDI) "एडमिन (पूर्ण अधिकार)" else "Admin (Full Control)"
        UserRole.USER -> if (lang == AppLanguage.HINDI) "सामान्य यूज़र" else "Standard User"
    }

    fun statusLabel(status: AccessStatus, lang: AppLanguage): String = when (status) {
        AccessStatus.PENDING -> if (lang == AppLanguage.HINDI) "अनुमोदन प्रतीक्षित" else "Pending Approval"
        AccessStatus.ACTIVE -> if (lang == AppLanguage.HINDI) "सक्रिय (अनुमत)" else "Active (Authorized)"
        AccessStatus.SUSPENDED -> if (lang == AppLanguage.HINDI) "निलंबित (रोका गया)" else "Suspended"
    }

    fun categoryTitle(category: PermissionCategory, lang: AppLanguage): String = when (category) {
        PermissionCategory.CORE_EVENTS ->
            if (lang == AppLanguage.HINDI) "1. मुख्य इवेंट्स एवं पंचांग पेज"
            else "1. Core Events & Panchanga Pages"
        PermissionCategory.TIMINGS_TRANSITS ->
            if (lang == AppLanguage.HINDI) "2. समय, होरा एवं ग्रह गोचर"
            else "2. Timings, Hora & Planetary Transits"
        PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI ->
            if (lang == AppLanguage.HINDI) "3. ज्योतिष, नवतारा, कुंडली एवं स्वर"
            else "3. Astrology, Navtara, Kundli & Swara"
        PermissionCategory.SYSTEM_EXPORTS ->
            if (lang == AppLanguage.HINDI) "4. स्थान, एक्सपोर्ट एवं सिस्टम सेटिंग्स"
            else "4. Locations, Exports & System Settings"
    }

    fun categorySubtitle(category: PermissionCategory, lang: AppLanguage): String = when (category) {
        PermissionCategory.CORE_EVENTS ->
            if (lang == AppLanguage.HINDI) "इवेंट टाइमलाइन, होम डैशबोर्ड, दैनिक पंचांग, कैलेंडर और शब्दकोश"
            else "Events timeline, Home dashboard, Daily Panchanga, Calendar, and Glossary"
        PermissionCategory.TIMINGS_TRANSITS ->
            if (lang == AppLanguage.HINDI) "होरा, आज की समय-सारणी, नवग्रह स्थिति और अलर्ट स्टूडियो"
            else "Hora, Today Schedule, Planetary Positions, and Notification Studio"
        PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI ->
            if (lang == AppLanguage.HINDI) "नवतारा चक्र, जन्म कुंडली, गुण मिलान, स्वर योग, तत्त्व एवं मंत्र"
            else "Navtara Chakra, Janam Kundli, Guna Milan, Swara Yoga, Tattva, and Mantras"
        PermissionCategory.SYSTEM_EXPORTS ->
            if (lang == AppLanguage.HINDI) "कस्टम कोऑर्डिनेट, HTML रिपोर्ट डाउनलोड/प्रिंट और गणना सेटिंग्स"
            else "Custom coordinates, HTML report export/print, and calculation settings"
    }

    fun pageTitle(page: AppPageKey, lang: AppLanguage): String = when (page) {
        AppPageKey.EVENTS_TIMELINE ->
            if (lang == AppLanguage.HINDI) "इवेंट्स टाइमलाइन (संक्षिप्त दृश्य)" else "Events Timeline (Minimal View)"
        AppPageKey.HOME ->
            if (lang == AppLanguage.HINDI) "मुख्य डैशबोर्ड (होम)" else "Full Home Dashboard"
        AppPageKey.DAILY_PANCHANGA ->
            if (lang == AppLanguage.HINDI) "दैनिक पंचांग (5 अंग व मुहूर्त)" else "Daily Panchanga (5 Angas & Muhurta)"
        AppPageKey.CALENDAR ->
            if (lang == AppLanguage.HINDI) "मासिक हिन्दू कैलेंडर" else "Monthly Hindu Calendar"
        AppPageKey.ANNUAL_FESTIVALS ->
            if (lang == AppLanguage.HINDI) "वार्षिक पर्व एवं व्रत सूची" else "Annual Festivals & Vratas"
        AppPageKey.HORA ->
            if (lang == AppLanguage.HINDI) "दैनिक होरा सारणी" else "Planetary Hora Table"
        AppPageKey.TODAY_SCHEDULE ->
            if (lang == AppLanguage.HINDI) "आज की पूर्ण समय-सारणी" else "Today's Chronological Schedule"
        AppPageKey.PLANETARY_POSITIONS ->
            if (lang == AppLanguage.HINDI) "नवग्रह स्पष्ट एवं गोचर" else "Planetary Positions (Navagraha)"
        AppPageKey.NAVTATA ->
            if (lang == AppLanguage.HINDI) "नवतारा चक्र एवं ताराबल" else "Navtara Chakra & Tarabala"
        AppPageKey.KUNDLI ->
            if (lang == AppLanguage.HINDI) "जन्म कुंडली एवं वर्ग चक्र" else "Janam Kundli & Varga Charts"
        AppPageKey.GUNA_MILAN ->
            if (lang == AppLanguage.HINDI) "अष्टकूट गुण मिलान" else "Ashtakoota Guna Milan"
        AppPageKey.SWARA_YOGA ->
            if (lang == AppLanguage.HINDI) "स्वर योग एवं नाड़ी संरेखण" else "Swara Yoga & Nadi Alignment"
        AppPageKey.TATTVA ->
            if (lang == AppLanguage.HINDI) "पंच तत्त्व एवं करण चक्र" else "Pancha Tattva & Karana Cycle"
        AppPageKey.NAVAGRAHA_MANTRAS ->
            if (lang == AppLanguage.HINDI) "नवग्रह मंत्र एवं वैदिक उपाय" else "Navagraha Mantras & Remedies"
        AppPageKey.LOCATIONS ->
            if (lang == AppLanguage.HINDI) "स्थान एवं शहर चयनकर्ता" else "Location & City Selector"
        AppPageKey.SETTINGS ->
            if (lang == AppLanguage.HINDI) "सेटिंग्स पेज" else "Settings Page"
        AppPageKey.ABOUT ->
            if (lang == AppLanguage.HINDI) "ऐप परिचय एवं सिद्धांत" else "About & Astronomical Notes"
    }

    fun pageSubtitle(page: AppPageKey, lang: AppLanguage): String = when (page) {
        AppPageKey.EVENTS_TIMELINE ->
            if (lang == AppLanguage.HINDI) "केवल आज के मुख्य इवेंट्स, मुहूर्त, व्रत व गोचर समय दिखाता है"
            else "Shows only active and upcoming events, windows, observances, and transits"
        AppPageKey.HOME ->
            if (lang == AppLanguage.HINDI) "सूर्य-चन्द्र आर्क, पंचांग सारांश और मॉड्यूल लॉन्चर"
            else "Celestial horizon arc, 5-limb overview, and module launcher"
        AppPageKey.DAILY_PANCHANGA ->
            if (lang == AppLanguage.HINDI) "तिथि, नक्षत्र, योग, करण, वार तथा चौघड़िया तालिका"
            else "Tithi, Nakshatra, Yoga, Karana, Vara, and Choghadiya tables"
        AppPageKey.CALENDAR ->
            if (lang == AppLanguage.HINDI) "मासिक तिथि ग्रिड और दैनिक पर्व चिह्न"
            else "Monthly lunar tithi grid and festival indicators"
        AppPageKey.ANNUAL_FESTIVALS ->
            if (lang == AppLanguage.HINDI) "पूरे वर्ष के एकादशी, प्रदोष, पूर्णिमा और प्रमुख त्योहार"
            else "Year-round Ekadashi, Pradosha, Purnima, and major festivals"
        AppPageKey.HORA ->
            if (lang == AppLanguage.HINDI) "दिन और रात के २४ होरा स्वामी व समय"
            else "24 day and night planetary Hora rulers and intervals"
        AppPageKey.TODAY_SCHEDULE ->
            if (lang == AppLanguage.HINDI) "सूर्योदय से अगले सूर्योदय तक सभी खगोलीय व मुहूर्त घटनाएँ"
            else "All events from sunrise to next sunrise with category filters"
        AppPageKey.PLANETARY_POSITIONS ->
            if (lang == AppLanguage.HINDI) "नवग्रहों के अंश, राशि, नक्षत्र पद, वक्री व अस्त स्थिति"
            else "Sidereal degrees, Rasi, Nakshatra pada, retrograde, and combustion"
        AppPageKey.NAVTATA ->
            if (lang == AppLanguage.HINDI) "२७ नक्षत्र मैट्रिक्स, ताराबल कैलेंडर व परिहार"
            else "27-star matrix, transit Tarabala calendar, and Parihara remedies"
        AppPageKey.KUNDLI ->
            if (lang == AppLanguage.HINDI) "लग्न/नवांश कुंडली, विंशोत्तरी दशा, योग एवं गोचर"
            else "D1/D9 charts, Vimshottari Dasha, Yogas, and Gochar transits"
        AppPageKey.GUNA_MILAN ->
            if (lang == AppLanguage.HINDI) "वर-कन्या ३६ गुण मिलान एवं मांगलिक संगति"
            else "36-point bride & groom compatibility and Manglik check"
        AppPageKey.SWARA_YOGA ->
            if (lang == AppLanguage.HINDI) "इड़ा, पिंगला, सुषुम्ना स्वर और नक्षत्र संधि"
            else "Ida, Pingala, Sushumna breath flow and Nakshatra rules"
        AppPageKey.TATTVA ->
            if (lang == AppLanguage.HINDI) "पृथ्वी, जल, अग्नि, वायु, आकाश तत्त्व चक्र"
            else "Prithvi, Jala, Agni, Vayu, and Akasha element cycles"
        AppPageKey.NAVAGRAHA_MANTRAS ->
            if (lang == AppLanguage.HINDI) "बीज, गायत्री, वेदोक्त मंत्र, जप माला व दान विधि"
            else "Beej, Gayatri, Vedokta mantras, Japa counter, and charities"
        AppPageKey.LOCATIONS ->
            if (lang == AppLanguage.HINDI) "शहर खोजने या GPS से स्थान बदलने की अनुमति"
            else "Search cities or detect GPS coordinates for calculation"
        AppPageKey.SETTINGS ->
            if (lang == AppLanguage.HINDI) "खाता, भाषा, थीम और ऐप प्राथमिकताएँ"
            else "Account, language, theme, and app preferences"
        AppPageKey.ABOUT ->
            if (lang == AppLanguage.HINDI) "खगोलीय इंजन और परंपरा विवरण"
            else "Astronomical engine and tradition notes"
    }

    fun featureTitle(feature: AppFeatureKey, lang: AppLanguage): String = when (feature) {
        AppFeatureKey.EVENT_DESCRIPTIONS ->
            if (lang == AppLanguage.HINDI) "इवेंट व व्रत की विस्तृत व्याख्या दिखाएँ"
            else "Show Detailed Event & Vrat Explanations"
        AppFeatureKey.GLOSSARY_DICTIONARY ->
            if (lang == AppLanguage.HINDI) "वैदिक शब्दकोश (Glossary / Dictionary) दिखाएँ"
            else "Show Vedic Glossary & Dictionary Definitions"
        AppFeatureKey.NOTIFICATIONS_STUDIO ->
            if (lang == AppLanguage.HINDI) "कस्टम अलार्म एवं नोटिफिकेशन स्टूडियो"
            else "Custom Alarms & Notification Studio"
        AppFeatureKey.TATTVA_NADI_DETAILS ->
            if (lang == AppLanguage.HINDI) "सूक्ष्म तत्त्व एवं स्वर नाड़ी विवरण"
            else "Micro-Tattva & Swara Nadi Details"
        AppFeatureKey.NAVTATA_JOURNAL_SAVE ->
            if (lang == AppLanguage.HINDI) "नवतारा प्रोफ़ाइल, डायरी एवं रिपोर्ट सेव करना"
            else "Save Navtara Profiles, Journal & Cloud Reports"
        AppFeatureKey.KUNDLI_ADVANCED_TABS ->
            if (lang == AppLanguage.HINDI) "कुंडली उन्नत टैब (षड्बल, अष्टकवर्ग, KP, जैमिनी)"
            else "Advanced Kundli Tabs (Shadbala, Ashtakavarga, KP, Jaimini)"
        AppFeatureKey.KUNDLI_SAVE_IMPORT ->
            if (lang == AppLanguage.HINDI) "कुंडली प्रोफ़ाइल सेव और JSON इम्पोर्ट"
            else "Save Kundli Charts & Import JSON"
        AppFeatureKey.TATTVA_HERO_GUIDANCE ->
            if (lang == AppLanguage.HINDI) "तत्त्व: सक्रिय गुण एवं मार्गदर्शन विवरण"
            else "Tattva: Active Quality & Guidance Notes"
        AppFeatureKey.TATTVA_TEST_CHIME ->
            if (lang == AppLanguage.HINDI) "तत्त्व: टेस्ट चाइम एवं हैप्टिक बटन"
            else "Tattva: Test Chime & Haptic Button"
        AppFeatureKey.TATTVA_NADI_POLARITY ->
            if (lang == AppLanguage.HINDI) "तत्त्व: इड़ा/पिंगला नाड़ी बैज एवं 60-मिनट पट्टी"
            else "Tattva: Ida/Pingala Nadi Badges & 60m Strip"
        AppFeatureKey.TATTVA_FOUR_PILLAR_GRID ->
            if (lang == AppLanguage.HINDI) "तत्त्व: 4-स्तम्भ सारांश कार्ड (वार/तिथि/होरा/करण)"
            else "Tattva: 4-Pillar Quick Grid (Vara/Tithi/Hora/Karana)"
        AppFeatureKey.TATTVA_HARMONY_REPORT ->
            if (lang == AppLanguage.HINDI) "तत्त्व: 4-स्तम्भ सामंजस्य (Peak Harmony) रिपोर्ट"
            else "Tattva: 4-Pillar Alignment & Peak Harmony"
        AppFeatureKey.TATTVA_HORA_STRIP ->
            if (lang == AppLanguage.HINDI) "तत्त्व: वर्तमान होरा 5-उप-तत्त्व पट्टी"
            else "Tattva: Current Hora 5-Tattva Strip"
        AppFeatureKey.TATTVA_VARA_SUMMARY ->
            if (lang == AppLanguage.HINDI) "तत्त्व: १. वार तत्त्व आज का मुख्य कार्ड"
            else "Tattva: 1. Today's Vara Tattva Banner"
        AppFeatureKey.TATTVA_VARA_MASTER_TABLE ->
            if (lang == AppLanguage.HINDI) "तत्त्व: १. संपूर्ण ७-वार मास्टर तालिका"
            else "Tattva: 1. 7-Weekday Master Table"
        AppFeatureKey.TATTVA_TITHI_SUMMARY ->
            if (lang == AppLanguage.HINDI) "तत्त्व: २. तिथि आरम्भ/समाप्ति पैनल"
            else "Tattva: 2. Tithi Start & End Panels"
        AppFeatureKey.TATTVA_TITHI_DAY_TABLE ->
            if (lang == AppLanguage.HINDI) "तत्त्व: २. आज की तिथि समय-सारणी तालिका"
            else "Tattva: 2. Today's Tithi Transitions Table"
        AppFeatureKey.TATTVA_TITHI_MASTER_TABLE ->
            if (lang == AppLanguage.HINDI) "तत्त्व: २. संपूर्ण ३०-तिथि मास्टर तालिका"
            else "Tattva: 2. 30-Tithi Master Table"
        AppFeatureKey.TATTVA_KARANA_DAY_TABLE ->
            if (lang == AppLanguage.HINDI) "तत्त्व: ३. दैनिक अर्ध-तिथि करण तालिका"
            else "Tattva: 3. Daily Half-Tithi Karana Table"
        AppFeatureKey.TATTVA_KARANA_MASTER_TABLE ->
            if (lang == AppLanguage.HINDI) "तत्त्व: ३. संपूर्ण ११-करण मास्टर तालिका"
            else "Tattva: 3. 11-Karana Master Reference Table"
        AppFeatureKey.TATTVA_MACRO_KARANA_CYCLE ->
            if (lang == AppLanguage.HINDI) "तत्त्व: ४. ६-मासिक करण तत्त्व चक्र व मास मिलान"
            else "Tattva: 4. 6-Month Macro Karana Cycle"
        AppFeatureKey.TATTVA_HORA_EXPLORER_TABLE ->
            if (lang == AppLanguage.HINDI) "तत्त्व: ५. २४-घंटे (120 काल) खोज समय-सारणी"
            else "Tattva: 5. 24-Hour (120-Window) Explorer"
        AppFeatureKey.TATTVA_MAHABHUTA_REFERENCE ->
            if (lang == AppLanguage.HINDI) "तत्त्व: ६. पंच महाभूत संदर्भ सूची व विवरण शीट"
            else "Tattva: 6. 5-Mahabhuta Reference & Inspector"
        AppFeatureKey.SWARA_TIER_SADHAK ->
            if (lang == AppLanguage.HINDI) "स्वर स्तर: साधक (बेसिक)"
            else "Swara Tier: Sādhaka (Basic)"
        AppFeatureKey.SWARA_TIER_YOGI ->
            if (lang == AppLanguage.HINDI) "स्वर स्तर: योगी (मध्यम)"
            else "Swara Tier: Yogī (Intermediate)"
        AppFeatureKey.SWARA_TIER_TAPASVI ->
            if (lang == AppLanguage.HINDI) "स्वर स्तर: तपस्वी (उन्नत + तत्त्व)"
            else "Swara Tier: Tapasvī (Advanced + Tattva)"
        AppFeatureKey.SWARA_GUIDANCE_WORKS ->
            if (lang == AppLanguage.HINDI) "स्वर: शुभ व त्याज्य कार्य मार्गदर्शन"
            else "Swara: Auspicious & Avoided Works Guidance"
        AppFeatureKey.SWARA_TITHI_RULES_TABLE ->
            if (lang == AppLanguage.HINDI) "स्वर: ३०-तिथि शुक्ल/कृष्ण सूर्योदय-सूर्यास्त तालिका"
            else "Swara: 30-Tithi Cycle Swara Rules Table"
        AppFeatureKey.SWARA_TATTVA_EXPLORER ->
            if (lang == AppLanguage.HINDI) "स्वर: पंच तत्त्व निर्देशिका व विवरण टैब"
            else "Swara: Pancha Tattva Explorer Tab"
        AppFeatureKey.SWARA_NAKSHATRA_ALIGNMENT ->
            if (lang == AppLanguage.HINDI) "स्वर: लाइव नक्षत्र संरेखण व सिम्युलेटर"
            else "Swara: Nakshatra Alignment & Simulator"
        AppFeatureKey.SWARA_MASTER_TABLES ->
            if (lang == AppLanguage.HINDI) "स्वर: २८-तारा व १२-राशि नाड़ी मास्टर तालिकाएँ"
            else "Swara: 28-Star & Zodiac Nadi Master Tables"
        AppFeatureKey.HTML_EXPORT_PRINT ->
            if (lang == AppLanguage.HINDI) "HTML रिपोर्ट डाउनलोड, प्रिंट एवं एक्सपोर्ट"
            else "HTML Report Download, Print & File Export"
        AppFeatureKey.CUSTOM_COORDINATES ->
            if (lang == AppLanguage.HINDI) "कस्टम अक्षांश/देशांतर कोऑर्डिनेट जोड़ना व बदलना"
            else "Add & Edit Custom Lat/Lon Coordinates"
        AppFeatureKey.CALCULATION_SETTINGS ->
            if (lang == AppLanguage.HINDI) "अयनांश, मास पद्धति व कैश सेटिंग्स बदलना"
            else "Modify Ayanamsha, Month System & Cache Settings"
    }

    fun featureSubtitle(feature: AppFeatureKey, lang: AppLanguage): String = when (feature) {
        AppFeatureKey.EVENT_DESCRIPTIONS ->
            if (lang == AppLanguage.HINDI) "बंद रहने पर केवल इवेंट का नाम और समय दिखेगा, अतिरिक्त शास्त्र टिप्पणी नहीं"
            else "When off, only event names and timestamps appear without scholarly commentary"
        AppFeatureKey.GLOSSARY_DICTIONARY ->
            if (lang == AppLanguage.HINDI) "बंद रहने पर शब्दकोश बटन और परिभाषा पॉपअप पूरी तरह छिपे रहेंगे"
            else "When off, all glossary buttons, dictionary sheets, and definitions are hidden"
        AppFeatureKey.NOTIFICATIONS_STUDIO ->
            if (lang == AppLanguage.HINDI) "कस्टम वॉयस/ध्वनि अलर्ट और रिमाइंडर नियम बनाने की सुविधा"
            else "Configure custom voice/sound alerts and reminder rules"
        AppFeatureKey.TATTVA_NADI_DETAILS ->
            if (lang == AppLanguage.HINDI) "होरा, पंचांग और समय-सारणी में नाड़ी संधि व तत्त्व चिप्स दिखाएँ"
            else "Display Nadi polarity badges and 5-Tattva breakdowns inside Hora & schedules"
        AppFeatureKey.NAVTATA_JOURNAL_SAVE ->
            if (lang == AppLanguage.HINDI) "व्यक्तिगत जन्म नक्षत्र प्रोफ़ाइल और दैनिक अनुभव क्लाउड में सहेजें"
            else "Persist native Nakshatra profiles, daily journal ratings, and period reports"
        AppFeatureKey.KUNDLI_ADVANCED_TABS ->
            if (lang == AppLanguage.HINDI) "कुंडली में षड्बल, अष्टकवर्ग, KP सब-लॉर्ड और जैमिनी कारक टैब खोलें"
            else "Unlock Shadbala, Ashtakavarga, KP cusps/sub-lords, and Jaimini Karaka tabs"
        AppFeatureKey.KUNDLI_SAVE_IMPORT ->
            if (lang == AppLanguage.HINDI) "कुंडली चार्ट को क्लाउड में सेव करने या JSON फ़ाइल लोड करने की सुविधा"
            else "Save birth charts to cloud storage and import external JSON chart files"
        AppFeatureKey.TATTVA_HERO_GUIDANCE ->
            if (lang == AppLanguage.HINDI) "बंद रखने पर केवल तत्त्व का नाम और समय दिखेगा, कोई अतिरिक्त विवरण नहीं"
            else "When off, hides quality subtitle and guidance paragraph from the main Hero card"
        AppFeatureKey.TATTVA_TEST_CHIME ->
            if (lang == AppLanguage.HINDI) "मुख्य कार्ड पर ध्वनि/हैप्टिक परीक्षण बटन दिखाएँ"
            else "Show Test Chime & Haptic button on the main Tattva card"
        AppFeatureKey.TATTVA_NADI_POLARITY ->
            if (lang == AppLanguage.HINDI) "तत्त्व पेज पर सभी इड़ा/पिंगला नाड़ी बैज और 60-मिनट नाड़ी पट्टी दिखाएँ"
            else "Show Ida/Pingala Nadi polarity badges and 60m alternating Nadi strip on Tattva"
        AppFeatureKey.TATTVA_FOUR_PILLAR_GRID ->
            if (lang == AppLanguage.HINDI) "वार, तिथि, होरा और करण तत्त्व का 4-कार्ड सारांश ग्रिड"
            else "4-card summary grid showing active Vara, Tithi, Hora, and Karana Tattva"
        AppFeatureKey.TATTVA_HARMONY_REPORT ->
            if (lang == AppLanguage.HINDI) "जब चारों स्तम्भों के तत्त्व मिलते हैं उन शुभ घंटों की रिपोर्ट"
            else "Peak Harmony Windows report showing when all 4 Tattva pillars align"
        AppFeatureKey.TATTVA_HORA_STRIP ->
            if (lang == AppLanguage.HINDI) "वर्तमान होरा के सभी 5 उप-तत्त्वों की प्रगति पट्टी व समय"
            else "Proportional progress strip and 5 sub-tattva timings for the active Hora"
        AppFeatureKey.TATTVA_VARA_SUMMARY ->
            if (lang == AppLanguage.HINDI) "आज के वार तत्त्व का सक्रिय बैनर"
            else "Today's active weekday Vara Tattva highlight banner"
        AppFeatureKey.TATTVA_VARA_MASTER_TABLE ->
            if (lang == AppLanguage.HINDI) "सोमवार से रविवार तक की 7-वार मास्टर तालिका बटन व सूची"
            else "Expandable 7-weekday Vara Tattva master reference table"
        AppFeatureKey.TATTVA_TITHI_SUMMARY ->
            if (lang == AppLanguage.HINDI) "तिथि आरम्भ/समाप्ति समय और तत्त्व तुलना पैनल"
            else "Side-by-side Tithi Start & End timing panels"
        AppFeatureKey.TATTVA_TITHI_DAY_TABLE ->
            if (lang == AppLanguage.HINDI) "आज बदलने वाली सभी तिथियों की समय-सारणी"
            else "Table of today's Tithi transitions and active time windows"
        AppFeatureKey.TATTVA_TITHI_MASTER_TABLE ->
            if (lang == AppLanguage.HINDI) "शुक्ल और कृष्ण पक्ष की सभी 30 तिथियों की संदर्भ तालिका"
            else "Expandable 30-Tithi Shukla & Krishna master reference table"
        AppFeatureKey.TATTVA_KARANA_DAY_TABLE ->
            if (lang == AppLanguage.HINDI) "आज के प्रत्येक अर्ध-तिथि करण व भद्रा चेतावनी की तालिका"
            else "Table of today's half-tithi Karana transitions and Bhadra cautions"
        AppFeatureKey.TATTVA_KARANA_MASTER_TABLE ->
            if (lang == AppLanguage.HINDI) "७ चर व ४ स्थिर करणों की संपूर्ण मास्टर तालिका"
            else "Expandable 11-Karana (Chara & Sthira) master reference table"
        AppFeatureKey.TATTVA_MACRO_KARANA_CYCLE ->
            if (lang == AppLanguage.HINDI) "चैत्र से शारदीय नवरात्रि तक ६-माह का करण चक्र व मास ओवरलैप"
            else "Chaitra to Sharad 6-month Karana Tattva cycle switcher and Maas overlaps"
        AppFeatureKey.TATTVA_HORA_EXPLORER_TABLE ->
            if (lang == AppLanguage.HINDI) "पूरे दिन-रात के १२० उप-तत्त्व काल खोजने की फ़िल्टर तालिका"
            else "Searchable 24-hour table of all 120 Hora sub-tattva time windows"
        AppFeatureKey.TATTVA_MAHABHUTA_REFERENCE ->
            if (lang == AppLanguage.HINDI) "पांचों महाभूतों की निर्देशिका और टैप करने पर विस्तृत शीट"
            else "All 5 Pancha Mahabhuta overview rows and tap-to-inspect bottom sheet"
        AppFeatureKey.SWARA_TIER_SADHAK ->
            if (lang == AppLanguage.HINDI) "सक्रिय नाड़ी बैनर और केवल सूर्योदय, मध्याह्न व सूर्यास्त के नाड़ी समय"
            else "Active Nadi banner plus Sunrise, Midday, and Sunset celestial windows with Nadis"
        AppFeatureKey.SWARA_TIER_YOGI ->
            if (lang == AppLanguage.HINDI) "सक्रिय नाड़ी बैनर और सूर्योदय, मध्याह्न, सूर्यास्त, चन्द्रोदय व चन्द्रास्त के सभी ५ समय"
            else "Active Nadi banner plus all 5 windows: Sunrise, Midday, Sunset, Moonrise, and Moonset with Nadis"
        AppFeatureKey.SWARA_TIER_TAPASVI ->
            if (lang == AppLanguage.HINDI) "पूर्ण तत्त्व हीरो, श्लोक, ५ खगोलीय काल तथा तिथि व नक्षत्र संधि नाड़ी सारणी"
            else "Full Hero with Tattva & Hora pills, Sloka, 5 windows, plus Tithi & Nakshatra boundary Swaras"
        AppFeatureKey.SWARA_GUIDANCE_WORKS ->
            if (lang == AppLanguage.HINDI) "सक्रिय नाड़ी के लिए शुभ व वर्जित कार्यों की सूची"
            else "Favorable and best-avoided activities for the currently active Nadi"
        AppFeatureKey.SWARA_TITHI_RULES_TABLE ->
            if (lang == AppLanguage.HINDI) "शुक्ल व कृष्ण पक्ष के ३० दिनों की सूर्योदय-सूर्यास्त नाड़ी तालिका"
            else "30-day Shukla & Krishna sunrise/sunset Swara rules with Paksha filters"
        AppFeatureKey.SWARA_TATTVA_EXPLORER ->
            if (lang == AppLanguage.HINDI) "पाँच महाभूतों के विस्तारित कार्ड व विवरण शीट का अलग टैब"
            else "Separate tab with 5 Pancha Tattva expandable cards and detail inspector"
        AppFeatureKey.SWARA_NAKSHATRA_ALIGNMENT ->
            if (lang == AppLanguage.HINDI) "आज की तिथि-नक्षत्र संरेखण जाँच और इंटरैक्टिव सिम्युलेटर"
            else "Live Tithi-Nakshatra Swara evaluation and interactive Paksha/Tithi/Pada simulator"
        AppFeatureKey.SWARA_MASTER_TABLES ->
            if (lang == AppLanguage.HINDI) "२८ नक्षत्र (अभिजित् सहित) और १२ राशियों की नाड़ी ध्रुवता तालिकाएँ"
            else "28-Star master Nadi table and 12-Zodiac Rasi polarity table"
        AppFeatureKey.HTML_EXPORT_PRINT ->
            if (lang == AppLanguage.HINDI) "पंचांग, नवतारा और कुंडली की HTML फ़ाइल डाउनलोड व प्रिंट करने का विकल्प"
            else "Download styled HTML files and print reports across Panchanga, Navtara, and Kundli"
        AppFeatureKey.CUSTOM_COORDINATES ->
            if (lang == AppLanguage.HINDI) "सटीक अक्षांश, देशांतर और टाइमज़ोन के साथ कस्टम स्थान सेव करना"
            else "Create, edit, or delete custom latitude/longitude coordinates and timezones"
        AppFeatureKey.CALCULATION_SETTINGS ->
            if (lang == AppLanguage.HINDI) "अयनांश (चित्रा/लहिरी/रमन/KP), अमान्त/पूर्णिमान्त और जन्म नक्षत्र बदलना"
            else "Change Ayanamsha model, Amanta/Purnimanta month system, and default Nakshatra"
    }
}
