package net.harimurti.tv.m3u

import android.util.Log
import com.google.gson.Gson
import net.harimurti.tv.model.Category
import net.harimurti.tv.model.Channel
import net.harimurti.tv.model.DrmLicense
import net.harimurti.tv.model.Playlist

class M3uTool {

    fun load(filepath: String?): Playlist {
        val playlist = Playlist()

        //parsing m3u file path
        val lists = M3U().parse(filepath)

        val hashMap= HashMap<String, ArrayList<Channel>>()
        var map: ArrayList<Channel>?

        val hashSet = HashSet<DrmLicense>()
        val drms = ArrayList<DrmLicense>()
        val cats= ArrayList<Category>()

        var category: Category?
        var ch: Channel?
        var drm: DrmLicense?

        for (item in lists!!) {
            //hashset drm (disable same value)
            if(!item.licenseKey.isNullOrEmpty()) {
                drm = DrmLicense()
                drm.drm_name = item.licenseName
                drm.drm_url = item.licenseKey
                hashSet.add(drm)
            }

            //hashmap (map same groupname as key)
            map = hashMap[item.groupName]
            //set map value as channel
            ch = Channel()
            if (map != null) {
                ch.name = item.channelName
                ch.stream_url = item.streamUrl
                ch.drm_name = item.licenseName
                map.add(ch)
            }else {
                map = ArrayList()
                ch.name = item.channelName
                ch.stream_url = item.streamUrl
                ch.drm_name = item.licenseName
                map.add(ch)
            }
            hashMap[item.groupName.toString()] = map
        }

        //set map as categories
        for(list in hashMap){
            category = Category()
            category.name = list.key
            category.channels = list.value
            cats.add(category)
        }
        playlist.categories = cats

        //set drm licenses
        drms.addAll(hashSet)
        playlist.drm_licenses = drms

        //set data to json format
        val gson1 = Gson().toJson(playlist)

        return Gson().fromJson(gson1,Playlist::class.java)
    }

    fun loadUrl(filepath: String?): Playlist {
        val playlist = Playlist()

        //parsing m3u file path
        val lists = M3U().parseUrl(filepath)

        val hashMap= HashMap<String, ArrayList<Channel>>()
        var map: ArrayList<Channel>?

        val hashSet = HashSet<DrmLicense>()
        val drms = ArrayList<DrmLicense>()
        val cats= ArrayList<Category>()

        var category: Category?
        var ch: Channel?
        var drm: DrmLicense?

        for (item in lists) {
            //hashset drm (disable same value)
            if(!item.licenseKey.isNullOrEmpty()) {
                drm = DrmLicense()
                drm.drm_name = item.licenseName
                drm.drm_url = item.licenseKey
                hashSet.add(drm)
            }

            //hashmap (map same groupname as key)
            map = hashMap[item.groupName]
            //set map value as channel
            ch = Channel()
            if (map != null) {
                ch.name = item.channelName
                ch.stream_url = item.streamUrl
                ch.drm_name = item.licenseName
                map.add(ch)
            }else {
                map = ArrayList()
                ch.name = item.channelName
                ch.stream_url = item.streamUrl
                ch.drm_name = item.licenseName
                map.add(ch)
            }
            hashMap[item.groupName.toString()] = map
        }

        //set map as categories
        for(list in hashMap){
            category = Category()
            category.name = list.key
            category.channels = list.value
            cats.add(category)
        }
        playlist.categories = cats

        //set drm licenses
        drms.addAll(hashSet)
        playlist.drm_licenses = drms

        //set data to json format
        val gson1 = Gson().toJson(playlist)

        Log.d("response",gson1)
        return Gson().fromJson(gson1,Playlist::class.java)
    }
}