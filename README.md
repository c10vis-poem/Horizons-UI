# Novus Agenti · NovA Claw

**A fully on-device agentic AI assistant for Android**
 *"The unprecedented driving force."*

> Cl0vis × Mer0vin6ian · Project POEM. App package: `com.horizons`. Codebase
.

---

## What this is

Novus Agenti treats apps, terminals, and cloud runtimes as modular
subroutines and orchestrates them from the phone. Heavy math (model inference)
is split off from the UI: a lightweight **Kotlin app** drives a **detached
native inference daemon** that runs the model on the **Hexagon NPU**, and the
two talk over a local loopback HTTP socket.

**Target hardware:*`arm64-v8a`.

## Architecture

```
┌─────────────────────────┐        ┌──────────────────────────────┐
│  Kotlin app (com.horizons)│  HTTP  │  detached inference daemon    │
│  Compose UI · AgentLoop   │◄──────►│  runs the model on Hexagon NPU│
│  NpuClient · CliffordSvc  │ :8080  │  (GenieX ⟶ HTP SDK / QAIRT)   │
└─────────────────────────┘        └──────────────────────────────┘
        │  CliffordService (watchdog) keeps the daemon alive:
        │  START_STICKY · specialUse FGS · exponential-backoff relaunch
```

- **App** — Jetpack Compose UI, an `AgentLoop` with a tool layer, `NpuClient`
  as the bridge to the daemon, `DaemonLauncher` to spawn it detached.
- **Daemon** — serves the model behind `127.0.0.1:8080`. The daemon is
  **serve-first**: it binds the port immediately and reports "alive but not
  ready" (`/health` 503) while the model loads, so the watchdog never
  crash-loops it. (See `wiki/GENIEX-DAEMON-PLAN.md` and `daemon/src/`.)
- **Watchdog** — `CliffordService` (CLIFFORD == Watchdog): a foreground
  service that relaunches only a genuinely dead daemon, with backoff and a
  strike cap.

### Runtime — a host, not a hard-wired model

The app is a **model-agnostic, runtime-agnostic host**. The thing behind the
socket is a swappable, uploadable binary, and the model is whatever you import
(`ModelImportActivity` / `RUNTIME_FILES`) — nothing about a specific model or
runtime is baked into the app.

Current direction (decided session 15): **backend = HTP SDK / QAIRT, runtime =
[GenieX](https://github.com/qualcomm/GenieX)**, run as `geniex serve` (an
OpenAI-compatible server on `127.0.0.1:18181/v1`) behind the same watchdog.
GenieX has **two backends**: a **GGML / llama.cpp** path that runs GGUF models
(NPU/GPU/CPU), and **Qualcomm AI Engine Direct (QAIRT)** — NPU-only, max
performance, which loads a compiled AI Hub bundle. The legacy `ort_engine` C++
daemon (ONNX Runtime + QNN EP) remains as a fallback runtime. See
`wiki/GENIEX-DAEMON-PLAN.md`.

## The model (current instance — not baked in)

The app runs whatever model you give it; the **current instance** is
[`Mer0vin8ian/Qwen3.5-9B`](https://huggingface.co/Mer0vin8ian/Qwen3.5-9B) as a
**Q4_0 GGUF**, served today via GenieX's **GGML backend** (Q4_0 is Qualcomm's
recommended precision for Hexagon). Size envelope target **≈ 5.5 GB** (hard
redline 7.0–7.2 GB).

**The max-performance QAIRT / NPU path for the 9B is not turnkey yet.**
Qualcomm's prebuilt AI Hub library currently ships only smaller Qwen variants
(a Qwen3.5 variant and a 0.8B text-only), **not** the 9B — so running the 9B on
the QAIRT NPU backend means **BYOM-compiling it ourselves** through the QAI Hub
workbench (the `compile/compile_qwen3_5_9b.py` compile track / Job 8), which
produces the AI Hub bundle GenieX then loads. Until that lands, the 9B runs on
the GGML / Q4_0 path.

## Portability — one brain, many UIs

Because inference is a **detached daemon speaking OpenAI HTTP**, the app isn't
locked to this phone. New hardware and a desktop don't mean a rewrite:

- **The brain is already portable.** GenieX (`geniex serve` → `:18181/v1`) runs
  on **Android (8 Elite), Windows ARM64 (Snapdragon X Elite), and Linux ARM64
  (Dragonwing)** — same binary family, same HTTP contract. A new phone or a
  desktop just runs GenieX + the model; **no rebuild of the inference layer.**
- **The UI is a thin client over that HTTP API.** The target front-end is a
  **web UI** talking to `:18181/v1`, so a single frontend serves phone and
  desktop from any browser — no from-scratch rewrite per platform. (The current
  Compose UI stays as the native Android shell; the web UI is the
  cross-surface path.)

New surface = point a UI at the same `:18181/v1`, not build a new app.

## Repo layout

| Path | What |
|---|---|
| `horizons/` | the Android app (`com.horizons`) — UI, agent loop, NpuClient, CliffordService, DaemonLauncher |
| `daemon/` | `ort_engine` C++ inference daemon (legacy runtime; CI-built) |
| `compile/` | the dormant ONNX/QAI-Hub compile pipeline — `compile_qwen3_5_9b.py`, `manifest.yaml` (target list, build order), `requirements-compile.txt` — not an app model binding |
| `knowledge/` | hand-distilled reference corpus (Drive-mirrored + repo-native architecture notes — see its README) |
| `wiki/` | active plans, compile-pipeline detail, job/failure log |
| `rules/`, `agents/`, `skills/` | operating rules, sub-agent briefs, agent skills |
| `.github/workflows/build-apk.yml` | builds the APK + daemon, publishes a debug release |

## Build

AGP 8.8.0 · Kotlin 2.1.0 · compileSdk 35 · minSdk 31 · JDK 17 · `arm64-v8a`.
CI (`build-apk.yml`) cross-compiles the daemon, builds the APK, and publishes
both to a `latest-debug` GitHub Release. Signing uses `release/debug.keystore`
(committed by design for debug builds).

## Status (honest)

- App: **scaffolded** and building; UI, watchdog, and daemon bridge exist.
- Daemon: `ort_engine` **builds in CI**; not yet verified on a physical
  HTP v79 device against a real compiled model.
- Compile pipeline: **dormant** — fallback only, triggers if the primary
  GGUF/GenieX path hard-fails. Job 8 is ready but not triggered.
- GenieX runtime integration: **in progress** (planning + repo wiring; the
  on-device install/run is done via Termux on the target phone).

Not a shipping product yet — this is an active build.

## More context

Everything an agent or contributor needs to resume is in **`CLAUDE.md`**
(full project context + live State of the Union — the single current-state
doc, no separate handoff files). The canonical repo is `c10vis-poem/Novus-Agenti`.
