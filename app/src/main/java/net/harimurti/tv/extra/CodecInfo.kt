package net.harimurti.tv.extra

import android.media.MediaCodecList

@Suppress("DEPRECATION")
class CodecInfo {
    companion object {
        // mimetype contains video, audio
        fun getDecoder(mimetype: String): String {
            val video = ArrayList<String>()
            val audio = ArrayList<String>()
            for (i in 0 until MediaCodecList.getCodecCount()) {
                val codecInfo = MediaCodecList.getCodecInfoAt(i)
                if (!codecInfo.isEncoder) continue
                for (j in codecInfo.supportedTypes) {
                    if (j.contains("video")) video.add(j.substringAfter("video/"))
                    if (j.contains("audio")) audio.add(j.substringAfter("audio/"))
                }
            }
            val listVideo = video.distinct().joinToString(", ")
            val listAudio = audio.distinct().joinToString(", ")
            return when {
                mimetype.equals("video", true) -> "Supported Video : ${listVideo}."
                mimetype.equals("audio", true) -> "Supported Audio : ${listAudio}."
                else -> "Supported Codec :\nVideo : ${listVideo}.\nAudio : ${listAudio}."
            }
        }
    }
}