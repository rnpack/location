#import "RCTNativeRNPackLocation.h"
#import "CoreLocation/CoreLocation.h"

@implementation RCTNativeRNPackLocation

NSString * const kLocationKeyStatus        = @"status";
NSString * const kLocationKeyCoarse        = @"coarse";
NSString * const kLocationKeyFine          = @"fine";

NSString * const kLocationKeyIosStatus     = @"iosStatus";
NSString * const kLocationKeyWhenInUse     = @"AuthorizedWhenInUse";
NSString * const kLocationKeyAlways        = @"AuthorizedAlways";
NSString * const kLocationKeyRestricted    = @"Restricted";
NSString * const kLocationKeyDenied        = @"Denied";
NSString * const kLocationKeyNotDetermined = @"NotDetermined";

NSString * const kLocationResponseKeyLatitude  =@"latitude";
NSString * const kLocationResponseKeyLongitude =@"longitude";
NSString * const kLocationResponseKeyAltitude   =@"altitude";
NSString * const kLocationResponseKeyAccuracy  =@"accuracy";
NSString * const kLocationResponseKeyTimestamp  =@"timestamp";

int locationProviderChangeListenerCount = 0;
int locationPermissionChangeListenerCount = 0;
int locationChangeListenerCount = 0;
int backgroundLocationChangeListenerCount = 0;


NSDictionary *locationAuthorizedResponse = @{
  kLocationKeyStatus: @(YES),
  // Android
  kLocationKeyCoarse: @(NO),
  kLocationKeyFine: @(NO),
  // iOS
  kLocationKeyIosStatus: kLocationKeyNotDetermined
};

NSDictionary *locationUpdate = @{
  kLocationResponseKeyLatitude: [NSNull null],
  kLocationResponseKeyLongitude: [NSNull null],
  kLocationResponseKeyAltitude: [NSNull null],
  kLocationResponseKeyAccuracy: [NSNull null],
  kLocationResponseKeyTimestamp: [NSNull null]
};

- (instancetype) init {
  self = [super init];
  
  if(self) {
    _locationManager = [[CLLocationManager alloc] init];
    _locationManager.delegate = self;
  }
  
  return self;
}

- (void)locationManagerDidChangeAuthorization:(CLLocationManager *)manager {
  if(locationProviderChangeListenerCount > 0) {
    NSNumber *status = [self isLocationEnabled];
    
    if(locationProviderChangeListenerCount > 0) {
      [self emitOnLocationProvidersChange:status];
    }
  }
  
  
  if(locationPermissionChangeListenerCount <= 0) {
    return;
  }
  
  NSDictionary *access = self.isLocationAuthorized;
  
  [self emitOnLocationPermissionsChange:access]; 
}

-(void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray<CLLocation *> *)locations {
  if(locationChangeListenerCount > 0 || backgroundLocationChangeListenerCount > 0 || self.locationResolver) {
    
    NSMutableDictionary *locationRes = [[NSMutableDictionary alloc] initWithDictionary:locationUpdate];
    
    for (CLLocation *location in locations) {
      locationRes[kLocationResponseKeyLatitude] = @(location.coordinate.latitude);
      locationRes[kLocationResponseKeyLongitude] = @(location.coordinate.longitude);
      locationRes[kLocationResponseKeyAltitude] = @(location.altitude);
      locationRes[kLocationResponseKeyAccuracy] = @(location.horizontalAccuracy);
      
      // Convert native NSDate timestamp to milliseconds unix epoch interval for JavaScript
      NSTimeInterval timestampInMs = [location.timestamp timeIntervalSince1970] * 1000;
      locationRes[kLocationResponseKeyTimestamp] = @(timestampInMs);
      
      if(locationChangeListenerCount > 0) {
        [self emitOnLocationChange:locationRes];
      }
      
      if(backgroundLocationChangeListenerCount > 0) {
        [self emitOnBackgroundLocationChange:locationRes];
      }
    }
    
    if(self.locationResolver) {
      self.locationResolver(locationRes);
      
      self.locationResolver = nil;
      self.locationRejecter = nil;
      
      if (!@available(iOS 9.0, *)) {
        if( locationChangeListenerCount <= 0) {
          
          [_locationManager stopUpdatingLocation];
        }
      }
    }
  }
}

- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error {
  
  NSLog(@"Location Manager Error: %@", error.localizedDescription);
  
  if (self.locationRejecter) {
    self.locationRejecter(@"E_LOCATION_FAILED", error.localizedDescription, error);
    self.locationResolver = nil;
    self.locationRejecter = nil;
  }
}

- (void)requestLocationPermission {
  NSDictionary *auth = self.isLocationAuthorized;

  if(auth[kLocationKeyIosStatus] == kLocationKeyNotDetermined) {
    [self.locationManager requestAlwaysAuthorization];
  }
  
  if(auth[kLocationKeyIosStatus] != kLocationKeyAlways && auth[kLocationKeyIosStatus] != kLocationKeyWhenInUse) {
    if(locationPermissionChangeListenerCount > 0) {
      [self emitOnLocationPermissionsChange:auth];
    }
  }
  
  if(auth[kLocationKeyIosStatus] == kLocationKeyDenied || auth[kLocationKeyIosStatus] == kLocationKeyRestricted) {
    if(locationPermissionChangeListenerCount > 0) {
      [self emitOnLocationPermissionsChange:auth];
    }
  }
}

- (NSNumber *)isLocationEnabled {
  return @([CLLocationManager locationServicesEnabled]);
}

- (NSDictionary *)isLocationAuthorized {
  
  CLLocationManager *locationManager = [[CLLocationManager alloc] init];
  CLAuthorizationStatus status;
  
  if(@available(iOS 14.0, *)) {
    status = locationManager.authorizationStatus;
  } else {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
    status = [CLLocationManager authorizationStatus];
#pragma clang diagnostic pop
  }
  
  NSMutableDictionary *mAuthorized = [[NSMutableDictionary alloc] initWithDictionary: locationAuthorizedResponse];
  
  switch (status) {
    case kCLAuthorizationStatusAuthorizedAlways:
      mAuthorized[kLocationKeyStatus] = @(YES);
      mAuthorized[kLocationKeyIosStatus] = kLocationKeyAlways;
      break;
      
    case kCLAuthorizationStatusAuthorizedWhenInUse:
      mAuthorized[kLocationKeyStatus] = @(YES);
      mAuthorized[kLocationKeyIosStatus] = kLocationKeyWhenInUse;
      break;
      
    case kCLAuthorizationStatusDenied:
      mAuthorized[kLocationKeyStatus] = @(NO);
      mAuthorized[kLocationKeyIosStatus] = kLocationKeyDenied;
      break;
      
    case kCLAuthorizationStatusRestricted:
      mAuthorized[kLocationKeyStatus] = @(NO);
      mAuthorized[kLocationKeyIosStatus] = kLocationKeyRestricted;
      break;
      
    case kCLAuthorizationStatusNotDetermined:
      mAuthorized[kLocationKeyStatus] = @(NO);
      mAuthorized[kLocationKeyIosStatus] = kLocationKeyNotDetermined;
      break;
      
    default:
      mAuthorized[kLocationKeyStatus] = @(NO);
      mAuthorized[kLocationKeyIosStatus] = kLocationKeyNotDetermined;
      break;
  }
  
  return mAuthorized;
}

- (void)openLocationPermissionsSettings {
  NSURL *settingsUrl = [NSURL URLWithString:UIApplicationOpenSettingsURLString];
  
  if (settingsUrl) {
    dispatch_async(dispatch_get_main_queue(), ^{
      if (@available(iOS 10.0, *)) {
        [[UIApplication sharedApplication] openURL:settingsUrl options:@{} completionHandler:nil];
      } else {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
        [[UIApplication sharedApplication] openURL:settingsUrl];
#pragma clang diagnostic pop
      }
    });
  }
}

- (void)openLocationProvidersSettings {
  [self openLocationPermissionsSettings];
}

