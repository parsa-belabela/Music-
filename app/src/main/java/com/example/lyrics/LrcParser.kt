package com.example.lyrics

import com.example.data.model.LyricsLine
import com.example.data.model.WordTimestamp

object LrcParser {
    private val TIME_TAG_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{2,3}))?]""")
    private val WORD_TAG_REGEX = Regex("""<(\d{1,2}):(\d{2})(?:[.:](\d{2,3}))?>([^<]*)""")

    fun parse(lrcContent: String, offsetMs: Long = 0L): List<LyricsLine> {
        if (lrcContent.isBlank()) return emptyList()

        val lines = mutableListOf<LyricsLine>()
        val rawLines = lrcContent.lines()

        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Check if metadata line like [ar:Artist] or [ti:Title]
            if (trimmed.startsWith("[ti:") || trimmed.startsWith("[ar:") ||
                trimmed.startsWith("[al:") || trimmed.startsWith("[by:") ||
                trimmed.startsWith("[offset:")
            ) {
                continue
            }

            val matches = TIME_TAG_REGEX.findAll(trimmed).toList()
            if (matches.isEmpty()) {
                // If it doesn't have a time tag, we can keep it as an unsynced line or append
                continue
            }

            val lineText = trimmed.replace(TIME_TAG_REGEX, "").trim()

            // Parse word-level karaoke tags if present: e.g. <00:12.34>Hello <00:13.10>World
            val words = mutableListOf<WordTimestamp>()
            val wordMatches = WORD_TAG_REGEX.findAll(lineText).toList()
            if (wordMatches.isNotEmpty()) {
                for (i in wordMatches.indices) {
                    val wMatch = wordMatches[i]
                    val start = parseTimestamp(wMatch.groupValues[1], wMatch.groupValues[2], wMatch.groupValues[3]) + offsetMs
                    val wordStr = wMatch.groupValues[4]
                    val end = if (i + 1 < wordMatches.size) {
                        val nextMatch = wordMatches[i + 1]
                        parseTimestamp(nextMatch.groupValues[1], nextMatch.groupValues[2], nextMatch.groupValues[3]) + offsetMs
                    } else {
                        start + 800L
                    }
                    words.add(WordTimestamp(word = wordStr, startMs = start, endMs = end))
                }
            }

            val cleanedText = if (words.isNotEmpty()) {
                words.joinToString("") { it.word }.trim()
            } else {
                lineText
            }

            for (match in matches) {
                val min = match.groupValues[1]
                val sec = match.groupValues[2]
                val msStr = match.groupValues[3]
                val timestamp = (parseTimestamp(min, sec, msStr) + offsetMs).coerceAtLeast(0L)
                lines.add(LyricsLine(timestampMs = timestamp, text = cleanedText, words = words))
            }
        }

        return lines.sortedBy { it.timestampMs }
    }

    private fun parseTimestamp(minStr: String, secStr: String, msStr: String?): Long {
        val minutes = minStr.toLongOrNull() ?: 0L
        val seconds = secStr.toLongOrNull() ?: 0L
        val millis = when (msStr?.length) {
            2 -> (msStr.toLongOrNull() ?: 0L) * 10
            3 -> msStr.toLongOrNull() ?: 0L
            1 -> (msStr.toLongOrNull() ?: 0L) * 100
            else -> 0L
        }
        return minutes * 60_000L + seconds * 1000L + millis
    }

    fun exportToLrc(lines: List<LyricsLine>, trackTitle: String? = null, artist: String? = null): String {
        val sb = StringBuilder()
        if (!trackTitle.isNullOrBlank()) sb.append("[ti:$trackTitle]\n")
        if (!artist.isNullOrBlank()) sb.append("[ar:$artist]\n")

        for (line in lines.sortedBy { it.timestampMs }) {
            val totalSec = line.timestampMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val cs = (line.timestampMs % 1000) / 10 // hundredths of a second
            val timeTag = "[%02d:%02d.%02d]".format(min, sec, cs)

            if (line.words.isNotEmpty()) {
                val wordsStr = line.words.joinToString("") { w ->
                    val wTotalSec = w.startMs / 1000
                    val wMin = wTotalSec / 60
                    val wSec = wTotalSec % 60
                    val wCs = (w.startMs % 1000) / 10
                    "<%02d:%02d.%02d>%s".format(wMin, wSec, wCs, w.word)
                }
                sb.append("$timeTag$wordsStr\n")
            } else {
                sb.append("$timeTag${line.text}\n")
            }
        }
        return sb.toString()
    }
}
