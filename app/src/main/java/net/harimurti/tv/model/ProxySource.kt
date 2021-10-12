package net.harimurti.tv.model

import com.google.gson.annotations.SerializedName

class ProxySource {
    @SerializedName("list")
    var proxysource: ArrayList<ListUrl> = ArrayList()
    @SerializedName("country")
    var country: ArrayList<Country> = ArrayList()

    class ListUrl {
        var path: String = ""
        var type: String = ""
    }

    class Country {
        var name: String? = null
        var code: String? = null
    }
}