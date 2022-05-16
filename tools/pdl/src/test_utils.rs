//! Various utility functions used in tests.

use crate::ast;
use crate::parser::parse_inline;
use std::fs;
use std::io::Write;
use std::path::Path;
use std::process::{Command, Stdio};
use tempfile::NamedTempFile;

/// Parse a string fragment as a PDL file.
///
/// # Panics
///
/// Panics on parse errors.
pub fn parse_str(text: &str) -> ast::Grammar {
    let mut db = ast::SourceDatabase::new();
    parse_inline(&mut db, String::from("stdin"), String::from(text)).expect("parse error")
}

/// Run `input` through `rustfmt`.
///
/// # Panics
///
/// Panics if `rustfmt` cannot be found in the same directory as the
/// test executable or if it returns a non-zero exit code.
pub fn rustfmt(input: &str) -> String {
    let mut rustfmt_path = std::env::current_exe().unwrap();
    rustfmt_path.set_file_name("rustfmt");
    let mut rustfmt = Command::new(&rustfmt_path)
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .spawn()
        .unwrap_or_else(|_| panic!("failed to start {:?}", &rustfmt_path));

    let mut stdin = rustfmt.stdin.take().unwrap();
    // Owned copy which we can move into the writing thread.
    let input = String::from(input);
    std::thread::spawn(move || {
        stdin.write_all(input.as_bytes()).expect("could not write to stdin");
    });

    let output = rustfmt.wait_with_output().expect("error executing rustfmt");
    assert!(output.status.success(), "rustfmt failed: {}", output.status);
    String::from_utf8(output.stdout).expect("rustfmt output was not UTF-8")
}

/// Compare two strings using `diff`
///
/// # Panics
///
/// Panics if `diff` cannot be found on `$PATH` or if it returns an
/// error.
pub fn diff(left: &str, right: &str) -> String {
    let mut temp_left = NamedTempFile::new().unwrap();
    temp_left.write_all(left.as_bytes()).unwrap();
    let mut temp_right = NamedTempFile::new().unwrap();
    temp_right.write_all(right.as_bytes()).unwrap();

    // We expect `diff` to be available on PATH.
    let output = Command::new("diff")
        .arg("--unified")
        .arg("--label")
        .arg("left")
        .arg("--label")
        .arg("right")
        .arg(temp_left.path())
        .arg(temp_right.path())
        .output()
        .expect("failed to run diff");
    let diff_trouble_exit_code = 2; // from diff(1)
    assert_ne!(
        output.status.code().unwrap(),
        diff_trouble_exit_code,
        "diff failed: {}",
        output.status
    );
    String::from_utf8(output.stdout).expect("diff output was not UTF-8")
}

/// Compare two strings and output a diff if they are not equal.
#[track_caller]
pub fn assert_eq_with_diff(left: &str, right: &str) {
    assert!(
        left == right,
        "texts did not match, left:\n{}\n\n\
             right:\n{}\n\n\
             diff:\n{}\n",
        left,
        right,
        diff(left, right)
    );
}

/// Compare a string with a snapshot file.
///
/// Updates the snapshot file if the `UPDATE_SNAPSHOTS` environment
/// variable is set.
#[track_caller]
pub fn assert_snapshot_eq<P: AsRef<Path>>(snapshot_path: P, actual_content: &str) {
    let snapshot_content = fs::read(&snapshot_path).unwrap_or_else(|err| {
        panic!("Could not read snapshot from {}: {}", snapshot_path.as_ref().display(), err)
    });
    let snapshot_content = String::from_utf8(snapshot_content).expect("Snapshot was not UTF-8");

    if std::env::var("UPDATE_SNAPSHOTS").is_ok() {
        if actual_content != snapshot_content {
            println!(
                "Updating snapshot {}: {} -> {} bytes",
                snapshot_path.as_ref().display(),
                snapshot_content.len(),
                actual_content.len()
            );
            fs::write(&snapshot_path, actual_content).unwrap_or_else(|err| {
                panic!("Could not write snapshot to {}: {}", snapshot_path.as_ref().display(), err)
            });
        }
    } else {
        assert_eq_with_diff(actual_content, &snapshot_content);
    }
}
