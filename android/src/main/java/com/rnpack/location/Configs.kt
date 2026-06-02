package com.rnpack.location

import com.google.android.gms.location.Granularity
import com.google.android.gms.location.Priority
import com.rnpack.location.data.LocationConfiguration

object Configs {
  val DEFAULT_LOCATION_CONFIGURATION: LocationConfiguration = LocationConfiguration(
    priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
    intervalMillis = 5000,
    minUpdateIntervalMillis = 10000,
    waitForAccurateLocation = false,
    minUpdateDistanceMeters = 5.0f,
    maxUpdateAgeMillis = 10000,
    durationMillis = Long.MAX_VALUE,
    maxUpdateDelayMillis = 2000,
    granularity = Granularity.GRANULARITY_PERMISSION_LEVEL,
  )
  const val BACKGROUND_LOCATION_CONFIG_SHARED_PREFERENCES_DATABASE = "BackgroundLocationPrefs"
}
