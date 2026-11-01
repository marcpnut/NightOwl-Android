# NightOwl for Android

A native Android phone client for the existing NightOwl server. This is a separate Git repository and does not modify the web app, webOS app, or server.

## Phone layout

- Home and What's New use vertically scrollable shelves, with horizontally swipeable cards sized for touch.
- Shows, movies, live TV, and search use two-column poster grids and paged results. Cards and buttons keep comfortable tap targets and readable labels.
- The bottom navigation stays at five primary destinations. Search, My List, and Settings are in the compact top bar; the logo and actions reduce slightly on narrow phones.
- Series details show season selectors and episode rows. Playback uses Android Media3 and supports the server's resolved provider streams, watch progress, and the next-episode prompt.

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

The sign-in form defaults to `https://[redacted]`. The Android client requires HTTPS for account sign-in and stores the session token encrypted with Android Keystore. The provider can return HTTP media URLs; cleartext media is enabled for playback compatibility. Use a trusted server URL and network when signing in.

The app connects to the existing NightOwl API. It does not store Xtream credentials, modify the server database, or create user accounts.
