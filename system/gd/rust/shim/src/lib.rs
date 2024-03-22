//! The main entry point for the legacy C++ code

mod init_flags;

// Force loading all of Crust code for Android build
#[allow(unused)]
#[cfg(target_os = "android")]
use crust::*;
