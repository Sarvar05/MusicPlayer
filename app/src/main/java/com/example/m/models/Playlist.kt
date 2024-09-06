package com.example.m.models

import android.os.Parcel
import android.os.Parcelable
data class Playlist(
    val name: String,
    val songs: MutableList<String> = mutableListOf()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.createStringArrayList() ?: mutableListOf()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeStringList(songs)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Playlist> {
        override fun createFromParcel(parcel: Parcel): Playlist = Playlist(parcel)
        override fun newArray(size: Int): Array<Playlist?> = arrayOfNulls(size)
    }
}