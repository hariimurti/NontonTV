package net.harimurti.tv.model

import android.os.Parcel
import android.os.Parcelable

class Channel private constructor(`in`: Parcel) : Parcelable {
    var name: String = `in`.readString().toString()
    var stream_url: String = `in`.readString().toString()
    var drm_name: String = `in`.readString().toString()

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(name)
        parcel.writeString(stream_url)
        parcel.writeString(drm_name)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<Channel> = object : Parcelable.Creator<Channel> {
            override fun createFromParcel(`in`: Parcel): Channel {
                return Channel(`in`)
            }

            override fun newArray(size: Int): Array<Channel?> {
                return arrayOfNulls(size)
            }
        }
    }
}