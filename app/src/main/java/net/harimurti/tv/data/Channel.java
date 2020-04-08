package net.harimurti.tv.data;

import android.os.Parcel;
import android.os.Parcelable;

public class Channel implements Parcelable {
    public int cid;
    public String name;
    public String stream_url;

    private Channel(Parcel in) {
        cid = in.readInt();
        name = in.readString();
        stream_url = in.readString();
    }

    public static final Creator<Channel> CREATOR = new Creator<Channel>() {
        @Override
        public Channel createFromParcel(Parcel in) {
            return new Channel(in);
        }

        @Override
        public Channel[] newArray(int size) {
            return new Channel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(cid);
        parcel.writeString(name);
        parcel.writeString(stream_url);
    }
}

