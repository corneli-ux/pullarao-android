# Pullarao 1 App — Android

A native Android client for the **Pullarao 1** open-source model, built with **Jetpack Compose** and **clean architecture**. All GLM capabilities are exposed as dedicated screens: streaming chat, vision analysis, image generation, video generation, TTS, ASR, web search, page reader, and a fine-tune studio stub for your future custom models.

Built to be pushed to GitHub and built in GitHub Actions — every push to `main` produces a debug APK artifact you can install immediately.

---

## ✨ Features

| Screen | Capability |
|---|---|
| **Chat** | Real-time SSE streaming, conversation history, system prompts, thinking mode, model picker |
| **Vision** | Camera + gallery + file picker → Pullarao 1 Vision analysis with thinking toggle |
| **Image Studio** | Text-to-image with 7 size presets, persistent gallery |
| **Video Studio** | Text-to-video with quality/size/fps/duration controls, async polling |
| **Speech** | TTS (3 voices, speed control, WAV/PCM/MP3) + ASR transcription |
| **Web Search** | GLM `web_search` function with snippets, sources, publish dates |
| **Page Reader** | GLM `page_reader` extracts title/HTML/tokens from any URL |
| **Fine-tune Studio** | Manage datasets and queue training jobs (stub — wire to your fine-tune endpoint) |
| **Settings** | API key, base URL, model defaults, temperature, max tokens, theme |

---

## 🧱 Architecture

Clean architecture with strict layering — UI never touches Retrofit, repositories never touch Compose.

```
app/src/main/java/com/glm/aiapp/
├── domain/                    # Pure Kotlin: no Android, no Hilt, no Retrofit
│   ├── model/                 # Entities + value objects
│   └── repository/            # Repository contracts (interfaces)
├── data/                      # Implements domain interfaces
│   ├── api/                   # Retrofit interface + SSE streaming client
│   ├── dto/                   # Wire DTOs (kotlinx.serialization)
│   ├── db/                    # Room entities, DAOs, AppDatabase
│   ├── prefs/                 # DataStore-backed settings
│   └── repository/            # Repository implementations
├── di/                        # Hilt module wiring everything together
└── ui/
    ├── theme/                 # Material 3 color scheme + typography
    ├── navigation/            # Compose Navigation destinations
    ├── components/             # Shared composables (bars, states)
    └── screens/                # One screen per feature, each with its own ViewModel
        ├── chat/              # SSE streaming + Room-backed history
        ├── vision/
        ├── image/
        ├── video/
        ├── speech/
        ├── search/
        ├── reader/
        ├── finetune/
        └── settings/
```

**Dependency rule:** `ui → domain ← data`. ViewModels depend on `domain.repository.*` interfaces; Hilt binds them to `data.repository.*Impl` at runtime.

---

## 🛠 Tech stack

- **Kotlin 2.1**, **Jetpack Compose** (Material 3), **Compose Navigation**
- **Hilt** for DI, **Room** for persistence, **DataStore** for settings
- **Retrofit + OkHttp** with **kotlinx.serialization** converter
- **Custom OkHttp SSE client** for streaming chat (no streaming library needed)
- **Coil** for image loading
- **Accompanist** for runtime permissions
- **AndroidX Core Splashscreen** for Android 12+ splash

---

## 🚀 Getting started

### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK 35 (compileSdk)
- Min SDK 26 (Android 8.0)

### Local development

1. Clone the repo
2. Open the `android-app/` folder in Android Studio
3. Wait for Gradle sync to finish
4. Add your GLM API key in **Settings → API key** inside the app, or set it in `~/.gradle/gradle.properties`:
   ```properties
   GLM_API_KEY=your_key_here
   GLM_BASE_URL=https://open.bigmodel.cn/api/paas/v4/
   ```
5. Run on a device or emulator (API 26+)

### Build from command line

```bash
cd android-app
./gradlew assembleDebug           # → app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug            # → installs on connected device
```

---

## 🤖 GitHub Actions

This repo ships **two workflows**:

### `android.yml` — builds a debug APK on every push

Triggers on every push to `main` / `master` / `develop` and every PR. Outputs:
- **`glm-ai-debug-apk`** artifact (30-day retention) — sideload onto any API 26+ device
- **`build-reports`** artifact (7-day retention) — Gradle / test reports for debugging

### `release.yml` — builds a signed release APK on tags

Triggers on `v*` tags (e.g. `git tag v1.0.0 && git push --tags`). Produces a signed release APK and creates a GitHub Release with the APK attached.

To enable release signing, add these **repository secrets** (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | `base64 -w0 release.keystore` output of your keystore file |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias inside the keystore |
| `SIGNING_KEY_PASSWORD` | Key password |

Generate a keystore locally:
```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias glm-ai \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -storepass CHANGE_ME -keypass CHANGE_ME
base64 -w0 release.keystore > keystore.b64
# Copy keystore.b64 contents into the SIGNING_KEYSTORE_BASE64 secret
```

Without signing secrets, `release.yml` still builds an unsigned release APK you can sign locally with `apksigner`.

---

## 🔌 GLM API compatibility

The app talks to any OpenAI-compatible endpoint. Defaults to Zhipu's hosted GLM endpoint:

```
https://open.bigmodel.cn/api/paas/v4/
```

To self-host Pullarao 1 and point the app at your own deployment, change **Settings → Base URL**. Endpoints used:

| Path | Method | Used by |
|---|---|---|
| `chat/completions` (SSE) | POST | Chat (streaming) |
| `chat/completions` | POST | Vision |
| `images/generations` | POST | Image Studio |
| `videos/generations` | POST | Video Studio (create task) |
| `async-result/{id}` | GET | Video Studio (poll) |
| `audio/speech` | POST | TTS |
| `audio/transcriptions` | POST (multipart) | ASR |
| `web_search` | POST | Web Search |
| `page_reader` | POST | Page Reader |

---

## 🧪 Testing

Tests live in `app/src/test/java/com/glm/aiapp/`. Run with:

```bash
./gradlew test
```

Hilt + Room + Turbine + MockK + Truth are wired up and ready.

---

## 📦 Project layout

```
android-app/
├── .github/workflows/          # CI: android.yml + release.yml
├── app/
│   ├── build.gradle.kts        # App module config + signing
│   ├── proguard-rules.pro      # R8 keep rules
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/glm/aiapp/ # See architecture above
│       └── res/                # Strings, colors, themes, drawables, XML
├── gradle/
│   ├── libs.versions.toml      # Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts            # Root build script
├── settings.gradle.kts
├── gradle.properties
├── gradlew                     # Unix wrapper (executable)
└── README.md                   # This file
```

---

## 🗺 Roadmap

- [ ] Replace stub FineTune repository with real `/fine-tune` endpoint
- [ ] Upload-vision flow that posts local files to a signed URL (currently expects public URLs)
- [ ] On-device whisper.cpp fallback for offline ASR
- [ ] Wear OS companion for quick chat / voice replies
- [ ] WorkManager periodic sync for video task polling (currently in-process)

---

## 📄 License

MIT — see `LICENSE` (add one if you want). GLM model weights are licensed separately by Zhipu AI; check their terms before deploying.
