// pub fn add(left: usize, right: usize) -> usize {
//     left + right
// }

// #[cfg(test)]
// mod tests {
//     use super::*;

//     #[test]
//     fn it_works() {
//         let result = add(2, 2);
//         assert_eq!(result, 4);
//     }
// }

// Note carefully the AIDL crates structure:
// * the AIDL module name: "com_example_android_remoteservice"
// * next "::aidl"
// * next the AIDL package name "::com::example::android"
// * the interface: "::IRemoteService"
// * finally, the 'BnRemoteService' and 'IRemoteService' submodules

//! This module implements the IRemoteService AIDL interface
use binder::{BinderFeatures, Interface, Result as BinderResult, Strong};
use HapClient_remoteservice::aidl::android::bluetooth::IRemoteService::{
    BnRemoteService, IRemoteService,
};

/// This struct is defined to implement IRemoteService AIDL interface.
pub struct MyService;

impl Interface for MyService {}

impl IRemoteService for MyService {
    fn getPid(&self) -> BinderResult<i32> {
        Ok(42)
    }

    fn basicTypes(&self, _: i32, _: i64, _: bool, _: f32, _: f64, _: &str) -> BinderResult<()> {
        // Do something interesting...
        Ok(())
    }
}
