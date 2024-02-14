# Bluetooth Lint Checks for AOSP

Custom Android Lint checks are written here to be executed against Bluetooth
Java and Kotlin source code.

## How to run Bluetooth lint checks against the code base

While lint checks should be automatically run by Gerrit, you may want to run it manually on the
code base.

Step 1: Build the lint report:
```
m Bluetooth-lint
```

Step 2: Find your lint output:
```
croot;
find out/soong/.intermediates/packages/modules/Bluetooth/ -type f -name "*lint-report*" -print;
```

Or:
```
aninja -t query Bluetooth-lint
```

Step 3: Use the lint report directory and run this to validate the file:
```
m out/soong/.intermediates/packages/modules/Bluetooth/android/app/Bluetooth/android_common/lint/lint-report.html
```

Step 4: Open the file in your favorite web browser.

## How to run Bluetooth lint unit tests

Unit tests can be ran with the following command:
```
atest BluetoothLintCheckerTest --host
```

## Documentation

- [Android Lint Docs](https://googlesamples.github.io/android-custom-lint-rules/)
- [Custom lint creation example from Android SystemUI team](https://g3doc.corp.google.com/company/teams/android-sysui/general_guides/writing_a_linter.md?cl=head)
- [Lint Check Unit Testing](https://googlesamples.github.io/android-custom-lint-rules/api-guide/unit-testing.md.html)
- [Android Lint source files](https://source.corp.google.com/studio-main/tools/base/lint/libs/lint-api/src/main/java/com/android/tools/lint/)
- [PSI source files](https://github.com/JetBrains/intellij-community/tree/master/java/java-psi-api/src/com/intellij/psi)
- [UAST source files](https://upsource.jetbrains.com/idea-ce/structure/idea-ce-7b9b8cc138bbd90aec26433f82cd2c6838694003/uast/uast-common/src/org/jetbrains/uast)
- [IntelliJ plugin for viewing PSI tree of files](https://plugins.jetbrains.com/plugin/227-psiviewer)
