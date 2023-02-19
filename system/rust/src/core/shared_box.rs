//! Wrapper around Rc<> to make ownership clearer
//!
//! The idea is to have ownership represented by a SharedBox<T>.
//! Temporary ownership can be held using a WeakBox<T>, which should
//! not be held across async points. This reduces the risk of accidental
//! lifetime extension.

use std::{
    ops::Deref,
    rc::{Rc, Weak},
};

#[derive(Debug)]
pub struct SharedBox<T: ?Sized>(Rc<T>);

impl<T> SharedBox<T> {
    pub fn new(t: T) -> Self {
        Self(t.into())
    }

    pub fn downgrade(&self) -> WeakBox<T> {
        WeakBox(Rc::downgrade(&self.0))
    }

    pub fn as_ref(&self) -> WeakBoxRef<T> {
        WeakBoxRef(self.0.deref(), Rc::downgrade(&self.0))
    }
}

impl<T> From<T> for SharedBox<T> {
    fn from(value: T) -> Self {
        Self(value.into())
    }
}

impl<T> Deref for SharedBox<T> {
    type Target = T;

    fn deref(&self) -> &Self::Target {
        self.0.deref()
    }
}

pub struct WeakBox<T>(Weak<T>);

impl<T> WeakBox<T> {
    pub fn with<U>(&self, f: impl FnOnce(Option<WeakBoxRef<T>>) -> U) -> U {
        f(self.0.upgrade().as_deref().map(|x| WeakBoxRef(x, self.0.clone())))
    }
}

pub struct WeakBoxRef<'a, T>(&'a T, Weak<T>);

impl<'a, T> WeakBoxRef<'a, T> {
    pub fn downgrade(&self) -> WeakBox<T> {
        WeakBox(self.1.clone())
    }
}

impl<'a, T> Deref for WeakBoxRef<'a, T> {
    type Target = T;

    fn deref(&self) -> &Self::Target {
        self.0
    }
}
