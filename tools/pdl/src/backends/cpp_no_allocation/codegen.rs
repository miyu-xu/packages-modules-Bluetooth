use std::fmt::Write;

#[macro_export]
macro_rules! push {
    ( $e:expr, $($t:tt)* ) => {
        writeln!($e, $($t)*).unwrap()
    }
}

pub struct State {
    pub code: String,
    indent_level: usize,
    at_line_start: bool,
}

impl State {
    pub fn new() -> Self {
        Self { code: String::new(), indent_level: 0, at_line_start: true }
    }

    pub fn within_block<T>(&mut self, f: impl FnOnce(&mut Self) -> T) -> T {
        push!(self, "{{");
        self.indent();
        let ret = f(self);
        self.dedent();
        push!(self, "}};");
        push!(self, "");
        ret
    }

    fn indent(&mut self) {
        self.indent_level += 1;
    }

    fn dedent(&mut self) {
        self.indent_level -= 1;
    }
}

impl Write for State {
    fn write_str(&mut self, s: &str) -> std::fmt::Result {
        if self.at_line_start {
            for _ in 0..self.indent_level * 4 {
                self.code.write_char(' ')?;
            }
        }
        self.at_line_start = false;
        self.code.write_str(s)?;
        if s.ends_with('\n') {
            self.at_line_start = true
        }
        Ok(())
    }
}

pub fn generate_backing_int(width: usize) -> Result<&'static str, String> {
    Ok(match width {
        x if x <= 8 => "uint8_t",
        x if x <= 16 => "uint16_t",
        x if x <= 32 => "uint32_t",
        x if x <= 64 => "uint64_t",
        _ => return Err("width too large to be stored in uint_t primitive".to_string()),
    })
}

pub fn snake_to_camel(snake: &str) -> String {
    let mut camel = String::new();
    let mut needs_capital = true;
    for char in snake.chars() {
        if char == '_' {
            needs_capital = true;
            continue;
        }
        if needs_capital {
            camel.push(char.to_ascii_uppercase())
        } else {
            camel.push(char.to_ascii_lowercase())
        }
        needs_capital = false;
    }
    camel
}
