# WikiCS for Android

A native Android reader for [Wiki ФКН](https://wikics.site/Wiki_%D0%A4%D0%9A%D0%9D), built for students who want a simpler way to browse courses from their phone.

Choose your study program and year once, then browse only the courses relevant to that selection. Articles open inside the app, can be bookmarked, and remain available from cache after they have been opened.

## Features

- Native Android interface with light and dark themes
- Program and study-year selection saved between launches
- Course filtering and search
- Section chips for specializations, minors, electives and streams
- Unpublished-course indicators and an option to hide them
- Built-in article reader with section navigation, selectable text, tables and MathML
- Bookmarks for courses and articles
- Local caching with fallback to previously downloaded content when offline
- Background refresh and retry handling
- External websites, videos, PDFs and downloads open in the appropriate app

The app uses native Android views for navigation, settings, course lists and search. A WebView is used only for rendering article content.

## Build and run

Requirements:

- JDK 17+
- Android SDK Platform 35
- Android Build Tools 35.0.0
- Android 8.0 / API 26 or newer

The project targets API 35 and uses Android Gradle Plugin 8.9.2 and Gradle 8.11.1.

Clone the repository and run the Gradle launcher once:

```sh
# Linux / macOS
chmod +x gradlew
./gradlew --version
```

```powershell
# Windows
.\gradlew.bat --version
```

On the first run, the launcher downloads the official Gradle distribution and creates the standard Gradle wrapper locally.

Then open the project in Android Studio and run the `app` module on a device or emulator.

To build a debug APK from the command line:

```sh
./gradlew :app:assembleDebug
```

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The repository also includes a GitHub Actions workflow that runs the core checks, Android lint and a debug build.

## Core checks

Some of the parsing and reader logic can be tested without the Android SDK:

```sh
sh tools/test.sh
java tools/ParseJava.java
```

On Windows, use `tools\test.bat` instead of the shell script.

The tests also generate HTML previews of article rendering in `test-output/`. Pre-generated examples are available in `docs/reader-light.html` and `docs/reader-dark.html`.

## Project structure

| File | Responsibility |
| --- | --- |
| `MainActivity.java` | Screens, settings, navigation, search and reader lifecycle |
| `Ui.java` | Shared colors, spacing, typography and icons |
| `Bookmarks.java` | Local bookmark persistence |
| `core/CatalogParser.java` | Parses program rows and study-year columns from the wiki |
| `core/Html.java` | Lightweight HTML parser used by the reader |
| `core/ArticleParser.java` | Extracts and sanitizes article content |
| `core/Article.java` | Generates reader HTML and light/dark styles |
| `core/WikiUrls.java` | URL mapping and external-link handling |
| `core/WikiRepository.java` | Network requests, caching and bundled snapshots |

## Data handling

The app has no accounts, analytics, advertising SDKs or tracking.

It only requests network access to read the public wiki. Article HTML is stored in the app's private storage for offline use. JavaScript is disabled in the reader WebView, and external resources are opened outside the app when appropriate.

The parser is intentionally built around the current MediaWiki page structure rather than implementing a full HTML5 browser parser, so major changes to the wiki markup may require parser updates.

Before creating a signed release, see `docs/DEVICE-CHECKS.md` and use your own release signing key.

The app source is MIT licensed. Bundled wiki snapshots remain subject to the original content authors' licensing; see `NOTICE.md`.
