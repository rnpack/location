package com.rnpack.location.data

import com.facebook.react.bridge.ReadableMap
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.Priority

data class LocationConfiguration(
  val priority: Int,
  val intervalMillis: Long,
  val minUpdateIntervalMillis: Long,
  val waitForAccurateLocation: Boolean,
  val minUpdateDistanceMeters: Float,
  val maxUpdateAgeMillis: Long,
  val durationMillis: Long,
  val maxUpdateDelayMillis: Long,
  val granularity: Int,
  val provider: String?
) {
  companion object {
    fun fromReadableMap(map: ReadableMap): LocationConfiguration {
      return LocationConfiguration(
        priority = if (map.hasKey("priority")) map.getDouble("priority")
          .toInt() else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        intervalMillis = if (map.hasKey("intervalMillis")) map.getDouble("intervalMillis")
          .toLong() else 30000L,
        minUpdateIntervalMillis = if (map.hasKey("minUpdateIntervalMillis")) map.getDouble("minUpdateIntervalMillis")
          .toLong() else 10000L,
        waitForAccurateLocation = if (map.hasKey("waitForAccurateLocation")) map.getBoolean("waitForAccurateLocation") else false,
        minUpdateDistanceMeters = if (map.hasKey("minUpdateDistanceMeters")) map.getDouble("minUpdateDistanceMeters")
          .toFloat() else 5f,
        maxUpdateAgeMillis = if (map.hasKey("maxUpdateAgeMillis")) map.getDouble("maxUpdateAgeMillis")
          .toLong() else 10000,
        durationMillis = if (map.hasKey("durationMillis")) map.getDouble("durationMillis")
          .toLong() else Long.MAX_VALUE,
        maxUpdateDelayMillis = if (map.hasKey("maxUpdateDelayMillis")) map.getDouble("maxUpdateDelayMillis")
          .toLong() else 2000,
        granularity = if (map.hasKey("granularity")) map.getDouble("granularity")
          .toInt() else Granularity.GRANULARITY_PERMISSION_LEVEL,
        provider = if (map.hasKey("provider")) if (LocationProvider.entries.any {
            it.value == map.getString(
              "provider"
            )
          }) map.getString("provider") else LocationProvider.FUSED.value else LocationProvider.FUSED.value
      )
    }
  }
}
