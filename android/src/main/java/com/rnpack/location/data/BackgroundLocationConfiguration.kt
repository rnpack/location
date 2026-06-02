package com.rnpack.location.data

import com.facebook.react.bridge.ReadableMap
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.Priority
import org.json.JSONObject

data class BackgroundLocationConfiguration(
  val url: String?,
  val priority: Int,
  val maxUpdateAgeMillis: Long,
  val granularity: Int,
  val durationMillis: Long,
) {
  companion object {
    fun fromReadableMap(map: ReadableMap): BackgroundLocationConfiguration {

      return BackgroundLocationConfiguration(
        url = if (map.hasKey("url")) map.getString("url") else null,
        priority = if (map.hasKey("priority")) map.getDouble("priority")
          .toInt() else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        maxUpdateAgeMillis = if (map.hasKey("maxUpdateAgeMillis")) map.getDouble("maxUpdateAgeMillis")
          .toLong() else 10000,
        durationMillis = if (map.hasKey("durationMillis")) map.getDouble("durationMillis")
          .toLong() else Long.MAX_VALUE,
        granularity = if (map.hasKey("granularity")) map.getDouble("granularity")
          .toInt() else Granularity.GRANULARITY_PERMISSION_LEVEL,
      )
    }

    fun fromJson(jsonString: String): BackgroundLocationConfiguration {
      val jsonObject = JSONObject(jsonString);

      return BackgroundLocationConfiguration(
        url = if (jsonObject.has("url") && !jsonObject.isNull("url")) jsonObject.getString("url") else null,
        priority = if (jsonObject.has("priority") && !jsonObject.isNull("priority")) jsonObject.getDouble("priority")
          .toInt() else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        maxUpdateAgeMillis = if (jsonObject.has("maxUpdateAgeMillis") && !jsonObject.isNull("maxUpdateAgeMillis")) jsonObject.getDouble("maxUpdateAgeMillis")
          .toLong() else 10000,
        durationMillis = if (jsonObject.has("durationMillis") && !jsonObject.isNull("durationMillis")) jsonObject.getDouble("durationMillis")
          .toLong() else Long.MAX_VALUE,
        granularity = if (jsonObject.has("granularity") && !jsonObject.isNull("granularity")) jsonObject.getDouble("granularity")
          .toInt() else Granularity.GRANULARITY_PERMISSION_LEVEL,
      )
    }
  }
}
