# @rnpack/location

[![npm version](https://img.shields.io/npm/v/@rnpack/location.svg?style=flat-square)](https://www.npmjs.com/package/@rnpack/location)
[![npm downloads](https://img.shields.io/npm/dm/@rnpack/location.svg?style=flat-square)](https://www.npmjs.com/package/@rnpack/location)
[![License](https://img.shields.io/npm/l/@rnpack/location.svg?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-android%20%7C%20ios-blue.svg?style=flat-square)](https://github.com/rnpack/location)

A high-performance location tracking and permissions management package for React Native, built using the modern **Turbo Modules** (New Architecture) framework. It provides smooth, battery-efficient, and precise location tracking across both iOS and Android.

---

## Features

- ⚡️ **Built on Turbo Modules**: Next-generation React Native performance with JSI (no JSON bridge serialization overhead).
- 📍 **Precise Tracking**: Support for high accuracy, balanced power, and passive location providers.
- 🔄 **Subscription Services**: Simple, event-based hooks for location provider status, permission status, and real-time location coordinates.
- 🛡️ **Permission Management**: Easy-to-use API for requesting foreground and background permissions, plus helpers to open system settings directly.
- 📱 **Android Foreground Service**: Run persistent foreground tracking with standard status-bar notifications (**Android Only**).
- 🌌 **Background Location Sync**: Keep tracking and sync location data directly to your API endpoint in the background (iOS & Android).

---

## Installation

```sh
npm install @rnpack/location
```
or with Yarn:
```sh
yarn add @rnpack/location
```
or with pnpm:
```sh
pnpm add @rnpack/location
```

---

## Platform Setup

### iOS Setup

Add the following keys to your `ios/YourAppName/Info.plist` file with description strings explaining why your app requires these permissions:

```xml
<!-- Foreground Location Permission -->
<key>NSLocationWhenInUseUsageDescription</key>
<string>We need access to your location to show local events and route details.</string>

<!-- Background Location Permission -->
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>We need continuous access to your location to track your journey even when the app is in the background.</string>
<key>NSLocationAlwaysUsageDescription</key>
<string>We need continuous access to your location to track your journey in the background.</string>
```

For background location tracking, enable **Location updates** under **Signing & Capabilities** -> **Background Modes** in Xcode.

---

### Android Setup

1. Add permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Foreground location permissions -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <!-- Background location permissions -->
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

    <!-- Foreground Service permissions (For Android Foreground Service) -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
    
    <!-- Wake Lock permission for waking up the app -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    
    <!-- Internet permission for background URL sync -->
    <uses-permission android:name="android.permission.INTERNET" />
</manifest>
```

2. Register the services, headless JS tasks, and receivers within the `<application>` tag of your `android/app/src/main/AndroidManifest.xml`:

```xml
<application ...>
    <!-- ... other components ... -->

    <!-- Foreground Location Service -->
    <service
      android:name="com.rnpack.location.services.LocationForegroundService"
      android:exported="false"
      android:foregroundServiceType="location" />
    <service
      android:name="com.rnpack.location.services.ForegroundLocationHeadlessJsTaskService"
      android:exported="false" />

    <!-- Background Location Service & Headless Task -->
    <receiver
      android:name="com.rnpack.location.services.BackgroundLocationReceiver"
      android:exported="false" />
    <service
      android:name="com.rnpack.location.services.BackgroundLocationHeadlessJsTaskService"
      android:exported="false" />

</application>
```

3. Add the required dependencies to your `android/app/build.gradle`:

```gradle
dependencies {
    // ... other dependencies ...
    implementation('com.google.android.gms:play-services-location:21.3.0')
    implementation('androidx.work:work-runtime-ktx:2.11.2')
}
```

---

## API Reference

### Methods

| Method | Return Type | Platform | Description |
| :--- | :--- | :--- | :--- |
| `requestLocationPermission()` | `void` | Both | Triggers the system dialog to request foreground location permission. |
| `requestBackgroundLocationPermission()` | `void` | Both | Triggers the system dialog to request background location permission. **Note:** On Android, this should only be called after normal (coarse/fine) location permission has already been granted. |
| `isLocationEnabled()` | `boolean` | Both | Checks if system location services (GPS/sensors) are active. |
| `isLocationAuthorized()` | [`LocationAuthorizedResponse`](#locationauthorizedresponse) | Both | Checks current status of location permissions for coarse, fine, and iOS-specific states. |
| `configureLocation(config)` | `void` | Both | Configures parameters for standard location tracking requests. |
| `getLastLocation()` | `Promise<LocationResponse>` | Both | Resolves the last cached location on the device. Faster and battery-friendly. |
| `getCurrentLocation(priority?)` | `Promise<LocationResponse>` | Both | Requests a current location update based on the specified priority level. |
| `getFreshCurrentLocation(priority?)` | `Promise<LocationResponse>` | Both | Bypasses caching and forces a fresh GPS fix from the hardware. |
| `openLocationProvidersSettings()` | `void` | Both | Directs the user to the device's system Location Provider settings. |
| `openLocationPermissionsSettings()` | `void` | Both | Directs the user to the application's details screen to adjust permissions. |
| `subscribeToLocationProvidersChange()` | `void` | Both | Begins monitoring changes in the system location provider status. |
| `unsubscribeFromLocationProvidersChange()` | `void` | Both | Stops monitoring changes in the system location provider status. |
| `onLocationProvidersChange(cb)` | `EventSubscription` | Both | Registers a callback for when system location providers turn ON/OFF. |
| `subscribeToLocationPermissionsChange()` | `void` | Both | Begins monitoring changes in app location permissions. |
| `unsubscribeFromLocationPermissionsChange()` | `void` | Both | Stops monitoring changes in app location permissions. |
| `onLocationPermissionsChange(cb)` | `EventSubscription` | Both | Registers a callback to receive permission updates. |
| `subscribeToLocationChange()` | `void` | Both | Starts active/passive location update streams. |
| `unsubscribeFromLocationChange()` | `void` | Both | Stops active/passive location update streams. |
| `onLocationChange(cb)` | `EventSubscription` | Both | Registers a callback to receive location updates. |
| `startLocationForegroundService()` | `void` | **Android Only** | Launches a persistent foreground service with a notification. |
| `stopLocationForegroundService()` | `void` | **Android Only** | Stops the active foreground service. |
| `onLocationForegroundServiceChange(cb)` | `EventSubscription` | **Android Only** | Registers a callback for foreground service location updates. |
| `wakeUpApp()` | `void` | **Android Only** | Triggers application wake-up from background state. |
| `configureBackgroundLocation(config)` | `void` | Both | Configures settings for background tracking and REST API endpoint sync. |
| `startLocationBackgroundService()` | `void` | Both | Starts background tracking and sync. |
| `stopLocationBackgroundService()` | `void` | Both | Stops background tracking. |
| `onBackgroundLocationChange(cb)` | `EventSubscription` | Both | Registers a callback for location updates received in the background. |

---

### Type Definitions

#### `LocationResponse`
```typescript
type LocationResponse = {
  latitude: number;
  longitude: number;
  altitude: number;
  accuracy: number;
  timestamp: number;
};
```

#### `LocationConfiguration`
```typescript
type LocationConfiguration = {
  priority?: LocationPriority;
  intervalMillis?: number;
  minUpdateIntervalMillis?: number;
  waitForAccurateLocation?: boolean;
  minUpdateDistanceMeters?: number;
  maxUpdateAgeMillis?: number;
  durationMillis?: number;
  maxUpdateDelayMillis?: number;
  granularity?: LocationGranularity;
};
```

#### `BackgroundLocationConfiguration`
```typescript
type BackgroundLocationConfiguration = {
  url: string; // Endpoint to POST location payload
  priority?: LocationPriority;
  maxUpdateAgeMillis?: number;
  granularity?: LocationGranularity;
  durationMillis?: number;
};
```

#### `LocationAuthorizedResponse`
```typescript
type LocationAuthorizedResponse = {
  status: boolean; // Generalized permission status
  coarse: boolean; // Android: COARSE permission status
  fine: boolean; // Android: FINE permission status
  iosStatus: IosLocationAuthorizationStatus; // iOS-specific authorization status
};
```

#### `IosLocationAuthorizationStatus`
```typescript
type IosLocationAuthorizationStatus =
  | 'AuthorizedWhenInUse'
  | 'AuthorizedAlways'
  | 'Restricted'
  | 'Denied'
  | 'NotDetermined';
```

#### `LocationPriority` (Enum)
```typescript
enum LocationPriority {
  PRIORITY_HIGH_ACCURACY = 100,
  PRIORITY_BALANCED_POWER_ACCURACY = 102,
  PRIORITY_LOW_POWER = 104,
  PRIORITY_PASSIVE = 105,
}
```

#### `LocationGranularity` (Enum)
```typescript
enum LocationGranularity {
  GRANULARITY_PERMISSION_LEVEL = 0,
  GRANULARITY_COARSE = 1,
  GRANULARITY_FINE = 2,
}
```

---

## Usage Guide & Code Examples

### 1. Basic Permission & Location Check
```tsx
import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View, Button } from 'react-native';
import {
  isLocationEnabled,
  isLocationAuthorized,
  requestLocationPermission,
  getCurrentLocation,
  type LocationResponse
} from '@rnpack/location';

export default function App() {
  const [location, setLocation] = useState<LocationResponse | null>(null);

  const fetchLocation = async () => {
    // Check if services are active
    const enabled = isLocationEnabled();
    if (!enabled) {
      console.log('Location services are disabled on this device.');
      return;
    }

    // Check and request permission
    const auth = isLocationAuthorized();
    if (!auth.status) {
      requestLocationPermission();
      return;
    }

    try {
      const result = await getCurrentLocation();
      setLocation(result);
    } catch (error) {
      console.error('Failed to get location:', error);
    }
  };

  return (
    <View style={styles.container}>
      <Button title="Get Current Location" onPress={fetchLocation} />
      {location && (
        <Text style={styles.text}>
          Lat: {location.latitude}, Lon: {location.longitude}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  text: { marginTop: 20, fontSize: 16 }
});
```

### 2. Live Location Subscription
```tsx
import React, { useEffect } from 'react';
import {
  subscribeToLocationChange,
  unsubscribeFromLocationChange,
  onLocationChange
} from '@rnpack/location';

export function TrackUser() {
  useEffect(() => {
    // 1. Subscribe to updates
    subscribeToLocationChange();

    // 2. Register callback listener
    const subscription = onLocationChange((location) => {
      console.log('New coordinates:', location.latitude, location.longitude);
    });

    return () => {
      // Clean up subscription & event listener
      subscription.remove();
      unsubscribeFromLocationChange();
    };
  }, []);

  return null;
}
```

### 3. Background Sync (Android & iOS)
Automatically track location and post coordinates to your API server in the background. **Note:** On Android, ensure you request and obtain background permission using `requestBackgroundLocationPermission()` *after* normal/foreground location permission is already granted before starting this service:
```tsx
import {
  configureBackgroundLocation,
  startLocationBackgroundService,
  stopLocationBackgroundService,
  onBackgroundLocationChange,
  LocationPriority
} from '@rnpack/location';

// 1. Configure the endpoint and tracking settings
configureBackgroundLocation({
  url: 'https://api.yourdomain.com/user/track-location',
  priority: LocationPriority.PRIORITY_BALANCED_POWER_ACCURACY,
});

// 2. Register a listener to track in-app if desired
const bgSub = onBackgroundLocationChange((location) => {
  console.log('Background location tracked locally:', location);
});

// 3. Start background updates
startLocationBackgroundService();

// 4. Stop service when done
// stopLocationBackgroundService();
```

### 4. Foreground Service Tracking (Android Only)
Keep location tracking active in the foreground even when the app is minimized (running with a status bar notification):
```tsx
import { Platform } from 'react-native';
import {
  startLocationForegroundService,
  stopLocationForegroundService,
  onLocationForegroundServiceChange
} from '@rnpack/location';

function startAndroidTracking() {
  if (Platform.OS !== 'android') {
    console.warn('Foreground location service is only supported on Android.');
    return;
  }

  // Start the service
  startLocationForegroundService();

  // Listen to updates
  const subscription = onLocationForegroundServiceChange((location) => {
    console.log('Foreground Service location update:', location);
  });
}
```

---

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT

## Author

<table>
  <tr>
    <td>
      <img src="https://avatars.githubusercontent.com/u/41302126?v=4" width="64" height="64" alt="Abiraman K">
    </td>
    <td>
      <a href="https://abiramank.github.io" target="_blank">Abiraman K</a>
    </td>
  </tr>
</table>

## Thank you

### Sponsors

Thank you to all our sponsors! [Become a sponsor](https://opencollective.com/rnpack#sponsor) and get your image on our README on GitHub.

<a href="https://opencollective.com/rnpack#sponsors" target="_blank"><img src="https://opencollective.com/rnpack/sponsors.svg?width=890" alt="rnpack"></a>

---
