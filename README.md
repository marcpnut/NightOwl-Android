# NightOwl for Android

A native Android phone client for the existing NightOwl server. This is a separate Git repository and does not modify the web app, webOS app, or server.

## Phone layout

- Home and What's New use vertically scrollable shelves, with horizontally swipeable cards sized for touch. Continue Watching is deduplicated by the server and its progress is shared across signed-in devices.
- Shows, movies, live TV, and search use two-column poster grids and paged results. Cards and buttons keep comfortable tap targets and readable labels.
- The bottom navigation stays at five primary destinations. Search, My List, and Settings are in the compact top bar; the logo and actions reduce slightly on narrow phones.
- Series details show season selectors and episode rows. Playback uses Android Media3, tries the provider stream first, asks before using a server remux after a failure, syncs progress, and offers the next episode near the end.
- Player controls are arranged for a phone: play/pause, 10-second skip controls, and the seek bar share one bottom row. Audio/subtitle selection and adding the playing show or movie to My List are available in the player. The app runs immersive with Android system bars hidden.
- Subtitle preferences and hidden Live TV/movie/show categories are stored with the server profile and sync across devices.

## Requirements

- JDK 17
- Android SDK Platform 37.0 and Build Tools 37.0.0
- Gradle 9.6 (the Gradle wrapper is included)

Build a debug APK with:

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Server and network

Enter your own NightOwl server address on the sign-in screen. Use HTTPS for account sign-in; the Android client stores the session token encrypted with Android Keystore. The provider can return HTTP media URLs; cleartext media is enabled for playback compatibility.

The app connects to the existing NightOwl API. It does not store Xtream credentials, modify the server database, or create user accounts.


## Downloads

Download the current Android phone APK [directly](https://github.com/marcpnut/NightOwl-Android/releases/download/v0.2.5/NightOwl-Android-0.2.5.apk) or view the [0.2.5 release notes](https://github.com/marcpnut/NightOwl-Android/releases/tag/v0.2.5). It is signed for sideloading.

