package com.rubensimon.ecolens.utils

import android.content.Context
import android.content.Intent

actual object PlatformShare {
    private var appContext: Context? = null

    fun setContext(context: Context) {
        appContext = context
    }

    actual fun shareText(title: String, text: String) {
        val context = appContext ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
