/******************************************************************************
 *
 *  Copyright 2005-2016 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

/******************************************************************************
 *
 *  This file contains compile-time configurable constants for advanced
 *  audio/video
 *
 ******************************************************************************/

#include "bta/include/bta_av_cfg.h"

#include <cstdint>

#include "bta/include/bta_av_api.h"
#include "internal_include/bt_target.h"
#include "stack/include/avrc_api.h"

tBTA_AV_CFG p_bta_av_cfg;

const uint16_t bta_av_rc_id[] = {
    0x0000, /* bit mask: 0=SELECT, 1=UP, 2=DOWN, 3=LEFT,
                         4=RIGHT, 5=RIGHT_UP, 6=RIGHT_DOWN, 7=LEFT_UP,
                         8=LEFT_DOWN, 9=ROOT_MENU, 10=SETUP_MENU, 11=CONT_MENU,
                         12=FAV_MENU, 13=EXIT */

    0, /* not used */

    0x0000, /* bit mask: 0=0, 1=1, 2=2, 3=3,
                         4=4, 5=5, 6=6, 7=7,
                         8=8, 9=9, 10=DOT, 11=ENTER,
                         12=CLEAR */

    0x0000, /* bit mask: 0=CHAN_UP, 1=CHAN_DOWN, 2=PREV_CHAN, 3=SOUND_SEL,
                         4=INPUT_SEL, 5=DISP_INFO, 6=HELP, 7=PAGE_UP,
                         8=PAGE_DOWN */

/* btui_app provides an example of how to leave the decision of rejecting a
 command or not
 * based on which media player is currently addressed (this is only applicable
 for AVRCP 1.4 or later)
 * If the decision is per player for a particular rc_id, the related bit is
 clear (not set)
 * bit mask: 0=POWER, 1=VOL_UP, 2=VOL_DOWN, 3=MUTE, 4=PLAY, 5=STOP,
             6=PAUSE, 7=RECORD, 8=REWIND, 9=FAST_FOR, 10=EJECT, 11=FORWARD,
             12=BACKWARD */
#if (BTA_AV_RC_PASS_RSP_CODE == AVRC_RSP_INTERIM)
    0x0070, /* PLAY | STOP | PAUSE */
#else       /* BTA_AV_RC_PASS_RSP_CODE != AVRC_RSP_INTERIM */
    0x1b7E, /* PLAY | STOP | PAUSE | FF | RW | VOL_UP | VOL_DOWN | MUTE | FW |
               BACK */
#endif /* BTA_AV_RC_PASS_RSP_CODE */

    0x0000, /* bit mask: 0=ANGLE, 1=SUBPICT */

    0, /* not used */

    0x0000 /* bit mask: 0=not used, 1=F1, 2=F2, 3=F3,
                        4=F4, 5=F5 */
};

#if (BTA_AV_RC_PASS_RSP_CODE == AVRC_RSP_INTERIM)
const uint16_t bta_av_rc_id_ac[] = {
    0x0000, /* bit mask: 0=SELECT, 1=UP, 2=DOWN, 3=LEFT,
                         4=RIGHT, 5=RIGHT_UP, 6=RIGHT_DOWN,
               7=LEFT_UP,
                         8=LEFT_DOWN, 9=ROOT_MENU, 10=SETUP_MENU,
               11=CONT_MENU,
                         12=FAV_MENU, 13=EXIT */

    0, /* not used */

    0x0000, /* bit mask: 0=0, 1=1, 2=2, 3=3,
                         4=4, 5=5, 6=6, 7=7,
                         8=8, 9=9, 10=DOT, 11=ENTER,
                         12=CLEAR */

    0x0000, /* bit mask: 0=CHAN_UP, 1=CHAN_DOWN, 2=PREV_CHAN,
               3=SOUND_SEL,
                         4=INPUT_SEL, 5=DISP_INFO, 6=HELP,
               7=PAGE_UP,
                         8=PAGE_DOWN */

    /* btui_app provides an example of how to leave the decision of
     * rejecting a command or not
     * based on which media player is currently addressed (this is
     * only applicable for AVRCP 1.4 or later)
     * If the decision is per player for a particular rc_id, the
     * related bit is set */
    0x1800, /* bit mask: 0=POWER, 1=VOL_UP, 2=VOL_DOWN, 3=MUTE,
                         4=PLAY, 5=STOP, 6=PAUSE, 7=RECORD,
                         8=REWIND, 9=FAST_FOR, 10=EJECT, 11=FORWARD,
                         12=BACKWARD */

    0x0000, /* bit mask: 0=ANGLE, 1=SUBPICT */

    0, /* not used */

    0x0000 /* bit mask: 0=not used, 1=F1, 2=F2, 3=F3,
                        4=F4, 5=F5 */
};
uint16_t* p_bta_av_rc_id_ac = (uint16_t*)bta_av_rc_id_ac;
#else
uint16_t* p_bta_av_rc_id_ac = NULL;
#endif

uint16_t* p_bta_av_rc_id = (uint16_t*)bta_av_rc_id;
