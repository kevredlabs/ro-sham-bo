---
title: 'Seeker Mobile Hello World - First Screen'
slug: 'seeker-mobile-hello-world-first-screen'
created: '2026-02-11'
status: 'ready-for-dev'
stepsCompleted: [1, 2, 3, 4]
tech_stack: ['Kotlin', 'Android', 'Jetpack Compose', 'Android Studio', 'Gradle', 'mobile-wallet-adapter-clientlib-ktx (from example)']
files_to_modify: ['android/ (copy from example-clientlib-ktx-app)', 'android/settings.gradle', 'android/app/build.gradle', 'android/app/src/main/AndroidManifest.xml', 'android/app/src/main/java/.../MainActivity.kt (or main screen)', 'android/README.md']
code_patterns: ['Android Kotlin project layout', 'Jetpack Compose UI', 'Gradle KTS', 'single-activity app']
test_patterns: ['Run on Android emulator (Mac)', 'Manual verification of Hello World screen']
---

# Tech-Spec: Seeker Mobile Hello World - First Screen

**Created:** 2026-02-11

## Overview

### Problem Statement

The team does not yet know the Seeker device and the Solana Mobile stack well. Before building the full seeker-rps game (wallet, transactions, gameplay), we need a minimal runnable Android app that runs on the Seeker, displays a single screen, and serves as a stable base for learning the device and the SDK. This reduces risk and validates the toolchain and deployment path early.

### Solution

