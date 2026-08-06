# BUILD STATUS — what exists, what does not

> **Verified 2026-08-06 (session 22)** against the merged tree on
> `claude/deprecated-repo-recovery-ycl0i8` (PR #34 = main + PR #33 + this
> session's work). Every row was checked by grepping for the **consumer**, not
> by reading a document.
>
> **Why this file exists.** `canon/STATE-OF-EXISTENCE.md` in the vault is the
> nominal build-state authority, but several of its rows were measured wrong or
> inherited from an uncontested assistant claim in a transcript. This is the
> mechanical counterpart: no prose, no inference, just what the tree contains.
>
> **`unverified` means built but never run on a device.** Nothing in this repo
> has been device-confirmed. CI proves it compiles and links, nothing more.

| Tag | Means |
|---|---|
| `built` | code exists and a consumer calls it |
| `unverified` | code exists, never confirmed on device |
| `partial` | some of it exists |
| `ABSENT` | nothing behind it |

---

## 1 · The circuit — Terminal → Router → Monitor

| # | Thing | State | Evidence |
|---|---|---|---|
| 1.1 | Terminal defines a runtime as parameters | `unverified` | `RuntimeDefStore` |
| 1.2 | Router loads / preps / holds (no gate) | `unverified` | `RouterPane.loadConfig()` |
| 1.3 | Monitor verifies and **dispatches** | `unverified` | `MonitorPane:92-100` → `DaemonLauncher` |
| 1.4 | `greenLight()` — binary presence | `unverified` | `RuntimeDefStore:119-149` |
| 1.5 | `greenLight()` — exec bit | `unverified` | `RuntimeDefStore:126-130` |
| 1.6 | `greenLight()` — asset availability | `unverified` | same |
| 1.7 | `greenLight()` — model plugged in | `unverified` | same |
| 1.8 | **Amperage check — arch + free RAM** | **`ABSENT`** | no `SUPPORTED_ABIS`, no `availMem` anywhere |
| 1.9 | Archives stores verified profiles | **`ABSENT`** | no `saveProfile` / `verifiedProfile` |
| 1.10 | Router SLEEP actually unloads | **`ABSENT`** | status flag only; no `unload` / `releaseModel` |

**1.8 is the important one.** It is the operator's own item 3 — *"is it going to
live on the hardware, is it compatible, is it too big"* — and it is the only
structural defence against an LMK kill. PR #33 deliberately left it out as a
separate change.

## 2 · The four parameter layers

Canon: **Weights · Runtime · Engine · Communication** (`aesop/PARAMETER-PACKET.md`
§2). None of it is first-class yet. This is the Router's **tuner deck**.

| # | Thing | State | Evidence |
|---|---|---|---|
| 2.1 | `RuntimeDef` shaped to the four layers | **`ABSENT`** | five flat fields |
| 2.2 | `temperature` as a real param | **`ABSENT`** | hardcoded `0.7` — `NpuClient:109`, `CloudLlmRuntime:122` |
| 2.3 | `verbosity` as a real param | **`ABSENT`** | `SettingsPane:385-396` **writes** it; **no reader exists** |
| 2.4 | `cores` / `n_threads` | **`ABSENT`** | hardcoded `2` — `SherpaOnnxTtsClient:39`, `MoonshineSttEngine:122` |
| 2.5 | **Compute-unit selector** (`npu`/`hybrid`/`gpu`/`cpu`) | **`ABSENT`** | no `deviceId`, no `HTP0` |
| 2.6 | Voice DSP — pitch / speed / depth | `partial` | speed + voice exist; pitch/depth absent |
| 2.7 | Packet as a plain editable file | **`ABSENT`** | JSON store, not an operator-editable file |

**2.5 blocks the consumer-grade goal.** Without it the app cannot be told to
target the NPU strictly, disperse across NPU/GPU/CPU, or run naturally — and on a
device with no NPU there is no graceful path.

## 3 · Voice

| # | Thing | State | Evidence |
|---|---|---|---|
| 3.1 | Silero VAD ships in the APK | `unverified` | fetched in CI as of this session |
| 3.2 | VAD wired | `unverified` | `VadFactory` → `SileroVadDetector` |
| 3.3 | In-process STT — **Moonshine** | `unverified` | `MoonshineSttEngine` (from PR #33) |
| 3.4 | `SttEngine` interface | `built` | `core/stt/SttEngine.kt` |
| 3.5 | **Whisper STT engine** | **`ABSENT`** | no `OfflineWhisperModelConfig` |
| 3.6 | Engine family as a parameter | **`ABSENT`** | Moonshine hardcoded as the only impl |
| 3.7 | Kokoro TTS in-process | `unverified` | `SherpaOnnxTtsClient` |
| 3.8 | Kokoro resolves from device folder | `built` | `KokoroModelManager.refresh()` |
| 3.9 | Barge-in | `unverified` | `VoiceLoopController:39,212,222` |
| 3.10 | **~60 s hard utterance cap** | **`ABSENT`** | only match was the 5-min screensaver timer |
| 3.11 | Dead `:8091` daemon client removed | **`ABSENT`** | `DaemonSttClient` still present as a fallback layer |

## 4 · Runtime / execution

| # | Thing | State | Evidence |
|---|---|---|---|
| 4.1 | `DaemonLauncher` detached launch | `unverified` | `sh -T-`, reparent to init |
| 4.2 | `CliffordService` watchdog | `unverified` | FGS `specialUse`, `START_STICKY`, 5-strike |
| 4.3 | Serve-first / alive≠ready in the daemon | `unverified` | `main.cpp:77-86` |
| 4.4 | Cross-process config reload | `unverified` | `RouterConfigStore.reloadIfChanged()` |
| 4.5 | **NPU manager harness** (dual-model switch, OOM catch) | **`ABSENT`** | the `NpuManager` in `CliffordService:284` is Qualcomm's `@hide` **performance-lock** service — a different thing entirely |
| 4.6 | **Dual-model ping-pong orchestrator** | **`ABSENT`** | no `contextSwitch` / `swapModel` |
| 4.7 | **Recovery daemon** | **`ABSENT`** | no restore-from-Archives path |
| 4.8 | Cloud connectors (OpenRouter / SambaNova / custom) | `unverified` | `CloudLlmRuntime:50,59` |
| 4.9 | `http_server` 8 KB `recv` — `image_b64` truncates | **broken** | `http_server.cpp:22-29` |

**4.5 and 4.7 are coupled.** A dying process cannot report its own death, so OOM
recovery needs an external observer. Detection belongs in `:clifford`; the
in-APK harness coordinates but must not own it.

## 5 · Sockets and the mesh

| # | Thing | State | Evidence |
|---|---|---|---|
| 5.1 | **Inbound listener** | **`ABSENT`** | zero `ServerSocket`, zero Ktor |
| 5.2 | **WebSocket loopback bridges** | **`ABSENT`** | no WebSocket of any kind |
| 5.3 | Outbound loopback to a daemon | `unverified` | `NpuClient` |

5.1 is what blocks Termux getting mic / voice / WebView-OAuth. **NPU access does
not need it** — the app dials out.

## 6 · UI and OS integration

| # | Thing | State | Evidence |
|---|---|---|---|
| 6.1 | `HomeGrid` home screen | **FROZEN** | blob `618cf4b6` — do not touch |
| 6.2 | Monitor pop-out tabs | `unverified` | `MonitorPopout` enum |
| 6.3 | Browser (WebView, multi-window) | `unverified` | `BrowserPane` |
| 6.4 | Archives file manager | `unverified` | `ArchiveStore` |
| 6.5 | Terminal shell (non-interactive) | `unverified` | `TaskerBridge.runDirectShell` |
| 6.6 | **PTY — TUI programs (vim/htop/tmux)** | **`ABSENT`** | pipe, not a pseudo-terminal |
| 6.7 | Operator's manual + `manual` command | `unverified` | added this session |
| 6.8 | **Help-desk agent over the manual** | **`ABSENT`** | `ManualStore` exists; no tool entry |
| 6.9 | **Pinch-to-zoom** | **`ABSENT`** | needs operator sign-off — changes how the frozen screen renders |
| 6.10 | MediaProjection screen vision | `partial` | `ChatPane:154` references the manager |
| 6.11 | Floating Horizons live tile | **`ABSENT`** | |
| 6.12 | Download-to-vault | **`ABSENT`** | |
| 6.13 | Chat artifacts / attachments / embedded webview | **`ABSENT`** | |
| 6.14 | Bake & Export in Settings | **`ABSENT`** | |
| 6.15 | Recommended-models list in the manual | **`ABSENT`** | needs the operator's model names |

## 7 · GUI builds — design locked, build deferred

All three are LOCKED specs with reference images. Design settled, build order
deferred by the operator. Not started.

| # | Thing | State |
|---|---|---|
| 7.1 | Router — Aiwa stereo stack, animated | **`ABSENT`** |
| 7.2 | Monitor — arcade cabinet, CRT oscilloscope | **`ABSENT`** |
| 7.3 | Terminal — matrix cascade layering | `partial` — `drawMatrixRain` exists, paints flat |

`fakesteak` source is **not in the vault** — the file there is a PDF screenshot.
Real source: `github.com/c10vis-poem/fakesteak` (CC0). Attach it when 7.3 starts.

---

## Shortest path to a usable device build

In dependency order. Everything else can wait.

1. **1.8 amperage check** — arch + free RAM. The only guard against a silent LMK kill.
2. **2.2–2.5 parameters** — `temperature`, `verbosity`, `cores`, **compute-unit
   selector**. This is the tuner deck, and 2.5 is what makes the app work on a
   phone that isn't a Razr.
3. **3.5 + 3.6 Whisper engine + family as a parameter** — sibling class behind the
   existing `SttEngine` interface. Then measure Whisper vs Moonshine on device and
   close blueprint §8.1 with data.
4. **3.10 utterance cap** — a few lines; stops background noise hanging the stream.
5. **3.11 delete `DaemonSttClient`** — the `:8091` fallback still masks failures.
6. **4.9 `http_server` read loop** — required before vision payloads round-trip.

After that: 4.5/4.7 (harness + recovery), then 5.1/5.2 (sockets), then Phase 5 GUI.
