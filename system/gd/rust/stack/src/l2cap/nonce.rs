#[derive(Copy, Clone, Eq, PartialEq, Debug, Hash)]
pub struct Nonce {
    value: u32,
}

pub struct NonceGenerator {
    next_val: u32,
}

impl NonceGenerator {
    pub fn new() -> Self {
        NonceGenerator { next_val: 0xfee1dead }
    }

    pub fn next(&mut self) -> Nonce {
        self.next_val += 1;
        Nonce { value: self.next_val }
    }
}
