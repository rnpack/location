import type { EventSubscription } from 'react-native';

import NativeRNPackLocation, {
  LocationPriority,
  type BackgroundLocationConfiguration,
  type LocationAuthorizedResponse,
  type LocationConfiguration,
  type LocationResponse,
} from './NativeRNPackLocation';

export * from './NativeRNPackLocation';

export function requestLocationPermission(): void {
  NativeRNPackLocation.requestLocationPermission();
}

export function requestBackgroundLocationPermission(): void {
  NativeRNPackLocation.requestBackgroundLocationPermission();
}

export function isLocationEnabled(): boolean {
  return NativeRNPackLocation.isLocationEnabled();
}

export function isLocationAuthorized(): LocationAuthorizedResponse {
  return NativeRNPackLocation.isLocationAuthorized();
}

export function subscribeToLocationProvidersChange(): void {
  NativeRNPackLocation.subscribeToLocationProvidersChange();
}

export function unsubscribeFromLocationProvidersChange(): void {
  NativeRNPackLocation.unsubscribeFromLocationProvidersChange();
}

export function onLocationProvidersChange(
  cb: (isOn: boolean) => void
): EventSubscription {
  return NativeRNPackLocation.onLocationProvidersChange(cb);
}

export function subscribeToLocationPermissionsChange(): void {
  NativeRNPackLocation.subscribeToLocationPermissionsChange();
}

export function unsubscribeFromLocationPermissionsChange(): void {
  NativeRNPackLocation.unsubscribeFromLocationPermissionsChange();
}

export function onLocationPermissionsChange(
  cb: (result: LocationAuthorizedResponse) => void
): EventSubscription {
  return NativeRNPackLocation.onLocationPermissionsChange(cb);
}

export function openLocationProvidersSettings() {
  NativeRNPackLocation.openLocationProvidersSettings();
}

export function openLocationPermissionsSettings() {
  NativeRNPackLocation.openLocationPermissionsSettings();
}

export async function getLastLocation() {
  return await NativeRNPackLocation.getLastLocation();
}

export async function getCurrentLocation(
  priority: LocationPriority = LocationPriority.PRIORITY_BALANCED_POWER_ACCURACY
) {
  return await NativeRNPackLocation.getCurrentLocation(priority);
}

export async function getFreshCurrentLocation(
  priority: LocationPriority = LocationPriority.PRIORITY_BALANCED_POWER_ACCURACY
) {
  return await NativeRNPackLocation.getFreshCurrentLocation(priority);
}

export function subscribeToLocationChange() {
  NativeRNPackLocation.subscribeToLocationChange();
}

export function unsubscribeFromLocationChange() {
  NativeRNPackLocation.unsubscribeFromLocationChange();
}

export function onLocationChange(cb: (location: LocationResponse) => void) {
  return NativeRNPackLocation.onLocationChange(cb);
}

export function startLocationForegroundService() {
  NativeRNPackLocation.startLocationForegroundService();
}

export function stopLocationForegroundService() {
  NativeRNPackLocation.stopLocationForegroundService();
}

export function onLocationForegroundServiceChange(
  cb: (location: LocationResponse) => void
) {
  return NativeRNPackLocation.onLocationForegroundServiceChange(cb);
}

export function startLocationBackgroundService() {
  NativeRNPackLocation.startLocationBackgroundService();
}

export function stopLocationBackgroundService() {
  NativeRNPackLocation.stopLocationBackgroundService();
}

export function onBackgroundLocationChange(
  cb: (location: LocationResponse) => void
) {
  return NativeRNPackLocation.onBackgroundLocationChange(cb);
}

export function wakeUpApp(): void {
  NativeRNPackLocation.wakeUpApp();
}

export function configureLocation(
  config: Partial<LocationConfiguration>
): void {
  NativeRNPackLocation.configureLocation(config);
}

export function configureBackgroundLocation(
  config: BackgroundLocationConfiguration
): void {
  NativeRNPackLocation.configureBackgroundLocation(config);
}

export async function isLocationMocked(): Promise<boolean> {
  return await NativeRNPackLocation.isLocationMocked();
}
