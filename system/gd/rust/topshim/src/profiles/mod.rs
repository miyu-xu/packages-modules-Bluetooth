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

impl<T> From<Sender<T>> for Dispatcher<T>
where
    T: Sized + Send + Debug + 'static,
{
    fn from(tx: Sender<T>) -> Self {
        let dispatch = Box::new(move |cb| {
            if let Err(cb) = tx.try_send(cb) {
                println!("Cannot send event: {:?}", cb);
            }
        });
        Dispatcher { dispatch }
    }
}
