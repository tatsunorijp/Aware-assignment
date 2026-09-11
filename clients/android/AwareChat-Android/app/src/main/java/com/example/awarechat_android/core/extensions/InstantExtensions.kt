package com.example.awarechat_android.core.extensions

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Returns a localized hour-and-minute representation for message metadata. */
fun Instant.toMessageTime(
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String = DateTimeFormatter
    .ofLocalizedTime(FormatStyle.SHORT)
    .withLocale(locale)
    .withZone(zoneId)
    .format(this)
