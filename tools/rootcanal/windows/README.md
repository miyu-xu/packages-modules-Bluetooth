# RootCanal standalone Windows support

This directory contains the Windows host shim used by HD's
`hd-rootcanal-adapter`. It embeds selected AOSP RootCanal controller sources in
the Rust executable; it does not build the upstream Linux `root-canal` desktop
program.

- `ffi_windows.cc` exposes the controller lifecycle, H4 input/output, link-layer
  input/output, and tick operations through a small C ABI.
- `crypto_windows.cc` implements RootCanal AES-128 with Windows CNG (`bcrypt`).
- `generated/` contains the packet runtime and C++ packet headers needed by a
  standalone Cargo build without the AOSP Soong generator pipeline.
- `../rust/{hci,llcp,lmp}_packets.rs` are the matching pre-generated Rust packet
  modules consumed by the `rootcanal-rs` build script.

The generated sources must be regenerated from their corresponding PDL files
when those specifications change. Do not edit their generated declarations by
hand.

From the HD workspace, validate the integration with:

```powershell
cargo test -p hd-rootcanal-adapter --target x86_64-pc-windows-gnu
cargo run --target x86_64-pc-windows-gnu -p xtask -- smoke
```
