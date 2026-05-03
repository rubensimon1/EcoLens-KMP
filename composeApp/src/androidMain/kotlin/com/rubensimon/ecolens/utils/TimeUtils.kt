package com.rubensimon.ecolens.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual object TimeUtils {
    actual fun getCurrentTimestamp(): Long = System.currentTimeMillis()
    actual fun getCurrentIsoDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    actual fun getYesterdayIsoDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        return sdf.format(yesterday)
    }
}
