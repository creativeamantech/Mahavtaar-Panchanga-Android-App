# Panchanga Power

https://github.com/creativeamantech/Mahavtaar-Panchangaa import this and set it up

This project was built with [Lovable](https://lovable.dev).

**Live app**: https://mahavtaarpanchanga.lovable.app

## Build with Lovable

Continue developing this project in the [Lovable editor](https://lovable.dev/projects/5b0879da-50ac-4251-9227-dcb6f065dd5d).

- **Ship faster**: describe what you want to build and Lovable handles the code.
- **Stay in sync**: every change made in Lovable is committed straight to this repository.
- **Full ownership**: this code is yours. Push to `main` on GitHub and your changes sync back into Lovable, ready for your next prompt.

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```

---

## Native Android Application (Mahavtaar Panchanga)

The native Android application (`com.mahavtaar.panchanga`) is a 100% offline-first, Clean Architecture + MVVM Kotlin & Jetpack Compose implementation preserving exact Drik Ganita scientific parity with the web engine via `io.github.cosinekitty:astronomy:2.1.19`.

### Module Architecture
- `:app` — Application shell, SplashScreen, adaptive `NavigationBar`/`NavigationRail`, type-safe Navigation Compose graph, Jetpack Glance home-screen widget, WorkManager notifications.
- `:core` — Coroutine dispatchers and deterministic timezone/HMS/DMS formatters.
- `:core-ui` — Material Design 3 Night Sky & Parchment themes, locally bundled Playfair Display / Plus Jakarta Sans / JetBrains Mono fonts, custom Compose `Canvas` lunar phase & solar arc visuals.
- `:core-database` — Room persistence (saved locations, offline Panchanga cache, Vrat reminders).
- `:core-location` — Google Play Services FusedLocationProviderClient + Haversine offline nearest-city matcher.
- `:astronomy-engine` — Pure Kotlin Drik Ganita Panchanga, Hora (24 unequal horas + 5 sub-Tattvas), Swara Yoga, Navagraha & transit scanner, verified against all 41 golden fixtures in `reference/panchanga/v1/`.
- `:panchanga-domain` — Domain models, repository contracts, and use cases.
- `:panchanga-data` — Preferences DataStore, offline global city catalog (`assets/cities.json`), Room-cached repository, shared date session holder.
- `:feature-home`, `:feature-daily-panchanga`, `:feature-calendar`, `:feature-hora`, `:feature-swara-yoga`, `:feature-settings` — Feature screens & Hilt ViewModels.

### Build & Verification Commands
```sh
# Verify all 41 golden reference parity fixtures + Hora/Swara edge cases
gradle :astronomy-engine:test

# Run Android Robolectric unit tests
gradle :app:testDebugUnitTest

# Build Debug APK
gradle :app:assembleDebug

# Build Release APK / Play Store AAB (set KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)
gradle :app:assembleRelease
gradle :app:bundleRelease
```

