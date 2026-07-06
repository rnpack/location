import { useEffect, useRef, useState } from 'react';
import {
  Text,
  View,
  StyleSheet,
  Button,
  ScrollView,
  Platform,
} from 'react-native';
import {
  SafeAreaProvider,
  useSafeAreaInsets,
} from 'react-native-safe-area-context';

import type { EventSubscription } from 'react-native';

import {
  isLocationEnabled,
  isLocationAuthorized,
  onLocationProvidersChange,
  subscribeToLocationProvidersChange,
  unsubscribeFromLocationProvidersChange,
  subscribeToLocationPermissionsChange,
  unsubscribeFromLocationPermissionsChange,
  onLocationPermissionsChange,
  openLocationProvidersSettings,
  openLocationPermissionsSettings,
  getCurrentLocation,
  getLastLocation,
  getFreshCurrentLocation,
  subscribeToLocationChange,
  unsubscribeFromLocationChange,
  onLocationChange,
  LocationPriority,
  startLocationBackgroundService,
  requestLocationPermission,
  requestBackgroundLocationPermission,
  stopLocationBackgroundService,
  configureBackgroundLocation,
  onBackgroundLocationChange,
  isLocationMocked,
} from '@rnpack/location';

import type {
  LocationResponse,
  LocationAuthorizedResponse,
} from '@rnpack/location';
import { ForegroundServiceLocationApp } from './ForegroundLocationApp';
import { BackgroundLocationLogApi } from './constants';

const isIos = Platform.OS === 'ios';

export default function App() {
  return (
    <SafeAreaProvider>
      <Main />
    </SafeAreaProvider>
  );
}

