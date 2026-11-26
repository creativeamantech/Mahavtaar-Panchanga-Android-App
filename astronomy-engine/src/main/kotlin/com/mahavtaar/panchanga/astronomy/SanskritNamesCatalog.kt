package com.mahavtaar.panchanga.astronomy

/**
 * Canonical Sanskrit diacritic names mirroring `src/data/sanskrit_names.json` 1:1.
 */
object SanskritNamesCatalog {

    val masas: Map<Int, String> = mapOf(
        1 to "Caitra",
        2 to "Vaiśākha",
        3 to "Jyeṣṭha",
        4 to "Āṣāḍha",
        5 to "Śrāvaṇa",
        6 to "Bhādrapada",
        7 to "Āśvina",
        8 to "Kārtika",
        9 to "Mārgaśīrṣa",
        10 to "Puṣya",
        11 to "Māgha",
        12 to "Phālguṇa"
    )

    data class EkadashiPair(val shukla: String, val krishna: String)

    val ekadashis: Map<Int, EkadashiPair> = mapOf(
        1 to EkadashiPair("Kāmadā", "Varūthinī"),
        2 to EkadashiPair("Mohinī", "Aparā"),
        3 to EkadashiPair("Nirjalā", "Yoginī"),
        4 to EkadashiPair("Devaśayanī", "Kāmikā"),
        5 to EkadashiPair("Pavitrā", "Ajā"),
        6 to EkadashiPair("Pārśva", "Indirā"),
        7 to EkadashiPair("Pāpāṅkuśā", "Ramā"),
        8 to EkadashiPair("Prabodhinī", "Utpannā"),
        9 to EkadashiPair("Mokṣadā", "Saphalā"),
        10 to EkadashiPair("Putradā", "Ṣaṭṭilā"),
        11 to EkadashiPair("Jayā", "Vijayā"),
        12 to EkadashiPair("Āmalakī", "Pāpamocanī")
    )

    val adhikaEkadashi = EkadashiPair("Padminī", "Paramā")

    val tithis: Map<Int, String> = mapOf(
        1 to "Śukla pakṣa prathamā",
        2 to "Śukla pakṣa dvitīyā",
        3 to "Śukla pakṣa tṛtīyā",
        4 to "Śukla pakṣa caturthī",
        5 to "Śukla pakṣa pañcamī",
        6 to "Śukla pakṣa ṣaṣṭhī",
        7 to "Śukla pakṣa saptamī",
        8 to "Śukla pakṣa aṣṭhamī",
        9 to "Śukla pakṣa navamī",
        10 to "Śukla pakṣa daśamī",
        11 to "Śukla pakṣa ekādaśī",
        12 to "Śukla pakṣa dvādaśī",
        13 to "Śukla pakṣa trayodaśī",
        14 to "Śukla pakṣa caturdaśī",
        15 to "Pūrṇimā",
        16 to "Kṛṣṇa pakṣa prathamā",
        17 to "Kṛṣṇa pakṣa dvitīyā",
        18 to "Kṛṣṇa pakṣa tṛtīyā",
        19 to "Kṛṣṇa pakṣa caturthī",
        20 to "Kṛṣṇa pakṣa pañcamī",
        21 to "Kṛṣṇa pakṣa ṣaṣṭhī",
        22 to "Kṛṣṇa pakṣa saptamī",
        23 to "Kṛṣṇa pakṣa aṣṭhamī",
        24 to "Kṛṣṇa pakṣa navamī",
        25 to "Kṛṣṇa pakṣa daśamī",
        26 to "Kṛṣṇa pakṣa ekādaśī",
        27 to "Kṛṣṇa pakṣa dvādaśī",
        28 to "Kṛṣṇa pakṣa trayodaśī",
        29 to "Kṛṣṇa pakṣa caturdaśī",
        30 to "Amāvāsyā"
    )

    val nakshatras: Map<Int, String> = mapOf(
        1 to "Aśvinī",
        2 to "Bharaṇī",
        3 to "Kṛttikā",
        4 to "Rohiṇī",
        5 to "Mṛgaśirā",
        6 to "Ārdrā",
        7 to "Punarvasū",
        8 to "Puṣya",
        9 to "Āśleṣā",
        10 to "Maghā",
        11 to "Pūrvaphalgunī",
        12 to "Uttaraphalgunī",
        13 to "Hasta",
        14 to "Cittā",
        15 to "Svāti",
        16 to "Viśākhā",
        17 to "Anurādhā",
        18 to "Jyeṣṭhā",
        19 to "Mūlā",
        20 to "Pūrvāṣāḍhā",
        21 to "Uttarāṣāḍhā",
        22 to "Śravaṇā",
        23 to "Dhaniṣṭhā",
        24 to "Śatabhiṣā",
        25 to "Pūrvābhādrā",
        26 to "Uttarābhādrā",
        27 to "Revatī"
    )

    val yogas: Map<Int, String> = mapOf(
        1 to "Viṣkambha",
        2 to "Prīti",
        3 to "Āyuṣmān",
        4 to "Saubhāgya",
        5 to "Śobhana",
        6 to "Atigaṇḍa",
        7 to "Sukarmā",
        8 to "Dhṛti",
        9 to "Śūla",
        10 to "Gaṇḍa",
        11 to "Vṛddhi",
        12 to "Dhruva",
        13 to "Vyāghāta",
        14 to "Harṣaṇa",
        15 to "Vajra",
        16 to "Siddhi",
        17 to "Vyatīpāta",
        18 to "Varīyān",
        19 to "Parigha",
        20 to "Śiva",
        21 to "Siddha",
        22 to "Sādhya",
        23 to "Śubha",
        24 to "Śukla",
        25 to "Brahma",
        26 to "Aindra",
        27 to "Vaidhṛti"
    )

