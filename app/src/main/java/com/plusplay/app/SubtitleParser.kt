package com.plusplay.app

data class SubtitleEntry(
    val startTime: Long,
    val endTime: Long,
    val text: String
)

/** Reads .srt subtitle files. */
object SubtitleParser {
    fun parse(content: String): List<SubtitleEntry> {
        val entries = mutableListOf<SubtitleEntry>()
        // Strip a UTF-8 byte order mark, otherwise the first cue is silently dropped
        val lines = content.removePrefix("﻿").replace("\r", "").split("\n")
        var i = 0

        while (i < lines.size) {
            // Skip subtitle number
            if (lines[i].trim().toIntOrNull() != null) {
                i++
                if (i >= lines.size) break

                // Parse time
                val times = lines[i].trim().split("-->")
                if (times.size == 2) {
                    val startTime = parseTime(times[0].trim())
                    val endTime = parseTime(times[1].trim())
                    i++

                    // Parse text
                    val textLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().isNotEmpty()) {
                        textLines.add(lines[i].trim())
                        i++
                    }

                    if (textLines.isNotEmpty()) {
                        entries.add(SubtitleEntry(startTime, endTime, textLines.joinToString("\n")))
                    }
                }
            }
            i++
        }
        return entries
    }

    fun parseTime(timeString: String): Long {
        // Format: 00:00:00,000 or 00:00:00.000
        val parts = timeString.replace(",", ".").split(":")
        if (parts.size == 3) {
            val hours = parts[0].toLongOrNull() ?: 0
            val minutes = parts[1].toLongOrNull() ?: 0
            val secondsParts = parts[2].split(".")
            val seconds = secondsParts[0].toLongOrNull() ?: 0
            val millis = if (secondsParts.size > 1) secondsParts[1].toLongOrNull() ?: 0 else 0
            return hours * 3600000 + minutes * 60000 + seconds * 1000 + millis
        }
        return 0
    }
}
