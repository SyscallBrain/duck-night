# Contributing to Duck Night

Thanks for your interest in Duck Night. Bug reports, translations, accessibility feedback and pull requests are welcome.

## Reporting bugs

Please open an issue with:

- what you did, what you expected and what happened instead;
- the screen or mode where it happened, and the difficulty if it applies;
- your device model and Android version;
- a screenshot when the problem is visual.

## Development setup

1. Install a JDK (17 or newer) and the Android SDK with platform 37.
2. Point Gradle at them:

   ```bash
   export JAVA_HOME=/path/to/jdk
   export ANDROID_HOME=/path/to/android-sdk
   ```

3. Build and test:

   ```bash
   ./gradlew :core:test :leveltool:test :app:testDebugUnitTest
   ./gradlew :app:assembleDebug
   ```

   Or run everything the CI runs, including lint and the permission and dependency checks:

   ```bash
   bash tools/verify.sh
   ```

No emulator is needed. The simulation lives in `:core` and is tested on the JVM. UI logic that can be separated from Compose (theme tokens, string resources, geometry) is tested on the JVM too; the rest is checked on a real device.

## Commit messages

- Write messages in English, in the imperative mood, with a type prefix: `feat:`, `fix:`, `content:`, `build:`, `ci:`, `docs:`, `test:`, `refactor:`, `chore:` (for example `feat: add the photo resolution`).
- **Do not add AI attribution.** No `Co-Authored-By` trailers, no "Generated with" lines and no tool names in the message.
- Enable the commit hook once per clone. It rejects those lines and checks the prefix:

  ```bash
  git config core.hooksPath tools/git-hooks
  ```

  Do not bypass it with `--no-verify`.

## Pull requests

- Create a branch from `develop` and keep each pull request focused on one change.
- Make sure `bash tools/verify.sh` passes and say in the description how you tested the change, including the device for UI changes.
- Add or update unit tests when you change the simulation, the photographer model, persistence or content tooling.
- Keep commits small.

## Coding conventions

- New code is in English: identifiers, comments, log and error messages.
- Every text the player sees goes in `app/src/main/res/values/strings.xml` (English), with a translation in `values-pt/strings.xml` (European Portuguese). Do not hardcode visible text in Kotlin.
- Colour values live only in `app/src/main/java/pt/tiago/ducknight/ui/theme/`. Screens read the tokens from `DuckTheme`.
- Match the style of the surrounding code. Comments explain *why*, not *what*.
- `:core` must stay free of Android, clock and platform-random dependencies, so that results are reproducible. Randomness comes from the versioned `SplitMix64` only.
- The simulation uses integer fixed-point arithmetic only. Do not add `Math.sin`, `Math.cos`, `Math.sqrt`, `Float` or `Double` to the simulation path.
- Any change to the simulation or the scoring needs a new `SIM_VERSION`; any change to the simulated photographer needs a new `BOT_VERSION`. Both require regenerating the affected certificates.
- The game is about photography. Never add a mechanic that shoots at living birds.

## Permissions and dependencies

Duck Night requests only `VIBRATE` and has no network permission. Do not add permissions, analytics, ads, Google Play Services or Firebase, and keep the release build free of non-free dependencies. `tools/check_permissions.sh` and `tools/check_deps.sh` enforce this.

## Translations

To add a language, copy `app/src/main/res/values/strings.xml` to `values-<code>/`, translate every translatable string and plural, and leave the strings marked `translatable="false"` out. Add the locale to `app/src/main/res/xml/locales_config.xml`. `StringsParityTest` checks that the keys and placeholders match. Check the result on a device, because Portuguese and English lengths differ.

## License

By contributing, you agree that your contributions are licensed under the [MIT License](LICENSE).
