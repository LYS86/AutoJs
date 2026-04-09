package com.stardust.autojs.shizuku

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson

data class Result(
    val code: Int,
    val error: String = "",
    val result: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(code)
        parcel.writeString(error)
        parcel.writeString(result)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Result> {
        override fun createFromParcel(parcel: Parcel): Result = Result(parcel)
        override fun newArray(size: Int): Array<Result?> = arrayOfNulls(size)

        fun fromJson(json: String): Result {
            return Gson().fromJson(json, Result::class.java)
        }
    }
}
