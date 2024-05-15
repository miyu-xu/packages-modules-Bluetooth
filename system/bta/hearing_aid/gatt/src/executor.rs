use std::cell::RefCell;
use std::future::Future;
use std::os::fd::{AsRawFd, RawFd};
use std::pin::Pin;
use std::sync::Arc;
use std::task::{Context, Poll, Wake};

use nix::sys::eventfd::{EfdFlags, EventFd};

struct EventFdWaker(EventFd);

impl EventFdWaker {
    fn clear(&self) {
        let _ = self.0.read();
    }
}

impl Wake for EventFdWaker {
    fn wake(self: Arc<Self>) {
        self.0.write(1).unwrap();
    }
}

struct Task {
    future: Pin<Box<dyn Future<Output = ()>>>,
    waker: Arc<EventFdWaker>,
}

thread_local! {
    static TASK: RefCell<Option<Task>> = RefCell::new(None);
}

pub fn setup(future: impl Future<Output = ()> + 'static) -> nix::Result<RawFd> {
    let eventfd = EventFd::from_value_and_flags(1, EfdFlags::EFD_NONBLOCK | EfdFlags::EFD_CLOEXEC)?;
    let raw_eventfd = eventfd.as_raw_fd();

    TASK.set(Some(Task { future: Box::pin(future), waker: Arc::new(EventFdWaker(eventfd)) }));

    Ok(raw_eventfd)
}

pub fn poll() -> Poll<()> {
    TASK.with_borrow_mut(|task| {
        let task = task.as_mut().expect("No task registered for this thread");
        task.waker.clear();
        let waker = task.waker.clone().into();
        let mut context = Context::from_waker(&waker);
        task.future.as_mut().poll(&mut context)
    })
}
