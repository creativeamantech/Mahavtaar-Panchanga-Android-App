package com.mahavtaar.panchanga.ui.export

import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.TimingInterval
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import java.util.Locale

object DailyPanchangaHtmlBuilder {

    fun build(snapshot: PanchangaSnapshot, lang: AppLanguage): String {
        val isHi = lang == AppLanguage.HINDI
        val coords = TimezoneCatalog.formatCoordinates(snapshot.latitude, snapshot.longitude, 5)
        val title = if (isHi) "दैनिक पंचांग — ${snapshot.dateDisplay}" else "Daily Vedic Panchanga — ${snapshot.dateDisplay}"

        val tithiRows = snapshot.tithi.joinToString("") { seg ->
            val endText = seg.ends ?: (if (isHi) "अहोरात्र" else "Full Day")
            "<tr><td>${escapeHtml(VedicLocalizer.tithiName(seg.number, seg.name, lang))}</td><td>${escapeHtml(endText)}</td><td>${seg.swaraInfo?.let { escapeHtml(VedicLocalizer.nadiShortLabel(it.startNadi, lang)) } ?: "—"}</td></tr>"
        }
        val nakshatraRows = snapshot.nakshatra.joinToString("") { seg ->
            val endText = seg.ends ?: (if (isHi) "अहोरात्र" else "Full Day")
            "<tr><td>${escapeHtml(VedicLocalizer.nakshatraName(seg.number, seg.name, lang))}</td><td>${escapeHtml(endText)}</td><td>${seg.swaraInfo?.let { escapeHtml(VedicLocalizer.nadiShortLabel(it.startNadi, lang)) } ?: "—"}</td></tr>"
        }
        val yogaRows = snapshot.yoga.joinToString("") { seg ->
            val endText = seg.ends ?: (if (isHi) "अहोरात्र" else "Full Day")
            "<tr><td>${escapeHtml(VedicLocalizer.yogaName(seg.number, seg.name, lang))}</td><td>${escapeHtml(endText)}</td></tr>"
        }
        val karanaRows = snapshot.karana.joinToString("") { seg ->
            val endText = seg.ends ?: (if (isHi) "अहोरात्र" else "Full Day")
            "<tr><td>${escapeHtml(VedicLocalizer.karanaName(seg.name, lang))}</td><td>${escapeHtml(endText)}</td></tr>"
        }

        val choghadiyaDayRows = snapshot.gauriChoghadiyaDay.joinToString("") { intervalRow(it, lang) }
        val choghadiyaNightRows = snapshot.gauriChoghadiyaNight.joinToString("") { intervalRow(it, lang) }

        val observancesHtml = if (snapshot.observances.isEmpty()) {
            "<p class='muted'>${if (isHi) "इस तिथि पर कोई विशेष पर्व/व्रत नहीं है।" else "No special Vrat or festival recorded for this lunar day."}</p>"
        } else {
            snapshot.observances.joinToString("") { obs ->
                "<div class='obs-card'><strong>${escapeHtml(VedicLocalizer.observanceTitle(obs, lang))}</strong><br/><span class='muted'>${escapeHtml(VedicLocalizer.observanceSubtitle(obs, lang))} · ${escapeHtml(VedicLocalizer.observanceDeity(obs, lang))}</span></div>"
            }
        }

        val dishaShoola = if (isHi) snapshot.dishaShoolaHi else snapshot.dishaShoolaEn
        val panchaka = if (isHi) snapshot.panchakaStatusHi else snapshot.panchakaStatusEn

        return """
<!DOCTYPE html>
<html lang="${if (isHi) "hi" else "en"}">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<title>${escapeHtml(title)}</title>
<style>
  @page { size: A4 portrait; margin: 14mm; }
  * { box-sizing: border-box; }
  body { font-family: -apple-system, BlinkMacSystemFont, "Noto Sans Devanagari", "Segoe UI", sans-serif; color: #1c1917; background: #fcfaf5; margin: 0; padding: 18px; line-height: 1.45; }
  .sheet { max-width: 900px; margin: 0 auto; background: #ffffff; border: 1px solid #e7dcc8; border-radius: 12px; padding: 20px; box-shadow: 0 2px 12px rgba(45,30,10,0.06); }
  .banner { background: linear-gradient(135deg, #2d1b36, #4a2346); color: #fff8eb; padding: 18px 20px; border-radius: 10px; margin-bottom: 16px; }
  .banner h1 { margin: 0 0 6px; font-size: 20px; }
  .banner .meta { font-size: 12px; opacity: 0.92; display: flex; flex-wrap: wrap; gap: 10px; }
  .pill { display: inline-block; padding: 3px 9px; border-radius: 999px; font-size: 11px; font-weight: 600; background: rgba(255,255,255,0.16); }
  .pill-good { background: #dcfce7; color: #166534; }
  .pill-bad { background: #fee2e2; color: #991b1b; }
  .grid-2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 14px; margin-bottom: 16px; }
  .grid-3 { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; margin-bottom: 16px; }
  .card { border: 1px solid #e7e5e4; border-radius: 8px; padding: 12px; background: #fffdf9; }
  h2 { font-size: 14px; text-transform: uppercase; letter-spacing: 0.04em; color: #6d28d9; margin: 14px 0 8px; border-bottom: 1px solid #ede9fe; padding-bottom: 4px; }
  table { width: 100%; border-collapse: collapse; font-size: 12.5px; }
  th, td { border-bottom: 1px solid #f1ece1; padding: 6px 8px; text-align: left; }
  th { background: #f8f4ec; color: #44403c; font-weight: 600; }
  .kv { display: flex; justify-content: space-between; padding: 4px 0; border-bottom: 1px dashed #ece7dc; font-size: 12.5px; }
  .kv:last-child { border-bottom: none; }
  .muted { color: #57534e; font-size: 12px; }
  .obs-card { background: #fef3c7; border-left: 4px solid #d97706; padding: 8px 12px; border-radius: 6px; margin-bottom: 8px; font-size: 12.5px; }
  .footer { margin-top: 16px; padding-top: 10px; border-top: 1px solid #e7e5e4; font-size: 11px; color: #78716c; text-align: center; }
  @media print { body { background: #fff; padding: 0; } .sheet { border: none; box-shadow: none; padding: 0; } }
</style>
</head>
<body>
<div class="sheet">
  <div class="banner">
    <h1>${if (isHi) "॥ महावतार पंचांग — दैनिक पञ्चाङ्गम् ॥" else "Mahavtaar Panchanga — Daily Vedic Ephemeris"}</h1>
    <div class="meta">
      <span class="pill">📅 ${escapeHtml(snapshot.dateDisplay)} (${escapeHtml(VedicLocalizer.vaaraName(snapshot.weekday, snapshot.vaara, lang))})</span>
      <span class="pill">📍 ${escapeHtml(snapshot.city)} (${escapeHtml(coords)})</span>
      <span class="pill">🕒 ${escapeHtml(snapshot.timezone)}</span>
      <span class="pill">🔭 ${escapeHtml(VedicLocalizer.ayanamshaLabel(snapshot.ayanamshaKey, lang))}${snapshot.ayanamshaDegrees?.let { " · ${"%.4f".format(Locale.US, it)}°" } ?: ""}</span>
    </div>
  </div>

  <div class="grid-3">
    <div class="card">
      <div class="kv"><span>${if (isHi) "संवत्सर" else "Samvatsara"}</span><strong>${escapeHtml(VedicLocalizer.samvatsaraName(snapshot.samvatsara, lang))}</strong></div>
      <div class="kv"><span>${if (isHi) "शक / विक्रम संवत्" else "Saka / Vikrama"}</span><strong>${snapshot.sakaYear} / ${snapshot.vikramaYear}</strong></div>
      <div class="kv"><span>${if (isHi) "मास / पक्ष" else "Masa / Paksha"}</span><strong>${escapeHtml(VedicLocalizer.masaName(snapshot.masaNumber, snapshot.isAdhika, snapshot.isKshaya, snapshot.masa, lang))} · ${escapeHtml(VedicLocalizer.pakshaName(snapshot.paksha, lang))}</strong></div>
      <div class="kv"><span>${if (isHi) "ऋतु / अयन" else "Rtu / Ayana"}</span><strong>${escapeHtml(VedicLocalizer.rtuName(snapshot.drikRtu, lang))} · ${escapeHtml(VedicLocalizer.ayanaName(snapshot.drikAyana, lang))}</strong></div>
    </div>
    <div class="card">
      <div class="kv"><span>${if (isHi) "सूर्योदय" else "Sunrise"}</span><strong>${escapeHtml(snapshot.sunrise)}</strong></div>
      <div class="kv"><span>${if (isHi) "सूर्यास्त" else "Sunset"}</span><strong>${escapeHtml(snapshot.sunset)}</strong></div>
      <div class="kv"><span>${if (isHi) "चन्द्रोदय" else "Moonrise"}</span><strong>${escapeHtml(snapshot.moonrise ?: snapshot.moonriseStatus)}</strong></div>
      <div class="kv"><span>${if (isHi) "चन्द्रास्त" else "Moonset"}</span><strong>${escapeHtml(snapshot.moonset ?: snapshot.moonsetStatus)}</strong></div>
    </div>
    <div class="card">
      <div class="kv"><span>${if (isHi) "सूर्य / चन्द्र राशि" else "Sun / Moon Sign"}</span><strong>${escapeHtml(VedicLocalizer.rasiByName(snapshot.sunRasi, lang))} / ${escapeHtml(VedicLocalizer.rasiByName(snapshot.moonRasi, lang))}</strong></div>
      <div class="kv"><span>${if (isHi) "चन्द्र कला" else "Lunar Phase"}</span><strong>${escapeHtml(VedicLocalizer.lunarPhaseName(snapshot.lunarPhase.phaseName, lang))} (${snapshot.lunarPhase.illuminationPercent}%)</strong></div>
      <div class="kv"><span>${if (isHi) "सक्रिय स्वर नाड़ी" else "Active Swara Nadi"}</span><strong>${escapeHtml(VedicLocalizer.nadiShortLabel(snapshot.swaraYoga.currentActiveSwara, lang))}</strong></div>
      <div class="kv"><span>${if (isHi) "दिनमान / रात्रिमान" else "Day / Night Span"}</span><strong>${escapeHtml(snapshot.dayDuration)} / ${escapeHtml(snapshot.nightDuration)}</strong></div>
    </div>
  </div>

  <h2>${if (isHi) "पञ्चाङ्ग के मुख्य पाँच अङ्ग" else "Core Five Angas"}</h2>
  <div class="grid-2">
    <div class="card">
      <strong>${if (isHi) "तिथि" else "Tithi"}</strong>
      <table><thead><tr><th>${if (isHi) "नाम" else "Name"}</th><th>${if (isHi) "समाप्ति" else "Ends"}</th><th>${if (isHi) "स्वर" else "Nadi"}</th></tr></thead><tbody>$tithiRows</tbody></table>
    </div>
    <div class="card">
      <strong>${if (isHi) "नक्षत्र" else "Nakshatra"}</strong>
      <table><thead><tr><th>${if (isHi) "नाम" else "Name"}</th><th>${if (isHi) "समाप्ति" else "Ends"}</th><th>${if (isHi) "स्वर" else "Nadi"}</th></tr></thead><tbody>$nakshatraRows</tbody></table>
    </div>
    <div class="card">
      <strong>${if (isHi) "योग" else "Yoga"}</strong>
      <table><thead><tr><th>${if (isHi) "नाम" else "Name"}</th><th>${if (isHi) "समाप्ति" else "Ends"}</th></tr></thead><tbody>$yogaRows</tbody></table>
    </div>
    <div class="card">
      <strong>${if (isHi) "करण" else "Karana"}</strong>
      <table><thead><tr><th>${if (isHi) "नाम" else "Name"}</th><th>${if (isHi) "समाप्ति" else "Ends"}</th></tr></thead><tbody>$karanaRows</tbody></table>
    </div>
  </div>

  <h2>${if (isHi) "शुभ एवं अशुभ मुहूर्त" else "Auspicious & Inauspicious Timings"}</h2>
  <div class="grid-2">
    <div class="card">
      <table>
        <thead><tr><th>${if (isHi) "शुभ मुहूर्त" else "Auspicious"}</th><th>${if (isHi) "अवधि" else "Window"}</th></tr></thead>
        <tbody>
          <tr><td>${if (isHi) "ब्रह्म मुहूर्त" else "Brahma Muhurta"}</td><td>${escapeHtml(snapshot.brahmaMuhurta.start)} – ${escapeHtml(snapshot.brahmaMuhurta.end)}</td></tr>
          <tr><td>${if (isHi) "अभिजित् मुहूर्त" else "Abhijit Muhurta"}</td><td>${escapeHtml(snapshot.abhijitMuhurta.start)} – ${escapeHtml(snapshot.abhijitMuhurta.end)}</td></tr>
          ${snapshot.godhuliMuhurta?.let { "<tr><td>${if (isHi) "गोधूलि मुहूर्त" else "Godhuli Muhurta"}</td><td>${escapeHtml(it.start)} – ${escapeHtml(it.end)}</td></tr>" } ?: ""}
          ${snapshot.vijayaMuhurta?.let { "<tr><td>${if (isHi) "विजय मुहूर्त" else "Vijaya Muhurta"}</td><td>${escapeHtml(it.start)} – ${escapeHtml(it.end)}</td></tr>" } ?: ""}
          ${snapshot.amritaKala.joinToString("") { "<tr><td>${if (isHi) "अमृत काल" else "Amrita Kala"}</td><td>${escapeHtml(it.start)} – ${escapeHtml(it.end)}</td></tr>" }}
        </tbody>
      </table>
    </div>
    <div class="card">
      <table>
        <thead><tr><th>${if (isHi) "अशुभ / त्याज्य काल" else "Inauspicious / Caution"}</th><th>${if (isHi) "अवधि" else "Window"}</th></tr></thead>
        <tbody>
          <tr><td>${if (isHi) "राहु काल" else "Rahu Kala"}</td><td>${escapeHtml(snapshot.rahuKala.start)} – ${escapeHtml(snapshot.rahuKala.end)}</td></tr>
          <tr><td>${if (isHi) "यमगण्ड" else "Yamaganda"}</td><td>${escapeHtml(snapshot.yamaganda.start)} – ${escapeHtml(snapshot.yamaganda.end)}</td></tr>
          <tr><td>${if (isHi) "गुलिक काल" else "Gulika Kala"}</td><td>${escapeHtml(snapshot.gulikaKala.start)} – ${escapeHtml(snapshot.gulikaKala.end)}</td></tr>
          ${snapshot.durmuhurta.joinToString("") { "<tr><td>${if (isHi) "दुर्मुहूर्त" else "Durmuhurta"}</td><td>${escapeHtml(it.start)} – ${escapeHtml(it.end)}</td></tr>" }}
          ${snapshot.varjyam.joinToString("") { "<tr><td>${if (isHi) "वर्ज्यम्" else "Varjyam"}</td><td>${escapeHtml(it.start)} – ${escapeHtml(it.end)}</td></tr>" }}
        </tbody>
      </table>
    </div>
  </div>

  <h2>${if (isHi) "दिन एवं रात्रि चौघड़िया" else "Day & Night Choghadiya"}</h2>
  <div class="grid-2">
    <div class="card">
      <strong>${if (isHi) "दिवा चौघड़िया (दिन)" else "Day Choghadiya"}</strong>
      <table><tbody>$choghadiyaDayRows</tbody></table>
    </div>
    <div class="card">
      <strong>${if (isHi) "रात्रि चौघड़िया (रात)" else "Night Choghadiya"}</strong>
      <table><tbody>$choghadiyaNightRows</tbody></table>
    </div>
  </div>

  <h2>${if (isHi) "दिशाशूल, पञ्चक एवं व्रत-पर्व" else "Disha Shoola, Panchaka & Observances"}</h2>
  <div class="card">
    ${if (dishaShoola.isNotBlank()) "<div class='kv'><span>${if (isHi) "दिशाशूल" else "Disha Shoola"}</span><strong>${escapeHtml(dishaShoola)}</strong></div>" else ""}
    ${if (panchaka.isNotBlank()) "<div class='kv'><span>${if (isHi) "पञ्चक स्थिति" else "Panchaka Status"}</span><strong>${escapeHtml(panchaka)}</strong></div>" else ""}
    <div class="kv"><span>${if (isHi) "भद्रा (विष्टि)" else "Bhadra Status"}</span><strong>${if (snapshot.hasBhadra) (if (isHi) "भद्रा उपस्थित" else "Bhadra Active") else (if (isHi) "भद्रा मुक्त" else "Clear / No Bhadra")}</strong></div>
    <div style="margin-top:8px;">$observancesHtml</div>
  </div>

  <div class="footer">
    ${if (isHi) "महावतार दृक् पञ्चाङ्ग ऑफ़लाइन रिपोर्ट · मास प्रणाली: ${VedicLocalizer.monthSystemShort(snapshot.monthSystem, lang)}" else "Generated offline by Mahavtaar Drik Engine · Month System: ${VedicLocalizer.monthSystemShort(snapshot.monthSystem, lang)}"}
  </div>
</div>
</body>
</html>
        """.trimIndent()
    }

    private fun intervalRow(interval: TimingInterval, lang: AppLanguage): String {
        val label = VedicLocalizer.muhurtaOrPeriodName(interval.name, lang)
        val badgeClass = if (interval.isAuspicious) "pill-good" else "pill-bad"
        val statusText = if (interval.isAuspicious) {
            if (lang == AppLanguage.HINDI) "शुभ" else "Good"
        } else {
            if (lang == AppLanguage.HINDI) "त्याज्य" else "Avoid"
        }
        return "<tr><td>${escapeHtml(label)}</td><td>${escapeHtml(interval.start)} – ${escapeHtml(interval.end)}</td><td><span class='pill $badgeClass'>$statusText</span></td></tr>"
    }

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
