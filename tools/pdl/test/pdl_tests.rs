use std::fs;
use std::io::Write;
use std::process::Command;
use std::process::Stdio;

fn strip_blank_lines(text: &str) -> String {
    text.lines().filter(|line| !line.trim().is_empty()).collect::<Vec<_>>().join("\n")
}

fn rustfmt(code: &str) -> String {
    let mut rustfmt = Command::new("rustfmt")
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .spawn()
        .expect("could not start rustfmt");

    // Write stdin in a separate thread to avoid deadlocks in case we
    // write faster than rustfmt reads.
    let mut stdin = rustfmt.stdin.take().unwrap();
    let owned_code = String::from(code);
    std::thread::spawn(move || {
        stdin.write_all(owned_code.as_bytes()).unwrap();
    });

    let output = rustfmt.wait_with_output().expect("rustfmt failed");
    assert!(output.status.success(), "rustfmt failure: {:?}, input:\n{}", output.status, code);
    String::from_utf8(output.stdout).unwrap()
}

fn pdl(pdl: &str) -> String {
    let tempdir = tempfile::tempdir().unwrap();
    let input = tempdir.path().join("code.pdl");
    fs::write(&input, pdl.as_bytes()).unwrap();
    let output = Command::new("pdl")
        .arg("--output-format")
        .arg("rust")
        .arg(input)
        .output()
        .expect("pdl failed");
    assert!(output.status.success(), "pdl failure: {:?}, input:\n{}", output, pdl);
    String::from_utf8(output.stdout).unwrap()
}

fn bluetooth_packetgen(pdl: &str) -> String {
    let tempdir = tempfile::tempdir().unwrap();
    let tempdir_path = tempdir.path().to_str().unwrap();
    let input = tempdir.path().join("code.pdl");
    let output = input.with_extension("rs");
    fs::write(&input, pdl.as_bytes()).unwrap();
    let status = Command::new("bluetooth_packetgen")
        .arg(&format!("--include={}", tempdir_path))
        .arg(&format!("--out={}", tempdir_path))
        .arg("--rust")
        .arg(input)
        .status()
        .expect("bluetooth_packetgen failed");
    assert!(status.success(), "bluetooth_packetgen failure: {:?}, input:\n{}", status, pdl);
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
