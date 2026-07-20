# Audio Journal

A deliberately simple Android voice recorder: one big record button, pause /
resume, stop — and every finished recording is saved on the device and
automatically uploaded to cloud storage (Google Drive by default, AWS S3
optionally) in the background.

## What it does

- **Record** — tap the big red microphone button. Recording runs in a
  foreground service, so it keeps going with the screen off or while you use
  other apps (you'll see an ongoing notification).
- **Pause / resume** — while recording, a pause button appears next to the
  stop button.
- **Stop** — finalizes the recording as an AAC `.m4a` file (small files,
  playable everywhere) named like `recording_2026-07-09_14-30-00.m4a`.
- **Automatic cloud upload** — when you stop, the file is queued with
  WorkManager and uploaded to your **Google Drive** (or an AWS S3 bucket if
  you switch backends). If you're offline, the upload waits for connectivity
  and retries with exponential backoff — the recording is never lost.
- **Folder per note type** — configure one or more Drive folders by **folder
  id** (unambiguous, unlike names); with several configured, a picker in the
  app chooses where the next recording goes. The first entry is the default.
- **Works without cloud setup** — until you connect Google Drive (or
  configure S3), the app simply keeps recordings on the device and says so in
  the UI.

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

## Configuring cloud upload

### Google Drive (the default)

No secrets go into the app — you sign in with the Google account on your
phone, and Google Play services brokers scoped, revocable OAuth tokens. What
you do need is a one-time (~15 min) registration in Google Cloud Console so
Google knows your app:

1. Go to <https://console.cloud.google.com/>, create a project (e.g.
   `audio-journal`).
2. **APIs & Services → Library** → search "Google Drive API" → **Enable**.
3. **APIs & Services → OAuth consent screen** → user type **External** → fill
   in the app name and your email. Under **Test users**, add your own Google
   account, and leave the publishing status on **Testing**.
4. **APIs & Services → Credentials → Create credentials → OAuth client ID** →
   application type **Android**:
   - Package name: `com.audiojournal.app`
   - SHA-1 fingerprint: run `./gradlew signingReport` in the project (or in
     Android Studio's Gradle panel) and copy the SHA-1 of the `debug` variant.

   No client secret is downloaded or embedded — the registration is matched
   against your app's package name and signing certificate at runtime.
5. (Optional) Choose your upload folders in `local.properties`:

   ```properties
   drive.folders=Journal=1AbCdEfGhIjKlMnOpQrStUv,Ideas=1ZyXwVuTsRqPoNmLkJiHgF
   ```

   Each entry is `Label=folderId` (label shown in the app, folder id used
   for the upload — ids are unambiguous, unlike folder names). Find a
   folder's id by opening it in Drive on the web; it's the last part of the
   URL: `drive.google.com/drive/folders/<folderId>`. The first entry is the
   default destination; with more than one, an in-app picker switches
   between them per recording. Unset means uploads go to the My Drive root.

   For CI-built APKs (the GitHub Actions artifact), set the same value as a
   repository **variable** named `DRIVE_FOLDERS` (repo Settings → Secrets
   and variables → Actions → Variables).
6. Rebuild, install, and tap **Connect Google Drive** in the app. Pick your
   account and approve the consent screen. Because the app is unverified,
   Google shows a warning — click **Advanced → Go to audio-journal (unsafe)**;
   it's your own app and your own Cloud project.

> **Scope note:** the app requests full Drive access so it can upload into
> pre-existing folders you point it at by id. If you'd rather restrict it to
> content the app itself creates, change `DRIVE_SCOPE` in
> `DriveAuthManager.kt` to `https://www.googleapis.com/auth/drive.file`.

### AWS S3 (optional alternative)

Set `upload.backend=s3` in `local.properties` to switch. Then:

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

3. Create an **access key** for that user and put everything into
   `local.properties`:

   ```properties
   upload.backend=s3
   s3.bucket=my-audio-journal
   s3.region=eu-central-1
   s3.accessKeyId=AKIA...
   s3.secretAccessKey=...
   ```

Recordings land under `recordings/<folder label>/` in the bucket, mirroring
the folder picker.

> **Security note:** unlike Drive, the S3 credentials are baked into your
> locally built APK. That is fine for a personal app you build and install
> yourself — but don't distribute that APK, and keep the IAM policy as narrow
> as shown above (upload-only, one prefix, one bucket).

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
│   ├── FolderConfig.kt       Parses the configured folder list
│   ├── UploadWorker.kt       WorkManager worker (retry with backoff)
│   ├── UploadScheduler.kt    Enqueues uploads with a network constraint
│   └── drive/
│       ├── DriveAuthManager.kt   OAuth via Play services (no app secrets)
│       ├── DriveApi.kt           Minimal Drive v3 REST client
│       ├── DriveJson.kt          Pure request/response helpers (unit tested)
│       └── DriveCloudUploader.kt Google Drive implementation (default)
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
- `CloudUploader` is an interface with Google Drive (default) and S3
  implementations, selected by the `upload.backend` build property; adding
  another backend means one new class and one changed line in `AppContainer`.

### Tests

```bash
./gradlew test                        # JVM unit tests (state machine, naming, retry logic)
./gradlew connectedDebugAndroidTest   # UI smoke test (needs an emulator/device)
./gradlew lint                        # Android lint
```

### Continuous integration

Every push runs [GitHub Actions](.github/workflows/android.yml): assemble,
lint, unit tests — and publishes the debug APK as a downloadable artifact.