function Main() {
  const onLocationProvidersChangeSubscriptionRef =
    useRef<null | EventSubscription>(null);
  const onLocationPermissionsChangeSubscriptionRef =
    useRef<null | EventSubscription>(null);
  const onLocationChangeSubscriptionRef = useRef<null | EventSubscription>(
    null
  );
  const onBackgroundLocationChangeSubscriptionRef =
    useRef<null | EventSubscription>(null);

  const [locationOn, setLocationOn] = useState<boolean>(false);
  const [locationAuthorized, setLocationAuthorized] =
    useState<LocationAuthorizedResponse>();
  const [lastLocation, setLastLocation] = useState<LocationResponse>();
  const [isGettingLastLocation, setIsGettingLastLocation] =
    useState<boolean>(false);
  const [currentLocation, setCurrentLocation] = useState<LocationResponse>();
  const [isGettingCurrentLocation, setIsGettingCurrentLocation] =
    useState<boolean>(false);
  const [freshCurrentLocation, setFreshCurrentLocation] =
    useState<LocationResponse>();
  const [isGettingFreshCurrentLocation, setIsGettingFreshCurrentLocation] =
    useState<boolean>(false);
  const [locationUpdate, setLocationUpdate] = useState<LocationResponse>();
  const [backgroundLocationUpdate, setBackgroundLocationUpdate] =
    useState<LocationResponse>();
  const [isMockedLocation, setIsMockedLocation] = useState<boolean>(false);

  useEffect(() => {
    const __isOn = isLocationEnabled();
    setLocationOn(__isOn);

    const authorizedResult = isLocationAuthorized();
    setLocationAuthorized(authorizedResult);

    configureBackgroundLocation({
      url: BackgroundLocationLogApi,
    });

    onLocationProvidersChangeSubscriptionRef.current =
      onLocationProvidersChange((isOn) => {
        setLocationOn(isOn);
      });

    onLocationPermissionsChangeSubscriptionRef.current =
      onLocationPermissionsChange((result) => {
        setLocationAuthorized(result);
      });

    onLocationChangeSubscriptionRef.current = onLocationChange((result) => {
      setLocationUpdate(result);
    });

    if (isIos) {
      onBackgroundLocationChangeSubscriptionRef.current =
        onBackgroundLocationChange((result) => {
          setBackgroundLocationUpdate(result);
        });
    }

    subscribeToLocationProvidersChange();

    subscribeToLocationPermissionsChange();

    subscribeToLocationChange();

    return () => {
      onLocationProvidersChangeSubscriptionRef.current?.remove();
      onLocationPermissionsChangeSubscriptionRef.current?.remove();

      if (isIos) {
        onBackgroundLocationChangeSubscriptionRef.current?.remove();
      }

      unsubscribeFromLocationProvidersChange();
      unsubscribeFromLocationPermissionsChange();
      unsubscribeFromLocationChange();
    };
  }, []);

  useEffect(() => {
    console.log('isOn: ', locationOn);
  }, [locationOn]);

  async function accessLastLocation() {
    try {
      setIsGettingLastLocation(true);

      const location = await getLastLocation();

      setLastLocation(location);
    } finally {
      setIsGettingLastLocation(false);
    }
  }

  async function accessCurrentLocation() {
    try {
      setIsGettingCurrentLocation(true);

      const location = await getCurrentLocation();

      setCurrentLocation(location);
    } finally {
      setIsGettingCurrentLocation(false);
    }
  }

  async function accessFreshCurrentLocation() {
    try {
      setIsGettingFreshCurrentLocation(true);

      const location = await getFreshCurrentLocation(
        LocationPriority.PRIORITY_HIGH_ACCURACY
      );

      setFreshCurrentLocation(location);
    } finally {
      setIsGettingFreshCurrentLocation(false);
    }
  }

  async function onPressIsLocaitonMocked() {
    const isMocked = await isLocationMocked();

    setIsMockedLocation(isMocked);
  }

  const safeAreaInsets = useSafeAreaInsets();

  return (
    <View
      style={[
        styles.container,
        {
          paddingTop: safeAreaInsets.top,
          paddingBottom: safeAreaInsets.bottom,
        },
      ]}
    >
      <ScrollView style={styles.content}>
        <Text>Location: {locationOn ? 'ON' : 'OFF'}</Text>
        <Text>
          Location Authorized Coarse:{' '}
          {locationAuthorized?.coarse ? 'ON' : 'OFF'}
        </Text>
        <Text>
          Location Authorized Fine: {locationAuthorized?.fine ? 'ON' : 'OFF'}
        </Text>
        <Text>Location Authorized iOS: {locationAuthorized?.iosStatus}</Text>

        <View style={styles.buttonContainer}>
          <Button
            title="Request Location Permission"
            onPress={requestLocationPermission}
          />
          <Button
            title="Request Background Location Permission"
            onPress={requestBackgroundLocationPermission}
          />
          <Button
            title="Open Location Providers Settings"
            onPress={openLocationProvidersSettings}
          />
          <Button
            title="Open Location Permission Settings"
            onPress={openLocationPermissionsSettings}
          />
          <Button
            title="Get Last Location"
            onPress={accessLastLocation}
            disabled={isGettingLastLocation}
          />
          <Button
            title="Get Current Location"
            onPress={accessCurrentLocation}
            disabled={isGettingCurrentLocation}
          />
          <Button
            title="Get Fresh Current Location"
            onPress={accessFreshCurrentLocation}
            disabled={isGettingFreshCurrentLocation}
          />
          <Button
            title="Start Location Background Service"
            onPress={startLocationBackgroundService}
          />
          <Button
            title="Stop Location Background Service"
            onPress={stopLocationBackgroundService}
          />
          <Button
            title="Is Location Mocked"
            onPress={onPressIsLocaitonMocked}
          />
        </View>
        <Text>
          Is Location Mocked: {isMockedLocation ? 'Mocked' : 'Not Mocked'}
        </Text>
        <Text>Last Location: {JSON.stringify(lastLocation)}</Text>
        <Text>Current Location: {JSON.stringify(currentLocation)}</Text>
        <Text>
          Fresh Current Location: {JSON.stringify(freshCurrentLocation)}
        </Text>
        <Text>Current Location Listener: {JSON.stringify(locationUpdate)}</Text>
        {isIos && (
          <Text>
            Background Location Listener:{' '}
            {JSON.stringify(backgroundLocationUpdate)}
          </Text>
        )}

        {Platform.OS === 'android' && <ForegroundServiceLocationApp />}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#FFFFFF',
    rowGap: 4,
  },
  content: {
    flexGrow: 1,
  },
  buttonContainer: {
    rowGap: 8,
  },
});
