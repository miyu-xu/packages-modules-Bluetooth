use tokio::task::JoinHandle;

#[derive(Debug)]
pub struct OwnedHandle<T> {
    handle: JoinHandle<T>,
}

impl<T> From<JoinHandle<T>> for OwnedHandle<T> {
    fn from(handle: JoinHandle<T>) -> Self {
        Self { handle }
    }
}

impl<T> Drop for OwnedHandle<T> {
    fn drop(&mut self) {
        self.handle.abort();
    }
}
