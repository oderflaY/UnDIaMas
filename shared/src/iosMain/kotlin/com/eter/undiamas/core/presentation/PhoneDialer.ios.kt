package com.eter.undiamas.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberPhoneDialer(): (String) -> Unit = remember {
    { rawNumber ->
        val number = sanitizePhoneNumber(rawNumber)
        if (number.isNotBlank()) {
            NSURL.URLWithString("tel://$number")?.let { url ->
                val application = UIApplication.sharedApplication
                if (application.canOpenURL(url)) {
                    application.openURL(url)
                }
            }
        }
    }
}
