package com.rubensimon.ecolens.utils

import platform.UserNotifications.*
import platform.Foundation.NSDate
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_async

actual object NotificationHelper {
    
    actual fun showNotification(title: String, message: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, error ->
            if (granted) {
                dispatch_async(dispatch_get_main_queue()) {
                    val content = UNMutableNotificationContent().apply {
                        setTitle(title)
                        setBody(message)
                        setSound(UNNotificationSound.defaultSound())
                    }

                    val timestamp = NSDate.date().timeIntervalSince1970.toLong()
                    val request = UNNotificationRequest.requestWithIdentifier(
                        identifier = "ecolens_alert_$timestamp",
                        content = content,
                        trigger = null // Inmediato
                    )

                    center.addNotificationRequest(request) { error ->
                        if (error != null) {
                            println("[NotificationHelper] Error iOS: ${error.localizedDescription}")
                        }
                    }
                }
            }
        }
    }
}
