package net.harimurti.tv.model

import android.os.Parcel
import android.os.Parcelable

open class License protected constructor(`in`: Parcel) : Parcelable {
    var domain: String = `in`.readString().toString()
    var drm_url: String = `in`.readString().toString()

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(domain)
        dest.writeString(drm_url)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<License?> = object : Parcelable.Creator<License?> {
            override fun createFromParcel(`in`: Parcel): License {
                return License(`in`)
            }

            override fun newArray(size: Int): Array<License?> {
                return arrayOfNulls(size)
            }
        }
    }

}