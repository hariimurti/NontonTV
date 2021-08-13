package net.harimurti.tv.m3u

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.RuntimeException
import java.util.regex.Matcher
import java.util.regex.Pattern

class M3uParser {

    companion object {
        private const val EXTM3U = "#EXTM3U"
        private const val KODIPROP = "#KODIPROP:"
        private const val EXTINF = "#EXTINF:"
        private const val EXTGRP = "#EXTGRP:"
        private const val COMMENT = "#"
        private const val COMMENT2 = "//"
        private const val EMPTY = ""
        private val REGEX_TITLE: Pattern =
            Pattern.compile(".*group-title=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE)
        private val REGEX_NAME: Pattern =
            Pattern.compile(".*,(.+?)$", Pattern.CASE_INSENSITIVE)
        private val REGEX_KODI: Pattern =
            Pattern.compile(".*license_key=(.+?)$", Pattern.CASE_INSENSITIVE)
        private val REGEX_KODI_NAME: Pattern =
            Pattern.compile(".*://(\\w+).*", Pattern.CASE_INSENSITIVE)
        private val REGEX_GRP: Pattern =
            Pattern.compile(".*:(.+?)$", Pattern.CASE_INSENSITIVE)
    }

    fun parse(stream: InputStream?): List<M3uItem> {
        if (stream == null) {
            throw ParsingException(0, "Cannot read stream")
        }
        val entries: MutableList<M3uItem> = ArrayList()
        var lineNumber = 0
        var line: String?
        try {
            val buffer = BufferedReader(InputStreamReader(stream))
            line = buffer.readLine()
            if (line == null) {
                throw ParsingException(0, "Empty stream")
            }
            lineNumber++
            checkStart(line)
            var entry: M3uItem? = null
            var kodi: String? = null
            var kodiName: String? = null
            var grp: String?
            while (buffer.readLine().also { line = it } != null) {
                lineNumber++
                when {
                    isEmpty(line) -> {
                        // Do nothing.
                    }
                    isKodi(line) -> {
                        kodi = regexKodi(line)
                        if(!kodi.isNullOrEmpty()) {
                            kodiName = regexKodiName(kodi)
                        }
                    }
                    isExtInf(line) -> {
                        entry = regexExtInf(line)
                        if(!kodi.isNullOrEmpty())
                            entry = entry.apply {
                                this.drmURL = kodi
                                this.drmName = kodiName }
                        kodi = null
                        kodiName = null
                    }
                    isExtGrp(line) -> {
                        grp = regexGrp(line)
                        if(!grp.isNullOrEmpty())
                            entry = entry.apply {
                                this!!.category = grp }
                        grp = null
                    }
                    isComment(line) -> {
                        // Do nothing.
                    }
                    else -> {
                        if (entry == null)
                            throw ParsingException(lineNumber, "Missing $EXTINF")

                        entries.add(entry.apply {
                            streamURL = line
                        })
                    }
                }
            }
        } catch (e: IOException) {
            throw ParsingException(lineNumber, "Cannot read file", e)
        }
        return entries
    }

    private fun checkStart(line: String?) {
        if (line != null) {
            if (!line.startsWith(EXTM3U)) {
                throw ParsingException(1, "First line should be $EXTM3U")
            }
        }
    }

    private fun isKodi(line: String?): Boolean {
        return line!!.startsWith(KODIPROP)
    }

    private fun isExtInf(line: String?): Boolean {
        return line!!.startsWith(EXTINF)
    }

    private fun isExtGrp(line: String?): Boolean {
        return line!!.startsWith(EXTGRP)
    }

    private fun isEmpty(line: String?): Boolean {
        return line!! == EMPTY
    }

    private fun isComment(line: String?): Boolean {
        return line!!.startsWith(COMMENT) || line.startsWith(COMMENT2)
    }

    private fun regexExtInf(line: String?): M3uItem {
        val category = regexLine(line, REGEX_TITLE)
        val channelName = regexLine(line, REGEX_NAME)
        val m3UItem = M3uItem()
        m3UItem.channelName = channelName
        m3UItem.category = category
        return m3UItem
    }

    private fun regexKodi(line: String?): String? {
        return regexLine(line, REGEX_KODI)
    }

    private fun regexKodiName(line: String?): String? {
        return regexLine(line, REGEX_KODI_NAME)
    }

    private fun regexGrp(line: String?): String? {
        return regexLine(line, REGEX_GRP)
    }

    private fun regexLine(line: String?, Pattern: Pattern): String? {
        val matcher: Matcher = Pattern.matcher(line.toString())
        return if (matcher.matches()) {
            matcher.group(1)
        } else null
    }

    class ParsingException : RuntimeException {
        var line: Int

        constructor(line: Int, message: String) : super("$message at line $line") {
            this.line = line
        }

        constructor(line: Int, message: String, cause: Exception?) : super("$message at line $line",cause) {
            this.line = line
        }
    }
}