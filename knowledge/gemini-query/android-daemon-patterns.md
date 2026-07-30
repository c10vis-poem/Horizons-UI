# Android Daemon & System Integration Patterns

> **Source caveat:** Extracted from Gemini brainstorming transcripts (July 2025).
> Content is unverified LLM output — useful as implementation guidelines,
> alternative architectural paths, and troubleshooting reference, but treat
> specific API signatures and version claims as "check before shipping."
> Original transcripts preserved in this folder for provenance.

---

## 1. Foreground Service as Daemon

Android has no true Linux-style daemon. The closest equivalent is a
**sticky foreground service** bound to a persistent notification channel.

### Core pattern

```kotlin
class DaemonEngine : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1001, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            while (isActive) {
                // daemon work loop
                delay(5000)
            }
        }
        return START_STICKY // OS recreates service if evicted
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

### Manifest requirements

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<service
    android:name=".services.DaemonEngine"
    android:foregroundServiceType="specialUse"
    android:exported="false" />

<receiver android:name=".receivers.BootReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

### ADB overrides for testing

```bash
# Whitelist from Doze / battery optimization
adb shell dumpsys deviceidle whitelist +com.horizons
```

### Relevance to Horizons

`CliffordService` already implements this pattern with `specialUse` +
`START_STICKY`. The notification channel, boot receiver, and battery
whitelist are all wired. This section exists as a reference if the
implementation ever needs to be rebuilt or debugged.

---

## 2. VoiceInteractionService — Device Assistant Hook

To register as an alternative device assistant (replacing Google Assistant
on long-press), the app implements `VoiceInteractionService`.

### Manifest wiring

```xml
<service
    android:name=".services.VoiceAssistantService"
    android:label="Horizons Assistant"
    android:permission="android.permission.BIND_VOICE_INTERACTION"
    android:exported="true">
    <meta-data
        android:name="android.voice_interaction"
        android:resource="@xml/assistant_interaction_info" />
    <intent-filter>
        <action android:name="android.service.voice.VoiceInteractionService" />
    </intent-filter>
</service>
```

### Resource file (`res/xml/assistant_interaction_info.xml`)

```xml
<voice-interaction-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:sessionService="com.horizons.services.AssistantSessionService"
    android:recognitionService="com.horizons.services.AssistantRecognitionService"
    android:supportsAssist="true"
    android:supportsLocalInteraction="true" />
```

### Service skeleton

```kotlin
class VoiceAssistantService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        // assistant lifecycle bound to system
    }

    override fun launchVoiceAssistFromKeyguard() {
        super.launchVoiceAssistFromKeyguard()
        // handle invocation from lock screen
    }
}
```

### ADB: force-bind as default assistant

```bash
adb shell settings put secure assistant \
    com.horizons/.services.VoiceAssistantService
```

### Relevance to Horizons

The voice layer (Pending #2 — Moonshine STT + Kokoro/Sherpa TTS as a
media daemon on `:8091`) will eventually need this wiring to intercept
the system assist gesture. The `AssistantSessionService` and
`AssistantRecognitionService` stubs still need to be built.

---

## 3. Termux RUN_COMMAND Integration

The Termux app exposes an intent-based API for executing commands from
other Android apps.

### Manifest permission

```xml
<uses-permission android:name="com.termux.permission.RUN_COMMAND" />
```

### Kotlin: execute a command in Termux

```kotlin
fun executeInTermux(context: Context, scriptPath: String, args: Array<String>) {
    val intent = Intent().apply {
        setClassName("com.termux", "com.termux.app.RunCommandService")
        action = "com.termux.RUN_COMMAND"
        putExtra("com.termux.RUN_COMMAND_PATH",
            "/data/data/com.termux/files/usr/bin/bash")
        putExtra("com.termux.RUN_COMMAND_ARGUMENTS",
            arrayOf(scriptPath) + args)
        putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
    }
    context.startService(intent)
}
```

### Relevance to Horizons

Session 20 operator ask: "let a Termux-side agent hit the NPU/HTP backend
by having the app run as a localhost server." This intent bridge is how
the Horizons app could trigger Termux-side scripts (e.g. launching
`llama-server` or `geniex serve` from the Termux environment) without
the user manually switching apps. Also relevant to the model-download
flow (terminal + browser, per session 20).

---

## 4. Dynamic APK Installation via PackageInstaller

For pulling and installing updated daemon binaries or companion tools at
runtime.

### Pattern

```kotlin
fun installApk(context: Context, apkFile: File) {
    val installer = context.packageManager.packageInstaller
    val params = PackageInstaller.SessionParams(
        PackageInstaller.SessionParams.MODE_FULL_INSTALL
    )
    val sessionId = installer.createSession(params)
    val session = installer.openSession(sessionId)

    apkFile.inputStream().use { input ->
        session.openWrite("payload", 0, -1).use { output ->
            input.copyTo(output)
            session.fsync(output)
        }
    }

    val intent = Intent(context, context.javaClass)
    val pending = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_MUTABLE
    )
    session.commit(pending.intentSender)
}
```

### ADB: allow install without user popup (testing only)

```bash
adb shell appops set com.horizons REQUEST_INSTALL_PACKAGES allow
```

### Relevance to Horizons

Alternative path if the runtime binary update flow (currently: user
manually drops files via ModelImportActivity) ever needs to support
automated pull-and-install from GitHub Releases or HuggingFace.

---

## 5. Dual-App Watchdog (Alternative Architecture — Not Used)

An alternative to the single-service watchdog: two apps that monitor each
other via `PACKAGE_REPLACED` / `PACKAGE_ADDED` broadcasts and
cross-restart each other on crash.

**Status:** The project chose the single-app `CliffordService` approach
instead. Documented here as an alternative path if single-process
reliability proves insufficient under heavy OOM pressure.

The original pattern used `BroadcastReceiver` watching cross-package
events with `FLAG_ACTIVITY_NEW_TASK` restarts. See the original Gemini
transcript (`july-3rd-horizons-apk.md`) for the full code if needed.

---

## Open Questions / Future Additions

<!-- Add new patterns, troubleshooting findings, or verified corrections below -->
