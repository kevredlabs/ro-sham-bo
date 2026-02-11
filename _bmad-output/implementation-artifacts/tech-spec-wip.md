---
title: 'Seeker Mobile Hello World - First Screen'
slug: 'seeker-mobile-hello-world-first-screen'
created: '2026-02-11'
status: 'review'
stepsCompleted: [1, 2, 3]
tech_stack: ['Kotlin', 'Android', 'Jetpack Compose', 'Android Studio', 'Gradle Kotlin DSL']
files_to_modify: ['android/settings.gradle.kts', 'android/build.gradle.kts', 'android/gradle.properties', 'android/app/build.gradle.kts', 'android/app/src/main/AndroidManifest.xml', 'android/app/src/main/java/com/kevredlabs/seekerrps/MainActivity.kt']
code_patterns: ['Android Kotlin project layout', 'Jetpack Compose UI', 'Gradle KTS', 'single-activity app']
test_patterns: ['Run on Android emulator (Mac)', 'Manual verification of Hello World screen']
---

# Tech-Spec: Seeker Mobile Hello World - First Screen

**Created:** 2026-02-11

## Overview

### Problem Statement

The team does not yet know the Seeker device and the Solana Mobile stack well. Before building the full seeker-rps game (wallet, transactions, gameplay), we need a minimal runnable Android app that runs on the Seeker, displays a single screen, and serves as a stable base for learning the device and the SDK. This reduces risk and validates the toolchain and deployment path early.

### Solution

Create a minimal Kotlin Android app targeting the Seeker device: one screen that displays "Hello World" (or equivalent), with no wallet integration and no blockchain transactions. The app establishes the project structure, build configuration, and navigation baseline that will later host the game UI (e.g. home screen + "Créer une partie" button). All implementation details (project setup, dependencies, run instructions) will be specified so a developer can implement from this spec alone.

### Scope

