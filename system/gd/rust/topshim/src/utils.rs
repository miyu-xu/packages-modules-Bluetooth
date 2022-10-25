//! Topshim utils.

use std::ffi::CString;
use std::marker::PhantomData;
use std::os::raw::c_char;

/// Lifetime-checked const pointer wrapper.
///
/// The wrapper holds the raw pointer and clones the lifetime from the pointing object,
/// which forces the compiler to check and fail when the wrapper lives longer than the data.
///
/// Example 1:
///     Get the pointer with from_ref(), and pass the pointer into() a C function.
///     ```
///     // let uuid: Option<Uuid>;
///     let uuid_ptr = match uuid {
///         Some(ref u) => LTCheckedPtr::from_ref(u),
///         None => LTCheckedPtr::null(),
///     };
///     // The pointer type would be `*const Uuid`.
///     ccall!(self, foo, uuid_ptr.into());
///     ```
///
/// Example 2:
///     Get the pointer from() a array-like type, such as slice, Vec, and String.
///     Cast and pass the pointer into a C function with cast_into().
///     ```
///     // let profile: Vec<u8>;
///     let profile_ptr = LTCheckedPtr::from(&profile);
///     // The pointer type would be `*const c_char`.
///     ccall!(self, bar, profile_ptr.cast_into::<c_char>());
///     ```
pub(crate) struct LTCheckedPtr<'a, T> {
    ptr: *const T,
    _covariant: PhantomData<&'a ()>,
}

impl<T> LTCheckedPtr<'static, T> {
    /// Returns a null pointer, which has static lifetime.
    pub(crate) fn null() -> Self {
        Self { ptr: std::ptr::null(), _covariant: PhantomData }
    }
}

impl<'a, T> LTCheckedPtr<'a, T> {
    /// Constructs a lifetime-checked constant pointer from a reference.
    pub(crate) fn from_ref(val: &'a T) -> Self {
        Self { ptr: val, _covariant: PhantomData }
    }

    /// Returns the casted raw constant pointer.
    pub(crate) fn cast_into<CT>(self) -> *const CT {
        self.ptr as *const CT
    }
}

impl<'a, T> Into<*const T> for LTCheckedPtr<'a, T> {
    fn into(self) -> *const T {
        self.ptr
    }
}

impl<'a, T> From<&'a [T]> for LTCheckedPtr<'a, T> {
    fn from(val: &'a [T]) -> Self {
        Self { ptr: val.as_ptr(), _covariant: PhantomData }
    }
}

impl<'a, T> From<&'a Vec<T>> for LTCheckedPtr<'a, T> {
    fn from(val: &'a Vec<T>) -> Self {
        Self { ptr: val.as_ptr(), _covariant: PhantomData }
    }
}

impl<'a, T> From<&'a Option<T>> for LTCheckedPtr<'a, T> {
    fn from(val: &'a Option<T>) -> Self {
        match val {
            Some(ref v) => Self { ptr: v, _covariant: PhantomData },
            None => LTCheckedPtr::null(),
        }
    }
}

impl<'a> From<&'a CString> for LTCheckedPtr<'a, c_char> {
    fn from(val: &'a CString) -> Self {
        Self { ptr: val.as_ptr(), _covariant: PhantomData }
    }
}

/// Lifetime-checked mutable pointer wrapper.
///
/// The wrapper holds the raw pointer and clones the lifetime from the pointing object,
/// which forces the compiler to check and fail when the wrapper lives longer than the data.
///
/// Example 1:
///     Get the pointer with from_ref(), and pass the pointer into() a C function.
///     ```
///     // let callbacks: Box<bt_callbacks_t>;
///     let cb_ptr = LTCheckedPtrMut::from_ref(&mut *callbacks);
///     // The pointer type would be `*mut bt_callbacks_t`.
///     ccall!(self, foo, cb_ptr.into());
///     ```
///
/// Example 2:
///     Get the pointer from() a array-like type, such as slice, Vec, and String.
///     Cast and pass the pointer into a C function with cast_into().
///     ```
///     // let mut report: [u8];
///     let report_ptr = LTCheckedPtrMut::from(&mut report);
///     // The pointer type would be `*mut c_char`.
///     ccall!(self, bar, report_ptr.cast_into::<c_char>());
///     ```
pub(crate) struct LTCheckedPtrMut<'a, T> {
    ptr: *mut T,
    _covariant: PhantomData<&'a ()>,
}

impl<'a, T> LTCheckedPtrMut<'a, T> {
    /// Constructs a lifetime-checked mutable pointer from a reference.
    pub(crate) fn from_ref(val: &'a mut T) -> Self {
        Self { ptr: val, _covariant: PhantomData }
    }

    /// Returns the casted raw mutable pointer.
    pub(crate) fn cast_into<CT>(self) -> *mut CT {
        self.ptr as *mut CT
    }
}

impl<'a, T> Into<*mut T> for LTCheckedPtrMut<'a, T> {
    fn into(self) -> *mut T {
        self.ptr
    }
}

impl<'a, T> From<&'a mut [T]> for LTCheckedPtrMut<'a, T> {
    fn from(val: &'a mut [T]) -> Self {
        Self { ptr: val.as_mut_ptr(), _covariant: PhantomData }
    }
}

impl<'a> From<&'a mut String> for LTCheckedPtrMut<'a, u8> {
    fn from(val: &'a mut String) -> Self {
        Self { ptr: val.as_mut_ptr(), _covariant: PhantomData }
    }
}
