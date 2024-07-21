import type { GeolocationConfiguration } from '@react-native-community/geolocation';

const locationConfig: GeolocationConfiguration = {
  skipPermissionRequests: false,
  authorizationLevel: 'whenInUse',
  enableBackgroundLocationUpdates: true,
  locationProvider: 'auto',
};

export { locationConfig };
