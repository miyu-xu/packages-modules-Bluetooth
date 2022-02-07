#ifndef LMP_H
#define LMP_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stddef.h>
#include <stdint.h>

/* Link Manager user operation
 * this should match the ABI of the LinkManagerOps struct
 * defined in src/manager.rs
 *
 * See src/manager.rs for documentation
 */
struct link_manager_ops {
  void* user_pointer;
  uint16_t (*get_handle)(void* user, uint8_t const address[6]);
  void (*get_address)(void* user, uint16_t handle, uint8_t result[6]);
  void (*extended_features)(void* user, uint8_t features_page,
                            uint8_t result[8]);

  void (*send_hci_event)(void* user, uint8_t const* data, size_t size);
  void (*send_lmp_packet)(void* user, uint8_t const to[6], uint8_t const* data,
                          size_t size);
};

/* See src/ffi.rs for documentation */
struct link_manager* link_manager_create(struct link_manager_ops ops);

/* See src/ffi.rs for documentation */
void link_manager_tick(struct link_manager* lm);

/* See src/ffi.rs for documentation */
void link_manager_add_link(struct link_manager* lm, uint8_t const peer[6]);

/* See src/ffi.rs for documentation */
void link_manager_remove_link(struct link_manager* lm, uint8_t const peer[6]);

/* See src/ffi.rs for documentation */
void link_manager_ingest_hci(struct link_manager* lm, uint8_t const* data,
                             size_t size);

/* See src/ffi.rs for documentation */
void link_manager_ingest_lmp(struct link_manager* lm, uint8_t const from[6],
                             uint8_t const* data, size_t size);

/* See src/ffi.rs for documentation */
void link_manager_destroy(struct link_manager* lm);

#ifdef __cplusplus
}
#endif

#endif /* LMP_H */