    val karanas: Map<Int, String> = buildMap {
        put(1, "Kiṃstughna")
        val cycle = listOf("Bava", "Bālava", "Kaulava", "Taitila", "Garaja", "Vaṇija", "Viṣṭi")
        for (i in 2..57) {
            put(i, cycle[(i - 2) % 7])
        }
        put(58, "Śakuni")
        put(59, "Catuṣpāda")
        put(60, "Nāgava")
    }

    val varas: Map<Int, String> = mapOf(
        0 to "Ravivāra",
        1 to "Somavāra",
        2 to "Maṅgalavāra",
        3 to "Budhavāra",
        4 to "Guruvāra",
        5 to "Śukravāra",
        6 to "Śanivāra"
    )

    val samvats: Map<Int, String> = mapOf(
        0 to "Akṣaya", 1 to "Prabhava", 2 to "Vibhava", 3 to "Śukla", 4 to "Pramoda",
        5 to "Prajāpati", 6 to "Āṅgirasa", 7 to "Śrīmukha", 8 to "Bhāva", 9 to "Yuva",
        10 to "Dhātrī", 11 to "Īśvara", 12 to "Bahudhānya", 13 to "Pramādhi", 14 to "Vikrama",
        15 to "Vṛṣa", 16 to "Citrabhānu", 17 to "Svabhānu", 18 to "Tāraṇa", 19 to "Pārthiva",
        20 to "Vyaya", 21 to "Sarvajit", 22 to "Sarvadhārī", 23 to "Virodhī", 24 to "Vikṛti",
        25 to "Khara", 26 to "Nandana", 27 to "Vijaya", 28 to "Jaya", 29 to "Manmatha",
        30 to "Durmukhī", 31 to "Hevilambī", 32 to "Vilambī", 33 to "Vikārī", 34 to "Śārvarī",
        35 to "Plava", 36 to "Śubhakṛt", 37 to "Śobhakṛt", 38 to "Krodhī", 39 to "Viśvāvasu",
        40 to "Parābhava", 41 to "Plavaṅga", 42 to "Kīlaka", 43 to "Saumya", 44 to "Sādhāraṇa",
        45 to "Virodhikṛti", 46 to "Paridhāvī", 47 to "Pramādīca", 48 to "Ānanda", 49 to "Rākṣasa",
        50 to "Nala", 51 to "Piṅgala", 52 to "Kalāyukti", 53 to "Siddhārthī", 54 to "Raudra",
        55 to "Durmati", 56 to "Dundubhi", 57 to "Rudhirodgārī", 58 to "Raktākṣī", 59 to "Krodhana"
    )

    val ritus: Map<Int, String> = mapOf(
        0 to "Vasanta",
        1 to "Grīṣma",
        2 to "Varṣā",
        3 to "Śarad",
        4 to "Hemanta",
        5 to "Śiśira"
    )

    val zodiac: Map<Int, String> = mapOf(
        0 to "meṣa", 1 to "vṛṣabha", 2 to "mithuna", 3 to "karkāṭaka",
        4 to "siṃha", 5 to "kanyā", 6 to "tulā", 7 to "vṛścika",
        8 to "dhanus", 9 to "makara", 10 to "kumbha", 11 to "mīna"
    )

    val zodiacEnglish: List<String> = listOf(
        "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
        "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    )

    val zodiacDevanagari: List<String> = listOf(
        "मेष", "वृषभ", "मिथुन", "कर्क", "सिंह", "कन्या",
        "तुला", "वृश्चिक", "धनु", "मकर", "कुम्भ", "मीन"
    )

    val sankrantiNames: List<String> = listOf(
        "Meṣa Saṅkrānti", "Vṛṣabha Saṅkrānti", "Mithuna Saṅkrānti", "Karkaṭa Saṅkrānti",
        "Siṁha Saṅkrānti", "Kanyā Saṅkrānti", "Tulā Saṅkrānti", "Vṛścika Saṅkrānti",
        "Dhanu Saṅkrānti", "Makara Saṅkrānti", "Kumbha Saṅkrānti", "Mīna Saṅkrānti"
    )

    val nakshatraEnglish: List<String> = listOf(
        "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashirsha", "Ardra",
        "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
        "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
        "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha",
        "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
    )

    val nakshatraDevanagari: List<String> = listOf(
        "अश्विनी", "भरणी", "कृत्तिका", "रोहिणी", "मृगशीर्षा", "आर्द्रा",
        "पुनर्वसु", "पुष्य", "आश्लेषा", "मघा", "पूर्वाफाल्गुनी", "उत्तराफाल्गुनी",
        "हस्त", "चित्रा", "स्वाती", "विशाखा", "अनुराधा", "ज्येष्ठा",
        "मूल", "पूर्वाषाढा", "उत्तराषाढा", "श्रवण", "धनिष्ठा", "शतभिषा",
        "पूर्वभाद्रपदा", "उत्तरभाद्रपदा", "रेवती"
    )
}