-(void)getCurrentLocation:(NSNumber *)priority resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  _locationResolver = resolve;
  _locationRejecter = reject;
  
  _locationManager.desiredAccuracy = kCLLocationAccuracyBest;
  _locationManager.distanceFilter = 5;
  
  if(locationChangeListenerCount > 0) {
    [_locationManager stopUpdatingLocation];
  }
  
  if(@available(iOS 9.0, *)) {
    [_locationManager requestLocation];
  }
  
  [_locationManager startUpdatingLocation];
}

- (void)getLastLocation:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [self getCurrentLocation:@1 resolve:resolve reject:reject];
}

- (void)getFreshCurrentLocation:(NSNumber *)priority resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [self getCurrentLocation:@1 resolve:resolve reject:reject];
}

- (void)subscribeToLocationProvidersChange {
  locationProviderChangeListenerCount++;
}

- (void)unsubscribeFromLocationProvidersChange {
  if(locationProviderChangeListenerCount > 0) {
    locationProviderChangeListenerCount--;
  } else {
    locationProviderChangeListenerCount = 0;
  }
}

- (void)subscribeToLocationPermissionsChange {
  locationPermissionChangeListenerCount++;
}

- (void)unsubscribeFromLocationPermissionsChange {
  if(locationPermissionChangeListenerCount > 0) {
    locationPermissionChangeListenerCount--;
  } else {
    locationPermissionChangeListenerCount = 0;
  }
}

- (void)subscribeToLocationChange {
  if(locationChangeListenerCount <= 0) {
    self.locationManager.desiredAccuracy = kCLLocationAccuracyBest;
    self.locationManager.distanceFilter = 5;
    
    [_locationManager startUpdatingLocation];
  }
  
  locationChangeListenerCount++;
  
  
}

-(void)unsubscribeFromLocationChange {
  if(locationChangeListenerCount > 1) {
    locationChangeListenerCount--;
  } else {
    [_locationManager stopUpdatingLocation];
    locationChangeListenerCount = 0;
    
    if(backgroundLocationChangeListenerCount > 0) {
      self.locationManager.allowsBackgroundLocationUpdates = true;
      self.locationManager.showsBackgroundLocationIndicator = true;
      self.locationManager.desiredAccuracy = kCLLocationAccuracyBest;
      self.locationManager.distanceFilter = 5;
      
      [_locationManager startUpdatingLocation];
    }
  }
}

- (void)startLocationBackgroundService {
  
  NSLog(@"startLocationBackgroundService calling...");
  
  if(backgroundLocationChangeListenerCount <= 0) {
    self.locationManager.allowsBackgroundLocationUpdates = true;
    self.locationManager.showsBackgroundLocationIndicator = true;
    self.locationManager.desiredAccuracy = kCLLocationAccuracyBest;
    self.locationManager.distanceFilter = 5;
    
    [_locationManager startUpdatingLocation];
  }
  
  backgroundLocationChangeListenerCount++;
}

- (void)stopLocationBackgroundService {
  NSLog(@"stopLocationBackgroundService calling...");
  
  if(backgroundLocationChangeListenerCount > 1) {
    backgroundLocationChangeListenerCount--;
  } else {
    [_locationManager stopUpdatingLocation];
    backgroundLocationChangeListenerCount = 0;
    
    if(locationChangeListenerCount > 0) {
      self.locationManager.desiredAccuracy = kCLLocationAccuracyBest;
      self.locationManager.distanceFilter = 5;
      
      [_locationManager startUpdatingLocation];
    }
  }
}

- (void)startLocationForegroundService { 
}

- (void)stopLocationForegroundService { 
}

- (void)wakeUpApp { 
}

- (void)configureBackgroundLocation:(JS::NativeRNPackLocation::BackgroundLocationConfiguration &)config {
}


- (void)configureLocation:(JS::NativeRNPackLocation::LocationConfiguration &)config {
}


- (void)requestBackgroundLocationPermission {
}

- (void)initialize {
}

-(void)invalidate {
  locationProviderChangeListenerCount = 0;
  locationPermissionChangeListenerCount = 0;
  locationChangeListenerCount = 0;
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
(const facebook::react::ObjCTurboModule::InitParams &)params
{
  return std::make_shared<facebook::react::NativeRNPackLocationSpecJSI>(params);
}

+ (NSString *)moduleName
{
  return @"NativeRNPackLocation";
}

@end
