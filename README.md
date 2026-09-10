# WikiCS for Android

A native, minimal Android reader for [Wiki ФКН](https://wikics.site/Wiki_%D0%A4%D0%9A%D0%9D).
Choose a program and study year once; the app shows that cell of the wiki's course table.

## What the app includes

- Native Android screens with off-white surfaces, violet accents, line icons and dark mode.
- First-run program/year selection, persistent settings, and a program switch in Settings.
- All nine program rows and four study years from the supplied 2026/27 undergraduate table.
  The wiki's `DSBA` anchor is presented as **ПАД · DSBA**.
- Strict program/year filtering, search within that selection, and section chips for
  specializations, minors, electives or streams present in the selected cell.
- Module labels, unpublished-page badges, and a setting to hide unpublished courses.
- An in-app article reader with selectable text, section navigation, preserved external
  resources, readable tables that scroll sideways, MathML, and configurable text size.
- Bookmarks scoped to the selected program and study year. Opened article HTML is cached
  in private app storage. Saved text remains available without a connection.
- Background refresh, error states, retry actions, and fallback to the last valid cache.
- External websites, videos, PDFs and downloads open in the appropriate browser/app.

## Build and run

Requirements: **JDK 17+, Android SDK Platform 35, Android Build Tools 35.0.0**.
The app supports **Android 8.0 / API 26 and later** and targets API 35.
Pinned build tools: Android Gradle Plugin 8.9.2 and Gradle 8.11.1.
There are no third-party app runtime dependencies.

1. Install [Android Studio](https://developer.android.com/studio) and use its SDK Manager
   to install Android SDK Platform 35 and Build Tools 35.0.0.
2. Extract this project. Make JDK 17 or Android Studio's bundled JDK available as `java`
   (or set `JAVA_HOME` to the JDK directory).
3. Run the initial launcher from the project directory:

   ```sh
   # Linux / macOS
   chmod +x gradlew
   ./gradlew --version
   ```

   ```powershell
   # Windows PowerShell
   .\gradlew.bat --version
   ```

   The included source-visible bootstrap downloads the official Gradle distribution,
   verifies its published SHA-256, and generates the **standard Gradle wrapper** locally.
   Later invocations use that generated standard wrapper. The first run needs internet.

4. Open the `WikiCS-Android` directory in Android Studio, allow Gradle sync, then Run `app`
   on a connected phone or emulator. Android Studio normally creates `local.properties`
   with your SDK path. For terminal builds, alternatively set `ANDROID_HOME` to your SDK.
5. To produce an installable debug APK:

   ```sh
   ./gradlew :app:assembleDebug
   ```

   On Windows use `.\gradlew.bat :app:assembleDebug`.
   Output: `app/build/outputs/apk/debug/app-debug.apk`.
   This is a locally signed debug build for testing, not a Play Store release.

For a cloud build, put the project at the root of a GitHub repository. The included
`.github/workflows/android.yml` runs the core checks, builds the debug APK, runs Android
lint, and attaches `WikiCS-debug-apk` to the completed Actions run. That workflow has
been provided but has not been executed from this environment.

## Run the verified core checks

No SDK, Gradle download or external dependencies are required:

```sh
sh tools/test.sh
java tools/ParseJava.java
```

On Windows, use `tools\test.bat` for the first command. These commands require a JDK.
The first command compiles the actual production core and runs the tests; the second
only parses the Android source's Java syntax. It does not replace an Android build.

The tests also generate `test-output/deep-learning-reader.html` and
`test-output/stochastic-reader-dark.html`, which can be opened in a desktop browser
to inspect the actual generated reader markup. These are article previews, not
screenshots of the native Android app.

Pre-generated copies of those two reader previews are included as
`docs/reader-light.html` and `docs/reader-dark.html` for inspection without building.

## Implementation map

| File | Responsibility |
| --- | --- |
| `MainActivity.java` | Native screens, settings, navigation, search, reader lifecycle |
| `Ui.java` | Shared colors, spacing, typography, vector-like native line icons |
| `Bookmarks.java` | Local bookmark persistence |
| `core/CatalogParser.java` | The actual `table.courses` program rows and year columns |
| `core/Html.java` | Bounded HTML tree parser for MediaWiki's server-rendered markup |
| `core/ArticleParser.java` | Content extraction, TOC, unavailable-page detection, sanitization |
| `core/Article.java` | Reader HTML and light/dark styles |
| `core/WikiUrls.java` | Old-domain mapping, read URLs, external-link routing |
| `core/WikiRepository.java` | HTTPS requests, atomic cache writes, bundled snapshots |

Java and Android platform views keep the app independent of a backend and third-party
UI libraries. Only article content uses a WebView; program selection, course lists,
search, navigation and settings are native Android controls.

## Data handling and limitations

- No account, analytics, ad SDK, tracking service, JavaScript bridge, or storage permission.
- Network permission is used to read the public wiki. Android backup is disabled.
- Untrusted page scripts, forms and embedded frames are removed. Reader JavaScript is
  disabled, mixed content is blocked, and file/content URL access is disabled.
- Known `wiki.cs.hse.ru` links are mapped to the supplied `wikics.site` mirror. Both
  trusted hosts are permitted for HTTPS server redirects. External resource URLs keep
  their original destination and open through an Android intent.
- Red links are converted from `action=edit&redlink=1` to normal read URLs. The actual
  `.noarticletext` response is displayed as “not published,” with a retry action.
- Invalid refresh results never replace a successfully parsed catalog cache.
- Offline storage covers HTML/text, not a recursive download of images, PDFs, videos,
  private LMS pages, or linked materials. Remote images may need internet.
- The small parser is deliberately built around the supplied MediaWiki HTML, rather
  than a complete HTML5 browser parser. A future structural change to `table.courses`
  may require updating `CatalogParser`; malformed responses keep the prior catalog.
- Page HTML is retained in private storage until Android app data is cleared or the app
  is uninstalled. This first version has no automatic disk-cache eviction.
- Live requests could not be verified here because the wiki returned gateway errors.
- Before distributing a release, run the device checks in `docs/DEVICE-CHECKS.md` and
  create your own release signing key. The included source contains no private keys.

Build compatibility reference:
[Android Gradle Plugin 8.9 documentation](https://developer.android.com/build/releases/agp-8-9-0-release-notes).

App source is MIT licensed. The bundled wiki snapshots are supplied course content;
their authorship and licensing remain with their original authors. See `NOTICE.md`.
