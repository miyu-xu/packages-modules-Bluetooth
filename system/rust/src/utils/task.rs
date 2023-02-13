//! This module provides utilities relating to async tasks

use std::future::Future;

use tokio::{runtime::Runtime, task::LocalSet};

pub fn block_on_locally<T>(f: impl Future<Output = T>) -> T {
    LocalSet::new().block_on(&Runtime::new().unwrap(), f)
}
