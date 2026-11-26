package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.MonthSystem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import kotlin.math.abs

/**
 * Automated numerical parity test suite validating the Kotlin [DrikPanchangaEngine]
 * against all 41 golden reference fixtures captured from `src/lib/panchangaEngine.server.ts`.
 */
class GoldenPanchangaParityTest {

    private val engine = DrikPanchangaEngine()
    private val json = Json { ignoreUnknownKeys = true }

    private fun resolveReferenceDir(): File {
        val candidates = listOf(
            File("reference/panchanga/v1"),
            File("../reference/panchanga/v1"),
            File("../../reference/panchanga/v1")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Could not locate reference/panchanga/v1 from ${File(".").absolutePath}")
    }

    @Test
    fun verifyAll41GoldenReferenceFixturesMatchWithinStrictTolerances() {
        val refDir = resolveReferenceDir()
        val manifestFile = File(refDir, "manifest.json")
        assertTrue("Manifest file must exist", manifestFile.exists())

        val manifestObj = json.parseToJsonElement(manifestFile.readText()).jsonObject
        val fixtures = manifestObj["fixtures"]!!.jsonArray
        assertEquals("Expected all 41 golden fixtures", 41, fixtures.size)

        val failures = mutableListOf<String>()

        for (entry in fixtures) {
            val fileName = entry.jsonObject["file"]!!.jsonPrimitive.content
            val fixtureFile = File(refDir, fileName)
            assertTrue("Missing fixture $fileName", fixtureFile.exists())

            val root = json.parseToJsonElement(fixtureFile.readText()).jsonObject
            val scenario = root["scenario"]!!.jsonObject
            val expected = root["expected"]!!.jsonObject

            val location = CityLocation(
                name = scenario["city"]!!.jsonPrimitive.content,
                latitude = scenario["latitude"]!!.jsonPrimitive.double,
                longitude = scenario["longitude"]!!.jsonPrimitive.double,
                timezone = scenario["timezone"]!!.jsonPrimitive.content
            )
            val date = LocalDate.parse(scenario["date"]!!.jsonPrimitive.content)
            val monthSystem = MonthSystem.fromKey(scenario["monthSystem"]!!.jsonPrimitive.content)
            val coord = CoordinateSelection.fromKey(scenario["coordinateSelection"]!!.jsonPrimitive.content)

            val actual = engine.computePanchanga(
                location = location,
                date = date,
                monthSystem = monthSystem,
                coordinateSelection = coord,
                includeExtendedTransits = false
            )

            val id = scenario["id"]!!.jsonPrimitive.content

            // 1. Sunrise / Sunset / Next Sunrise timestamps (<= 2 seconds tolerance)
            checkTimestamp(id, "sunrise_ms", expected["sunrise_ms"]!!.jsonPrimitive.long, actual.sunriseMs, 2000L, failures)
            checkTimestamp(id, "sunset_ms", expected["sunset_ms"]!!.jsonPrimitive.long, actual.sunsetMs, 2000L, failures)
            checkTimestamp(id, "next_sunrise_ms", expected["next_sunrise_ms"]!!.jsonPrimitive.long, actual.nextSunriseMs, 2000L, failures)

            // 2. Moonrise / Moonset presence parity
            val expMoonriseStatus = expected["moonrise_status"]!!.jsonPrimitive.content
            val expMoonsetStatus = expected["moonset_status"]!!.jsonPrimitive.content
            if (expMoonriseStatus != actual.moonriseStatus) {
                failures.add("$id: moonrise_status expected=$expMoonriseStatus actual=${actual.moonriseStatus}")
            }
            if (expMoonsetStatus != actual.moonsetStatus) {
                failures.add("$id: moonset_status expected=$expMoonsetStatus actual=${actual.moonsetStatus}")
            }

            // 3. Calendar metadata exact parity
            checkExact(id, "vaara", expected["vaara"]!!.jsonPrimitive.content, actual.vaara, failures)
            checkExact(id, "paksha", expected["paksha"]!!.jsonPrimitive.content, actual.paksha, failures)
            checkExact(id, "masa", expected["masa"]!!.jsonPrimitive.content, actual.masa, failures)
            checkExact(id, "is_adhika", expected["is_adhika"]!!.jsonPrimitive.boolean, actual.isAdhika, failures)
            checkExact(id, "samvatsara", expected["samvatsara"]!!.jsonPrimitive.content, actual.samvatsara, failures)
            checkExact(id, "saka_year", expected["saka_year"]!!.jsonPrimitive.int, actual.sakaYear, failures)
            checkExact(id, "vikrama_year", expected["vikrama_year"]!!.jsonPrimitive.int, actual.vikramaYear, failures)
            checkExact(id, "kali_day", expected["kali_day"]!!.jsonPrimitive.long, actual.kaliDay, failures)
            checkExact(id, "ayana", expected["ayana"]!!.jsonPrimitive.content, actual.ayana, failures)
            checkExact(id, "rtu", expected["rtu"]!!.jsonPrimitive.content, actual.rtu, failures)

            // 4. Ayanamsha degrees parity (<= 0.001 deg)
            val expAyanDeg = expected["ayanamsa_degrees"]?.jsonPrimitive?.doubleOrNull
            if (expAyanDeg != null && actual.ayanamshaDegrees != null) {
                if (abs(expAyanDeg - actual.ayanamshaDegrees!!) > 0.001) {
                    failures.add("$id: ayanamsa_degrees expected=$expAyanDeg actual=${actual.ayanamshaDegrees}")
                }
            }

            // 5. Five Angas parity (Tithi, Nakshatra, Yoga, Karana)
            verifyAngaArray(id, "tithi", expected["tithi"]!!.jsonArray, actual.tithi.map { Triple(it.number, it.name, it.endTimeMs) }, failures)
            verifyAngaArray(id, "nakshatra", expected["nakshatra"]!!.jsonArray, actual.nakshatra.map { Triple(it.number, it.name, it.endTimeMs) }, failures)
            verifyAngaArray(id, "yoga", expected["yoga"]!!.jsonArray, actual.yoga.map { Triple(it.number, it.name, it.endTimeMs) }, failures)
            verifyAngaArray(id, "karana", expected["karana"]!!.jsonArray, actual.karana.map { Triple(it.number, it.name, it.endTimeMs) }, failures)

            // 6. Rahu Kala & Choghadiya parity
            val expRahu = expected["rahu_kala"]!!.jsonObject
            checkTimestamp(id, "rahu.start", expRahu["startTimeMs"]!!.jsonPrimitive.double.toLong(), actual.rahuKala.startTimeMs ?: 0L, 2000L, failures)
            checkTimestamp(id, "rahu.end", expRahu["endTimeMs"]!!.jsonPrimitive.double.toLong(), actual.rahuKala.endTimeMs ?: 0L, 2000L, failures)

            val expChoghadiyaDay = expected["gauri_choghadiya_day"]!!.jsonArray
            checkExact(id, "choghadiya_day.size", expChoghadiyaDay.size, actual.gauriChoghadiyaDay.size, failures)
            if (expChoghadiyaDay.size == actual.gauriChoghadiyaDay.size) {
                for (i in expChoghadiyaDay.indices) {
                    val expName = expChoghadiyaDay[i].jsonObject["name"]!!.jsonPrimitive.content
                    checkExact(id, "choghadiya_day[$i].name", expName, actual.gauriChoghadiyaDay[i].name, failures)
                }
            }

            // 7. Navagraha sidereal longitudes parity (<= 0.002 deg)
            val expPlanets = expected["planets"]!!.jsonArray
            checkExact(id, "planets.size", expPlanets.size, actual.planets.size, failures)
            for (pElem in expPlanets) {
                val pObj = pElem.jsonObject
                val pId = pObj["id"]!!.jsonPrimitive.content
                val actualPlanet = actual.planets.firstOrNull { it.id == pId }
                if (actualPlanet == null) {
                    failures.add("$id: missing planet $pId")
                    continue
                }
                val expSid = pObj["siderealLongitude"]!!.jsonPrimitive.double
                val diff = angularDiff(expSid, actualPlanet.siderealLongitude)
                if (diff > 0.002) {
                    failures.add("$id: planet[$pId].siderealLongitude expected=$expSid actual=${actualPlanet.siderealLongitude} diff=$diff")
                }
                val expRasiNum = pObj["rasiNumber"]!!.jsonPrimitive.int
                checkExact(id, "planet[$pId].rasiNumber", expRasiNum, actualPlanet.rasiNumber, failures)
            }
        }

        if (failures.isNotEmpty()) {
            val preview = failures.take(25).joinToString("\n")
            throw AssertionError("Golden parity failed with ${failures.size} discrepancies:\n$preview")
        }
    }

    private fun verifyAngaArray(
        fixtureId: String,
        label: String,
        expectedArr: JsonArray,
        actualList: List<Triple<Int, String, Long?>>,
        failures: MutableList<String>
    ) {
        if (expectedArr.size != actualList.size) {
            failures.add("$fixtureId: $label.size expected=${expectedArr.size} actual=${actualList.size}")
            return
        }
        for (i in expectedArr.indices) {
            val obj = expectedArr[i].jsonObject
            val expNum = obj["number"]!!.jsonPrimitive.int
            val expName = obj["name"]!!.jsonPrimitive.content
            val expEndMs = obj["endTimeMs"]?.jsonPrimitive?.longOrNull
            val (actNum, actName, actEndMs) = actualList[i]
            checkExact(fixtureId, "$label[$i].number", expNum, actNum, failures)
            checkExact(fixtureId, "$label[$i].name", expName, actName, failures)
            if (expEndMs != null && actEndMs != null) {
                checkTimestamp(fixtureId, "$label[$i].endTimeMs", expEndMs, actEndMs, 5000L, failures)
            }
        }
    }

    private fun checkTimestamp(
        fixtureId: String,
        field: String,
        expectedMs: Long,
        actualMs: Long,
        toleranceMs: Long,
        failures: MutableList<String>
    ) {
        val diff = abs(expectedMs - actualMs)
        if (diff > toleranceMs) {
            failures.add("$fixtureId: $field diff=${diff}ms (expected=$expectedMs, actual=$actualMs)")
        }
    }

    private fun <T> checkExact(
        fixtureId: String,
        field: String,
        expected: T,
        actual: T,
        failures: MutableList<String>
    ) {
        if (expected != actual) {
            failures.add("$fixtureId: $field expected=$expected actual=$actual")
        }
    }

    private fun angularDiff(a: Double, b: Double): Double {
        val raw = abs(a - b) % 360.0
        return if (raw > 180.0) 360.0 - raw else raw
    }
}
