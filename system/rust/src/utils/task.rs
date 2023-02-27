//! This module provides utilities relating to async tasks, typically for usage
//! only in test

use std::{future::Future, time::Duration};

use tokio::{
    runtime::Builder,
    select,
    task::{spawn_local, LocalSet},
};

/// Run the supplied future on a single-threaded runtime
pub fn block_on_locally<T>(f: impl Future<Output = T>) -> T {
    LocalSet::new().block_on(
        &Builder::new_current_thread().enable_time().build().unwrap(),
        async move {
            tokio::time::pause();
            f.await
        },
    )
}

/// Check if the supplied future immediately resolves.
/// Returns Ok(T) if it resolves, or Err(JoinHandle<T>) if it does not.
/// Correctly handles spurious wakeups (unlike Future::poll).
///
/// MUST only be run in an environment where time is mocked.
pub async fn try_await<T: 'static>(
    f: impl Future<Output = T> + 'static,
) -> Result<T, impl Future<Output = T>> {
    let mut handle = spawn_local(f);

    select! {
        t = &mut handle => Ok(t.unwrap()),
        _ = tokio::time::sleep(Duration::from_secs(1000)) => {
            Err(async { handle.await.unwrap() })
        },
    }
}
