package com.mahavtaar.panchanga.ui.export

import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.KundliHouseEntry
import com.mahavtaar.panchanga.domain.model.KundliReport
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import java.util.Locale

object KundliHtmlBuilder {

    fun build(report: KundliReport, lang: AppLanguage): String {
        val isHi = lang == AppLanguage.HINDI
        val inp = report.input
        val coords = TimezoneCatalog.formatCoordinates(inp.location.latitude, inp.location.longitude, 5)
        val birthTime = "%02d:%02d:%02d".format(Locale.US, inp.hour, inp.minute, inp.second)
        val birthDate = "%04d-%02d-%02d".format(Locale.US, inp.year, inp.month, inp.day)
        val lagnaName = VedicLocalizer.rasiNameByZeroIndex(report.lagnaSignIndex, lang)
        val moonRasiName = VedicLocalizer.rasiNameByZeroIndex(report.moonSignIndex, lang)
        val moonNakName = VedicLocalizer.nakshatraNameByNumber(report.moonNakshatraNumber, lang)

        val svgD1 = buildSouthIndianSvg(
            title = if (isHi) "लग्न कुण्डली (D1)" else "Rasi Chart (D1)",
            lagnaSignIndex = report.lagnaSignIndex,
            houses = report.housesD1,
            report = report,
            lang = lang
        )
        val svgD9 = buildSouthIndianSvg(
            title = if (isHi) "नवांश कुण्डली (D9)" else "Navamsha Chart (D9)",
            lagnaSignIndex = report.navamshaLagnaSignIndex,
            houses = report.housesD9,
            report = report,
            lang = lang
        )

        val planetRows = report.planets.joinToString("") { p ->
            val flags = buildList {
                if (p.isRetrograde) add(if (isHi) "वक्री" else "R")
                if (p.isCombust) add(if (isHi) "अस्त" else "C")
            }.joinToString("·").ifBlank { "—" }
            """
            <tr>
              <td>${escapeHtml(p.symbol)} <strong>${escapeHtml(if (isHi) p.nameHi else p.nameEn)}</strong></td>
              <td>${escapeHtml(VedicLocalizer.rasiNameByZeroIndex(p.signIndex, lang))}</td>
              <td>${escapeHtml(p.dmsLabel)}</td>
              <td>H${p.houseD1}</td>
              <td>${escapeHtml(VedicLocalizer.nakshatraNameByNumber(p.nakshatraNumber, lang))} (${p.pada})</td>
              <td>$flags</td>
              <td>${escapeHtml(if (isHi) p.dignityHi else p.dignityEn)}</td>
            </tr>
            """.trimIndent()
        }

        val dashaRows = report.vimshottariDashas.joinToString("") { md ->
            val badge = if (md.isCurrent) "<span class='pill pill-active'>${if (isHi) "सक्रिय" else "ACTIVE"}</span>" else ""
            val adList = md.antardashas.joinToString(", ") { ad ->
                "${if (isHi) ad.planetNameHi else ad.planetNameEn} (${ad.startDate}→${ad.endDate})"
            }
            """
            <tr>
              <td><strong>${escapeHtml(if (isHi) md.planetNameHi else md.planetNameEn)}</strong> $badge</td>
              <td>${escapeHtml(md.startDate)} – ${escapeHtml(md.endDate)} (${"%.1f".format(Locale.US, md.durationYears)}y)</td>
              <td class="muted">${escapeHtml(adList)}</td>
            </tr>
            """.trimIndent()
        }

        val shadbalaRows = report.shadbala.joinToString("") { s ->
            val strongBadge = if (s.isStrong) "✓" else "!"
            "<tr><td><strong>${escapeHtml(if (isHi) s.nameHi else s.nameEn)}</strong></td><td>${"%.1f".format(Locale.US, s.sthanaBala)}</td><td>${"%.1f".format(Locale.US, s.digBala)}</td><td>${"%.1f".format(Locale.US, s.kalaBala)}</td><td>${"%.1f".format(Locale.US, s.cheshtaBala)}</td><td>${"%.1f".format(Locale.US, s.naisargikaBala)}</td><td>${"%.1f".format(Locale.US, s.drikBala)}</td><td><strong>${"%.2f".format(Locale.US, s.totalRupas)}</strong> / ${"%.1f".format(Locale.US, s.requiredRupas)} ($strongBadge)</td></tr>"
        }

        val savCells = (0 until 12).joinToString("") { idx ->
            val valBindu = report.ashtakavarga.sarvashtakavarga.getOrNull(idx) ?: 0
            "<th>${escapeHtml(VedicLocalizer.rasiShortByZeroIndex(idx, lang))}<br/><span>$valBindu</span></th>"
        }

        val kpRows = report.kpTable.joinToString("") { k ->
            "<tr><td>${escapeHtml(if (isHi) k.nameHi else k.nameEn)}</td><td>${escapeHtml(k.dmsLabel)}</td><td>H${k.houseOccupied}</td><td>${escapeHtml(k.signLord)} / ${escapeHtml(k.starLord)} / <strong>${escapeHtml(k.subLord)}</strong></td></tr>"
        }

        val karakaRows = report.jaiminiKarakas.joinToString("") { c ->
            "<tr><td><strong>${escapeHtml(c.code)}</strong> (${escapeHtml(if (isHi) c.titleHi else c.titleEn)})</td><td>${escapeHtml(if (isHi) c.planetNameHi else c.planetNameEn)}</td><td>${"%.2f".format(Locale.US, c.degreeInSign)}°</td></tr>"
        }

        val yogaRows = report.yogas.filter { it.isPresent || it.isMitigated }.joinToString("") { y ->
            val status = when {
                y.isPresent -> if (isHi) "उपस्थित" else "Present"
                else -> if (isHi) "भंग / शमित" else "Mitigated"
            }
            "<tr><td><strong>${escapeHtml(if (isHi) y.nameHi else y.nameEn)}</strong></td><td>$status</td><td>${escapeHtml(if (isHi) y.descriptionHi else y.descriptionEn)}</td></tr>"
        }.ifBlank { "<tr><td colspan='3' class='muted'>—</td></tr>" }

        val gocharRows = report.gocharEffects.joinToString("") { g ->
            val vedha = if (g.hasVedha) " (${if (isHi) "वेध" else "Vedha"}: ${escapeHtml((if (isHi) g.vedhaPlanetNameHi else g.vedhaPlanetNameEn) ?: "")})" else ""
            "<tr><td>${escapeHtml(if (isHi) g.nameHi else g.nameEn)}</td><td>${escapeHtml(VedicLocalizer.rasiNameByZeroIndex(g.transitSignIndex, lang))} (H${g.houseFromMoon})</td><td>${if (g.isFavorable) "✓" else "!"} ${escapeHtml(if (isHi) g.verdictHi else g.verdictEn)}$vedha</td></tr>"
        }

        return """
<!DOCTYPE html>
<html lang="${if (isHi) "hi" else "en"}">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<title>${escapeHtml(inp.name)} — Kundli Report</title>
<style>
  @page { size: A4 portrait; margin: 12mm; }
  * { box-sizing: border-box; }
  body { font-family: -apple-system, BlinkMacSystemFont, "Noto Sans Devanagari", "Segoe UI", sans-serif; color: #1c1917; background: #fbf8f1; margin: 0; padding: 16px; line-height: 1.4; }
  .sheet { max-width: 960px; margin: 0 auto; background: #fff; border: 1px solid #e7dcc8; border-radius: 12px; padding: 20px; }
  .banner { background: linear-gradient(135deg, #3b0764, #7c2d12); color: #fffbeb; padding: 18px 20px; border-radius: 10px; margin-bottom: 16px; }
  .banner h1 { margin: 0 0 6px; font-size: 20px; }
  .meta { display: flex; flex-wrap: wrap; gap: 8px; font-size: 12px; }
  .pill { background: rgba(255,255,255,0.18); padding: 3px 9px; border-radius: 999px; }
  .pill-active { background: #dcfce7; color: #166534; font-weight: 700; font-size: 10px; }
  h2 { font-size: 13.5px; text-transform: uppercase; letter-spacing: 0.04em; color: #7c2d12; margin: 16px 0 8px; border-bottom: 1px solid #fed7aa; padding-bottom: 4px; page-break-after: avoid; }
  .charts-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; margin-bottom: 14px; }
  .chart-box { border: 1px solid #e7dcc8; border-radius: 8px; padding: 10px; background: #fffdf8; text-align: center; }
  table { width: 100%; border-collapse: collapse; font-size: 11.8px; margin-bottom: 12px; }
  th, td { border: 1px solid #ece7dc; padding: 5px 7px; text-align: left; vertical-align: top; }
  th { background: #fef3c7; color: #78350f; font-weight: 600; }
  .muted { color: #57534e; font-size: 11px; }
  .grid-2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 12px; }
  .card { border: 1px solid #e7e5e4; border-radius: 8px; padding: 10px; background: #fffdf9; font-size: 12px; }
  @media print { body { background: #fff; padding: 0; } .sheet { border: none; padding: 0; } }
</style>
</head>
<body>
<div class="sheet">
  <div class="banner">
    <h1>${if (isHi) "॥ वैदिक जन्म कुण्डली महाग्रन्थ — ${escapeHtml(inp.name)} ॥" else "Vedic Janam Kundli Report — ${escapeHtml(inp.name)}"}</h1>
    <div class="meta">
      <span class="pill">🗓 $birthDate · $birthTime</span>
      <span class="pill">📍 ${escapeHtml(inp.location.name)} ($coords · ${escapeHtml(inp.location.timezone)})</span>
      <span class="pill">⬆️ ${if (isHi) "लग्न" else "Lagna"}: ${escapeHtml(lagnaName)} (${escapeHtml(report.lagnaDms)})</span>
      <span class="pill">🌙 ${escapeHtml(moonRasiName)} · ${escapeHtml(moonNakName)} P${report.moonPada}</span>
    </div>
  </div>

  <h2>1. ${if (isHi) "जन्म विवरण एवं लग्न चक्र (D1 व D9)" else "Birth Summary & South Indian Charts (D1 & D9)"}</h2>
  <div class="charts-row">
    <div class="chart-box">$svgD1</div>
    <div class="chart-box">$svgD9</div>
  </div>

  <h2>2. ${if (isHi) "नवग्रह स्पष्ट एवं नक्षत्र स्थिति" else "Planetary Longitudes & Nakshatra Positions"}</h2>
  <table>
    <thead><tr><th>${if (isHi) "ग्रह" else "Planet"}</th><th>${if (isHi) "राशि" else "Sign"}</th><th>${if (isHi) "अंश" else "DMS"}</th><th>${if (isHi) "भाव" else "H"}</th><th>${if (isHi) "नक्षत्र (चरण)" else "Nakshatra (Pada)"}</th><th>${if (isHi) "अवस्था" else "State"}</th><th>${if (isHi) "बल" else "Dignity"}</th></tr></thead>
    <tbody>$planetRows</tbody>
  </table>

  <h2>3. ${if (isHi) "विंशोत्तरी महादशा एवं अन्तर्दशा" else "Vimshottari Mahadasha & Antardasha"}</h2>
  <div class="card" style="margin-bottom:8px;"><strong>${if (isHi) "जन्म दशा शेष:" else "Birth Balance:"}</strong> ${escapeHtml(if (isHi) report.birthBalanceHi else report.birthBalanceEn)} · <strong>${if (isHi) "वर्तमान:" else "Active:"}</strong> ${escapeHtml(if (isHi) report.currentMahadashaLabelHi else report.currentMahadashaLabelEn)}</div>
  <table>
    <thead><tr><th>${if (isHi) "महादशा" else "Mahadasha"}</th><th>${if (isHi) "अवधि" else "Period"}</th><th>${if (isHi) "अन्तर्दशाएँ" else "Antardashas"}</th></tr></thead>
    <tbody>$dashaRows</tbody>
  </table>

  <h2>4. ${if (isHi) "षड्बल एवं सर्वाष्टकवर्ग" else "Shadbala Strength & Sarvashtakavarga"}</h2>
  <table>
    <thead><tr><th>${if (isHi) "ग्रह" else "Planet"}</th><th>Sthana</th><th>Dig</th><th>Kala</th><th>Cheshta</th><th>Naisargika</th><th>Drik</th><th>Rupas / Req</th></tr></thead>
    <tbody>$shadbalaRows</tbody>
  </table>
  <table><thead><tr>$savCells</tr></thead></table>

  <h2>5. ${if (isHi) "के.पी. (कृष्णमूर्ति) व जैमिनी चर कारक" else "KP Sub-Lords & Jaimini Chara Karakas"}</h2>
  <div class="grid-2">
    <div><table><thead><tr><th>${if (isHi) "ग्रह" else "Planet"}</th><th>DMS</th><th>H</th><th>Sign / Star / Sub</th></tr></thead><tbody>$kpRows</tbody></table></div>
    <div><table><thead><tr><th>${if (isHi) "कारक" else "Karaka"}</th><th>${if (isHi) "ग्रह" else "Planet"}</th><th>${if (isHi) "अंश" else "Deg"}</th></tr></thead><tbody>$karakaRows</tbody></table></div>
  </div>

  <h2>6. ${if (isHi) "योग, दोष एवं गोचर फल" else "Yogas, Doshas & Transit Summary"}</h2>
  <div class="card" style="margin-bottom:8px;">
    <strong>${if (isHi) "दोष:" else "Doshas:"}</strong> ${escapeHtml(if (isHi) report.doshas.manglikLabelHi else report.doshas.manglikLabelEn)} · ${escapeHtml(if (isHi) report.doshas.kalsarpaLabelHi else report.doshas.kalsarpaLabelEn)} · ${escapeHtml(if (isHi) report.doshas.sadeSatiLabelHi else report.doshas.sadeSatiLabelEn)}
  </div>
  <table><thead><tr><th>Yoga</th><th>${if (isHi) "स्थिति" else "Status"}</th><th>${if (isHi) "फल" else "Details"}</th></tr></thead><tbody>$yogaRows</tbody></table>
  <table><thead><tr><th>${if (isHi) "गोचर ग्रह" else "Transit"}</th><th>${if (isHi) "राशि / भाव" else "Sign / H"}</th><th>${if (isHi) "परिणाम" else "Verdict"}</th></tr></thead><tbody>$gocharRows</tbody></table>

  ${report.audit?.let { a ->
      """
      <h2>7. ${if (isHi) "गणितीय स्थिरांक एवं प्रमाण" else "Calculation Constants & Provenance Audit"}</h2>
      <div class="card">
        <strong>${escapeHtml(a.engineVersion)}</strong> · Chart Hash: <code>${escapeHtml(a.chartHash)}</code><br/>
        JD: ${"%.6f".format(Locale.US, a.julianDay)} · LST: ${"%.4f".format(Locale.US, a.lstDeg)}° · Ayanamsha (${escapeHtml(a.ayanamshaModel)}): ${"%.5f".format(Locale.US, a.ayanamshaDeg)}°<br/>
        Coordinates: ${"%.5f".format(Locale.US, a.latitude)}°, ${"%.5f".format(Locale.US, a.longitude)}° · TZ: ${escapeHtml(a.timezone)} · Sidereal Year: 365.256363d / Tropical: 365.242199d
      </div>
      """.trimIndent()
  } ?: ""}
</div>
</body>
</html>
        """.trimIndent()
    }

