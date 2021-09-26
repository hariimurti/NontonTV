package net.harimurti.tv.extra

import net.harimurti.tv.extension.findPattern
import net.harimurti.tv.extension.isStreamUrl
import net.harimurti.tv.extension.normalize
import net.harimurti.tv.extension.toCRC32
import net.harimurti.tv.model.*

class M3uTool {
    fun parse(content: String?): Playlist {
        val result = Playlist()
        var chRaw = ChannelRaw()
        var chReset = true
        val lines = content?.lines() ?: throw Exception("Empty Content")
        lines.forEach {
            if (it.isBlank()) return@forEach
            if (it.startsWith("#EXTVLCOPT")) {
                if (it.contains("user-agent"))
                    chRaw.userAgent = it.findPattern(".*http-user-agent=(.+?)\$")
                if (it.contains("referrer"))
                    chRaw.referer = it.findPattern(".*http-referrer=(.+?)\$")
                chReset = false
                return@forEach
            }
            if (it.startsWith("#EXTGRP")) {
                chRaw.group = it.findPattern(".*:(.+?)\$")
                chReset = false
                return@forEach
            }
            if (it.startsWith("#KODIPROP")) {
                if (it.contains("license_type"))
                    chRaw.drmType = it.findPattern(".*license_type=(.+?)\$")
                if (it.contains("license_key"))
                    chRaw.drmKey = it.findPattern(".*license_key=(.+?)\$")
                chReset = false
                return@forEach
            }
            if (it.startsWith("#EXTINF")) {
                if (chReset && !chRaw.name.isNullOrBlank()) chRaw = ChannelRaw()

                chRaw.name = it.findPattern(".*,(.+?)\$")
                chRaw.group = it.findPattern(".*group-title=\"(.*?)\".*") ?: chRaw.group
                chRaw.logoUrl = it.findPattern(".*tvg-logo=\"(.*?)\".*")

                if (chRaw.name.isNullOrBlank()) chRaw.name = "NO NAME"
                if (chRaw.group.isNullOrBlank()) chRaw.group = "UNCATAGORIZED"

                chReset = true
                return@forEach
            }
            if (it.isStreamUrl()) {
                chReset = true
                chRaw.streamUrl = it.trim()
                if (!chRaw.userAgent.isNullOrBlank())
                    chRaw.streamUrl += "|user-agent=${chRaw.userAgent}"
                if (!chRaw.referer.isNullOrBlank())
                    chRaw.streamUrl += "|referer=${chRaw.referer}"

                val drmId = chRaw.drmKey?.toCRC32()
                val drmIsExist = result.drmLicenses.firstOrNull { d -> d.id == drmId } != null
                if (drmId != null && !drmIsExist) {
                    result.drmLicenses.add(
                        DrmLicense().apply {
                            id = drmId
                            key = chRaw.drmKey.toString()
                            type = chRaw.drmType.toString()
                        }
                    )
                }
                val channel = Channel().apply {
                    name = chRaw.name.normalize()
                    logoUrl = chRaw.logoUrl
                    streamUrl = chRaw.streamUrl
                    this.drmId = drmId
                }
                val catName = chRaw.group.normalize()
                val category = result.categories.firstOrNull { c -> c.name == catName }
                if (category == null) {
                    result.categories.add(
                        Category().apply {
                            name = catName
                            channels = arrayListOf(channel)
                        }
                    )
                }
                else {
                    val lastIndex = category.channels?.indexOfLast { c -> c.name == chRaw.name } ?: -1
                    if (lastIndex >= 0) {
                        channel.name = "${chRaw.name} #${lastIndex + 1}"
                    }
                    category.channels?.add(channel)
                }
            }
        }
        return result
    }
}