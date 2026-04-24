# Neerly Mobile — Development Rules

> Claude reads this first. Humans should too.

## What this is
Android super-app for Neerly (customer + vendor + driver + admin-light in one APK).
Kotlin 2.0, Jetpack Compose, min SDK 24, target SDK 34.

Visual design: see [`design-reference/`](design-reference/) — the original Figma-style JSX mocks + `design.css`. All screens ported to Compose must match these tokens. The authoritative design-token mapping lives in the backend repo: [`neerly-backend/docs/DESIGN_SYSTEM.md`](../neerly-backend/docs/DESIGN_SYSTEM.md).

## Stack
- Kotlin 2.0, AGP 8.5+, Gradle 8.x Kotlin DSL
- Compose BOM 2024.09 (or latest stable)
- Hilt for DI
- Retrofit + Moshi for REST
- OkHttp with auth interceptor (JWT refresh on 401)
- Coroutines + Flow
- Firebase Auth (Phone provider) + Firebase Messaging (FCM)
- Google Maps Compose
- DataStore + EncryptedSharedPreferences for tokens
- Coil for images
- Timber for logging
- Compose Navigation

## Package layout
```
com.neerly.mobile/
├── App.kt                       # Hilt Application
├── MainActivity.kt              # Single activity
├── core/
│   ├── design/                  # Theme, colors, type, components
│   ├── network/                 # Retrofit, interceptors, API types
│   ├── auth/                    # Token store, auth state, refresh logic
│   └── util/
├── feature/
│   ├── auth/                    # Welcome, Phone, OTP, Name, Address
│   ├── role/                    # Role picker
│   ├── customer/                # Browse, cart, orders, subs, wallet, profile
│   ├── vendor/                  # Dashboard, orders, catalog, earnings, team
│   ├── driver/                  # Off-duty, on-duty, assignment, navigation
│   └── admin/                   # Admin-light dashboards
└── navigation/                  # NavHost + graph
```

## Rules
- Compose first. No XML layouts. No Fragments.
- One screen = one `@Composable` function named `<Role><Screen>Screen` (e.g., `CustomerHomeScreen`).
- Screens take a ViewModel (Hilt `hiltViewModel()`) and navigation callbacks; never reach into DI from inside.
- ViewModel holds `UiState` as a `StateFlow<Data>`. `Data` is a sealed class (Loading / Success / Error).
- Design tokens live in `core/design/Tokens.kt` — never hard-code hex colors or dp values.
- Role-specific accent colors come from `NeerlyTheme(role = Role.CUSTOMER)`.
- Tokens stored in `EncryptedSharedPreferences` backed by Android Keystore. Never in plain SharedPreferences.
- Every network call uses `NeerlyApi` (Retrofit); auth interceptor auto-adds `Authorization: Bearer`.
- On 401, interceptor calls `/auth/refresh`; on `AUTH_REFRESH_REUSE_DETECTED`, logs out + navigates to Welcome.
- `X-Request-Id` auto-generated per write request (idempotency).
- Telugu + Hindi strings in `res/values-te/` + `res/values-hi/` alongside `res/values/`.

## Screens to build (Session 1)
Port these from `design-reference/flows-customer.jsx`:
- Splash, Welcome, Phone, OTP, Name, Language picker, Location permission, Address/GPS, Home (empty stub)

## Running
```bash
# Open in Android Studio Koala+ or run:
./gradlew :app:installDebug
```
