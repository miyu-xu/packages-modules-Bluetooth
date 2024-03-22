/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "jni_uuid.h"

#include "com_android_bluetooth.h"

using bluetooth::Uuid;

jmethodID uuidGetMsb;
jmethodID uuidGetLsb;

void uuid_init(JNIEnv* env) {
  const android::JNIJavaMethod javaUuidMethods[] = {
      {"getMostSignificantBits", "()J", &uuidGetMsb},
      {"getLeastSignificantBits", "()J", &uuidGetLsb},
  };
  GET_JAVA_METHODS(env, "java/util/UUID", javaUuidMethods);
}

static Uuid from_java_uuid(jlong uuid_msb, jlong uuid_lsb) {
  std::array<uint8_t, Uuid::kNumBytes128> uu;
  for (int i = 0; i < 8; i++) {
    uu[7 - i] = (uuid_msb >> (8 * i)) & 0xFF;
    uu[15 - i] = (uuid_lsb >> (8 * i)) & 0xFF;
  }
  return Uuid::From128BitBE(uu);
}

Uuid jobject_to_uuid(JNIEnv* env, jobject j_uuid) {
  return from_java_uuid(env->CallLongMethod(j_uuid, uuidGetMsb),
                        env->CallLongMethod(j_uuid, uuidGetLsb));
}

uint64_t uuid_to_lsb(const Uuid& uuid) {
  uint64_t lsb = 0;

  auto uu = uuid.To128BitBE();
  for (int i = 8; i <= 15; i++) {
    lsb <<= 8;
    lsb |= uu[i];
  }

  return lsb;
}

uint64_t uuid_to_msb(const Uuid& uuid) {
  uint64_t msb = 0;

  auto uu = uuid.To128BitBE();
  for (int i = 0; i <= 7; i++) {
    msb <<= 8;
    msb |= uu[i];
  }

  return msb;
}