Use the official **Hello World: Android** example ([example-clientlib-ktx-app](https://github.com/solana-mobile/mobile-wallet-adapter/tree/main/examples/example-clientlib-ktx-app)) as the project base: copy it into the repo as `android/`, adapt project identity to seeker-rps, and for this slice simplify the main screen to display only "Hello World" (no wallet flow or blockchain transaction required to pass acceptance criteria). The example already provides Kotlin, Jetpack Compose, Gradle, and Mobile Wallet Adapter clientlib-ktx; keeping that stack makes the project ready for the next slice (wallet + "Créer une partie") without a second migration.

### Scope

**In Scope:**
- New Android/Kotlin project (or module) for Seeker
- Single screen showing "Hello World" (or a clear equivalent)
- Build and run on **Android emulator** (primary dev target on Mac; physical Seeker not required for this slice)
- Reference to [Solana Mobile docs](https://docs.solanamobile.com/) (Kotlin/Seeker setup, project structure)
- Minimal navigation/structure so the app is ready to add a second screen later (e.g. "Créer une partie")

**Out of Scope (for this slice's AC):**
- Requiring wallet connection or blockchain transactions to see the main screen (wallet/tx code from the base example may stay in repo but must not be the default launch path)
- Game logic, backend, or smart contract
- QR / NFC / game codes
- Styling beyond what is needed for a clear Hello World screen

## Context for Development

### Codebase Patterns

**Base project:** [example-clientlib-ktx-app](https://github.com/solana-mobile/mobile-wallet-adapter/tree/main/examples/example-clientlib-ktx-app) (Hello World: Android — "A Kotlin app that writes a message on the blockchain"). No existing mobile code in seeker-rps; we copy this example into the repo and adapt it.

- **Project layout:** Copy the entire example (root `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/wrapper/`, `app/`) into `android/` at repo root. The example may use Groovy `build.gradle`; that is fine—keep or migrate to Kotlin DSL as needed.
- **UI:** Jetpack Compose; the example already has a main screen. For this slice, replace or simplify it to a single "Hello World" screen so that the app launches and shows that text without requiring a wallet or transaction.
- **Build:** Gradle (Android Studio). Preserve the example's dependencies (including `mobile-wallet-adapter-clientlib-ktx`) so the project is ready for the next slice; for this slice the main screen must not trigger wallet or blockchain calls to pass AC.
- **Conventions:** Kotlin-first, Compose; same stack as the official Solana Mobile Kotlin dApp example.

### Files to Reference

| File / URL | Purpose |
| ---------- | ------- |
| [example-clientlib-ktx-app](https://github.com/solana-mobile/mobile-wallet-adapter/tree/main/examples/example-clientlib-ktx-app) | **Base project** — copy into `android/`, then adapt identity and simplify main screen to Hello World |
| `_bmad-output/planning-artifacts/product-brief-seeker-rps-2026-02-05.md` | Product vision, tech stack (Kotlin for mobile), Seeker context |
| [Solana Mobile – Developer Documentation](https://docs.solanamobile.com/developers/overview) | Overview, Kotlin and development setup |
| [Development Setup](https://docs.solanamobile.com/developers/development-setup) | Android Studio, device/emulator setup (emulator sufficient for dev) |
| [Sample App Collection](https://docs.solanamobile.com/sample-apps/sample_app_overview) | Hello World: Android and other sample apps |
| [Configuring the Android Emulator](https://developer.android.com/studio/run/emulator) | Official Android emulator setup on Mac |

### Technical Decisions

- **Base:** Use **Hello World: Android** (example-clientlib-ktx-app) as the single source of truth for project structure, Gradle setup, and dependencies (Kotlin, Compose, clientlib-ktx). Copy into `android/` and adapt; do not create the Android project from scratch.
- **Dev environment:** Develop and run on **Mac using Android Studio and Android emulator**. Physical Seeker device is not required for this slice ([Solana Mobile](https://docs.solanamobile.com/developers/development-setup)).
- **Target directory:** `android/` in repo root (contents = adapted example).
- **This slice:** Main screen shows only "Hello World"; no wallet connection or transaction required for AC. Wallet/blockchain code from the example can remain in the repo but must not be the default path on launch (or simplify the default screen to Hello World only).

## Implementation Plan

### Tasks

- [ ] **Task 1: Copy example-clientlib-ktx-app into the repo as `android/`**
  - Action: Clone or download [example-clientlib-ktx-app](https://github.com/solana-mobile/mobile-wallet-adapter/tree/main/examples/example-clientlib-ktx-app) and copy its full contents (root `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/` wrapper, `app/`) into `android/` at the seeker-rps repo root. Ensure `android/settings.gradle` (or `.kts`) includes `include(":app")` and that the Gradle wrapper is present so the project can be opened in Android Studio and synced.
  - Notes: Do not create an Android project from scratch; the example is the single source of truth for structure and dependencies.

- [ ] **Task 2: Adapt project identity to seeker-rps**
  - Files: `android/settings.gradle` (or `settings.gradle.kts`), `android/app/build.gradle` (or `app/build.gradle.kts`), `android/app/src/main/AndroidManifest.xml`, and any Kotlin package directories.
  - Action: Set root project name to `seekerrps` (or equivalent). Set application id and namespace to `com.kevredlabs.seekerrps`. Set app label (e.g. "Seeker RPS") in the manifest. Rename/move the default package to `com.kevredlabs.seekerrps` so that all source files and the manifest reference this package consistently.
  - Notes: Keep the example's dependency versions and clientlib-ktx; only identity (name, package, label) changes.

- [ ] **Task 3: Simplify main screen to "Hello World" for this slice**
  - File: The main Activity and its Compose UI (e.g. `android/app/src/main/java/.../MainActivity.kt` and any main screen composable).
  - Action: Replace or simplify the default screen so that on launch the user sees a single screen with the text "Hello World" (or an equivalent clearly visible greeting). The app must launch and display this without requiring a wallet connection or blockchain transaction (so that AC 3 and AC 4 pass without installing a wallet). You may keep the example's wallet/transaction code elsewhere (e.g. behind a button or removed for now) as long as the default path is Hello World only.
  - Notes: Preserve Compose and project structure so that the next slice can re-add or wire the "write message on blockchain" flow.

- [ ] **Task 4: Add run instructions**
  - File: `android/README.md`
  - Action: Add short instructions for any developer: (1) Clone the seeker-rps repo, open the `android/` folder in Android Studio, sync Gradle. (2) **On emulator (Mac):** Create an AVD if needed (e.g. Pixel 6, API 34), select "app" and the emulator, Run. (3) **On Seeker:** Connect Seeker via USB (USB debugging enabled), select "app" and the Seeker device, Run. No per-developer config—each dev uses their own Mac and their own Seeker. Confirm "Hello World" is visible in both cases.
  - Notes: Keep README brief; multi-developer setup is documented in this spec's Notes.

### Acceptance Criteria

- [ ] **AC 1:** Given the repository has no `android/` directory, when the developer completes Tasks 1–3 (copy example, adapt identity, simplify main screen to Hello World), then the directory `android/` exists and contains a valid Gradle project (e.g. `settings.gradle`, `build.gradle`, and `app/` module) that syncs and builds.

- [ ] **AC 2:** Given the project is open in Android Studio (File > Open > `android/`), when the developer runs "Sync Project with Gradle Files", then the project syncs successfully with no resolution or build script errors.

- [ ] **AC 3:** Given an Android emulator is running on the developer's machine (e.g. Mac) or a device is connected, when the developer selects the "app" run configuration and runs the app, then the app installs and launches on the emulator or device without crashing.

- [ ] **AC 4:** Given the app has launched, when the user views the main (and only) screen, then the text "Hello World" (or an equivalent clearly visible greeting) is displayed on screen.

- [ ] **AC 5:** Given the project is fully set up, when the developer runs `./gradlew assembleDebug` from the `android/` directory, then the build completes successfully and produces a debug APK under `android/app/build/outputs/apk/debug/`.

- [ ] **AC 6 (edge case):** Given the developer has not installed Android Studio or the Android SDK, when they follow the spec and the referenced [Development Setup](https://docs.solanamobile.com/developers/development-setup) (install Android Studio, configure emulator), then they can open the project, sync, and run the app on an emulator as in AC 2–4.

## Additional Context

### Dependencies

- **From the base example (example-clientlib-ktx-app):** Use the same Android SDK, Kotlin, Gradle, and app dependencies as in the [example](https://github.com/solana-mobile/mobile-wallet-adapter/tree/main/examples/example-clientlib-ktx-app), including `mobile-wallet-adapter-clientlib-ktx`, Compose, and any web3/RPC libs the example uses. Do not remove them; they are required for the next slice (wallet + transactions). For this slice, the main screen simply does not invoke wallet or blockchain.
- **IDE:** Android Studio (latest stable) on Mac for development; emulator or Seeker for running the app.

### Testing Strategy

- **Manual testing (required):** Open project in Android Studio, sync Gradle, run the "app" configuration on an Android emulator. Verify that the app launches and the main screen displays "Hello World". Document result (pass/fail) for AC 3 and AC 4.
- **Build verification:** Run `./gradlew assembleDebug` from `android/` to confirm the project builds and an APK is produced (AC 5).
- **Optional (out of scope for this slice):** A Compose UI test that launches the app and asserts the presence of "Hello World" text (e.g. `composeTestRule.onNodeWithText("Hello World").assertIsDisplayed()`) can be added in a later iteration.

### Notes

- **Base:** [Hello World: Android](https://github.com/solana-mobile/mobile-wallet-adapter/tree/main/examples/example-clientlib-ktx-app) (example-clientlib-ktx-app) is used as the project base so the stack (Kotlin, Compose, clientlib-ktx) matches the official sample; adding wallet and "Créer une partie" in the next slice is then a small step.
- Product brief now includes a References section with the Solana Mobile docs link.
- This spec is the first slice of seeker-rps; follow-up specs will add wallet, "Créer une partie", and on-chain flows.
- **Dev workflow:** Android Studio on Mac + Android emulator (no physical Seeker required for this slice; same app will run on Seeker when needed).
- **Run on emulator:** After opening `android/` in Android Studio and syncing, use Tools > Device Manager to create an AVD (e.g. Pixel 6, API 34) if needed, then select the "app" configuration and the emulator in the run dropdown and click Run. The "Hello World" screen should appear on the emulator.
- **Multi-developer setup (two devs, each with own Mac and own Seeker):** The same repo and single debug build support both workflows with no per-developer config. Each developer: (1) Clones the repo and opens `android/` in Android Studio on their Mac. (2) **Emulator:** Creates their own AVD if needed and runs "app" on it for day-to-day dev. (3) **Seeker:** Connects their own Seeker via USB (enable USB debugging on the device), selects it in the run-target dropdown next to "app", and runs—the same debug APK installs on that Seeker. No shared device list or machine-specific paths; each dev independently chooses "emulator" or "Seeker" as run target. Task 6 run instructions (e.g. in `android/README.md`) should mention both: run on emulator (Mac) and run on connected Seeker (USB).
- **Known limitation:** No automated tests are required for this spec; manual verification is sufficient. Package name `com.kevredlabs.seekerrps` can be changed if the project uses a different group/package convention.
