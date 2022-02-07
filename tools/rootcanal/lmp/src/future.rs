use std::sync::Arc;
use std::task::{Wake, Waker};

/// Pins a value on the stack
macro_rules! pin {
    ($($x:ident),* $(,)?) => { $(
        // Move the value to ensure that it is owned
        let mut $x = $x;
        // Shadow the original binding so that it can't be directly accessed
        // ever again.
        #[allow(unused_mut)]
        let mut $x = unsafe {
            std::pin::Pin::new_unchecked(&mut $x)
        };
    )* }
}

pub(crate) use pin;

pub struct NoopWaker;

impl Wake for NoopWaker {
    fn wake(self: Arc<Self>) {}
}

impl NoopWaker {
    #[allow(clippy::new_ret_no_self)]
    pub fn new() -> Waker {
        Arc::new(Self).into()
    }
}
