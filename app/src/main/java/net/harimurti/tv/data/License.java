package net.harimurti.tv.data;

import android.os.Parcel;
import android.os.Parcelable;

public class License implements Parcelable {
    public String domain;
    public String drm_url;

    protected License(Parcel in) {
        domain = in.readString();
        drm_url = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(domain);
        dest.writeString(drm_url);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<License> CREATOR = new Creator<License>() {
        @Override
        public License createFromParcel(Parcel in) {
            return new License(in);
        }

        @Override
        public License[] newArray(int size) {
            return new License[size];
        }
    };
}
