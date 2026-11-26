package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.KundliReport
import com.mahavtaar.panchanga.domain.model.NavtaraResult
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Bilingual shareable report formatters (EN / HI text + Kundli JSON export)
 * for Daily Panchanga, Navtara Chakra, and Janam Kundli.
 */
object PanchangaReportFormatter {

    private val prettyJson = Json { prettyPrint = true }

    fun formatDailyPanchanga(snapshot: PanchangaSnapshot, lang: AppLanguage): String = buildString {
        if (lang == AppLanguage.HINDI) {
            appendLine("॥ महावतार पंचांग — दैनिक पंचांग ॥")
            appendLine("दिनांक: ${snapshot.dateIso} | स्थान: ${snapshot.city}")
            appendLine("संवत्सर: ${snapshot.samvatsara} | मास: ${snapshot.masa} (${snapshot.paksha})")
            appendLine("सूर्योदय: ${snapshot.sunrise} | सूर्यास्त: ${snapshot.sunset}")
            appendLine("तिथि: ${snapshot.tithi.joinToString(", ") { "${it.name}${it.ends?.let { e -> " ($e तक)" } ?: ""}" }}")
            appendLine("नक्षत्र: ${snapshot.nakshatra.joinToString(", ") { "${it.name}${it.ends?.let { e -> " ($e तक)" } ?: ""}" }}")
            appendLine("योग: ${snapshot.yoga.joinToString(", ") { it.name }} | करण: ${snapshot.karana.joinToString(", ") { it.name }}")
            appendLine("अभिजित् मुहूर्त: ${snapshot.abhijitMuhurta.start} – ${snapshot.abhijitMuhurta.end}")
            appendLine("राहु काल: ${snapshot.rahuKala.start} – ${snapshot.rahuKala.end}")
            if (snapshot.dishaShoolaHi.isNotBlank()) appendLine("दिशाशूल: ${snapshot.dishaShoolaHi}")
            if (snapshot.observances.isNotEmpty()) {
                appendLine("पावन पर्व: ${snapshot.observances.joinToString(", ") { it.title }}")
            }
        } else {
            appendLine("Mahavtaar Panchanga — Daily Vedic Summary")
            appendLine("Date: ${snapshot.dateIso} | City: ${snapshot.city}")
            appendLine("Samvatsara: ${snapshot.samvatsara} | Masa: ${snapshot.masa} (${snapshot.paksha})")
            appendLine("Sunrise: ${snapshot.sunrise} | Sunset: ${snapshot.sunset}")
            appendLine("Tithi: ${snapshot.tithi.joinToString(", ") { "${it.name}${it.ends?.let { e -> " (until $e)" } ?: ""}" }}")
            appendLine("Nakshatra: ${snapshot.nakshatra.joinToString(", ") { "${it.name}${it.ends?.let { e -> " (until $e)" } ?: ""}" }}")
            appendLine("Yoga: ${snapshot.yoga.joinToString(", ") { it.name }} | Karana: ${snapshot.karana.joinToString(", ") { it.name }}")
            appendLine("Abhijit Muhurta: ${snapshot.abhijitMuhurta.start} – ${snapshot.abhijitMuhurta.end}")
            appendLine("Rahu Kala: ${snapshot.rahuKala.start} – ${snapshot.rahuKala.end}")
            if (snapshot.dishaShoolaEn.isNotBlank()) appendLine("Disha Shoola: ${snapshot.dishaShoolaEn}")
            if (snapshot.observances.isNotEmpty()) {
                appendLine("Observances: ${snapshot.observances.joinToString(", ") { it.title }}")
            }
        }
    }.trim()

    fun formatNavtaraSummary(
        birthNakshatraName: String,
        birthPada: Int,
        todayNakshatraName: String,
        todayResult: NavtaraResult,
        gandantaTitle: String,
        lang: AppLanguage
    ): String = buildString {
        if (lang == AppLanguage.HINDI) {
            appendLine("॥ नवतारा चक्र विश्लेषण — महावतार पंचांग ॥")
            appendLine("जन्म नक्षत्र: $birthNakshatraName (चरण $birthPada)")
            appendLine("आज का नक्षत्र: $todayNakshatraName")
            appendLine("तारा: ${todayResult.taraNameHi} (पर्याय ${todayResult.paryaya}) | शुभता: ${todayResult.score}/100")
            appendLine("प्रकृति: ${todayResult.natureHi} (${todayResult.intensityLevelHi})")
            appendLine("विवरण: ${todayResult.descriptionHi}")
            appendLine("गण्डान्त स्थिति: $gandantaTitle")
            todayResult.parihara?.let { p ->
                appendLine("परिहार — देवता: ${p.deityHi} | मंत्र: ${p.mantraHi} | दान: ${p.donationHi}")
            }
        } else {
            appendLine("Mahavtaar Panchanga — Navtara Chakra Analysis")
            appendLine("Birth Star: $birthNakshatraName (Pada $birthPada)")
            appendLine("Today's Star: $todayNakshatraName")
            appendLine("Tara: ${todayResult.taraNameEn} (Cycle ${todayResult.paryaya}) | Score: ${todayResult.score}/100")
            appendLine("Nature: ${todayResult.natureEn} (${todayResult.intensityLevelEn})")
            appendLine("Guidance: ${todayResult.descriptionEn}")
            appendLine("Gandanta Status: $gandantaTitle")
            todayResult.parihara?.let { p ->
                appendLine("Remedy — Deity: ${p.deityEn} | Mantra: ${p.mantraEn} | Donation: ${p.donationEn}")
            }
        }
    }.trim()

