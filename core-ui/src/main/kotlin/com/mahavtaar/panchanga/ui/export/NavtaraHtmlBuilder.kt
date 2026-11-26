package com.mahavtaar.panchanga.ui.export

import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.NavtaraDayCategory
import com.mahavtaar.panchanga.domain.model.NavtaraFeedbackRating
import com.mahavtaar.panchanga.domain.model.NavtaraPeriodReport
import com.mahavtaar.panchanga.domain.model.NavtaraResult
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.localization.VedicSwaraNavtaraCatalog
import java.time.LocalDate

object NavtaraHtmlBuilder {

    fun build(
        report: NavtaraPeriodReport,
        matrix: List<NavtaraResult>,
        lang: AppLanguage
    ): String {
        val isHi = lang == AppLanguage.HINDI
        val birthNakName = VedicLocalizer.nakshatraNameByNumber(report.birthNakshatraIndex, lang)
        val birthMeta = VedicSwaraNavtaraCatalog.nakshatraMeta(report.birthNakshatraIndex)
        val padaInfo = VedicSwaraNavtaraCatalog.padaPurushartha(report.birthPada)
        val gandanta = VedicSwaraNavtaraCatalog.checkGandanta(report.birthNakshatraIndex, report.birthPada)
        val coords = TimezoneCatalog.formatCoordinates(report.location.latitude, report.location.longitude, 5)
        val title = if (isHi) {
            "नवतारा गोचर रिपोर्ट — ${report.profileName}"
        } else {
            "Navtara Chakra Report — ${report.profileName}"
        }

        val weekdays = VedicLocalizer.weekdayShortLabels(lang)
        val calendarHeader = weekdays.joinToString("") { "<div class='cal-head'>${escapeHtml(it)}</div>" }

        val firstDate = runCatching { LocalDate.parse(report.startDateIso) }.getOrNull()
        val leadingBlanks = if (firstDate != null) (firstDate.dayOfWeek.value % 7) else 0
        val blankCells = (0 until leadingBlanks).joinToString("") { "<div class='cal-cell empty'></div>" }

        val calCells = report.days.joinToString("") { day ->
            val cls = when (day.category) {
                NavtaraDayCategory.AUSPICIOUS -> "cat-good"
                NavtaraDayCategory.MIXED -> "cat-mixed"
                NavtaraDayCategory.CAUTION -> "cat-bad"
                NavtaraDayCategory.NEUTRAL -> "cat-neutral"
            }
            val taraName = if (isHi) day.currentTara.taraNameHi else day.currentTara.taraNameEn
            val starShort = VedicLocalizer.nakshatraName(day.nakshatraNumber, day.nakshatraName, lang)
            val fbBadge = day.feedback?.let { "<div class='cal-fb'>${it.rating.emoji}</div>" } ?: ""
            """
            <div class="cal-cell $cls">
              <div class="cal-top"><span>${day.dayOfMonth}</span>$fbBadge</div>
              <div class="cal-tara">${escapeHtml(taraName)}</div>
              <div class="cal-star">${escapeHtml(starShort)}</div>
            </div>
            """.trimIndent()
        }

        val dayCardsHtml = report.days.joinToString("") { day ->
            val cls = when (day.category) {
                NavtaraDayCategory.AUSPICIOUS -> "cat-good"
                NavtaraDayCategory.MIXED -> "cat-mixed"
                NavtaraDayCategory.CAUTION -> "cat-bad"
                NavtaraDayCategory.NEUTRAL -> "cat-neutral"
            }
            val taraLabel = if (isHi) day.currentTara.taraNameHi else day.currentTara.taraNameEn
            val natureLabel = if (isHi) day.currentTara.natureHi else day.currentTara.natureEn
            val nextTara = day.nextTara
            val nextNakName = day.nextNakshatraName
            val nextTaraBadge = if (nextTara != null && nextNakName != null) {
                val nLabel = if (isHi) nextTara.taraNameHi else nextTara.taraNameEn
                val nStar = VedicLocalizer.nakshatraName(day.nextNakshatraNumber ?: day.nakshatraNumber, nextNakName, lang)
                " → ${escapeHtml(nStar)} (${escapeHtml(nLabel)})"
            } else ""
            val fbHtml = day.feedback?.let { fb ->
                val ratingLabel = if (isHi) fb.rating.labelHi else fb.rating.labelEn
                val notePart = if (fb.note.isNotBlank()) " — ${escapeHtml(fb.note)}" else ""
                "<div class='fb-box'><strong>${fb.rating.emoji} ${escapeHtml(ratingLabel)}</strong>$notePart</div>"
            } ?: ""

            """
            <div class="day-card $cls">
              <div class="day-head">
                <div><strong>${escapeHtml(day.dateIso)}</strong> · ${escapeHtml(VedicLocalizer.vaaraName(day.weekday, day.vaara, lang))}</div>
                <div class="score-badge">${escapeHtml(taraLabel)} (${day.currentTara.score}/100)</div>
              </div>
              <div class="day-body">
                <div><strong>${if (isHi) "तिथि" else "Tithi"}:</strong> ${escapeHtml(VedicLocalizer.tithiName(day.tithiNumber, day.tithiName, lang))}${day.tithiEnds?.let { " (${escapeHtml(it)})" } ?: ""}</div>
                <div><strong>${if (isHi) "नक्षत्र" else "Nakshatra"}:</strong> ${escapeHtml(VedicLocalizer.nakshatraName(day.nakshatraNumber, day.nakshatraName, lang))}${day.nakshatraEnds?.let { " (${escapeHtml(it)})" } ?: ""}$nextTaraBadge</div>
                <div><strong>${if (isHi) "प्रभाव" else "Quality"}:</strong> ${escapeHtml(natureLabel)} · ${escapeHtml(if (isHi) day.recommendationHi else day.recommendationEn)}</div>
                $fbHtml
              </div>
            </div>
            """.trimIndent()
        }

        val dayTableRows = report.days.joinToString("") { day ->
            val taraLabel = if (isHi) day.currentTara.taraNameHi else day.currentTara.taraNameEn
            val nextTaraRow = day.nextTara
            val nextNakRowName = day.nextNakshatraName
            val nextInfo = if (nextTaraRow != null && nextNakRowName != null) {
                val nLabel = if (isHi) nextTaraRow.taraNameHi else nextTaraRow.taraNameEn
                val nStar = VedicLocalizer.nakshatraName(day.nextNakshatraNumber ?: day.nakshatraNumber, nextNakRowName, lang)
                "<br/><span class='muted'>→ $nStar ($nLabel)</span>"
            } else ""
            val fbText = day.feedback?.let { "${it.rating.emoji} ${escapeHtml(it.note)}" } ?: "—"
            """
            <tr>
              <td><strong>${escapeHtml(day.dateIso)}</strong><br/><span class="muted">${escapeHtml(VedicLocalizer.vaaraName(day.weekday, day.vaara, lang))}</span></td>
              <td>${escapeHtml(VedicLocalizer.tithiName(day.tithiNumber, day.tithiName, lang))}<br/><span class="muted">${escapeHtml(day.tithiEnds ?: "")}</span></td>
              <td>${escapeHtml(VedicLocalizer.nakshatraName(day.nakshatraNumber, day.nakshatraName, lang))}<br/><span class="muted">${escapeHtml(day.nakshatraEnds ?: "")}</span>$nextInfo</td>
              <td><strong>${escapeHtml(taraLabel)}</strong> (${day.currentTara.score})<br/><span class="muted">${escapeHtml(if (isHi) day.recommendationHi else day.recommendationEn)}</span></td>
              <td>$fbText</td>
            </tr>
            """.trimIndent()
        }

        val transitionRows = if (report.transitions.isEmpty()) {
            "<tr><td colspan='4' class='muted'>${if (isHi) "कोई तारा परिवर्तन नहीं" else "No transitions recorded"}</td></tr>"
        } else {
            report.transitions.joinToString("") { tr ->
                val fromStar = VedicLocalizer.nakshatraName(tr.fromNakshatraNumber, tr.fromNakshatraName, lang)
                val toStar = VedicLocalizer.nakshatraName(tr.toNakshatraNumber, tr.toNakshatraName, lang)
                val fromTara = if (isHi) tr.fromTara.taraNameHi else tr.fromTara.taraNameEn
                val toTara = if (isHi) tr.toTara.taraNameHi else tr.toTara.taraNameEn
                "<tr><td>${escapeHtml(tr.dateIso)}</td><td>${escapeHtml(tr.transitionTime)}</td><td>${escapeHtml(fromStar)} ($fromTara)</td><td>${escapeHtml(toStar)} (<strong>${escapeHtml(toTara)}</strong>)</td></tr>"
            }
        }

        val matrixRows = matrix.joinToString("") { res ->
            val starName = VedicLocalizer.nakshatraNameByNumber(res.targetNakshatraIndex, lang)
            val taraName = if (isHi) res.taraNameHi else res.taraNameEn
            val nature = if (isHi) res.natureHi else res.natureEn
            val desc = if (isHi) res.descriptionHi else res.descriptionEn
            val rem = res.parihara?.let { p ->
                val deity = if (isHi) p.deityHi else p.deityEn
                val mantra = if (isHi) p.mantraHi else p.mantraEn
                val donation = if (isHi) p.donationHi else p.donationEn
                "$deity · $mantra · $donation"
            } ?: "—"
            "<tr><td>#${res.targetNakshatraIndex} ${escapeHtml(starName)}</td><td>${res.paryaya}</td><td><strong>${escapeHtml(taraName)}</strong> (${res.score})</td><td>${escapeHtml(nature)}</td><td>${escapeHtml(desc)}<br/><span class='muted'>${escapeHtml(rem)}</span></td></tr>"
        }

        return """
<!DOCTYPE html>
<html lang="${if (isHi) "hi" else "en"}">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<title>${escapeHtml(title)}</title>
<style>
  @page { size: A4 portrait; margin: 12mm; }
  * { box-sizing: border-box; }
  body { font-family: -apple-system, BlinkMacSystemFont, "Noto Sans Devanagari", "Segoe UI", sans-serif; color: #1c1917; background: #faf7f0; margin: 0; padding: 16px; line-height: 1.45; }
  .sheet { max-width: 960px; margin: 0 auto; background: #fff; border: 1px solid #e7dcc8; border-radius: 12px; padding: 20px; }
  .banner { background: linear-gradient(135deg, #1e1b4b, #4c1d95); color: #fefce8; padding: 18px 20px; border-radius: 10px; margin-bottom: 14px; }
  .banner h1 { margin: 0 0 6px; font-size: 20px; }
  .meta { display: flex; flex-wrap: wrap; gap: 8px; font-size: 12px; }
  .pill { background: rgba(255,255,255,0.16); padding: 3px 9px; border-radius: 999px; }
  .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 10px; margin-bottom: 16px; }
  .stat-card { border-radius: 8px; padding: 10px 12px; border: 1px solid #e7e5e4; text-align: center; }
  .stat-num { font-size: 22px; font-weight: 700; }
  .cat-good { background: #f0fdf4; border-color: #86efac; }
  .cat-mixed { background: #fefce8; border-color: #fde047; }
  .cat-bad { background: #fef2f2; border-color: #fca5a5; }
  .cat-neutral { background: #f8fafc; border-color: #cbd5e1; }
  .info-box { background: #fffdf7; border: 1px solid #e7dcc8; border-radius: 8px; padding: 12px; margin-bottom: 14px; font-size: 12.5px; }
  .toolbar { display: flex; gap: 8px; justify-content: flex-end; margin-bottom: 10px; }
  .btn { border: 1px solid #6d28d9; background: #f5f3ff; color: #5b21b6; padding: 6px 12px; border-radius: 8px; font-size: 12px; font-weight: 600; cursor: pointer; }
  .btn.active { background: #6d28d9; color: #fff; }
  h2 { font-size: 14px; text-transform: uppercase; letter-spacing: 0.04em; color: #5b21b6; margin: 16px 0 8px; border-bottom: 1px solid #ede9fe; padding-bottom: 4px; }
  .cal-grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 6px; margin-bottom: 16px; }
  .cal-head { text-align: center; font-weight: 700; font-size: 11px; color: #57534e; padding: 4px; }
  .cal-cell { border: 1px solid #e7e5e4; border-radius: 6px; padding: 6px; min-height: 62px; font-size: 11px; }
  .cal-cell.empty { background: transparent; border: none; }
  .cal-top { display: flex; justify-content: space-between; font-weight: 700; }
  .cal-tara { font-weight: 600; color: #312e81; margin-top: 2px; }
  .cal-star { color: #57534e; font-size: 10px; }
  .day-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 10px; }
  .day-card { border: 1px solid #e7e5e4; border-radius: 8px; padding: 10px; font-size: 12px; }
  .day-head { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(0,0,0,0.06); padding-bottom: 5px; margin-bottom: 6px; }
  .score-badge { font-weight: 700; color: #4c1d95; }
  .fb-box { margin-top: 6px; padding: 5px 8px; background: rgba(255,255,255,0.85); border-radius: 6px; border: 1px dashed #a8a29e; }
  table { width: 100%; border-collapse: collapse; font-size: 12px; margin-bottom: 14px; }
  th, td { border-bottom: 1px solid #ece7dc; padding: 6px 8px; text-align: left; vertical-align: top; }
  th { background: #f5f3ff; color: #312e81; font-weight: 600; }
  .muted { color: #57534e; font-size: 11px; }
  .hidden { display: none; }
  @media print { .toolbar { display: none; } #cardsView { display: none !important; } #tableView { display: block !important; } body { background: #fff; padding: 0; } .sheet { border: none; padding: 0; } }
</style>
</head>
<body>
<div class="sheet">
  <div class="banner">
    <h1>${if (isHi) "॥ नवतारा चक्र — मासिक / गोचर विश्लेषण रिपोर्ट ॥" else "Navtara Chakra — Monthly & Custom Period Transit Report"}</h1>
    <div class="meta">
      <span class="pill">👤 ${escapeHtml(report.profileName)}</span>
      <span class="pill">⭐ ${escapeHtml(birthNakName)} (${if (isHi) "चरण" else "Pada"} ${report.birthPada} · ${escapeHtml(if (isHi) padaInfo.nameHi else padaInfo.nameEn)})</span>
      <span class="pill">📅 ${escapeHtml(report.startDateIso)} → ${escapeHtml(report.endDateIso)}</span>
      <span class="pill">📍 ${escapeHtml(report.location.name)} (${escapeHtml(coords)} · ${escapeHtml(report.location.timezone)})</span>
    </div>
  </div>

  <div class="info-box">
    <strong>${if (isHi) "जन्म नक्षत्र विवरण:" else "Birth Star Metadata:"}</strong>
    ${if (isHi) "स्वामी: ${birthMeta.lordHi} · देवता: ${birthMeta.deityHi} · गण: ${birthMeta.ganaHi} · नाड़ी: ${birthMeta.nadiHi}" else "Lord: ${birthMeta.lordEn} · Deity: ${birthMeta.deityEn} · Gana: ${birthMeta.ganaEn} · Nadi: ${birthMeta.nadiEn}"}<br/>
    <strong>${if (isHi) "गण्डान्त स्थिति:" else "Gandanta Check:"}</strong> ${escapeHtml(if (isHi) gandanta.titleHi else gandanta.titleEn)} — ${escapeHtml(if (isHi) gandanta.guidanceHi else gandanta.guidanceEn)}
  </div>

  <div class="stats-grid">
    <div class="stat-card cat-good"><div class="stat-num">${report.stats.auspiciousCount}</div><div>${if (isHi) "शुभ दिवस" else "Auspicious Days"}</div></div>
    <div class="stat-card cat-mixed"><div class="stat-num">${report.stats.mixedCount}</div><div>${if (isHi) "संधि / मिश्रित" else "Transition / Mixed"}</div></div>
    <div class="stat-card cat-bad"><div class="stat-num">${report.stats.cautionCount}</div><div>${if (isHi) "सावधानी दिवस" else "Caution Days"}</div></div>
    <div class="stat-card cat-neutral"><div class="stat-num">${report.stats.neutralCount}</div><div>${if (isHi) "सामान्य दिवस" else "Neutral Days"}</div></div>
    <div class="stat-card"><div class="stat-num">${report.stats.feedbackCount}</div><div>${if (isHi) "अनुभव डायरी प्रविष्टि" else "Journal Entries"} (${NavtaraFeedbackRating.AUSPICIOUS.emoji} ${report.stats.auspiciousFeedbackCount} · ${NavtaraFeedbackRating.NEUTRAL.emoji} ${report.stats.neutralFeedbackCount} · ${NavtaraFeedbackRating.CAUTION.emoji} ${report.stats.cautionFeedbackCount})</div></div>
  </div>

  <h2>${if (isHi) "मासिक कैलेण्डर दृश्य" else "Calendar Matrix Overview"}</h2>
  <div class="cal-grid">$calendarHeader$blankCells$calCells</div>

  <h2>${if (isHi) "दैनिक गोचर एवं अनुभव विवरण" else "Day-by-Day Transit & Experience Breakdown"}</h2>
  <div class="toolbar">
    <button id="btnCards" class="btn active" onclick="toggleNavtaraView('cards')">${if (isHi) "कार्ड दृश्य" else "Cards View"}</button>
    <button id="btnTable" class="btn" onclick="toggleNavtaraView('table')">${if (isHi) "प्रिंट तालिका दृश्य" else "Print Table View"}</button>
  </div>
  <div id="cardsView" class="day-cards">$dayCardsHtml</div>
  <div id="tableView" class="hidden">
    <table>
      <thead><tr><th>${if (isHi) "दिनांक" else "Date"}</th><th>${if (isHi) "तिथि" else "Tithi"}</th><th>${if (isHi) "नक्षत्र" else "Nakshatra"}</th><th>${if (isHi) "तारा व फल" else "Tara & Guidance"}</th><th>${if (isHi) "अनुभव डायरी" else "Journal"}</th></tr></thead>
      <tbody>$dayTableRows</tbody>
    </table>
  </div>

  <h2>${if (isHi) "प्रमुख नक्षत्र एवं तारा परिवर्तन" else "Key Nakshatra & Tara Transitions"}</h2>
  <table>
    <thead><tr><th>${if (isHi) "दिनांक" else "Date"}</th><th>${if (isHi) "समय" else "Time"}</th><th>${if (isHi) "से (नक्षत्र / तारा)" else "From"}</th><th>${if (isHi) "में (नक्षत्र / तारा)" else "To"}</th></tr></thead>
    <tbody>$transitionRows</tbody>
  </table>

  <h2>${if (isHi) "२७ नक्षत्र नवतारा पूर्ण चक्र एवं परिहार" else "Complete 27-Star Navtara Matrix & Remedies"}</h2>
  <table>
    <thead><tr><th>${if (isHi) "नक्षत्र" else "Nakshatra"}</th><th>${if (isHi) "पर्याय" else "Cycle"}</th><th>${if (isHi) "तारा" else "Tara"}</th><th>${if (isHi) "प्रकृति" else "Nature"}</th><th>${if (isHi) "फल एवं परिहार" else "Guidance & Parihara"}</th></tr></thead>
    <tbody>$matrixRows</tbody>
  </table>
</div>
<script>
function toggleNavtaraView(mode) {
  var c = document.getElementById('cardsView');
  var t = document.getElementById('tableView');
  var bc = document.getElementById('btnCards');
  var bt = document.getElementById('btnTable');
  if (mode === 'table') {
    c.classList.add('hidden'); t.classList.remove('hidden');
    bc.classList.remove('active'); bt.classList.add('active');
  } else {
    t.classList.add('hidden'); c.classList.remove('hidden');
    bt.classList.remove('active'); bc.classList.add('active');
  }
}
</script>
</body>
</html>
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
