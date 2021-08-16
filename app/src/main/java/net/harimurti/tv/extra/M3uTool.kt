package net.harimurti.tv.extra

import net.harimurti.tv.model.M3U
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.math.BigInteger
import java.security.MessageDigest
import java.util.regex.Matcher
import java.util.regex.Pattern

class M3uTool {
    companion object {
        private val REGEX_GROUP: Pattern =
            Pattern.compile(".*group-title=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE)
        private val REGEX_NAME: Pattern =
            Pattern.compile(".*,(.+?)$", Pattern.CASE_INSENSITIVE)
        private val REGEX_KODI: Pattern =
            Pattern.compile(".*license_key=(.+?)$", Pattern.CASE_INSENSITIVE)
        private val REGEX_GRP: Pattern =
            Pattern.compile(".*:(.+?)$", Pattern.CASE_INSENSITIVE)

        fun parse(content: String?): List<M3U> {
            val result: MutableList<M3U> = ArrayList()
            var lineNumber = 0
            var line: String?
            try {
                val buffer = BufferedReader(StringReader(content))
                line = buffer.readLine()
                if (line == null) {
                    throw M3U.ParsingException(0, "Empty stream")
                }

                lineNumber++

                var m3u= M3U()
                while (buffer.readLine().also { line = it } != null) {
                    lineNumber++
                    when {
                        isExtGrp(line) -> {
                            // reset if GroupName is set
                            if(!m3u.groupName.isNullOrEmpty())
                                m3u= M3U()

                            // set group name
                            m3u.groupName = regexGrp(line)
                        }
                        isExtInf(line) -> {
                            // reset if ChannelName is set
                            if(!m3u.channelName.isNullOrEmpty())
                                m3u= M3U()

                            // set channel name
                            m3u.channelName = regexCh(line)

                            // set group name
                            m3u.groupName = regexTitle(line)
                        }
                        isKodi(line) -> {
                            // set drm license
                            m3u.licenseKey = regexKodi(line)
                            m3u.licenseName = md5(regexKodi(line).toString())
                        }
                        isStream(line) -> {
                            if(m3u.channelName.isNullOrEmpty() || m3u.channelName!!.startsWith(M3U.EXTINF))
                                m3u.channelName = "NO NAME"
                            if(m3u.groupName.isNullOrBlank())
                                m3u.groupName = "UNCATAGORIZED"

                            // add channel
                            m3u.streamUrl = line
                            result.add(m3u)

                            // reset channel
                            m3u= M3U()
                        }
                    }
                }
            } catch (e: IOException) {
                throw M3U.ParsingException(lineNumber, "Cannot read file", e)
            }
            return result
        }

        private fun isExtGrp(line: String?): Boolean {
            return line!!.startsWith(M3U.EXTGRP)
        }

        private fun isExtInf(line: String?): Boolean {
            return line!!.startsWith(M3U.EXTINF)
        }

        private fun isKodi(line: String?): Boolean {
            return line!!.startsWith(M3U.KODIPROP) && line.contains("license_key")
        }

        private fun isStream(line: String?): Boolean {
            return line!!.lowercase().startsWith("http")
        }

        private fun regexCh(line: String?): String? {
            return regexLine(line, REGEX_NAME)
        }

        private fun regexTitle(line: String?): String? {
            return regexLine(line, REGEX_GROUP)
        }

        private fun regexKodi(line: String?): String? {
            return regexLine(line, REGEX_KODI)
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

        private fun md5(input:String): String {
            val md = MessageDigest.getInstance("MD5")
            return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
        }
    }
}