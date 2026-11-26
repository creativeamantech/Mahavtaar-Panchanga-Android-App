package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.domain.model.GunaMilanReport
import com.mahavtaar.panchanga.domain.model.KootaScoreItem
import kotlin.math.roundToInt

/**
 * Pure Kotlin 36-Point Ashtakoot Guna Milan engine matching `src/lib/gunaMilanEngine.ts`.
 * Computes Varna (1), Vashya (2), Tara (3), Yoni (4), Graha Maitri (5), Gana (6),
 * Bhakoot (7 + cancellation), Nadi (8 + cancellation), and Manglik compatibility.
 */
object GunaMilanEngine {

    private val rasiLords = listOf(
        "Mars", "Venus", "Mercury", "Moon", "Sun", "Mercury",
        "Venus", "Mars", "Jupiter", "Saturn", "Saturn", "Jupiter"
    )

    private val rasiVarnaScore = intArrayOf(
        3, 2, 1, 4, 3, 2, 1, 4, 3, 2, 1, 4
    )

    private val nakshatraYoni = intArrayOf(
        0, 1, 2, 3, 3, 4, 5, 2, 5,
        6, 6, 7, 8, 9, 8, 9, 10, 10,
        4, 11, 12, 11, 13, 0, 13, 7, 1
    )

    private val yoniEnemies = listOf(
        0 to 8, 1 to 13, 2 to 11, 3 to 12, 4 to 10, 5 to 6, 7 to 9
    )

    private val ganaMap = intArrayOf(
        0, 1, 2, 1, 0, 1, 0, 0, 2,
        2, 1, 1, 0, 2, 0, 2, 0, 2,
        2, 1, 1, 0, 2, 2, 1, 1, 0
    )

    private val nadiMap = intArrayOf(
        0, 1, 2, 2, 1, 0, 0, 1, 2,
        0, 1, 2, 2, 1, 0, 0, 1, 2,
        0, 1, 2, 2, 1, 0, 0, 1, 2
    )

    private val friendship = mapOf(
        "Sun" to mapOf("Sun" to 1, "Moon" to 1, "Mars" to 1, "Mercury" to 0, "Jupiter" to 1, "Venus" to -1, "Saturn" to -1),
        "Moon" to mapOf("Sun" to 1, "Moon" to 1, "Mars" to 0, "Mercury" to 1, "Jupiter" to 0, "Venus" to 0, "Saturn" to 0),
        "Mars" to mapOf("Sun" to 1, "Moon" to 1, "Mars" to 1, "Mercury" to -1, "Jupiter" to 1, "Venus" to 0, "Saturn" to 0),
        "Mercury" to mapOf("Sun" to 1, "Moon" to -1, "Mars" to 0, "Mercury" to 1, "Jupiter" to 0, "Venus" to 1, "Saturn" to 0),
        "Jupiter" to mapOf("Sun" to 1, "Moon" to 1, "Mars" to 1, "Mercury" to -1, "Jupiter" to 1, "Venus" to -1, "Saturn" to 0),
        "Venus" to mapOf("Sun" to -1, "Moon" to -1, "Mars" to 0, "Mercury" to 1, "Jupiter" to 0, "Venus" to 1, "Saturn" to 1),
        "Saturn" to mapOf("Sun" to -1, "Moon" to -1, "Mars" to -1, "Mercury" to 1, "Jupiter" to 0, "Venus" to 1, "Saturn" to 1)
    )

