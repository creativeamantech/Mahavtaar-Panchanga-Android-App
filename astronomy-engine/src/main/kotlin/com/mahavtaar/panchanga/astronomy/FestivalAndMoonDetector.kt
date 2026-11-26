package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.domain.model.LunarPhaseInfo
import com.mahavtaar.panchanga.domain.model.VratObservance
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * Dynamic Vrata, Festival, and Lunar Phase calculator preserving `FestivalCard.tsx` and `MoonPhaseVisualizer.tsx`.
 */
object FestivalAndMoonDetector {

    fun computeLunarPhase(primaryTithiNum: Int, paksha: String): LunarPhaseInfo {
        val isSukla = paksha == "Śukla" || primaryTithiNum <= 15
        val elongationDeg = ((primaryTithiNum - 0.5) * 12.0) % 360.0
        val elongationRad = elongationDeg * PI / 180.0
        val illumination = (((1.0 - cos(elongationRad)) / 2.0) * 100.0).roundToInt().coerceIn(0, 100)

        val phaseName = when {
            primaryTithiNum == 30 || primaryTithiNum == 0 -> "New Moon (Amāvāsyā)"
            primaryTithiNum == 15 -> "Full Moon (Pūrṇimā)"
            primaryTithiNum == 8 -> "First Quarter (Śukla Aṣṭamī)"
            primaryTithiNum == 23 -> "Last Quarter (Kṛṣṇa Aṣṭamī)"
            primaryTithiNum < 8 -> "Waxing Crescent"
            primaryTithiNum < 15 -> "Waxing Gibbous"
            primaryTithiNum < 23 -> "Waning Gibbous"
            else -> "Waning Crescent"
        }

        return LunarPhaseInfo(
            phaseName = phaseName,
            illuminationPercent = illumination,
            elongationDegrees = elongationDeg,
            isWaxing = isSukla
        )
    }

