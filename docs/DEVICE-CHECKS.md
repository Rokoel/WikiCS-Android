# Device verification still to run

These checks require the Android SDK and a device/emulator; they were not performed
in the authoring environment. The JVM core checks were performed successfully.

1. Run `./gradlew :app:assembleDebug :app:lintDebug` and install the APK.
2. On first launch choose ПАД · DSBA, then 3 курс. Confirm 20 entries with no filter.
   Deep Learning and Stochastic Processes should open; Time Series should show the
   unpublished state. A refresh may change this count as the live wiki changes.
3. Select ПИ and then different years. Confirm that no DSBA courses remain in the list.
   Switch back to DSBA year 3; search “Calculus” should return no results.
4. Use the Finance specialization chip; the supplied snapshot should show three courses.
5. Open Deep Learning. Check the teacher table, contents anchors, text selection,
   zoom, and external LMS/Google Drive links. Check Stochastic Processes similarly.
6. Bookmark a page, close the app, enable airplane mode, and reopen it from Saved.
   Its text should load. An uncached page should show a clear retry/error screen.
7. Exercise Android Back, native reader Back, contents anchors, and rapid navigation
   while requests are in flight. Old requests must not replace a newer page.
8. Rotate on the home screen and while reading. Confirm profile, query, active tab,
   reader history and saved-page state. Check reader scroll restoration.
9. Check light, dark and system themes, large system font sizes, TalkBack, portrait,
   landscape and a tablet. Check inset spacing and keyboard behavior.
10. Run on API 26 and a recent Android version. If a linked resource needs login,
    confirm it opens in the user's normal browser rather than asking for credentials
    in the app reader.
