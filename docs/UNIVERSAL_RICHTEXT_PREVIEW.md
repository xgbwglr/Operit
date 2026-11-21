# Operit universal rich-text preview build

This branch modifies the **existing Operit Android app**, not a separate AI Shell.

## Current implementation

- Provider-independent normalization of GFM tables without boundary pipes.
- Support for tilde fenced-code blocks in completed messages.
- Stable code fence extraction, preserving inner fences and incomplete streams.
- Real UTF-8 code-file export via the Android document picker, available from code block toolbar where an activity is present.
- Editable visual checkboxes for GFM task lists.
- Graceful plain-text fallback if Markdown parsing fails.
- The existing Mermaid preview, LaTeX, table and XML-based tool UI remain in place.

Current limits:
- Full CommonMark/GFM parity, persisted task checkbox state, advanced chart cards and automatic workspace artifact discovery are not yet implemented.
- The new GFM compatibility transform runs for completed messages, not while tokens are still streaming.
- All changes are in the fork's `feat/universal-richtext-compat` branch; the default branch is unchanged.

## Build an installable debug APK with GitHub Actions

1. Open [Actions](https://github.com/xgbwglr/Operit/actions) in the fork.
2. GitHub usually disables Actions after forking. If prompted, enable workflows.
3. Open [Android Build](https://github.com/xgbwglr/Operit/actions/workflows/android-build.yml).
4. Choose **Run workflow**, branch `feat/universal-richtext-compat`, task `assembleDebug`.
5. After the job succeeds, open the run and download `operit-android-<run-number>` in **Artifacts**.
6. Extract the ZIP and install the included debug APK on an arm64 Android device.

The debug APK has the application ID suffix `.debug` and can coexist with the official release. It will have independent app data and must be configured separately.

The Android project needs its standard native toolchain, Node dependencies, Rust ripgrep build, Gradle and Android SDK. Use the checked-in workflow instead of trying a small Java-only Gradle build.

## Test cases for different providers

Ask at least two models (e.g. Gemini and DeepSeek or Claude and Qwen) to output:

- A GFM table with and without leading or trailing pipes.
- A Kotlin or Python fenced code block, including a fence inside the code.
- A tilde code fence with a language tag.
- A task list (`- [ ]`, `- [x]`).
- Display and inline LaTeX formulas.
- A Mermaid code block, then render and switch back to source.
- A large response; compare behavior during streaming and after completion.
- Export generated code using the file button, then open the saved file.
- Reopen the conversation and confirm the original Markdown is unchanged.

Do not install an unverified release APK over your official installation. The debug build is meant for safe, side-by-side testing.
