#include <jni.h>
#include "RNPackLocationOnLoad.hpp"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
  return margelo::nitro::RNPackLocation::initialize(vm);
}
