package com.ruirui.orbit.android.ui.map

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

fun Long.toTimeString(): String = timeFormat.format(Date(this))