    fun detectObservances(
        primaryTithiNum: Int,
        masaNumber: Int,
        isAdhika: Boolean,
        paksha: String,
        sunRasiIndex: Int? = null
    ): List<VratObservance> {
        val list = mutableListOf<VratObservance>()
        val isSukla = paksha == "Śukla" || primaryTithiNum <= 15

        when (primaryTithiNum) {
            11, 26 -> {
                val pair = if (isAdhika) SanskritNamesCatalog.adhikaEkadashi
                else SanskritNamesCatalog.ekadashis[masaNumber] ?: SanskritNamesCatalog.EkadashiPair("Ekādaśī", "Ekādaśī")
                val specificName = if (isSukla) pair.shukla else pair.krishna
                list.add(
                    VratObservance(
                        id = "ekadashi-$masaNumber-$primaryTithiNum",
                        title = "Śrī Hari $specificName Ekādaśī",
                        subtitle = "Supreme Fasting Day Dedicated to Lord Mahāviṣṇu",
                        deity = "Lord Viṣṇu / Kṛṣṇa",
                        category = "ekadashi",
                        significance = "Ekādaśī is revered as Bhakti-jananī. Fasting and chanting today purifies consciousness and deepens devotion.",
                        practices = listOf(
                            "Observe Upavāsa (fasting) or light Sattvic phalāhāra",
                            "Chant Viṣṇu Sahasranāma or Oṁ Namo Bhagavate Vāsudevāya",
                            "Refrain from grains and beans"
                        )
                    )
                )
            }
            15 -> {
                val specialTitle = when (masaNumber) {
                    12 -> "Holi / Phālguṇa Pūrṇimā"
                    4 -> "Guru Pūrṇimā (Vyāsa Pūrṇimā)"
                    5 -> "Rakṣā Bandhana / Śrāvaṇa Pūrṇimā"
                    8 -> "Kārtika Pūrṇimā (Dev Dīpāvalī)"
                    else -> "Pūrṇimā (Satyanārāyaṇa Vrata)"
                }
                list.add(
                    VratObservance(
                        id = "purnima-$masaNumber",
                        title = specialTitle,
                        subtitle = "Full Moon Day for Satyanārāyaṇa Pūjā, Holy Snāna, and Charity",
                        deity = "Śrī Satyanārāyaṇa & Candra Deva",
                        category = "purnima",
                        significance = "The Full Moon symbolizes complete illumination of mind and spirit. Auspicious for Satyanārāyaṇa Kathā and Dāna.",
                        practices = listOf(
                            "Perform or listen to Śrī Satyanārāyaṇa Kathā",
                            "Offer Arghya to the Full Moon at moonrise",
                            "Practice Dāna (charity) and evening lamp offering"
                        )
                    )
                )
            }
            30, 0 -> {
                val title = when (masaNumber) {
                    7 -> "Dīpāvalī / Lakṣmī Pūjā (Amāvāsyā)"
                    11 -> "Maunī Amāvāsyā"
                    6 -> "Sarvapitṛ Amāvāsyā"
                    else -> "Amāvāsyā (Pitṛ Tarpaṇam)"
                }
                list.add(
                    VratObservance(
                        id = "amavasya-$masaNumber",
                        title = title,
                        subtitle = "Sacred Day for Ancestral Homage (Pitṛ Tarpaṇa) and Inward Reflection",
                        deity = "Pitṛs (Ancestors) & Lord Sūrya",
                        category = "amavasya",
                        significance = "Amāvāsyā strengthens ancestral gratitude and quiet contemplation.",
                        practices = listOf(
                            "Offer water and black sesame seeds (Pitṛ Tarpaṇam)",
                            "Feed cows, birds, and those in need",
                            "Favor quiet reflection over major commercial launches"
                        )
                    )
                )
            }
            13, 28 -> {
                list.add(
                    VratObservance(
                        id = "pradosha-$masaNumber-$primaryTithiNum",
                        title = "Pradoṣa Vrata",
                        subtitle = "Sacred Trayodaśī Twilight Pūjā for Lord Śiva",
                        deity = "Bhagavān Sadāśiva & Mātā Pārvatī",
                        category = "pradosham",
                        significance = "Evening twilight on Trayodaśī is sacred for Śiva Abhiṣeka, dissolution of obstacles, and inner peace.",
                        practices = listOf(
                            "Perform Śiva Abhiṣeka with milk, water, and Bilva leaves",
                            "Chant Mahāmṛtyuñjaya Mantra or Oṁ Namaḥ Śivāya"
                        )
                    )
                )
            }
            4, 19 -> {
                val isSankashti = !isSukla
                val title = when {
                    isSukla && masaNumber == 6 -> "Gaṇeśa Caturthī (Vināyaka Mahotsava)"
                    isSankashti -> "Saṅkaṣṭī Caturthī Vrata"
                    else -> "Vināyaka Caturthī"
                }
                list.add(
                    VratObservance(
                        id = "chaturthi-$masaNumber-$primaryTithiNum",
                        title = title,
                        subtitle = "Auspicious Day Dedicated to Lord Śrī Gaṇeśa",
                        deity = "Lord Śrī Gaṇeśa",
                        category = "chaturthi",
                        significance = "Worshipping Vighnahartā Gaṇeśa removes obstacles and blesses wisdom and auspicious beginnings.",
                        practices = listOf(
                            "Offer Durva grass and Modak to Lord Gaṇeśa",
                            if (isSankashti) "Break fast after sighting the Moonrise" else "Recite Saṅkaṭa Nāśana Gaṇeśa Stotram"
                        )
                    )
                )
            }
            29 -> {
                val isMaha = masaNumber == 11 || masaNumber == 12
                list.add(
                    VratObservance(
                        id = "shivaratri-$masaNumber",
                        title = if (isMaha) "Māsika / Mahā Śivarātri" else "Māsika Śivarātri",
                        subtitle = "Kṛṣṇa Caturdaśī Night Vigil Dedicated to Lord Śiva",
                        deity = "Lord Mahādeva",
                        category = "shivaratri",
                        significance = "Sacred nocturnal worship of Śiva Liṅga during Niśītha Kāla.",
                        practices = listOf(
                            "Observe fasting and evening/midnight Rudrābhiṣeka",
                            "Chant Oṁ Namaḥ Śivāya"
                        )
                    )
                )
            }
        }

        // Major lunar-date festivals
        if (!isAdhika) {
            when (masaNumber to primaryTithiNum) {
                1 to 1 -> list.add(
                    VratObservance(
                        id = "chaitra-navaratri",
                        title = "Caitra Navarātri & Vikram New Year",
                        subtitle = "Ghaṭasthāpana & Hindu Nav Saṁvatsara",
                        deity = "Mā Durgā",
                        category = "festival",
                        significance = "Inception of the Vedic lunar new year and nine sacred nights of Śakti Sādhana."
                    )
                )
                1 to 9 -> list.add(
                    VratObservance(
                        id = "rama-navami",
                        title = "Śrī Rāma Navamī",
                        subtitle = "Appearance Day of Maryādā Puruṣottama Śrī Rāma",
                        deity = "Lord Śrī Rāma",
                        category = "festival",
                        significance = "Celebration of Dharma, devotion, and the noon advent of Lord Rāma."
                    )
                )
                6 to 23 -> list.add(
                    VratObservance(
                        id = "krishna-janmashtami",
                        title = "Śrī Kṛṣṇa Janmāṣṭamī",
                        subtitle = "Midnight Appearance of Bhagavān Śrī Kṛṣṇa",
                        deity = "Lord Śrī Kṛṣṇa",
                        category = "festival",
                        significance = "Fasting until Niśītha Kāla midnight and devotional chanting."
                    )
                )
                7 to 10 -> list.add(
                    VratObservance(
                        id = "vijayadashami",
                        title = "Vijayadaśamī (Dussehra)",
                        subtitle = "Triumph of Dharma over Adharma",
                        deity = "Lord Śrī Rāma & Mā Aparājitā",
                        category = "festival",
                        significance = "Highly auspicious day for new learning, tools worship, and righteous undertakings."
                    )
                )
                11 to 5 -> list.add(
                    VratObservance(
                        id = "vasant-panchami",
                        title = "Vasanta Pañcamī (Saraswatī Pūjā)",
                        subtitle = "Onset of Spring & Worship of Goddess Saraswatī",
                        deity = "Mā Saraswatī",
                        category = "festival",
                        significance = "Auspicious Abujha Muhūrta for education, music, and arts."
                    )
                )
            }
        }
        return list
    }
}
