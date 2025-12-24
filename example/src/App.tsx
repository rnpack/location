import { useState, useEffect } from 'react';
import { StyleSheet, View } from 'react-native';
import { colors, Text, DesignProvider } from 'react-native-design';

import type { GeolocationResponse } from '@react-native-community/geolocation';

import { LocationHelper, multiply } from '@rnpack/location';

import { locationConfig } from './configs';

const result = multiply(3, 7);

export default function App() {
  const [isAuthorized, setIsAuthorized] = useState<boolean>(true);
  const [isEnabled, setIsEnabled] = useState<boolean>(true);
  const [location, setLocation] = useState<GeolocationResponse>();

  useEffect(() => {
    mount();
  }, []);

  function mount(): void {}

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

  return (
    <DesignProvider>
      <View style={styles.container}>
        <Text>
          Location Authorization:{' '}
          {isAuthorized ? 'Authorized' : 'Not Authorized'}
        </Text>
        <Text>Location Mode: {isEnabled ? 'On' : 'Off'}</Text>
        <Text>Location: {JSON.stringify(location)}</Text>

        <View style={styles.resultContainer}>
          <Text>Result: {result}</Text>
        </View>
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
        timeInterval={8000}
      />
    </DesignProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors?.white?.normal?.main,
  },
  resultContainer: {
    alignItems: 'center',
    justifyContent: 'center',
  },
});