**In Scope:**
- New Android/Kotlin project (or module) for Seeker
- Single screen showing "Hello World" (or a clear equivalent)
- Build and run on **Android emulator** (primary dev target on Mac; physical Seeker not required for this slice)
- Reference to [Solana Mobile docs](https://docs.solanamobile.com/) (Kotlin/Seeker setup, project structure)
- Minimal navigation/structure so the app is ready to add a second screen later (e.g. "Créer une partie")

**Out of Scope:**
- Wallet integration (Mobile Wallet Adapter, Seed Vault)
- Any blockchain transactions or RPC calls
- Game logic, backend, or smart contract
- QR / NFC / game codes
- Styling beyond what is needed for a clear Hello World screen

## Context for Development

### Codebase Patterns

**Confirmed Clean Slate:** No existing mobile code in the repo. This spec defines the initial structure.

- **Project layout:** New Android project under `android/` at repo root (standard Gradle multi-file layout: root `build.gradle.kts`, `settings.gradle.kts`, `app/` module with its own `build.gradle.kts`).
- **UI:** Jetpack Compose (recommended by [Solana Mobile Kotlin Quickstart](https://docs.solanamobile.com/android-native/quickstart)); single-activity, one composable screen.
- **Build:** Gradle with Kotlin DSL (`.kts`), Android Studio as IDE.
- **Conventions:** Kotlin-first, Compose for UI, no Solana SDK dependencies in this slice (add in a later spec when adding wallet/tx).

### Files to Reference

| File / URL | Purpose |
| ---------- | ------- |
| `_bmad-output/planning-artifacts/product-brief-seeker-rps-2026-02-05.md` | Product vision, tech stack (Kotlin for mobile), Seeker context |
| [Solana Mobile – Developer Documentation](https://docs.solanamobile.com/developers/overview) | Overview, Kotlin and development setup |
| [Development Setup](https://docs.solanamobile.com/developers/development-setup) | Android Studio, device/emulator setup (emulator sufficient for dev) |
| [Kotlin Quickstart](https://docs.solanamobile.com/android-native/quickstart) | Jetpack Compose scaffold reference; we use a minimal new project instead |
| [Kotlin Project Setup](https://docs.solanamobile.com/android-native/setup) | Solana dependencies (not used in this Hello World slice) |
| [Configuring the Android Emulator](https://developer.android.com/studio/run/emulator) | Official Android emulator setup on Mac |

### Technical Decisions

- **Stack:** Kotlin on Android with Jetpack Compose; build with Android Studio, Gradle Kotlin DSL.
- **Dev environment:** Develop and run on **Mac using Android Studio and Android emulator**. Physical Seeker device is not required for this slice ([Solana Mobile: "You can test your app during development on any Android device or emulator"](https://docs.solanamobile.com/developers/development-setup)).
- **Target directory:** New project at `android/` in repo root (app module `android/app/`).
- **Scope:** Hello World only; no wallet, no Solana SDK dependencies, no transactions. Solana Mobile SDK can be added in a follow-up spec.

## Implementation Plan

### Tasks

- [ ] **Task 1: Create Android project root and Gradle configuration**
  - File: `android/settings.gradle.kts`
  - Action: Create file with `pluginManagement` (repositories: google, mavenCentral, gradlePluginPortal), `dependencyResolutionManagement`, `rootProject.name = "seekerrps"`, and `include(":app")`.
  - Notes: Use Kotlin DSL. Ensure Android Gradle Plugin and Kotlin version are resolved from pluginManagement.

- [ ] **Task 2: Create root build script and Gradle wrapper**
  - File: `android/build.gradle.kts`
  - Action: Apply `com.android.application` and `org.jetbrains.kotlin.android` with `apply false` in a plugins block; no allprojects block needed if using dependencyResolutionManagement in settings. Use AGP 8.x and Kotlin 1.9+ compatible versions.
  - File: `android/gradle.properties`
  - Action: Add `android.useAndroidX=true`, `kotlin.code.style=official`, `android.nonTransitiveRClass=true`. Optionally JVM args for Gradle if needed.
  - Notes: Run `gradle wrapper` from `android/` (with Gradle 8.x) to generate `gradle/wrapper/gradle-wrapper.properties` and wrapper JAR, or create wrapper files so the project can be opened in Android Studio and synced.

- [ ] **Task 3: Create app module build configuration**
  - File: `android/app/build.gradle.kts`
  - Action: Apply `com.android.application` and `org.jetbrains.kotlin.android`. Set `namespace = "com.kevredlabs.seekerrps"`, `compileSdk = 34` (or 35), `minSdk = 24`, `targetSdk = 34`, `versionCode = 1`, `versionName = "1.0"`. Enable Compose: `buildFeatures { compose = true }`, `composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }` (match to Kotlin version). Add dependencies: `implementation(platform("androidx.compose:compose-bom:2024.01.00"))`, `implementation("androidx.compose.ui:ui")`, `implementation("androidx.compose.material3:material3")`, `implementation("androidx.activity:activity-compose:1.8.2")`, `implementation("androidx.core:core-ktx:1.12.0")`.
  - Notes: No Solana Mobile or wallet dependencies. Use a single defaultProguardFile for release. Main source set should have no extra config.

- [ ] **Task 4: Create AndroidManifest and main launcher activity**
  - File: `android/app/src/main/AndroidManifest.xml`
  - Action: Declare package (or use namespace from build.gradle). Add `<application>` with `android:label` (e.g. "Seeker RPS"), `android:theme` (e.g. `@style/Theme.Seekerrps` or Material theme). Single `<activity>` for `MainActivity`: `android:exported="true"`, `android:theme`, and `<intent-filter>` with `MAIN` and `LAUNCHER`.
  - Notes: Min SDK 24 so no need for legacy multidex for this minimal app.

- [ ] **Task 5: Implement MainActivity with Compose "Hello World" screen**
  - File: `android/app/src/main/java/com/kevredlabs/seekerrps/MainActivity.kt`
  - Action: Create `MainActivity : ComponentActivity()` that calls `setContent { HelloWorldScreen() }`. Create `@Composable fun HelloWorldScreen()` that displays `Text("Hello World")` (or "Hello World" in a `Surface`/`Box` with basic padding so it is clearly visible). Use Material3 theme if desired (e.g. `MaterialTheme.colorScheme`); keep styling minimal.
  - Notes: Single screen only; no navigation, no ViewModel. Package must match namespace in build.gradle and manifest.

- [ ] **Task 6: Add run instructions to project**
  - File: `android/README.md` (or document in this tech-spec)
  - Action: Add short instructions for any developer: (1) Clone repo, open `android/` in Android Studio, sync Gradle. (2) **On emulator (Mac):** Create an AVD if needed (e.g. Pixel 6, API 34), select "app" + emulator, Run. (3) **On Seeker:** Connect Seeker via USB (USB debugging enabled), select "app" + the Seeker device in the run target, Run. No per-developer config—each dev uses their own Mac and their own Seeker. Confirm "Hello World" is visible in both cases.
  - Notes: Keep README brief; multi-developer setup is documented in this spec's Notes.

### Acceptance Criteria

- [ ] **AC 1:** Given the repository has no `android/` directory, when the developer completes Tasks 1–5, then the directory `android/` exists and contains a valid Gradle project (e.g. `settings.gradle.kts`, `build.gradle.kts`, and `app/` module with its `build.gradle.kts`).

- [ ] **AC 2:** Given the project is open in Android Studio (File > Open > `android/`), when the developer runs "Sync Project with Gradle Files", then the project syncs successfully with no resolution or build script errors.

- [ ] **AC 3:** Given an Android emulator is running on the developer's machine (e.g. Mac) or a device is connected, when the developer selects the "app" run configuration and runs the app, then the app installs and launches on the emulator or device without crashing.

- [ ] **AC 4:** Given the app has launched, when the user views the main (and only) screen, then the text "Hello World" (or an equivalent clearly visible greeting) is displayed on screen.

- [ ] **AC 5:** Given the project is fully set up, when the developer runs `./gradlew assembleDebug` from the `android/` directory, then the build completes successfully and produces a debug APK under `android/app/build/outputs/apk/debug/`.

- [ ] **AC 6 (edge case):** Given the developer has not installed Android Studio or the Android SDK, when they follow the spec and the referenced [Development Setup](https://docs.solanamobile.com/developers/development-setup) (install Android Studio, configure emulator), then they can open the project, sync, and run the app on an emulator as in AC 2–4.

## Additional Context

### Dependencies

- **Android SDK:** compileSdk 34 (or 35), minSdk 24, targetSdk 34. Install via Android Studio SDK Manager.
- **Kotlin:** 1.9.x or 1.10.x; Kotlin Compose compiler extension version must match (e.g. 1.5.8 for Kotlin 1.9).
- **Android Gradle Plugin:** 8.2.x or 8.3.x; Gradle 8.2+.
- **Libraries (app module):** Compose BOM, `androidx.compose.ui:ui`, `androidx.compose.material3:material3`, `androidx.activity:activity-compose`, `androidx.core:core-ktx`. No Solana Mobile or RPC/wallet libraries for this slice.
- **IDE:** Android Studio (latest stable) on Mac for development; emulator for running the app. No dependency on a physical Seeker device.

### Testing Strategy

- **Manual testing (required):** Open project in Android Studio, sync Gradle, run the "app" configuration on an Android emulator. Verify that the app launches and the main screen displays "Hello World". Document result (pass/fail) for AC 3 and AC 4.
- **Build verification:** Run `./gradlew assembleDebug` from `android/` to confirm the project builds and an APK is produced (AC 5).
- **Optional (out of scope for this slice):** A Compose UI test that launches the app and asserts the presence of "Hello World" text (e.g. `composeTestRule.onNodeWithText("Hello World").assertIsDisplayed()`) can be added in a later iteration.

### Notes

- Product brief now includes a References section with the Solana Mobile docs link.
- This spec is the first slice of seeker-rps; follow-up specs will add wallet, "Créer une partie", and on-chain flows.
- **Dev workflow:** Android Studio on Mac + Android emulator (no physical Seeker required for this slice; same app will run on Seeker when needed).
- **Run on emulator:** After opening `android/` in Android Studio and syncing, use Tools > Device Manager to create an AVD (e.g. Pixel 6, API 34) if needed, then select the "app" configuration and the emulator in the run dropdown and click Run. The "Hello World" screen should appear on the emulator.
- **Multi-developer setup (two devs, each with own Mac and own Seeker):** The same repo and single debug build support both workflows with no per-developer config. Each developer: (1) Clones the repo and opens `android/` in Android Studio on their Mac. (2) **Emulator:** Creates their own AVD if needed and runs "app" on it for day-to-day dev. (3) **Seeker:** Connects their own Seeker via USB (enable USB debugging on the device), selects it in the run-target dropdown next to "app", and runs—the same debug APK installs on that Seeker. No shared device list or machine-specific paths; each dev independently chooses "emulator" or "Seeker" as run target. Task 6 run instructions (e.g. in `android/README.md`) should mention both: run on emulator (Mac) and run on connected Seeker (USB).
- **Known limitation:** No automated tests are required for this spec; manual verification is sufficient. Package name `com.kevredlabs.seekerrps` can be changed if the project uses a different group/package convention.
