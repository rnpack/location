package com.rnpack.location.data

import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeMap

data class LocationAccessPermissionResult(
  val status: Boolean,
  val coarse: Boolean,
  val fine: Boolean,
  val iosStatus: String? = "NotDetermined"
) {
  fun toWritableMap(): WritableNativeMap {
    return WritableNativeMap().apply {
      putBoolean("status", status)
      putBoolean("coarse", coarse)
      putBoolean("fine", fine)
      putString("ios", iosStatus)
    }
  }

  companion object {
    fun fromReadableMap(map: ReadableMap): LocationAccessPermissionResult {
      return LocationAccessPermissionResult(
        status = if (map.hasKey("status")) map.getBoolean("status") else false,
        coarse = if (map.hasKey("coarse")) map.getBoolean("coarse") else false,
        fine = if (map.hasKey("fine")) map.getBoolean("fine") else false,
        iosStatus = if (map.hasKey("ios")) map.getString("ios")
          ?: "NotDetermined" else "NotDetermined"
      )
    }
  }
}
