#ifndef CRUST_LOADER_H
#define CRUST_LOADER_H

#include <cstdarg>
#include <cstdint>
#include <cstdlib>
#include <new>
#include <ostream>

extern "C" {

bool load_initial_crust_jni(JNIEnv* jni_env_raw);

}  // extern "C"

#endif  // CRUST_LOADER_H
