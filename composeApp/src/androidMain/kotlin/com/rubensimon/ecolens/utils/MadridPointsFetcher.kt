package com.rubensimon.ecolens.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.net.URL
import java.util.Locale

import com.rubensimon.ecolens.data.models.maps.EcoLatLng
import com.rubensimon.ecolens.data.models.maps.RecyclingPoint

object MadridPointsFetcher {
    private const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L

    private const val URL_PUNTOS_FIJOS_XML = "https://datos.madrid.es/egob/catalogo/200284-0-puntos-limpios.xml"
    private const val URL_PUNTOS_MOVILES_CSV = "https://datos.madrid.es/egob/catalogo/300101-1-puntos-limpios-moviles.csv"
    private const val URL_PUNTOS_PROXIMIDAD_CSV = "https://datos.madrid.es/egob/catalogo/300198-1-puntos-proximidad.csv"
    private const val URL_PUNTOS_MOVILES_24H_CSV = "https://datos.madrid.es/egob/catalogo/900026-0-puntos-limpios-residuos.csv"

    suspend fun loadAllRecyclingPoints(context: Context): List<RecyclingPoint> = withContext(Dispatchers.IO) {
        val all = mutableListOf<RecyclingPoint>()
        try { all += loadFixedPointsFromXml(context, URL_PUNTOS_FIJOS_XML) } catch (e: Exception) { Log.e("Map", "Error XML", e) }
        try { all += loadPointsFromCsv(context, URL_PUNTOS_MOVILES_CSV, "MOVIL") } catch (e: Exception) { Log.e("Map", "Error CSV", e) }
        try { all += loadPointsFromCsv(context, URL_PUNTOS_PROXIMIDAD_CSV, "PROXIMIDAD") } catch (e: Exception) { Log.e("Map", "Error CSV", e) }
        try { all += loadPointsFromCsv(context, URL_PUNTOS_MOVILES_24H_CSV, "MOVIL_24H") } catch (e: Exception) { Log.e("Map", "Error CSV", e) }
        all.filter { it.position.latitude != 0.0 && it.position.longitude != 0.0 }
    }

    private suspend fun loadFixedPointsFromXml(context: Context, url: String): List<RecyclingPoint> {
        val xml = fetchWithCache(context, url, "puntos_fijos.xml")
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())

        val points = mutableListOf<RecyclingPoint>()
        var inContent = false
        var currentName: String? = null
        var currentLat: Double? = null
        var currentLng: Double? = null
        var currentSnippet: String? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals("contenido", true)) inContent = true
                    if (inContent && parser.name.equals("atributo", true)) {
                        val attrName = parser.getAttributeValue(null, "nombre") ?: ""
                        when (attrName.uppercase(Locale.getDefault())) {
                            "NOMBRE" -> currentName = safeNextText(parser)
                            "LATITUD" -> currentLat = safeNextText(parser).toDoubleOrNull()
                            "LONGITUD" -> currentLng = safeNextText(parser).toDoubleOrNull()
                            "HORARIO" -> {
                                val t = safeNextText(parser)
                                if (!t.isNullOrBlank()) currentSnippet = t
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("contenido", true)) {
                        if (!currentName.isNullOrBlank() && currentLat != null && currentLng != null) {
                            points += RecyclingPoint("FIJO", currentName!!, EcoLatLng(currentLat!!, currentLng!!), currentSnippet)
                        }
                        currentName = null; currentLat = null; currentLng = null; currentSnippet = null
                        inContent = false
                    }
                }
            }
            parser.next()
        }
        return points
    }

    private fun safeNextText(parser: XmlPullParser): String {
        return try { parser.nextText() ?: "" } catch (_: Exception) { "" }
    }

    private suspend fun loadPointsFromCsv(context: Context, url: String, kind: String): List<RecyclingPoint> {
        val csv = fetchWithCache(context, url, "puntos_$kind.csv")
        val lines = csv.split("\n").map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val header = parseCsvLine(lines.first(), ';')
        val idxLat = header.indexOfFirst { it.uppercase().contains("LATITUD") }
        val idxLng = header.indexOfFirst { it.uppercase().contains("LONGITUD") }
        val idxName = header.indexOfFirst { it.uppercase().contains("NOMBRE") || it.uppercase().contains("DENOMINACION") }
        val idxAddress = header.indexOfFirst { it.uppercase().contains("DIRECCION") || it.uppercase().contains("VIA") }

        val points = mutableListOf<RecyclingPoint>()
        for (i in 1 until lines.size) {
            val cols = parseCsvLine(lines[i], ';')
            val lat = cols.getOrNull(idxLat)?.replace(",", ".")?.toDoubleOrNull()
            val lng = cols.getOrNull(idxLng)?.replace(",", ".")?.toDoubleOrNull()
            if (lat == null || lng == null) continue

            val name = cols.getOrNull(idxName)?.takeIf { it.isNotBlank() } ?: "Punto $kind"
            val addr = cols.getOrNull(idxAddress)?.takeIf { it.isNotBlank() }
            points += RecyclingPoint(kind, name, EcoLatLng(lat, lng), addr)
        }
        return points
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when (ch) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i++ }
                    else { inQuotes = !inQuotes }
                }
                delimiter -> {
                    if (inQuotes) sb.append(ch)
                    else { out.add(sb.toString()); sb.clear() }
                }
                else -> sb.append(ch)
            }
            i++
        }
        out.add(sb.toString())
        return out.map { it.trim() }
    }

    private suspend fun fetchWithCache(context: Context, url: String, cacheFileName: String): String {
        val cacheFile = File(context.cacheDir, cacheFileName)
        val now = System.currentTimeMillis()
        if (cacheFile.exists() && now - cacheFile.lastModified() < CACHE_TTL_MS) {
            return cacheFile.readText()
        }
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw java.io.IOException("Error de red: ${response.code}")
        val content = response.body?.string() ?: ""
        try { cacheFile.writeText(content) } catch (_: Exception) {}
        return content
    }
}
