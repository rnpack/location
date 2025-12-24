#include <jni.h>
#include "rnpacklocationOnLoad.hpp"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
  return margelo::nitro::rnpacklocation::initialize(vm);
}
