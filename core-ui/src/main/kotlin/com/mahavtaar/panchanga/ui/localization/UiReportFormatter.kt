package com.mahavtaar.panchanga.ui.localization

import android.content.Context
import android.content.Intent
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.KundliReport
import com.mahavtaar.panchanga.domain.model.NavtaraResult
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot

object UiReportFormatter {

    fun shareText(context: Context, chooserTitle: String, textBody: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
            putExtra(Intent.EXTRA_TEXT, textBody)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun formatDailyPanchanga(snapshot: PanchangaSnapshot, lang: AppLanguage): String = buildString {
        if (lang == AppLanguage.HINDI) {
            appendLine("॥ महावतार पंचांग — दैनिक पंचांग ॥")
            appendLine("दिनांक: ${snapshot.dateIso} | स्थान: ${snapshot.city}")
            appendLine("संवत्सर: ${VedicLocalizer.samvatsaraName(snapshot.samvatsara, lang)} | मास: ${VedicLocalizer.masaName(snapshot.masaNumber, snapshot.isAdhika, snapshot.isKshaya, snapshot.masa, lang)} (${VedicLocalizer.pakshaName(snapshot.paksha, lang)})")
            appendLine("सूर्योदय: ${snapshot.sunrise} | सूर्यास्त: ${snapshot.sunset}")
            appendLine("तिथि: ${snapshot.tithi.joinToString(", ") { "${VedicLocalizer.tithiName(it.number, it.name, lang)}${it.ends?.let { e -> " ($e तक)" } ?: ""}" }}")
            appendLine("नक्षत्र: ${snapshot.nakshatra.joinToString(", ") { "${VedicLocalizer.nakshatraName(it.number, it.name, lang)}${it.ends?.let { e -> " ($e तक)" } ?: ""}" }}")
            appendLine("योग: ${snapshot.yoga.joinToString(", ") { VedicLocalizer.yogaName(it.number, it.name, lang) }} | करण: ${snapshot.karana.joinToString(", ") { VedicLocalizer.karanaName(it.name, lang) }}")
            appendLine("अभिजित् मुहूर्त: ${snapshot.abhijitMuhurta.start} – ${snapshot.abhijitMuhurta.end}")
            appendLine("राहु काल: ${snapshot.rahuKala.start} – ${snapshot.rahuKala.end}")
            if (snapshot.dishaShoolaHi.isNotBlank()) appendLine("दिशाशूल: ${snapshot.dishaShoolaHi}")
            if (snapshot.panchakaStatusHi.isNotBlank()) appendLine("पञ्चक: ${snapshot.panchakaStatusHi}")
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
            if (snapshot.panchakaStatusEn.isNotBlank()) appendLine("Panchaka: ${snapshot.panchakaStatusEn}")
        }
    }.trim()

    fun formatNavtaraSummary(
        nativeName: String,
        birthNakshatraName: String,
        birthPada: Int,
        todayNakshatraName: String,
        todayResult: NavtaraResult,
        gandantaTitle: String,
        lang: AppLanguage
    ): String = buildString {
        if (lang == AppLanguage.HINDI) {
            appendLine("॥ नवतारा चक्र विश्लेषण — महावतार पंचांग ॥")
            if (nativeName.isNotBlank()) appendLine("जातक: $nativeName")
            appendLine("जन्म नक्षत्र: $birthNakshatraName (चरण $birthPada)")
            appendLine("आज का नक्षत्र: $todayNakshatraName")
            appendLine("तारा: ${todayResult.taraNameHi} (पर्याय ${todayResult.paryaya}) | स्कोर: ${todayResult.score}/100")
            appendLine("प्रकृति: ${todayResult.natureHi} (${todayResult.intensityLevelHi})")
            appendLine("विवरण: ${todayResult.descriptionHi}")
            appendLine("गण्डान्त स्थिति: $gandantaTitle")
            todayResult.parihara?.let { p ->
                appendLine("परिहार — देवता: ${p.deityHi} | मन्त्र: ${p.mantraHi} | दान: ${p.donationHi}")
            }
        } else {
            appendLine("Mahavtaar Panchanga — Navtara Chakra Analysis")
            if (nativeName.isNotBlank()) appendLine("Native: $nativeName")
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
        if (lang == AppLanguage.HINDI) {
            appendLine("॥ जन्म कुण्डली विवरण — ${report.input.name} ॥")
            appendLine("जन्म: ${report.input.day}/${report.input.month}/${report.input.year} ${"%02d:%02d".format(report.input.hour, report.input.minute)} | ${report.input.location.name}")
            appendLine("लग्न: ${VedicLocalizer.rasiNameByZeroIndex(report.lagnaSignIndex, lang)} (${report.lagnaDms}) | चन्द्र राशि: ${VedicLocalizer.rasiNameByZeroIndex(report.moonSignIndex, lang)}")
            appendLine("चन्द्र नक्षत्र: ${VedicLocalizer.nakshatraNameByNumber(report.moonNakshatraNumber, lang)} चरण ${report.moonPada}")
            appendLine("दशा शेष: ${report.birthBalanceHi} | वर्तमान दशा: ${report.currentMahadashaLabelHi}")
            appendLine("दोष: ${report.doshas.manglikLabelHi} | ${report.doshas.kalsarpaLabelHi} | ${report.doshas.sadeSatiLabelHi}")
            val presentYogas = report.yogas.filter { it.isPresent }
            if (presentYogas.isNotEmpty()) {
                appendLine("प्रमुख योग (${presentYogas.size}): ${presentYogas.joinToString(", ") { it.nameHi }}")
            }
            appendLine("\n--- ग्रह स्थिति ---")
            report.planets.forEach { p ->
                appendLine("${p.nameHi}: ${VedicLocalizer.rasiNameByZeroIndex(p.signIndex, lang)} ${p.dmsLabel} (भाव ${p.houseD1}, ${p.dignityHi})")
            }
        } else {
            appendLine("Janam Kundli Report — ${report.input.name}")
            appendLine("Birth: ${report.input.year}-${"%02d-%02d".format(report.input.month, report.input.day)} ${"%02d:%02d".format(report.input.hour, report.input.minute)} | ${report.input.location.name}")
            appendLine("Ascendant: ${VedicLocalizer.rasiNameByZeroIndex(report.lagnaSignIndex, lang)} (${report.lagnaDms}) | Moon Sign: ${VedicLocalizer.rasiNameByZeroIndex(report.moonSignIndex, lang)}")
            appendLine("Moon Nakshatra: ${VedicLocalizer.nakshatraNameByNumber(report.moonNakshatraNumber, lang)} Pada ${report.moonPada}")
            appendLine("Balance: ${report.birthBalanceEn} | Active Dasha: ${report.currentMahadashaLabelEn}")
            appendLine("Doshas: ${report.doshas.manglikLabelEn} | ${report.doshas.kalsarpaLabelEn} | ${report.doshas.sadeSatiLabelEn}")
            val presentYogas = report.yogas.filter { it.isPresent }
            if (presentYogas.isNotEmpty()) {
                appendLine("Active Yogas (${presentYogas.size}): ${presentYogas.joinToString(", ") { it.nameEn }}")
            }
            appendLine("\n--- Planetary Longitudes ---")
            report.planets.forEach { p ->
                appendLine("${p.nameEn}: ${VedicLocalizer.rasiNameByZeroIndex(p.signIndex, lang)} ${p.dmsLabel} (H${p.houseD1}, ${p.dignityEn})")
            }
        }
        report.audit?.let {
            appendLine("\nAudit Hash: ${it.chartHash} | Ayanamsha: ${it.ayanamshaModel} (${"%.4f".format(it.ayanamshaDeg)}°)")
        }
    }.trim()
}
