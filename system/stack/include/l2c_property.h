/*
 * Copyright 2024 The Android Open Source Project
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

#include <cstdint>

/*******************************************************************************
 **
 ** Function         L2CA_LeCreditDefault
 **
 ** Description      Check system property to override the number of
 **                  default LE credits.  This is initial amount of credits
 **                  we send, and amount to which we increase credits once
 **                  they fall below threshold
 **
 ** Parameters:      None
 **
 ** Returns          The default number of LE credits.
 **
 ******************************************************************************/
uint16_t L2CA_LeCreditDefault();

/*******************************************************************************
 **
 ** Function         L2CA_LeCreditThreshold
 **
 ** Description      Check system property to override the number of
 **                  LE threadhold credits. If credit count on remote fall
 **                  below this value, we send back credits to reach default
 **                  value.
 **
 ** Parameters:      None
 **
 ** Returns          The threshold number of LE credits.
 **
 ******************************************************************************/
uint16_t L2CA_LeCreditThreshold();
