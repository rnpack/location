package com.rnpack.location

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.rnpack.location.data.LocationAccessPermissionResult
import com.rnpack.location.data.LocationConfiguration


class RNPackLocationImpl : RNPackLocation {

  var cancellationTokenSource = CancellationTokenSource()
  var locationProvidersChangeSubscriptionsCount = 0
  var locationChangeSubscriptionsCount = 0
  var appPermissionRequestCode = Constants.LOCATION_PERMISSION_REQ_CODE

  val onRequestPermissionsResultListener = object : PermissionListener {
    override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<String>,
      grantResults: IntArray
    ): Boolean {

      Log.d(
        Constants.LOG_TAG,
        "onRequestPermissionsResultListener calling...requestCode: $requestCode"
      )

      if (requestCode == appPermissionRequestCode) {

        for (i in permissions.indices) {
          val permission = permissions[i]
          val isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED

          when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION ->
              Log.d(Constants.LOG_TAG, "fine " + if (isGranted) "granted" else "denied")

            Manifest.permission.ACCESS_COARSE_LOCATION ->
              Log.d(Constants.LOG_TAG, "coarse " + if (isGranted) "granted" else "denied")

            Manifest.permission.ACCESS_BACKGROUND_LOCATION ->
              Log.d(Constants.LOG_TAG, "background " + if (isGranted) "granted" else "denied")
          }
        }
        return true
      }
      return false
    }
  }

  override fun requestAccessLocationPermission(
    permissionAwareActivity: PermissionAwareActivity,
    permissions: Array<String>,
    permissionRequestCode: Int
  ) {
    appPermissionRequestCode = permissionRequestCode

    permissionAwareActivity.requestPermissions(
      permissions,
      permissionRequestCode,
      onRequestPermissionsResultListener
    )
  }

  override fun requestBackgroundLocationPermission(
    reactApplicationContext: ReactApplicationContext,
    permissionAwareActivity: PermissionAwareActivity,
    permissions: Array<String>,
    permissionRequestCode: Int
  ) {

    val result = isLocationAuthorized(reactApplicationContext)

    if (!result.fine) {
      return
    }

    appPermissionRequestCode = permissionRequestCode

    permissionAwareActivity.requestPermissions(
      permissions,
      permissionRequestCode,
      onRequestPermissionsResultListener
    )
  }

  @RequiresApi(Build.VERSION_CODES.P)
  override fun isLocationEnabledApi28(reactApplicationContext: ReactApplicationContext): Boolean {
    val locationManager: LocationManager =
      reactApplicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val isOn: Boolean = locationManager.isLocationEnabled

    return isOn
  }

  override fun isLocationEnabledApi27(reactApplicationContext: ReactApplicationContext): Boolean {
    val locationManager: LocationManager =
      reactApplicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val isOn: Boolean =
      locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
        LocationManager.NETWORK_PROVIDER
      )

    return isOn
  }

  override fun isLocationEnabled(reactApplicationContext: ReactApplicationContext): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      isLocationEnabledApi28(reactApplicationContext)
    } else {
      isLocationEnabledApi27(reactApplicationContext)
    }
  }

  override fun isLocationAuthorizedCoarse(reactApplicationContext: ReactApplicationContext): Boolean {
    return ContextCompat.checkSelfPermission(
      reactApplicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
  }

  override fun isLocationAuthorizedFine(reactApplicationContext: ReactApplicationContext): Boolean {
    return ContextCompat.checkSelfPermission(
      reactApplicationContext, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
  }

  override fun isLocationAuthorized(reactApplicationContext: ReactApplicationContext): LocationAccessPermissionResult {
    val isFine: Boolean = isLocationAuthorizedFine(reactApplicationContext)
    val isCoarse: Boolean = isLocationAuthorizedCoarse(reactApplicationContext)

    return LocationAccessPermissionResult(
      status = isFine || isCoarse,
      fine = isFine,
      coarse = isCoarse
    )
  }

  override fun subscribeToLocationProvidersChange(
    reactApplicationContext: ReactApplicationContext,
    locationProvidersChangeReceiver: BroadcastReceiver
  ) {

    Log.d(
      Constants.LOG_TAG,
      "subscribeToLocationProvidersChange calling...$locationProvidersChangeSubscriptionsCount"
    )

    locationProvidersChangeSubscriptionsCount++

    if (locationProvidersChangeSubscriptionsCount <= 1) {
      val intentFilter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)

      reactApplicationContext.registerReceiver(locationProvidersChangeReceiver, intentFilter)
    }
  }

  override fun unsubscribeFromLocationProvidersChange(
    reactApplicationContext: ReactApplicationContext,
    locationProvidersChangeReceiver: BroadcastReceiver
  ) {
    Log.d(Constants.LOG_TAG, "unsubscribeFromLocationProvidersChange calling...")

    if (locationProvidersChangeSubscriptionsCount > 1) {
      locationProvidersChangeSubscriptionsCount--
    }

    if (locationProvidersChangeSubscriptionsCount <= 0) {
      try {
        reactApplicationContext.unregisterReceiver(locationProvidersChangeReceiver)
      } catch (e: Exception) {
        Log.d(Constants.LOG_TAG, "unsubscribeFromLocationProvidersChange: " + e.message)
      }
    }
  }

  @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
  override fun getLastLocation(
    reactApplicationContext: ReactApplicationContext,
    onSuccess: (Location) -> Unit,
    onError: (Exception) -> Unit
  ) {
    val isAuthorized = isLocationAuthorized(reactApplicationContext)

    if (!isAuthorized.coarse && !isAuthorized.fine) {
      onError(SecurityException("Location permission not granted"))
    }

    val fusedLocationProviderClient: FusedLocationProviderClient =
      LocationServices.getFusedLocationProviderClient(reactApplicationContext)

    fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
      if (location != null) {
        onSuccess(location)
      } else {
        onError(NullPointerException("Last Location was null"))
      }
    }.addOnFailureListener { exception ->
      onError(exception)
    }
  }

  @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
  override fun getFreshCurrentLocation(
    reactApplicationContext: ReactApplicationContext,
    priority: Int,
    onSuccess: (Location) -> Unit,
    onError: (Exception) -> Unit
  ) {

    cancelLocationRequest()

    val fusedLocationProviderClient: FusedLocationProviderClient =
      LocationServices.getFusedLocationProviderClient(reactApplicationContext)

    fusedLocationProviderClient.getCurrentLocation(
      priority, cancellationTokenSource.token
    ).addOnSuccessListener { location ->
      if (location != null) {
        onSuccess(location)
      } else {
        onError(NullPointerException("Location was null"))
      }
    }.addOnFailureListener { exception ->
      onError(exception)
    }
  }

  @RequiresPermission(
    anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]
  )
  override fun getCurrentLocation(
    reactApplicationContext: ReactApplicationContext,
    priority: Int,
    locationConfig: LocationConfiguration,
    onSuccess: (Location) -> Unit,
    onError: (Exception) -> Unit
  ) {
    getLastLocation(reactApplicationContext, onSuccess = { lastLocation ->
      val isFresh =
        System.currentTimeMillis() - lastLocation.time < locationConfig.maxUpdateAgeMillis

      if (isFresh) {
        onSuccess(lastLocation)
      } else {
        getFreshCurrentLocation(
          reactApplicationContext,
          priority,
          onSuccess,
          onError
        )
      }
    }, onError = {
      getFreshCurrentLocation(reactApplicationContext, priority, onSuccess, onError)
    })
  }

  override fun cancelLocationRequest() {
    cancellationTokenSource.cancel()
    cancellationTokenSource = CancellationTokenSource()
  }

  @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
  override fun subscribeToLocationChange(
    reactApplicationContext: ReactApplicationContext,
    locationRequest: LocationRequest,
    locationCallback: LocationCallback
  ) {
    locationChangeSubscriptionsCount++

    if (locationChangeSubscriptionsCount <= 1) {

      val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(reactApplicationContext)

      fusedLocationProviderClient.requestLocationUpdates(
        locationRequest,
        locationCallback,
        Looper.getMainLooper()
      )
    }
  }

  override fun unsubscribeFromLocationChange(
    reactApplicationContext: ReactApplicationContext,
    locationCallback: LocationCallback
  ) {
    if (locationChangeSubscriptionsCount > 1) {
      locationChangeSubscriptionsCount--
    }

    if (locationChangeSubscriptionsCount <= 0) {

      val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient((reactApplicationContext))

      fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
  }

  override fun invalidateAllSubscriptions(reactApplicationContext: ReactApplicationContext) {
    locationChangeSubscriptionsCount = 0
  }
}
