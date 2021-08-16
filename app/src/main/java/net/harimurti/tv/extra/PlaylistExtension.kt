package net.harimurti.tv.extra

import com.google.gson.Gson
import com.google.gson.JsonParseException
import net.harimurti.tv.model.*

fun List<M3U>?.toPlaylist(): Playlist? {
    if (this == null) return null
    val playlist = Playlist()

    val hashMap= HashMap<String, ArrayList<Channel>>()
    var map: ArrayList<Channel>?

    val hashSet = HashSet<DrmLicense>()
    val drms = ArrayList<DrmLicense>()
    val cats= ArrayList<Category>()

    var category: Category?
    var ch: Channel?
    var drm: DrmLicense?

    for (item in this) {
        //hashset drm (disable same value)
        if(!item.licenseKey.isNullOrEmpty()) {
            drm = DrmLicense()
            drm.name = item.licenseName
            drm.url = item.licenseKey
            hashSet.add(drm)
        }

        //hashmap (map same groupname as key)
        map = hashMap[item.groupName]
        //set map value as channel
        ch = Channel()
        if (map != null) {
            ch.name = item.channelName
            ch.streamUrl = item.streamUrl
            ch.drmName = item.licenseName
            map.add(ch)
        }else {
            map = ArrayList()
            ch.name = item.channelName
            ch.streamUrl = item.streamUrl
            ch.drmName = item.licenseName
            map.add(ch)
        }
        hashMap[item.groupName.toString()] = map
    }

    //set map as categories
    for(entry in hashMap){
        category = Category()
        category.name = entry.key
        category.channels = entry.value
        cats.add(category)
    }
    playlist.categories = cats

    //set drm licenses
    drms.addAll(hashSet)
    playlist.drmLicenses = drms

    return playlist
}

fun String?.toPlaylist(): Playlist? {
    var content = this

    // trying to fix char encoding
    try { content = this?.toByteArray(Charsets.ISO_8859_1)?.let { String(it, Charsets.UTF_8) } }
    catch (e: Exception) { e.printStackTrace() }

    // trying to parse json first
    try { return Gson().fromJson(content, Playlist::class.java) }
    catch (e: JsonParseException) { e.printStackTrace() }

    // if not json then m3u
    try { return M3uTool.parse(content).toPlaylist() }
    catch (e: Exception) { e.printStackTrace() }

    // content cant be parsed
    return null
}