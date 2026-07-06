package com.rnpack.location.dtos

import android.location.Location
import android.os.Build
import com.facebook.react.bridge.WritableNativeMap

data class LocationDTO(
  val latitude: Double,
  val longitude: Double,
  val altitude: Double,
  val accuracy: Double,
  val timestamp: Long,
  val isMocked: Boolean
) {

  fun toWritableMap(): WritableNativeMap {
    return WritableNativeMap().apply {
      putDouble(LocationDTO::latitude.name, latitude)
      putDouble(LocationDTO::longitude.name, longitude)
      putDouble(LocationDTO::altitude.name, altitude)
      putDouble(LocationDTO::accuracy.name, accuracy)
      putLong(LocationDTO::timestamp.name, timestamp)
      putBoolean(LocationDTO::isMocked.name, isMocked)
    }
  }

  companion object {
    fun fromAndroidLocation(location: Location): LocationDTO {
      return LocationDTO(
        latitude = location.latitude,
        longitude = location.longitude,
        altitude = location.altitude,
        accuracy = location.accuracy.toDouble(),
        timestamp = location.time,
        isMocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) location.isMock else location.isFromMockProvider
      )
    }
  }
}
