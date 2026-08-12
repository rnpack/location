package com.rnpack.location.data

import android.location.LocationManager

enum class LocationProvider(val value: String) {
  GPS(LocationManager.GPS_PROVIDER),
  NETWORK(LocationManager.NETWORK_PROVIDER),
  PASSIVE(LocationManager.PASSIVE_PROVIDER),
  FUSED(LocationManager.FUSED_PROVIDER)
}

