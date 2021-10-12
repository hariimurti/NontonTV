package net.harimurti.tv.extra

import com.google.gson.Gson
import com.google.gson.JsonArray
import net.harimurti.tv.extension.*
import net.harimurti.tv.model.*
import java.util.*

class ProxyTool {
    fun parse(content: String?,type: String): ProxyList {
        val result = ProxyList()
        var proxyData: ProxyData
        val pattern = "^.*?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d+)(\$| (\\w*) (\\D*?) \\[(.*?)\\]\$)"
        val lines = content?.lines() ?: throw Exception("Empty Content")
        lines.forEach {
            if (it.isBlank()) return@forEach
            if (it.isProxy(pattern)) {
                proxyData = ProxyData()
                proxyData.type = type
                proxyData.ip = it.findPatternGroup(pattern,1)
                proxyData.port = it.findPatternGroup(pattern,2)
                proxyData.response = it.findPatternGroup(pattern,4) ?: ""
                proxyData.country = it.findPatternGroup(pattern,5) ?: "UN"
                proxyData.isp = it.findPatternGroup(pattern,6) ?: ""
                result.proxies.add(proxyData)
            }
        }
        return result
    }

    fun parse2(content: String?): ProxyList {
        val result = ProxyList()
        var proxyData: ProxyData
        val lines = content?.lines() ?: throw Exception("Empty Content")
        val json = StringBuilder()
        lines.forEach {
            if (it.isBlank()) return@forEach
            json.append(it)
        }
        var jsonRes = "[" + json.toString().replace("}", "},") + "]"
        jsonRes = jsonRes.replace(",]","]")

        val jsonArray = Gson().fromJson(jsonRes, JsonArray::class.java)

        for (jsonObject in jsonArray) {
            val it = jsonObject.asJsonObject
            proxyData = ProxyData()
            val type = when(it.get("type").asString){
                "http" -> "HTTP"
                "https" -> "HTTP"
                else -> "SOCKS"
            }
            proxyData.type = type
            proxyData.ip = it.get("host").asString
            proxyData.port = it.get("port").asString
            proxyData.response = it.get("response_time").asString
            proxyData.country = it.get("country").asString
            proxyData.isp = ""
            result.proxies.add(proxyData)
        }
        return result
    }

    private fun String?.isProxy(pattern: String): Boolean {
        if (this == null) return false
        return Regex(pattern)
            .matches(this)
    }

    private fun String?.findPatternGroup(pattern: String, group: Int): String? {
        if (this == null) return null
        val result = Regex(pattern, RegexOption.IGNORE_CASE).matchEntire(this)
        return if(result?.groups?.get(group) != null){
            result.groups[group]?.value
        } else null
    }

}