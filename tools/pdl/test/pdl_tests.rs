use std::fs;
use std::io::Write;
use std::process::{Command, Stdio};

fn strip_blank_lines(text: &str) -> String {
    text.lines().filter(|line| !line.trim().is_empty()).collect::<Vec<_>>().join("\n")
}

/// Run `code` through `rustfmt`.
///
/// # Panics
///
/// Panics if `rustfmt` cannot be found in the same directory as the
/// test binary or if it returns a non-zero exit code.
pub fn rustfmt(input: &str) -> String {
    // We expect to find `rustfmt` as a sibling to the test
    // executable. It ends up there when referenced using the `data`
    // property in an Android.pb file.
    let mut rustfmt_path = std::env::current_exe().unwrap();
    rustfmt_path.set_file_name("rustfmt");
    let mut rustfmt = Command::new(&rustfmt_path)
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .spawn()
        .expect("failed to start rustfmt");

    let mut stdin = rustfmt.stdin.take().expect("rustfmt stdin was None");
    let input = String::from(input);
    std::thread::spawn(move || {
        stdin.write_all(input.as_bytes()).expect("could not write to stdin");
    });

    let output = rustfmt.wait_with_output().expect("error executing rustfmt");
    assert!(output.status.success(), "rustfmt failed: {}", output.status);
    String::from_utf8(output.stdout).expect("rustfmt output was not UTF-8")
}

/// Run `code` through `pdl`.
///
/// # Panics
///
/// Panics if `pdl` cannot be found on `$PATH` or if it returns a
/// non-zero exit code.
fn pdl(code: &str) -> String {
    let tempdir = tempfile::tempdir().unwrap();
    let input = tempdir.path().join("input.pdl");
    fs::write(&input, code.as_bytes()).unwrap();
    let mut pdl_path = std::env::current_exe().unwrap();
    pdl_path.set_file_name("rustfmt");
    let output = Command::new(&pdl_path)
        .arg("--output-format")
        .arg("rust")
        .arg(input)
        .output()
        .expect("pdl failed");
    assert!(output.status.success(), "pdl failure: {:?}, input:\n{}", output, code);
    String::from_utf8(output.stdout).unwrap()
}

/// Run `code` through `bluetooth_packetgen`.
///
/// # Panics
///
/// Panics if `bluetooth_packetgen` cannot be found on `$PATH` or if
/// it returns a non-zero exit code.
fn bluetooth_packetgen(code: &str) -> String {
    let tempdir = tempfile::tempdir().unwrap();
    let tempdir_path = tempdir.path().to_str().unwrap();
    let input = tempdir.path().join("input.pdl");
    let output = input.with_extension("rs");
    fs::write(&input, code.as_bytes()).unwrap();
    let mut bluetooth_packetgen_path = std::env::current_exe().unwrap();
    bluetooth_packetgen_path.set_file_name("rustfmt");
    let status = Command::new(&bluetooth_packetgen_path)
        .arg(&format!("--include={}", tempdir_path))
        .arg(&format!("--out={}", tempdir_path))
        .arg("--rust")
        .arg(input)
        .status()
        .expect("bluetooth_packetgen failed");
    assert!(status.success(), "bluetooth_packetgen failure: {:?}, input:\n{}", status, code);
    fs::read_to_string(output).unwrap()
}

#[test]
fn test_prelude() {
    let pdl_code = r#"
little_endian_packets
"#;
    let new_rust = rustfmt(&pdl(pdl_code));
    let old_rust = rustfmt(&bluetooth_packetgen(pdl_code));
    assert_eq!(strip_blank_lines(&new_rust), strip_blank_lines(&old_rust));
}

#[test]
fn test_simple_le_packet() {
    let pdl_code = r#"
        little_endian_packets

        packet Foo {
          a: 8,
          b: 16,
        }
    "#;

    let new_rust = rustfmt(&pdl(pdl_code));
    let old_rust = rustfmt(&bluetooth_packetgen(pdl_code));
    assert_eq!(strip_blank_lines(&new_rust), strip_blank_lines(&old_rust));
}
