pub mod a2dp;
pub mod avrcp;
pub mod gatt;
pub mod hfp;
pub mod hid_host;
pub mod sdp;

use std::fmt::Debug;
use tokio::sync::mpsc::Sender;

pub struct Dispatcher<T> {
    pub dispatch: Box<dyn Fn(T) + Send>,
}

pub trait Callback {
    fn into_dispatcher(tx: Sender<Self>) -> Dispatcher<Self>
    where
        Self: Sized + Debug + Send + 'static,
    {
        let dispatch = Box::new(move |cb| {
            if let Err(cb) = tx.try_send(cb) {
                println!("Cannot send envet: {:?}", cb);
            }
        });
        Dispatcher { dispatch }
    }
}