    fun compute(
        boyName: String,
        boyNakshatra: Int,
        boyPada: Int,
        boyRasi: Int,
        boyManglik: Boolean,
        girlName: String,
        girlNakshatra: Int,
        girlPada: Int,
        girlRasi: Int,
        girlManglik: Boolean
    ): GunaMilanReport {
        val bNak = boyNakshatra.coerceIn(1, 27)
        val gNak = girlNakshatra.coerceIn(1, 27)
        val bRasi = ((boyRasi % 12) + 12) % 12
        val gRasi = ((girlRasi % 12) + 12) % 12
        val bLord = rasiLords[bRasi]
        val gLord = rasiLords[gRasi]

        // 1. Varna (1)
        val varnaScore = if (rasiVarnaScore[bRasi] >= rasiVarnaScore[gRasi]) 1.0 else 0.0
        val varna = KootaScoreItem(
            nameEn = "Varna",
            nameHi = "वर्ण",
            maxScore = 1.0,
            obtainedScore = varnaScore,
            isDosha = varnaScore == 0.0,
            isCancelled = false,
            descriptionEn = if (varnaScore == 1.0) "Balanced temperament and spiritual harmony." else "Mutual respect bridges temperament differences.",
            descriptionHi = if (varnaScore == 1.0) "वर व कन्या के वर्ण में उत्तम संतुलन है।" else "आपसी समझ एवं सम्मान से कार्यशैली में सामंजस्य रहेगा।"
        )

        // 2. Vashya (2)
        val vashyaScore = if (vashyaGroup(bRasi) == vashyaGroup(gRasi) || bRasi == gRasi) 2.0 else 1.0
        val vashya = KootaScoreItem(
            nameEn = "Vashya",
            nameHi = "वश्य",
            maxScore = 2.0,
            obtainedScore = vashyaScore,
            isDosha = false,
            isCancelled = false,
            descriptionEn = if (vashyaScore == 2.0) "Strong mutual affection and natural magnetism." else "Healthy balance of mutual influence.",
            descriptionHi = if (vashyaScore == 2.0) "परस्पर आकर्षण, प्रेम व समर्पण का पूर्ण सामंजस्य है।" else "सामान्य एवं संतुलित वश्य मिलान।"
        )

        // 3. Tara (3)
        val t1 = ((gNak - bNak + 27) % 9) + 1
        val t2 = ((bNak - gNak + 27) % 9) + 1
        var taraScore = 0.0
        if (t1 !in setOf(3, 5, 7)) taraScore += 1.5
        if (t2 !in setOf(3, 5, 7)) taraScore += 1.5
        val tara = KootaScoreItem(
            nameEn = "Tara (Dina)",
            nameHi = "तारा (दिन)",
            maxScore = 3.0,
            obtainedScore = taraScore,
            isDosha = taraScore < 1.5,
            isCancelled = false,
            descriptionEn = when {
                taraScore >= 3.0 -> "Auspicious health, longevity, and shared fortune."
                taraScore >= 1.5 -> "Moderate Tara compatibility favoring one partner."
                else -> "Tara Dosha present; Mahamrityunjaya chanting recommended."
            },
            descriptionHi = when {
                taraScore >= 3.0 -> "तारा मिलान पूर्णतः शुभ है। आरोग्य एवं भाग्य वृद्धि होगी।"
                taraScore >= 1.5 -> "मध्यम तारा मिलान। एक पक्ष के लिए अत्यन्त शुभ।"
                else -> "तारा दोष उपस्थित। महामृत्युंजय जप श्रेयस्कर रहेगा।"
            }
        )

        // 4. Yoni (4)
        val bYoni = nakshatraYoni[bNak - 1]
        val gYoni = nakshatraYoni[gNak - 1]
        val yoniScore = when {
            bYoni == gYoni -> 4.0
            yoniEnemies.any { (a, b) -> (bYoni == a && gYoni == b) || (bYoni == b && gYoni == a) } -> 0.0
            else -> 2.5
        }
        val yoni = KootaScoreItem(
            nameEn = "Yoni",
            nameHi = "योनि",
            maxScore = 4.0,
            obtainedScore = yoniScore,
            isDosha = yoniScore == 0.0,
            isCancelled = false,
            descriptionEn = when {
                yoniScore >= 3.0 -> "Excellent physical and emotional compatibility."
                yoniScore > 0.0 -> "Balanced instinctive harmony."
                else -> "Opposing Yoni animals; patience strengthens understanding."
            },
            descriptionHi = when {
                yoniScore >= 3.0 -> "शारीरिक व मानसिक अनुकूलता तथा वैवाहिक सुख उत्तम है।"
                yoniScore > 0.0 -> "सामान्य एवं सुखद योनि सामंजस्य।"
                else -> "विपरीत योनि है; आपसी समझ में धैर्य अपेक्षित है।"
            }
        )

        // 5. Graha Maitri (5)
        val gmScore = grahaMaitriScore(bLord, gLord)
        val grahaMaitri = KootaScoreItem(
            nameEn = "Graha Maitri",
            nameHi = "ग्रह मैत्री",
            maxScore = 5.0,
            obtainedScore = gmScore,
            isDosha = gmScore <= 1.0,
            isCancelled = false,
            descriptionEn = when {
                gmScore >= 4.0 -> "Rashi lords are friends—strong intellectual bond."
                gmScore >= 2.5 -> "Neutral planetary friendship—steady companionship."
                else -> "Rashi lords differ; conscious communication helps."
            },
            descriptionHi = when {
                gmScore >= 4.0 -> "राशि स्वामियों में मित्रता है। विचारों में मधुरता रहेगी।"
                gmScore >= 2.5 -> "मध्यम राशि मित्रता। सहयोग बना रहेगा।"
                else -> "राशि स्वामियों में भिन्नता; नियमित संवाद लाभकारी है।"
            }
        )

        // 6. Gana (6)
        val bGana = ganaMap[bNak - 1]
        val gGana = ganaMap[gNak - 1]
        val ganaScore = when {
            bGana == gGana -> 6.0
            bGana == 0 && gGana == 1 -> 5.0
            bGana == 1 && gGana == 0 -> 6.0
            bGana == 0 && gGana == 2 -> 1.0
            bGana == 1 && gGana == 2 -> 0.5
            else -> 0.0
        }
        val gana = KootaScoreItem(
            nameEn = "Gana",
            nameHi = "गण",
            maxScore = 6.0,
            obtainedScore = ganaScore,
            isDosha = ganaScore == 0.0,
            isCancelled = false,
            descriptionEn = when {
                ganaScore >= 5.0 -> "Harmonious nature, outlook, and lifestyle."
                ganaScore >= 3.0 -> "Moderate temperament alignment."
                else -> "Gana Dosha indicates contrasting temperaments."
            },
            descriptionHi = when {
                ganaScore >= 5.0 -> "दोनों के स्वभाव व जीवन दृष्टिकोण में उत्तम मेल है।"
                ganaScore >= 3.0 -> "मध्यम गण मिलान।"
                else -> "गण दोष उपस्थित। स्वभाव में भिन्नता रहेगी।"
            }
        )

        // 7. Bhakoot (7)
        val rasiDiff = ((gRasi - bRasi + 12) % 12) + 1
        var bhakootScore = 7.0
        var bhakootDosha = false
        var bhakootCancelled = false
        if (rasiDiff in setOf(2, 12, 6, 8, 5, 9)) {
            bhakootDosha = true
            if (bLord == gLord || friendship[bLord]?.get(gLord) == 1) {
                bhakootCancelled = true
                bhakootScore = 7.0
            } else {
                bhakootScore = 0.0
            }
        }
        val bhakoot = KootaScoreItem(
            nameEn = "Bhakoot",
            nameHi = "भकूट",
            maxScore = 7.0,
            obtainedScore = bhakootScore,
            isDosha = bhakootDosha && !bhakootCancelled,
            isCancelled = bhakootCancelled,
            descriptionEn = when {
                bhakootCancelled -> "Bhakoot Dosha cancelled due to friendly/same Rashi lords."
                bhakootScore == 7.0 -> "Auspicious family prosperity, love, and growth."
                else -> "Bhakoot Dosha ($rasiDiff axis) present; remedy advised."
            },
            descriptionHi = when {
                bhakootCancelled -> "राशि स्वामी समान अथवा मित्र होने से भकूट दोष निरस्त है।"
                bhakootScore == 7.0 -> "भकूट स्थिति शुभ है। समृद्धि एवं परिवार में सुख रहेगा।"
                else -> "भकूट दोष उपस्थित। आर्थिक व स्वास्थ्य पक्ष में सावधानी रखें।"
            }
        )

        // 8. Nadi (8)
        val bNadi = nadiMap[bNak - 1]
        val gNadi = nadiMap[gNak - 1]
        var nadiScore = 8.0
        var nadiDosha = false
        var nadiCancelled = false
        if (bNadi == gNadi) {
            nadiDosha = true
            if ((bNak == gNak && boyPada != girlPada) || (bRasi == gRasi && bNak != gNak)) {
                nadiCancelled = true
                nadiScore = 8.0
            } else {
                nadiScore = 0.0
            }
        }
        val nadi = KootaScoreItem(
            nameEn = "Nadi",
            nameHi = "नाड़ी",
            maxScore = 8.0,
            obtainedScore = nadiScore,
            isDosha = nadiDosha && !nadiCancelled,
            isCancelled = nadiCancelled,
            descriptionEn = when {
                nadiCancelled -> "Nadi Dosha cancelled by differing Pada or Nakshatra in same Rashi."
                nadiScore == 8.0 -> "Excellent vitality, genetic harmony, and progeny wellness."
                else -> "Same Nadi Dosha present; classical remedies recommended."
            },
            descriptionHi = when {
                nadiCancelled -> "भिन्न चरण या एक राशि में भिन्न नक्षत्र होने से नाड़ी दोष निरस्त है।"
                nadiScore == 8.0 -> "नाड़ी मिलान निर्दोष है। स्वास्थ्य, ओज व संतान सुख उत्तम रहेगा।"
                else -> "समान नाड़ी दोष उपस्थित। महामृत्युंजय जप व स्वर्ण दान अनुशंसित है।"
            }
        )

        val kootas = listOf(varna, vashya, tara, yoni, grahaMaitri, gana, bhakoot, nadi)
        val total = kootas.sumOf { it.obtainedScore }
        val percentage = ((total / 36.0) * 100.0).roundToInt()

        val (verdictEn, verdictHi, recommended) = when {
            total >= 28.0 -> Triple("Excellent Match (Uttama)", "उत्कृष्ट एवं अति शुभ मिलान", true)
            total >= 21.0 -> Triple("Good Match (Shubha)", "उत्तम मिलान (विवाह योग्य)", true)
            total >= 18.0 -> Triple("Average Match (Madhyama)", "मध्यम मिलान (दोष परिहार सहित)", true)
            else -> Triple("Low Score (Consult Astrologer)", "अल्प गुण (ज्योतिषीय परामर्श आवश्यक)", false)
        }

        val (manglikOk, manglikEn, manglikHi) = when {
            boyManglik && girlManglik -> Triple(
                true,
                "Both are Manglik — Manglik Dosha cancels mutually.",
                "दोनों मांगलिक होने से मांगलिक दोष स्वतः निरस्त हो जाता है।"
            )
            !boyManglik && !girlManglik -> Triple(
                true,
                "Neither chart has Manglik Dosha — fully compatible.",
                "दोनों ही मांगलिक दोष से मुक्त हैं।"
            )
            boyManglik -> Triple(
                false,
                "Groom is Manglik while Bride is not; Mangala Shanti recommended.",
                "वर मांगलिक है और कन्या नहीं; मंगल शान्ति अनुष्ठान अनुशंसित है।"
            )
            else -> Triple(
                false,
                "Bride is Manglik while Groom is not; Mangala Shanti recommended.",
                "कन्या मांगलिक है और वर नहीं; मंगल शान्ति अनुष्ठान अनुशंसित है।"
            )
        }

        val (remedyEn, remedyHi) = when {
            nadi.isDosha -> "Perform Mahamrityunjaya Japa, Rudrabhisheka, and Swarna Daan for Nadi Shanti." to
                "नाड़ी दोष शान्ति हेतु महामृत्युंजय मंत्र जप, रुद्राभिषेक एवं स्वर्ण-दान कराएँ।"
            bhakoot.isDosha -> "Recite Vishnu Sahasranama and donate Chana Dal & jaggery on Thursdays." to
                "भकूट दोष शान्ति हेतु विष्णु सहस्रनाम पाठ व गुरुवार को चने की दाल-गुड़ का दान करें।"
            else -> "Worship Lord Shiva and Goddess Parvati together for lifelong marital harmony." to
                "सुखद दाम्पत्य जीवन हेतु भगवान शिव एवं माता पार्वती की उपासना शुभकारी है।"
        }

        return GunaMilanReport(
            boyName = boyName.ifBlank { "Groom" },
            girlName = girlName.ifBlank { "Bride" },
            boyNakshatra = bNak,
            girlNakshatra = gNak,
            boyRasi = bRasi,
            girlRasi = gRasi,
            kootas = kootas,
            totalScore = total,
            maxScore = 36.0,
            percentage = percentage,
            verdictEn = verdictEn,
            verdictHi = verdictHi,
            isRecommended = recommended,
            manglikCompatible = manglikOk,
            manglikNoteEn = manglikEn,
            manglikNoteHi = manglikHi,
            remedyEn = remedyEn,
            remedyHi = remedyHi
        )
    }

    private fun vashyaGroup(rasi: Int): Int = when (rasi) {
        0, 1, 8, 9 -> 0 // Chatushpada
        2, 5, 6, 10 -> 1 // Manava
        3, 11 -> 2 // Jalachara
        4 -> 3 // Vanchara
        else -> 4 // Keeta
    }

    private fun grahaMaitriScore(l1: String, l2: String): Double {
        if (l1 == l2) return 5.0
        val r1 = friendship[l1]?.get(l2) ?: 0
        val r2 = friendship[l2]?.get(l1) ?: 0
        return when {
            r1 == 1 && r2 == 1 -> 5.0
            (r1 == 1 && r2 == 0) || (r1 == 0 && r2 == 1) -> 4.0
            r1 == 0 && r2 == 0 -> 3.0
            (r1 == 1 && r2 == -1) || (r1 == -1 && r2 == 1) -> 1.0
            (r1 == 0 && r2 == -1) || (r1 == -1 && r2 == 0) -> 0.5
            else -> 0.0
        }
    }
}
