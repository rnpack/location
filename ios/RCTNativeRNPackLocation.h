#import <NativeRNPackLocationSpec/NativeRNPackLocationSpec.h>
#import <CoreLocation/CoreLocation.h>
#import <React/RCTInitializing.h>
#import <React/RCTInvalidating.h>

@interface RCTNativeRNPackLocation : NativeRNPackLocationSpecBase <NativeRNPackLocationSpec, CLLocationManagerDelegate, RCTInitializing, RCTInvalidating>

@property (nonatomic, strong, nonnull) CLLocationManager *locationManager;
@property (nonatomic, copy, nullable) RCTPromiseResolveBlock locationResolver;
@property (nonatomic, copy, nullable) RCTPromiseRejectBlock locationRejecter;

@end
