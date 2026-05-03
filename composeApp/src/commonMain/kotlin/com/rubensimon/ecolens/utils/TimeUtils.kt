package com.rubensimon.ecolens.utils

import kotlinx.datetime.*

expect object TimeUtils {
    fun getCurrentTimestamp(): Long
    fun getCurrentIsoDate(): String
    fun getYesterdayIsoDate(): String
}

fun TimeUtils.getTimeAgo(isoDate: String?): String {
    if (isoDate == null) return "Recientemente"
    return try {
        val instant = Instant.parse(isoDate.substringBefore("+").let { if (it.endsWith("Z")) it else it + "Z" })
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        
        val dateEvent = instant.toLocalDateTime(tz).date
        val dateNow = now.toLocalDateTime(tz).date
        
        val daysDiff = dateNow.toEpochDays() - dateEvent.toEpochDays()
        
        when {
            daysDiff <= 0 -> {
                val duration = now - instant
                val hours = duration.toComponents { _, hours, _, _, _ -> hours }
                val minutes = duration.toComponents { _, _, minutes, _, _ -> minutes }
                when {
                    hours >= 1 -> "Hace ${hours}h"
                    minutes >= 1 -> "Hace ${minutes} min"
                    else -> "Ahora mismo"
                }
            }
            daysDiff == 1 -> "Ayer"
            daysDiff == 2 -> "Antes de ayer"
            else -> "Hace $daysDiff días"
        }
    } catch (e: Exception) {
        "Recientemente"
    }
}
