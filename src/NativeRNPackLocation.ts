import {
  TurboModuleRegistry,
  type TurboModule,
  type CodegenTypes,
} from 'react-native';

export type IosLocationAuthorizationStatus =
  | 'AuthorizedWhenInUse'
  | 'AuthorizedAlways'
  | 'Restricted'
  | 'Denied'
  | 'NotDetermined';

export type LocationAuthorizedResponse = {
  status: boolean;
  // Android
  coarse: boolean;
  fine: boolean;
  // iOS
  iosStatus: IosLocationAuthorizationStatus;
};

export type LocationResponse = {
  latitude: number;
  longitude: number;
  altitude: number;
  accuracy: number;
  timestamp: number;
  isMocked: boolean;
};

export enum LocationPriority {
  PRIORITY_HIGH_ACCURACY = 100,
  PRIORITY_BALANCED_POWER_ACCURACY = 102,
  PRIORITY_LOW_POWER = 104,
  PRIORITY_PASSIVE = 105,
}

export enum LocationGranularity {
  GRANULARITY_PERMISSION_LEVEL = 0,
  GRANULARITY_COARSE = 1,
  GRANULARITY_FINE = 2,
}

export type LocationConfiguration = {
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

export type BackgroundLocationConfiguration = {
  url: string;
  priority?: LocationPriority;
  maxUpdateAgeMillis?: number;
  granularity?: LocationGranularity;
  durationMillis?: number;
};

export interface Spec extends TurboModule {
  configureLocation(config: LocationConfiguration): void;
  requestLocationPermission(): void;
  requestBackgroundLocationPermission(): void;

  isLocationEnabled(): boolean;
  isLocationAuthorized(): LocationAuthorizedResponse;
  getLastLocation(): Promise<LocationResponse>;
  getCurrentLocation(priority?: LocationPriority): Promise<LocationResponse>;
  getFreshCurrentLocation(
    priority?: LocationPriority
  ): Promise<LocationResponse>;
  openLocationProvidersSettings(): void;
  openLocationPermissionsSettings(): void;

  subscribeToLocationProvidersChange(): void;
  unsubscribeFromLocationProvidersChange(): void;
  subscribeToLocationPermissionsChange(): void;
  unsubscribeFromLocationPermissionsChange(): void;
  subscribeToLocationChange(): void;
  unsubscribeFromLocationChange(): void;

  readonly onLocationProvidersChange: CodegenTypes.EventEmitter<boolean>;
  readonly onLocationPermissionsChange: CodegenTypes.EventEmitter<LocationAuthorizedResponse>;
  readonly onLocationChange: CodegenTypes.EventEmitter<LocationResponse>;

  // Foreground service
  startLocationForegroundService: () => void;
  stopLocationForegroundService: () => void;
  readonly onLocationForegroundServiceChange: CodegenTypes.EventEmitter<LocationResponse>;
  wakeUpApp: () => void;

  // Background Service
  configureBackgroundLocation(config: BackgroundLocationConfiguration): void;
  startLocationBackgroundService(): void;
  stopLocationBackgroundService(): void;
  readonly onBackgroundLocationChange: CodegenTypes.EventEmitter<LocationResponse>;

  // Mock
  isLocationMocked(): Promise<boolean>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('NativeRNPackLocation');
