# Seeker RPS – Android app

Minimal Android app (Kotlin, Jetpack Compose) for the Seeker device. Base: [Hello World: Android](https://github.com/solana-mobile/mobile-wallet-adapter/tree/main/examples/example-clientlib-ktx-app). This slice shows a single "Hello World" screen; wallet and blockchain flows will be added in a follow-up.

## Run the app

1. **Open the project**  
   In Android Studio: **File → Open** → select the `android` folder (this directory). Wait for Gradle sync to finish.

2. **On emulator (Mac)**  
   - Create an AVD if needed: **Tools → Device Manager → Create Device** (e.g. Pixel 6, API 34).  
   - In the run bar, select the **"app"** configuration and your **emulator**.  
   - Click **Run** (green triangle). The app should install and show "Hello World".

3. **On Seeker (device)**  
   - Enable **USB debugging** on the Seeker (Settings → Developer options).  
   - Connect the Seeker via USB.  
   - In the run bar, select **"app"** and the **Seeker device**.  
   - Click **Run**. The app should install and show "Hello World".

No per-developer config: each developer uses their own Mac and their own Seeker; same repo and build for everyone.

## Build from command line

From the `android` directory:

```bash
./gradlew assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
