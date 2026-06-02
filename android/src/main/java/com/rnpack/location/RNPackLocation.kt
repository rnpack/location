package com.rnpack.location

import android.content.BroadcastReceiver
import android.location.Location
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.PermissionAwareActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.rnpack.location.data.LocationAccessPermissionResult
import com.rnpack.location.data.LocationConfiguration

interface RNPackLocation {
  fun isLocationEnabledApi28(reactApplicationContext: ReactApplicationContext): Boolean
  fun isLocationEnabledApi27(reactApplicationContext: ReactApplicationContext): Boolean
  fun isLocationEnabled(reactApplicationContext: ReactApplicationContext): Boolean
  fun isLocationAuthorizedCoarse(reactApplicationContext: ReactApplicationContext): Boolean
  fun isLocationAuthorizedFine(reactApplicationContext: ReactApplicationContext): Boolean
  fun isLocationAuthorized(reactApplicationContext: ReactApplicationContext): LocationAccessPermissionResult
  fun getLastLocation(
    reactApplicationContext: ReactApplicationContext,
    onSuccess: (Location) -> Unit,
    onError: (Exception) -> Unit,
  )

  fun getFreshCurrentLocation(
    reactApplicationContext: ReactApplicationContext,
    priority: Int,
    onSuccess: (Location) -> Unit,
    onError: (Exception) -> Unit,
  )

  fun getCurrentLocation(
    reactApplicationContext: ReactApplicationContext,
    priority: Int,
    locationConfig: LocationConfiguration,
    onSuccess: (Location) -> Unit,
    onError: (Exception) -> Unit,
  )

  fun cancelLocationRequest()


  fun subscribeToLocationProvidersChange(
    reactApplicationContext: ReactApplicationContext,
    locationProvidersChangeReceiver: BroadcastReceiver
  )

  fun unsubscribeFromLocationProvidersChange(
    reactApplicationContext: ReactApplicationContext,
    locationProvidersChangeReceiver: BroadcastReceiver
  )

  fun subscribeToLocationChange(
    reactApplicationContext: ReactApplicationContext,
    locationRequest: LocationRequest,
    locationCallback: LocationCallback
  );

  fun unsubscribeFromLocationChange(
    reactApplicationContext: ReactApplicationContext,
    locationCallback: LocationCallback
  );

  fun invalidateAllSubscriptions(reactApplicationContext: ReactApplicationContext)

  fun requestAccessLocationPermission(
    permissionAwareActivity: PermissionAwareActivity,
    permissions: Array<String>,
    permissionRequestCode: Int
  ): Unit

  fun requestBackgroundLocationPermission(
    reactApplicationContext: ReactApplicationContext,
    permissionAwareActivity: PermissionAwareActivity,
    permissions: Array<String>,
    permissionRequestCode: Int
  ): Unit
}
