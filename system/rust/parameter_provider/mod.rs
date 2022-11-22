//! ParameterProvider holds global constants that do not change while the stack is running.

mod ffi;

#[allow(missing_docs)]
pub struct ParameterProvider {}

impl ParameterProvider {
    /// Constructor. Since this is a module, do not allow Default construction.
    #[allow(clippy::new_without_default)]
    pub fn new() -> Self {
        Self {}
    }

    /// Whether CommonCriteriaMode is enabled. It is passed in from Java at initial startup.
    pub fn is_common_criteria_mode(&self) -> bool {
        ffi::IsCommonCriteriaMode()
    }
}
