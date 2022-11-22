pub trait ObjectProxy<'b> {
    type T: 'b + ?Sized;

    fn with<F: for<'a> FnOnce(&'a mut Self::T) + Send + 'b>(&self, f: F);

    fn specialize<U: ?Sized, F: Fn(&mut Self::T) -> &mut U + Send + 'b + Copy>(
        &self,
        f: F,
    ) -> WrappingObjectProxy<Self, F> {
        WrappingObjectProxy { wrapped: self, func: f }
    }

    fn type_erase<'a>(&'a self) -> DynamicObjectProxy<'a, 'b, Self::T>
    where
        Self: Sized,
    {
        return DynamicObjectProxy { proxy: self };
    }
}

pub struct WrappingObjectProxy<'a, W: ?Sized, F> {
    wrapped: &'a W,
    func: F,
}

impl<'a, 'b, U: ?Sized, W: ObjectProxy<'b> + ?Sized, F: Fn(&mut W::T) -> &mut U + Sync>
    ObjectProxy<'b> for WrappingObjectProxy<'a, W, F>
where
    W::T: 'a + 'b,
    U: 'a + 'b,
    F: Copy + Send + 'b,
{
    type T = U;

    fn with<G: for<'d> FnOnce(&'d mut Self::T) + Send + 'b>(&self, f: G) {
        let func = self.func;
        self.wrapped.with(move |x| f(func(x)))
    }
}

pub trait DynObjectProxy<'b> {
    type T: 'b + ?Sized;

    fn with_dyn(&self, f: Box<dyn for<'a> FnOnce(&'a mut Self::T) + Send + 'b>);
}

impl<'b, T> DynObjectProxy<'b> for T
where
    T: ObjectProxy<'b>,
{
    type T = <Self as ObjectProxy<'b>>::T;

    fn with_dyn(&self, f: Box<dyn for<'a> FnOnce(&'a mut Self::T) + Send + 'b>) {
        self.with(f)
    }
}

pub struct DynamicObjectProxy<'reference, 'underlying, T: ?Sized> {
    proxy: &'reference dyn DynObjectProxy<'underlying, T = T>,
}

impl<'reference, 'underlying, T: ?Sized> DynamicObjectProxy<'reference, 'underlying, T>
where
    'underlying: 'reference,
{
    pub fn with(&self, f: impl for<'view> FnOnce(&'view mut T) + Send + 'underlying)
    where
        T: 'underlying,
    {
        self.proxy.with_dyn(Box::new(move |x| f(x)))
    }
}
