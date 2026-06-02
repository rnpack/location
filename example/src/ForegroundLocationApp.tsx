import { useEffect, useRef, useState } from 'react';

import {
  Button,
  PermissionsAndroid,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import type { EventSubscription, PermissionStatus } from 'react-native';

import {
  onLocationForegroundServiceChange,
  startLocationForegroundService,
  stopLocationForegroundService,
} from '@rnpack/location';
import type { LocationResponse } from '@rnpack/location';

export function ForegroundServiceLocationApp() {
  const onLocationForegroundServiceChangeSubscriptionRef =
    useRef<null | EventSubscription>(null);

  const [locationForegroundServiceUpdate, setLocationForegroundServiceUpdate] =
    useState<LocationResponse>();

  useEffect(() => {
    onLocationForegroundServiceChangeSubscriptionRef.current =
      onLocationForegroundServiceChange(async (result) => {
        console.log('onLocationForegroundServiceChange Event Emit: ', result);

        setLocationForegroundServiceUpdate(result);
      });
  }, []);

  async function requestNotificationPermission() {
    const result: PermissionStatus = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
    );

    if (result === 'granted') {
      console.info('Post notification Permission Granted');
    } else {
      console.info('Post notification Permission Denied: ', { result });
    }
  }

  async function startForegroundService() {
    await requestNotificationPermission();

    startLocationForegroundService();
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Foreground Service - Android only</Text>

      <Button
        title="Start Location Foreground Service"
        onPress={startForegroundService}
      />

      <Button
        title="Stop Location Foreground Service"
        onPress={stopLocationForegroundService}
      />
      <Text>
        Location Foreground Service Listener:{' '}
        {JSON.stringify(locationForegroundServiceUpdate)}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    paddingVertical: 16,
  },
  title: {
    fontSize: 28,
  },
});
