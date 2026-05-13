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
        
        // Fallback de seguridad: Red completa de Puntos Fijos de Madrid (16 centros principales)
        if (all.none { it.kind == "FIJO" }) {
            all += listOf(
                RecyclingPoint("FIJO", "Punto Limpio Arganzuela", EcoLatLng(40.3963, -3.6968), "C/ Palos de la Frontera, 11"),
                RecyclingPoint("FIJO", "Punto Limpio Barajas", EcoLatLng(40.4727, -3.5714), "C/ Almirante Diego de Alvear, s/n"),
                RecyclingPoint("FIJO", "Punto Limpio Carabanchel", EcoLatLng(40.3644, -3.7314), "C/ San Antolín, s/n"),
                RecyclingPoint("FIJO", "Punto Limpio Chamartín", EcoLatLng(40.4633, -3.6744), "Avda. Alberto Alcocer, 46"),
                RecyclingPoint("FIJO", "Punto Limpio Ciudad Lineal", EcoLatLng(40.4431, -3.6425), "Avda. Daroca, s/n"),
                RecyclingPoint("FIJO", "Punto Limpio Fuencarral-El Pardo", EcoLatLng(40.4822, -3.7121), "C/ Nuestra Señora de Valverde, 193"),
                RecyclingPoint("FIJO", "Punto Limpio Hortaleza", EcoLatLng(40.4744, -3.6408), "C/ Tomás Redondo, 8"),
                RecyclingPoint("FIJO", "Punto Limpio Latina", EcoLatLng(40.3855, -3.7432), "C/ Concejal Francisco José Jiménez Martín"),
                RecyclingPoint("FIJO", "Punto Limpio Moncloa-Aravaca", EcoLatLng(40.4503, -3.7547), "Avda. de Valladolid, s/n"),
                RecyclingPoint("FIJO", "Punto Limpio Moratalaz", EcoLatLng(40.4072, -3.6339), "C/ Vinateros, s/n"),
                RecyclingPoint("FIJO", "Punto Limpio Puente de Vallecas", EcoLatLng(40.3844, -3.6542), "C/ Josepa Díaz, s/n"),
                RecyclingPoint("FIJO", "Punto Limpio San Blas-Canillejas", EcoLatLng(40.4358, -3.6067), "C/ San Romualdo, 20"),
                RecyclingPoint("FIJO", "Punto Limpio Tetuán", EcoLatLng(40.4650, -3.7028), "Paseo de la Dirección, s/n"),
                RecyclingPoint("FIJO", "Punto Limpio Usera", EcoLatLng(40.3708, -3.7022), "C/ Cristo de la Victoria, 245"),
                RecyclingPoint("FIJO", "Punto Limpio Vicálvaro", EcoLatLng(40.4011, -3.5988), "C/ San Cipriano, 81"),
                RecyclingPoint("FIJO", "Punto Limpio Villa de Vallecas", EcoLatLng(40.3700, -3.6108), "C/ Luis I, s/n"),
                RecyclingPoint("FIJO", "Punto Limpio Villaverde", EcoLatLng(40.3347, -3.7011), "C/ Resina, s/n")
            )
        }

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
                            "LATITUD" -> currentLat = safeNextText(parser).replace(",", ".").toDoubleOrNull()
                            "LONGITUD" -> currentLng = safeNextText(parser).replace(",", ".").toDoubleOrNull()
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
