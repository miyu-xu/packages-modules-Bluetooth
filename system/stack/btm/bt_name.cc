

#include "stack/include/bt_name.h"

#include <string.h>

#include "osi/include/compat.h"

bool btm_loc_bd_name_is_set(const tBTM_LOC_BD_NAME& btm_loc_bd_name) {
  return btm_loc_bd_name[0] != 0;
}
const char* btm_loc_bd_name_text(const tBTM_LOC_BD_NAME& btm_loc_bd_name) {
  return (const char*)&btm_loc_bd_name;
}
size_t btm_loc_bd_name_length(const tBTM_LOC_BD_NAME& btm_loc_bd_name) {
  return strnlen((const char*)&btm_loc_bd_name, BTM_MAX_REM_BD_NAME_LEN);
}
size_t btm_loc_bd_name_set(tBTM_LOC_BD_NAME& btm_loc_bd_name,
                           const char* name) {
  return strlcpy((char*)&btm_loc_bd_name, name, BTM_MAX_LOC_BD_NAME_LEN);
}
