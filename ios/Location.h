
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNLocationSpec.h"

@interface Location : NSObject <NativeLocationSpec>
#else
#import <React/RCTBridgeModule.h>

@interface Location : NSObject <RCTBridgeModule>
#endif

@end
