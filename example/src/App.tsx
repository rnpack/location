import { useState, Fragment, useEffect } from 'react';
import { StyleSheet, View } from 'react-native';
import { colors, Text } from 'react-native-design';
import type {
  GeolocationError,
  GeolocationResponse,
} from '@react-native-community/geolocation';

import { LocationHelper, useLocation } from '@rnpack/location';

import { locationConfig } from './configs';

export default function App() {
  const { getCurrentLocation } = useLocation({
    config: locationConfig,
    onGetCurrentLocationSuccess,
    onGetCurrentLocationError,
  });

  const [isAuthorized, setIsAuthorized] = useState<boolean>(true);
  const [isEnabled, setIsEnabled] = useState<boolean>(true);
  const [location, setLocation] = useState<GeolocationResponse>();

  useEffect(() => {
    mount();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function mount(): void {
    getCurrentLocation();
  }

  function onLocationChange(_location: GeolocationResponse) {
    console.info('Calling location change: ', _location);
    setLocation(_location);
  }

  function onLocationAuthorizationChange(_isAuthorized: boolean) {
    setIsAuthorized(_isAuthorized);
  }

  function onLocationAdapterStateChange(_isEnabled: boolean) {
    setIsEnabled(_isEnabled);
  }

  function onGetCurrentLocationSuccess(position: GeolocationResponse) {
    setLocation(position);
  }
  function onGetCurrentLocationError(error: GeolocationError) {
    console.log('Get current location Error: ', error?.message);
    setLocation(undefined);
  }

  return (
    <Fragment>
      <View style={styles.container}>
        <Text>
          Location Authorization:{' '}
          {isAuthorized ? 'Authorized' : 'Not Authorized'}
        </Text>
        <Text>Location Mode: {isEnabled ? 'On' : 'Off'}</Text>
        <Text>Location: {JSON.stringify(location)}</Text>
      </View>
      <LocationHelper
        locationConfig={locationConfig}
        onLocationChange={onLocationChange}
        locationAuthorizationTitle="Allow Location Authorization"
        locationAuthorizationAcceptText="Allow"
        locationAuthorizationContent={
          <Text variant="label">
            Location authorization is required to check location permissions and
            adapter state. Please authorize the location.
          </Text>
        }
        onLocationAuthorizationChange={onLocationAuthorizationChange}
        enableLocationTitle="Enable Location"
        enableLocationAcceptText="Enable"
        enableLocationContent={
          <Text variant="label">
            Location is required to check location permissions and adapter
            state. Please enable the location.
          </Text>
        }
        onLocationAdapterStateChange={onLocationAdapterStateChange}
      />
    </Fragment>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    backgroundColor: colors?.white?.normal?.main,
  },
});