    fun formatKundliText(report: KundliReport, lang: AppLanguage): String = buildString {
        val signNames = if (lang == AppLanguage.HINDI) SanskritNamesCatalog.zodiacDevanagari else SanskritNamesCatalog.zodiacEnglish
        val naks = if (lang == AppLanguage.HINDI) SanskritNamesCatalog.nakshatraDevanagari else SanskritNamesCatalog.nakshatraEnglish
        if (lang == AppLanguage.HINDI) {
            appendLine("॥ जन्म कुण्डली विवरण — ${report.input.name} ॥")
            appendLine("जन्म: ${report.input.day}/${report.input.month}/${report.input.year} ${"%02d:%02d".format(report.input.hour, report.input.minute)} | ${report.input.location.name}")
            appendLine("लग्न: ${signNames[report.lagnaSignIndex]} (${report.lagnaDms}) | चन्द्र राशि: ${signNames[report.moonSignIndex]}")
            appendLine("चन्द्र नक्षत्र: ${naks[(report.moonNakshatraNumber - 1).coerceIn(0, 26)]} चरण ${report.moonPada}")
            appendLine("वर्तमान दशा: ${report.currentMahadashaLabelHi}")
            appendLine("दोष विश्लेषण: ${report.doshas.manglikLabelHi} | ${report.doshas.kalsarpaLabelHi} | ${report.doshas.sadeSatiLabelHi}")
            val presentYogas = report.yogas.filter { it.isPresent }
            if (presentYogas.isNotEmpty()) {
                appendLine("प्रमुख योग (${presentYogas.size}): ${presentYogas.joinToString(", ") { it.nameHi }}")
            }
            appendLine("\n--- ग्रह स्थिति ---")
            report.planets.forEach { p ->
                appendLine("${p.nameHi}: ${signNames[p.signIndex]} ${p.dmsLabel} (भाव ${p.houseD1}, ${p.dignityHi})")
            }
        } else {
            appendLine("Janam Kundli Report — ${report.input.name}")
            appendLine("Birth: ${report.input.year}-${"%02d-%02d".format(report.input.month, report.input.day)} ${"%02d:%02d".format(report.input.hour, report.input.minute)} | ${report.input.location.name}")
            appendLine("Ascendant: ${signNames[report.lagnaSignIndex]} (${report.lagnaDms}) | Moon Sign: ${signNames[report.moonSignIndex]}")
            appendLine("Moon Nakshatra: ${naks[(report.moonNakshatraNumber - 1).coerceIn(0, 26)]} Pada ${report.moonPada}")
            appendLine("Active Dasha: ${report.currentMahadashaLabelEn}")
            appendLine("Doshas: ${report.doshas.manglikLabelEn} | ${report.doshas.kalsarpaLabelEn} | ${report.doshas.sadeSatiLabelEn}")
            val presentYogas = report.yogas.filter { it.isPresent }
            if (presentYogas.isNotEmpty()) {
                appendLine("Active Yogas (${presentYogas.size}): ${presentYogas.joinToString(", ") { it.nameEn }}")
            }
            appendLine("\n--- Planetary Longitudes ---")
            report.planets.forEach { p ->
                appendLine("${p.nameEn}: ${signNames[p.signIndex]} ${p.dmsLabel} (H${p.houseD1}, ${p.dignityEn})")
            }
        }
        report.audit?.let {
            appendLine("\nAudit Hash: ${it.chartHash} | Ayanamsha: ${it.ayanamshaModel} (${"%.4f".format(it.ayanamshaDeg)}°)")
        }
    }.trim()

    fun formatKundliJson(report: KundliReport): String = prettyJson.encodeToString(report)
}