    private fun buildSouthIndianSvg(
        title: String,
        lagnaSignIndex: Int,
        houses: List<KundliHouseEntry>,
        report: KundliReport,
        lang: AppLanguage
    ): String {
        val isHi = lang == AppLanguage.HINDI
        // South Indian fixed sign grid coordinates (col, row) for signIndex 0..11 (Aries..Pisces)
        val signPositions = mapOf(
            11 to (0 to 0), 0 to (1 to 0), 1 to (2 to 0), 2 to (3 to 0),
            10 to (0 to 1), 3 to (3 to 1),
            9 to (0 to 2), 4 to (3 to 2),
            8 to (0 to 3), 7 to (1 to 3), 6 to (2 to 3), 5 to (3 to 3)
        )
        val planetsBySign = (0..11).associateWith { s ->
            houses.firstOrNull { it.signIndex == s }?.planetIds?.map { pid ->
                val p = report.planets.firstOrNull { it.id == pid }
                if (p != null) (if (isHi) p.shortHi else p.shortEn) + (if (p.isRetrograde) "*" else "") else pid
            }.orEmpty()
        }
        val cells = signPositions.entries.joinToString("") { (signIdx, pos) ->
            val x = pos.first * 70
            val y = pos.second * 70
            val label = VedicLocalizer.rasiShortByZeroIndex(signIdx, lang)
            val isLagna = signIdx == lagnaSignIndex
            val pList = planetsBySign[signIdx].orEmpty().joinToString(" ")
            val lagnaMark = if (isLagna) " ⬆" else ""
            """
            <rect x="$x" y="$y" width="70" height="70" fill="${if (isLagna) "#fef3c7" else "#fffdf9"}" stroke="#b45309" stroke-width="1"/>
            <text x="${x + 4}" y="${y + 13}" font-size="9" font-weight="bold" fill="#78350f">${escapeHtml(label)}$lagnaMark</text>
            <text x="${x + 4}" y="${y + 32}" font-size="9.5" fill="#1c1917">${escapeHtml(pList)}</text>
            """.trimIndent()
        }
        return """
        <svg viewBox="0 0 280 280" width="240" height="240" xmlns="http://www.w3.org/2000/svg">
          $cells
          <rect x="70" y="70" width="140" height="140" fill="#fffbeb" stroke="#b45309" stroke-width="1"/>
          <text x="140" y="135" font-size="11" font-weight="bold" text-anchor="middle" fill="#7c2d12">${escapeHtml(title)}</text>
          <text x="140" y="152" font-size="9" text-anchor="middle" fill="#57534e">${escapeHtml(report.input.name)}</text>
        </svg>
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
