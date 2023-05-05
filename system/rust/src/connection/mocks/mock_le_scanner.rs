use std::{cell::Cell, rc::Rc};

use crate::connection::le_scanner::{LeScanner, LeScannerFilterControls};

#[derive(Clone, Debug)]
pub struct MockLeScanner {
    targeted_announcement_filter_enabled: Rc<Cell<bool>>,
}

impl MockLeScanner {
    pub fn new() -> Self {
        Self { targeted_announcement_filter_enabled: Default::default() }
    }

    pub fn is_targeted_announcement_filter_enabled(&self) -> bool {
        self.targeted_announcement_filter_enabled.get()
    }
}

impl LeScanner for MockLeScanner {
    fn register_callbacks(
        &mut self,
        callbacks: impl crate::connection::le_scanner::LeScannerCallbacks + 'static,
    ) -> Result<(), ()> {
        // no-op
        Ok(())
    }
}

impl LeScannerFilterControls for MockLeScanner {
    fn set_targeted_announcement_filter_enabled(&mut self, enable: bool) {
        self.targeted_announcement_filter_enabled.set(enable);
    }
}
