package com.rubensimon.ecolens.utils

import platform.Foundation.NSDate
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.date
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.timeIntervalSince1970

actual object TimeUtils {
    actual fun getCurrentTimestamp(): Long = (NSDate.date().timeIntervalSince1970 * 1000).toLong()
    actual fun getCurrentIsoDate(): String = NSISO8601DateFormatter().stringFromDate(NSDate.date())
    actual fun getYesterdayIsoDate(): String {
        val yesterday = NSDate.dateWithTimeIntervalSinceNow(-24.0 * 60.0 * 60.0)
        return NSISO8601DateFormatter().stringFromDate(yesterday)
    }
}
