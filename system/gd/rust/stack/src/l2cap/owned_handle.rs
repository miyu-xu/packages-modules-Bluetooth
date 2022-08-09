use tokio::task::JoinHandle;

pub struct OwnedHandle<T> {
    handle: JoinHandle<T>,
}

impl<T> OwnedHandle<T> {
    pub fn new(handle: JoinHandle<T>) -> Self {
        Self { handle }
    }
}

impl<T> Drop for OwnedHandle<T> {
    fn drop(&mut self) {
        self.handle.abort();
    }
}
