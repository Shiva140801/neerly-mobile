# neerly-mobile

Android super-app for **Neerly** — a three-sided water-delivery marketplace launching in Hyderabad. One APK, four roles (customer · vendor · driver · admin-light).

## Stack

- Kotlin 2.0
- Jetpack Compose
- min SDK 24 · target SDK 34
- Hilt · Retrofit + Moshi · Coroutines · Firebase Auth + Messaging · Google Maps Compose · EncryptedSharedPreferences · DataStore · Coil · Timber

## Quick start

```bash
# 1. Open in Android Studio Koala+
# 2. Drop google-services.json into app/
# 3. Uncomment the google-services plugin in app/build.gradle.kts
# 4. Download Plus Jakarta Sans + Instrument Serif + JetBrains Mono TTFs into app/src/main/res/font/
# 5. Fill google_maps_api_key in app/src/main/res/values/strings.xml
# 6. Run on an emulator or device
```

Emulator talks to the local backend at `http://10.0.2.2:8080`.

## Tests

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest   # needs emulator / device
```

## Design reference

The `design-reference/` directory contains the original Figma-style JSX mocks + `design.css`. Every Compose screen we build must match those tokens. See the authoritative design-system doc in the backend repo: `neerly-backend/docs/DESIGN_SYSTEM.md`.

## Coding rules

See [CLAUDE.md](CLAUDE.md).

## Related repos

- `neerly-backend` — Kotlin + Spring Boot modular monolith (API)
- `neerly-admin` — Angular 18 admin portal
