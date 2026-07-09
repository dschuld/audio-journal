# Audio Journal

A deliberately simple Android voice recorder: one big record button, pause /
resume, stop — and every finished recording is saved on the device and
automatically uploaded to cloud storage (AWS S3) in the background.

## What it does

- **Record** — tap the big red microphone button. Recording runs in a
  foreground service, so it keeps going with the screen off or while you use
  other apps (you'll see an ongoing notification).
- **Pause / resume** — while recording, a pause button appears next to the
  stop button.
- **Stop** — finalizes the recording as an AAC `.m4a` file (small files,
  playable everywhere) named like `recording_2026-07-09_14-30-00.m4a`.
- **Automatic cloud upload** — when you stop, the file is queued with
  WorkManager and uploaded to `s3://<your bucket>/recordings/`. If you're
  offline, the upload waits for connectivity and retries with exponential
  backoff — the recording is never lost.
- **Works without cloud setup** — with no S3 credentials configured, the app
  simply keeps recordings on the device and says so in the UI.

Recordings live in the app's private storage:
`Android/data/com.audiojournal.app/files/recordings/` (also browsable via USB).

## Getting started (no Android experience needed)

### 1. Install Android Studio

Download it from <https://developer.android.com/studio> and run the installer
with default settings. It bundles everything you need (Android SDK, emulator,
JDK).

### 2. Open the project

In Android Studio: **File → Open…** and select this repository's folder.
The first "Gradle sync" downloads dependencies and takes a few minutes.

### 3. Run it in the emulator

1. **Tools → Device Manager → Create virtual device** — pick any phone
   (e.g. Pixel 8), accept the suggested system image, finish.
2. Press the green **Run ▶** button in the toolbar.
3. The emulator boots and the app launches. Grant the microphone permission
   when asked. The emulator can use your computer's microphone: in the
   emulator's side panel choose **⋯ (Extended controls) → Microphone → Virtual
   microphone uses host audio input**.

### 4. Install it on your phone

**Option A – straight from Android Studio (easiest):**

1. On the phone, enable developer mode: **Settings → About phone → tap
   "Build number" seven times**, then **Settings → System → Developer options
   → enable "USB debugging"**.
2. Connect the phone via USB and accept the "Allow USB debugging?" prompt.
3. Your phone now appears in Android Studio's device dropdown — press
   **Run ▶**.

**Option B – install the APK file:**

1. Build it: **Build → Build App Bundle(s) / APK(s) → Build APK(s)** in
   Android Studio (or `./gradlew assembleDebug` on the command line). The file
   ends up at `app/build/outputs/apk/debug/app-debug.apk`.
   Alternatively, download the `audio-journal-debug-apk` artifact that the
   GitHub Actions CI build attaches to every push — no local build needed.
2. Copy the APK to your phone (USB, cloud drive, email to yourself…), open it
   there, and allow "install from unknown sources" when prompted.

## Configuring cloud upload (AWS S3)

The app uploads to an S3 bucket using credentials you provide at build time.

### One-time AWS setup

1. In the [S3 console](https://s3.console.aws.amazon.com/), create a bucket
   (e.g. `my-audio-journal`), keeping "Block all public access" **on**.
2. In the [IAM console](https://console.aws.amazon.com/iam/), create a user
   (e.g. `audio-journal-app`) **without** console access, and attach only this
   inline policy (replace the bucket name):

   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": "s3:PutObject",
         "Resource": "arn:aws:s3:::my-audio-journal/recordings/*"
       }
     ]
   }
   ```

3. Create an **access key** for that user (IAM → the user → Security
   credentials → Create access key → "Application running outside AWS") and
   note the key ID and secret.

### Tell the app about it

Copy the `s3.*` lines from [`local.properties.sample`](local.properties.sample)
into `local.properties` in the project root (Android Studio creates that file
automatically; it is gitignored so the secrets stay out of git):

```properties
s3.bucket=my-audio-journal
s3.region=eu-central-1
s3.accessKeyId=AKIA...
s3.secretAccessKey=...
```

Rebuild and reinstall the app. After stopping a recording the UI shows
"Queued for cloud upload" and the file appears in your bucket under
`recordings/` as soon as the device is online.

> **Security note:** the credentials are baked into your locally built APK.
> That is fine for a personal app you build and install yourself — but don't
> distribute that APK, and keep the IAM policy as narrow as shown above
> (upload-only, one folder, one bucket). A future version could switch to a
> Google Drive sign-in or a presigned-URL backend to avoid on-device secrets.

## Development

### Architecture

```
app/src/main/java/com/audiojournal/app/
├── AudioJournalApp.kt        Application + hand-rolled DI container
├── MainActivity.kt           Single activity hosting the Compose UI
├── recording/
│   ├── RecordingEngine.kt    Pure-Kotlin state machine (idle → recording ⇄ paused)
│   ├── AudioRecorder.kt      Interface + MediaRecorder implementation
│   ├── RecordingService.kt   Foreground service that keeps the mic alive
│   └── RecorderState.kt      State model
├── storage/
│   └── RecordingStore.kt     Output directory + timestamped file names
├── upload/
│   ├── CloudUploader.kt      Destination-agnostic upload interface
│   ├── S3CloudUploader.kt    AWS S3 implementation
│   ├── UploadWorker.kt       WorkManager worker (retry with backoff)
│   └── UploadScheduler.kt    Enqueues uploads with a network constraint
└── ui/
    ├── RecorderScreen.kt     The one screen (Jetpack Compose, Material 3)
    ├── RecorderViewModel.kt  Bridges UI ↔ engine/service
    └── TimeFormat.kt         Elapsed-time formatting
```

Design choices:

- The recording state machine (`RecordingEngine`) contains no Android
  framework types, so all of its behavior is covered by fast JVM unit tests.
- Recording runs in a **foreground service** with the `microphone` service
  type — required on modern Android for the mic to stay usable in the
  background.
- Uploads go through **WorkManager**, which persists queued uploads across
  app restarts and reboots and only runs them when the network is up.
- `CloudUploader` is an interface: adding Google Drive or another backend
  later means one new class and one changed line in `AppContainer`.

### Tests

```bash
./gradlew test                        # JVM unit tests (state machine, naming, retry logic)
./gradlew connectedDebugAndroidTest   # UI smoke test (needs an emulator/device)
./gradlew lint                        # Android lint
```

### Continuous integration

Every push runs [GitHub Actions](.github/workflows/android.yml): assemble,
lint, unit tests — and publishes the debug APK as a downloadable artifact.
