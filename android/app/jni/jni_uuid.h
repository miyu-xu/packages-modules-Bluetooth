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

#include "jni.h"
#include "types/bluetooth/uuid.h"

void init(JNIEnv* env);

bluetooth::Uuid jobject_to_uuid(jobject j_uuid);

uint64_t uuid_get_lsb(const bluetooth::Uuid& uuid);

uint64_t uuid_get_msb(const bluetooth::Uuid& uuid);
