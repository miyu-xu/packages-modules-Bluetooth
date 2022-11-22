mod ffi;

pub struct ParameterProvider {}

impl ParameterProvider {
    pub fn new() -> Self {
        Self {}
    }
    pub fn is_common_criteria_mode(&self) -> bool {
        ffi::IsCommonCriteriaMode()
    }
}
