package net.harimurti.tv.m3u

import android.util.Log
import net.harimurti.tv.model.Playlist
import java.io.InputStream

class M3uTool {

    fun load(stream: InputStream?): Playlist {
        val playlist = Playlist()
        val lists = M3uParser().parse(stream)
        Log.e("readM3u", lists.toString())
        //val gson = Gson().toJson(lists.toString())
        //Log.e("readM3u", gson)
        /*val sortedList= HashMap<String, MutableList<M3uItem>>()
        var temp: MutableList<M3uItem>?
        for (item in lists) {
            temp = sortedList[item.category]
            if (temp != null)
                temp.add(item)
            else {
                temp = ArrayList()
                temp.add(item)
            }
            sortedList[item.category.toString()] = temp
        }*/
        return playlist
    }
}