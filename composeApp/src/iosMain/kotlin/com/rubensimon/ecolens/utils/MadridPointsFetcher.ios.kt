package com.rubensimon.ecolens.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import com.rubensimon.ecolens.data.models.maps.EcoLatLng
import com.rubensimon.ecolens.data.models.maps.RecyclingPoint
import platform.Foundation.*
import kotlinx.cinterop.*

private data class RawRecyclingPoint(
    val kind: String,
    val name: String,
    val position: Pair<Double, Double>,
    val snippet: String?
)

object MadridPointsFetcherIOS {

    private const val URL_PUNTOS_FIJOS_XML = "https://datos.madrid.es/egob/catalogo/200284-0-puntos-limpios.xml"
    private const val URL_PUNTOS_MOVILES_CSV = "https://datos.madrid.es/egob/catalogo/300101-1-puntos-limpios-moviles.csv"
    private const val URL_PUNTOS_PROXIMIDAD_CSV = "https://datos.madrid.es/egob/catalogo/300198-1-puntos-proximidad.csv"
    private const val URL_PUNTOS_MOVILES_24H_CSV = "https://datos.madrid.es/egob/catalogo/900026-0-puntos-limpios-residuos.csv"

    suspend fun loadAllRecyclingPoints(): List<RecyclingPoint> = withContext(Dispatchers.IO) {
        val all = mutableListOf<RawRecyclingPoint>()
        try { all += loadFixedPointsFromXml(URL_PUNTOS_FIJOS_XML) } catch (e: Exception) {}
        try { all += loadPointsFromCsv(URL_PUNTOS_MOVILES_CSV, "MOVIL") } catch (e: Exception) {}
        try { all += loadPointsFromCsv(URL_PUNTOS_PROXIMIDAD_CSV, "PROXIMIDAD") } catch (e: Exception) {}
        try { all += loadPointsFromCsv(URL_PUNTOS_MOVILES_24H_CSV, "MOVIL_24H") } catch (e: Exception) {}
        all
            .filter { it.position.first != 0.0 && it.position.second != 0.0 }
            .map { raw ->
                RecyclingPoint(
                    kind = raw.kind,
                    name = raw.name,
                    position = EcoLatLng(raw.position.first, raw.position.second),
                    snippet = raw.snippet
                )
            }
    }

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    private fun fetchContent(urlStr: String): String {
        val url = NSURL.URLWithString(urlStr) ?: return ""
        var content: String? = null
        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            content = NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, errorPtr.ptr)
        }
        return content ?: ""
    }

    private fun loadFixedPointsFromXml(url: String): List<RawRecyclingPoint> {
        val xml = fetchContent(url)
        if (xml.isBlank()) return emptyList()
        val points = mutableListOf<RawRecyclingPoint>()
        
        val contents = xml.split("<contenido>")
        for (i in 1 until contents.size) {
            val block = contents[i].substringBefore("</contenido>")
            var name = extractXmlTag(block, "NOMBRE")
            var latStr = extractXmlTag(block, "LATITUD")
            var lngStr = extractXmlTag(block, "LONGITUD")
            var horario = extractXmlTag(block, "HORARIO")
            val lat = latStr.toDoubleOrNull()
            val lng = lngStr.toDoubleOrNull()
            if (name.isNotBlank() && lat != null && lng != null) {
                points.add(RawRecyclingPoint("FIJO", name, Pair(lat, lng), horario.takeIf { it.isNotBlank() }))
            }
        }
        return points
    }

    private fun extractXmlTag(block: String, attribute: String): String {
        val search = "nombre=\"$attribute\">"
        val idx = block.indexOf(search)
        if (idx == -1) return ""
        val start = idx + search.length
        val end = block.indexOf("</atributo>", start)
        if (end == -1) return ""
        return block.substring(start, end).trim()
    }

    private fun loadPointsFromCsv(url: String, kind: String): List<RawRecyclingPoint> {
        val csv = fetchContent(url)
        val lines = csv.split("\n").map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val header = lines.first().split(";")
        val idxLat = header.indexOfFirst { it.uppercase().contains("LATITUD") }
        val idxLng = header.indexOfFirst { it.uppercase().contains("LONGITUD") }
        val idxName = header.indexOfFirst { it.uppercase().contains("NOMBRE") || it.uppercase().contains("DENOMINACION") }
        val idxAddress = header.indexOfFirst { it.uppercase().contains("DIRECCION") || it.uppercase().contains("VIA") }

        val points = mutableListOf<RawRecyclingPoint>()
        for (i in 1 until lines.size) {
            val cols = lines[i].split(";")
            val lat = cols.getOrNull(idxLat)?.replace(",", ".")?.toDoubleOrNull()
            val lng = cols.getOrNull(idxLng)?.replace(",", ".")?.toDoubleOrNull()
            if (lat == null || lng == null) continue

            val name = cols.getOrNull(idxName)?.takeIf { it.isNotBlank() } ?: "Punto $kind"
            val addr = cols.getOrNull(idxAddress)?.takeIf { it.isNotBlank() }
            points.add(RawRecyclingPoint(kind, name, Pair(lat, lng), addr))
        }
        return points
    }
}
