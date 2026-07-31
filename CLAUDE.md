# CLAUDE.md — Novus Agenti / Omni Claw

> **RESUME PROMPT — COPY THIS BLOCK VERBATIM TO START ANY NEW SESSION**
>
> ```
> Project: Novus Agenti (Omni Claw) — on-device agentic AI assistant.
> App repo: c10vis-poem/Horizons-UI   Vault: c10vis-poem/OBSIDIAN-Master_Wiki
> Protocol: c10vis-poem/aesop        Termux CLI: c10vis-poem/claude-code-android
>
> ### FIRST: git branch -a. DO NOT ASSUME main IS THE BASE.
> `main` does NOT contain the working home screen. The correct base is
> RELEASE-correct-home-screen-984b0610 (= frozen 984b061 + 2 CI-only commits).
> The two lineages differ in app code ONLY in HomeGrid.kt + its 2 fonts.
> HomeGrid.kt is OPERATOR-FROZEN at 984b061 — do not touch it, for any reason,
> including a CI-breaking fix, without explicit sign-off.
> Active branch (both repos): claude/app-crash-landing-yc955p
> Horizons-UI PR #32 (draft, base = RELEASE-correct-home-screen-984b0610).
> APK: releases/tag/debug-claude-app-crash-landing-yc955p -> horizons.apk
>
> ### WHAT THIS IS
> A manual, modular workbench, NOT a black box. Core law: "Daemons stay dumb,
> the user is the loader" — boots EMPTY and stable; nothing runs until the user
> flips a fuse. Seven tiles feed a center-hub Router.
>
> AUTHORITY MODEL (operator, 2026-07-31) — the circuit is IN SERIES:
>   Settings = supply (assets/keys). Can hand a config to the Router.
>              NO authority to run it.
>   Terminal = writes the fuse spec (parameters only). Never executes.
>   Monitor  = THE SWITCH IN THE LOOP. Verifies wiring. Open/loose => no
>              circuit, however perfect the fuse. Stores nothing, DISPATCHES.
>   Router   = fuse box + breaker. Carries current. Doesn't argue.
> Validation must be LIVE at flip time (a series switch has no memory), but the
> check belongs to the MONITOR. RouterPane.switchOn() currently re-implements it
> instead of consulting it — that is the one real defect. The operator EXPLICITLY
> REJECTED the Router as hardened gatekeeper; the gate is the Monitor's.
>
> ### THE CRASH (why this branch exists)
> Symptom: crashes ~90s after landing, worse each launch, "still trying to
> connect." FIXED + CI green (unverified on device):
>  - FailureMonitor.install() built a full report SYNCHRONOUSLY ON THE MAIN
>    THREAD in Application.onCreate(), walking every crash + log file.
>  - Every "tail" read was really readText()/readLines() on the WHOLE file.
>  - Breadcrumb crash.log had NO cap and NO rotation.
>    => closed loop: crash -> bigger log -> heavier boot -> more crashes.
>  - CliffordService runs in :clifford, so its RouterConfigStore was a DIFFERENT
>    INSTANCE that loaded once in init and never re-read => a Router flip in the
>    UI was permanently invisible to the launcher. Fixed via reloadIfChanged().
> STILL UNPROVEN: the trigger of the FIRST ~90s crash. Prime suspect is Android
> LMK (leaves NO stack trace) — Kokoro pulls ~200MB at boot then loads a 326MB
> ONNX in-process. Get the answer on device, no laptop needed:
>   cd /sdcard/Android/data/com.horizons/files/diag && tail -40 crash.log
> Stack trace present => JVM exception. Empty but app died => killed from
> outside (LMK/FGS), and no trace will ever appear. boot.log lines are now
> tagged [main]/[clifford] so "did :clifford come up?" is answerable.
>
> ### PHASE 1 SCOPE (operator): the app must run its WebView browser, its voice
> layer, and a backend for the Termux agent. NOT about hosted models.
>  - Browser: DONE — moved to Monitor, shared component, links open new tabs.
>  - Monitor pop-out tabs CONSOLE/TERMINAL/BROWSER: DONE.
>  - Voice: BROKEN IN THE MIDDLE. TTS works in-process (sherpa AAR). STT points
>    at 127.0.0.1:8091 and NOTHING BINDS IT, so the loop falls back to
>    llmRuntime.streamAudio and looks like a model problem. It isn't.
>    FIX = run Moonshine STT in-process on the AAR already shipping.
>  - Termux backend: DOES NOT EXIST. The app has ZERO inbound listeners.
>    NOTE: the app-spawned daemon already binds 127.0.0.1:8080 under the app's
>    UID and Termux shares loopback — so NPU access needs no inversion. What
>    Termux can't get on its own is mic/voice/WebView-OAuth. That's the listener.
>
> ### KNOWN GAPS
>  - temperature is hardcoded (NpuClient:101, CloudLlmRuntime:122). Verbosity
>    HAS a Settings slider that NOTHING READS. Cores don't exist. These must
>    become real RuntimeDef params BEFORE P1.3 (runtime-agnostic launcher), or
>    the launcher gets reopened twice.
>  - greenLight() checks only 2 of the canon's 4 boxes (engine, assets). No
>    arch/RAM amperage check, no handshake check. And switchOn() SKIPS THE GATE
>    ENTIRELY when no RuntimeDef matches — so cloud/PWA/terminal configs bypass
>    the Monitor today.
>  - HomeGrid.kt:69 computes npuReady from startsWith("Adreno 830"), which the
>    NO-BACKEND fallback string also matches. FROZEN — report, don't fix.
>  - Two launcher icons: .MainActivity AND .uilocal.LocalHomeActivity both carry
>    MAIN/LAUNCHER. They run different code; test the tile icon. Operator asked
>    about removing the second LAUNCHER entry — not yet done.
>
> ### KNOWLEDGE — Drive is the source of truth
> Use mcp__Google_Drive__* directly; the operator's material is all there.
> Vault repo now carries the FULL migration: 1305 files, 363 md, 96 jsonl,
> RAG_LIBRARY + BM25 index. To pull more Drive content use the
> drive-to-obsidian-migration skill — do NOT hand-sync.
> Two data-bank docs are AI transcripts, not specs (the "Gemini duel" doc
> retracts its own central claim; 2026-07-17 is one the operator calls a snow
> job). Architecture in them is canon; implementation claims are not.
> UI direction captured: ROUTER = stereo stack, MONITOR = arcade cabinet,
> TERMINAL = fakesteak matrix cascade. NOT to be built yet.
>
> Use /memory to reload. HF_TOKEN / QAI_HUB_API_TOKEN come from the environment.
> Never hardcode them.
> ```

---

## /memory — Slash Command

Type `/memory` in any Claude Code session to reload full project context.

**Sequence (all first-read = MARKDOWN; JSONL is grep-only, never first-read):**
1. Read `CLAUDE.md` (this file, all sections, incl. the current
   `## State of the Union` — there is no separate handoff file)
2. Read `knowledge/omni-claw-defined/` — what the app IS + how it works
3. Read `EXECUTIONS.md` — the build dock
4. For anything else, use the `project-memory` skill (knowledge/ -> vault -> Drive)
5. Produce a SOTU summary + next action, confirm before touching any file

---

## State of the Union — 2026-07-31 (session 20)

**Repo moved.** Work is in `c10vis-poem/Horizons-UI` now, not `Novus-Agenti`.
Vault is `c10vis-poem/OBSIDIAN-Master_Wiki`. Protocol spec is `c10vis-poem/aesop`.

### The base-branch trap — read before anything

`main` does **NOT** contain the working home screen. The correct base is
**`RELEASE-correct-home-screen-984b0610`** (frozen `984b061` + 2 CI-only
commits). The lineages differ in app code **only** in `HomeGrid.kt` + 2 fonts.
This session started on `main` by mistake and had to re-base. **Run
`git branch -a` before choosing a base** — there is more real work on unmerged
branches in these repos than on `main`.

### Session 20 — crash root-caused, browser moved, vault recovered

- **The diagnostics were amplifying the crash.** `FailureMonitor.install()` ran
  a full disk-walking report **synchronously on the main thread** in
  `onCreate()`; every "tail" read was a whole-file `readText()`/`readLines()`;
  `crash.log` had **no cap and no rotation**. Closed loop: crash → bigger log →
  heavier boot → more crashes. All three bounded; new `core/diag/FileTail.kt`
  does seek-based reads. The operator called this before I did.
- **The fuse could never reach the daemon.** `CliffordService` runs in
  `:clifford`, so its `RouterConfigStore` was a *different instance* that loaded
  once in `init` and never re-read — a Router flip in the UI was permanently
  invisible to the launcher. Fixed with `reloadIfChanged()`.
- Breadcrumbs now tagged `[main]`/`[clifford]` — "did `:clifford` come up?" is
  finally answerable from the log.
- **Browser moved to Monitor** as a shared `ui/browser/BrowserPane.kt`;
  `setSupportMultipleWindows` was `false` so links could never open tabs — fixed,
  which also unbreaks OAuth popups. Monitor gained CONSOLE/TERMINAL/BROWSER
  pop-out tabs.
- **PR #32 open (draft), CI green, APK published.** Not verified on device.
- **Vault recovered.** The full Drive→Obsidian migration (1300 files, RAG library
  46 docs/1786 chunks, BM25 index) existed unmerged on
  `claude/drive-obsidian-migration-th115b` while `main` sat empty and four
  `docs/mirror-*` branches carried 82 files. Merged. Vault is now 1305 files.

**Still unproven:** the trigger of the first ~90 s crash. Prime suspect is
Android **LMK** — leaves no stack trace. Needs the on-device `crash.log`.

### Phase 1 (operator scope): browser · voice · Termux backend

Explicitly **not** about hosted models.

| Piece | State |
|---|---|
| WebView browser | **DONE** — in Monitor, tabs work |
| Monitor pop-outs | **DONE** |
| Voice layer | **BROKEN MID-CHAIN** — TTS in-process ✓, STT points at dead `:8091`. Fix: Moonshine in-process on the AAR already shipping. |
| Termux backend | **ABSENT** — app has zero inbound listeners. Needed for mic/voice/OAuth, *not* NPU (the app-spawned daemon already binds `:8080` and Termux shares loopback). |

### Authority model — LAW (operator, 2026-07-31)

Series circuit, two independent authorities. **Settings** supplies and may hand
a config to the Router but has **no authority to run it**. **Monitor is the
switch in the loop** — stores nothing, but dispatches, and holds the verdict.
**Router** is fuse box + breaker: carries current, doesn't argue. Validation is
**live at flip time** (a series switch has no memory) but the check is the
**Monitor's** — `RouterPane.switchOn()` re-implementing it is the one real
defect. The operator **explicitly rejected** the Router-as-gatekeeper.

### Next, in order

1. **Moonshine STT in-process** — smallest change, closes the voice loop.
2. **Termux backend listener** — exposes voice/mic/OAuth over loopback.
3. **Runtime params first-class** (temperature/verbosity/cores) **before** P1.3,
   or the launcher gets reopened twice.
4. **P1.3** — drive `DaemonLauncher` from the flipped `RuntimeDef`.
5. **Close the `switchOn()` bypass** — configs with no `RuntimeDef` skip the
   gate entirely today, so cloud/PWA/terminal never reach the Monitor.

### Flagged, not actioned

- `HomeGrid.kt:69` reports NPU-ready for the *no-backend* fallback string.
  **FROZEN — report only.**
- Two launcher icons (`.MainActivity` + `.uilocal.LocalHomeActivity`) run
  different code; confusing during triage. Removing the second `LAUNCHER` entry
  is a pending operator call.
- `verbosity` has a Settings slider **nothing reads**; `debugLogLevel` likewise.

---

## Repo File Map

```
c10vis-poem/Horizons-UI  (public)  — vault: c10vis-poem/OBSIDIAN-Master_Wiki

CLAUDE.md                     ← THIS FILE (architecture-of-record + current SOTU)
agents/
  build-runner.yaml             horizons-build-runner (Android CI, separate from compile)
  sub-agent.system.md           Novus-Agenti stack (single canonical agent brief)
daemon/                          ort_engine C++ daemon (legacy runtime, CI-built)
  src/engine.cpp, http_server.cpp, tokenizer.cpp, sampler.h, main.cpp
rules/
  AAR_DECOMPILE.md              QNN artifact inspection (archived, Nexa-era)
  AT_BAT_PROTOCOL.md
  CACHE_PROMPT_RULES.md
  GIT_HYGIENE.md
skills/
  horizons-wiki/SKILL.md        architecture bundle (CLAUDE.md + daemon-reference)
  project-memory/SKILL.md       knowledge/ corpus retrieval (two-tier)
  termux-mobile-dev/SKILL.md
knowledge/                       project knowledge corpus (see README.md)
  omni-claw-defined/             ALWAYS-READ core project definition
  research-npu/  proofs/  fragmented-qat/  google-dev-docs/  gemini-query/
                                  Drive-mirrored, retrieve-on-demand
  qairt-sdk/                      Drive-mirrored (QNN HTP manual, .md + .jsonl)
  daemon-reference/               repo-native (moved from wiki/): GPT-DAEMON-REFERENCE.md,
                                  NPU-RUNTIME-PATHS.md
  claude-code-reference/          general Claude Code knowledge (moved from wiki/):
                                  PROMPT-CACHING.md — reference only, hard rules are in
                                  this file's Cache Prompting section, not there
  device-inventory/               recovered on-device audit snapshot (2026-07-13):
                                  DEVICE-INVENTORY.md — SDKs, model files, Termux
                                  toolchain actually on the Razr Ultra; re-verify
                                  before trusting exact versions/sizes
compile/                        dormant compile-pipeline domain (was models/ + scripts/,
                                  merged since both only ever served this one pipeline)
  manifest.yaml                  FALLBACK ONLY — see its own header
  compile_qwen3_5_9b.py          fallback compile script (dormant, see wiki/COMPILE-PIPELINE.md)
  requirements-compile.txt       pip deps for the staged Colab compile
wiki/
  COMPILE-PIPELINE.md            dormant fallback pipeline (Single-Path Architecture,
                                  Size Envelope, Hexagon HTP Constraints, Job 8 command)
  GENIEX-DAEMON-PLAN.md          GenieX runtime plan + model/vision daemon split
  JOB_EXECUTION_LOG.md           combined compile-job + strike/failure ledger
  FEATURE-SPEC.md                UI tile spec
  BUILD-ACTION-PLAN.md
  research/                      reference notes on forked tools (android-reverse-engineering-skill,
                                  claude-skills) — not project architecture, kept separate from knowledge/
horizons/                        Android app
  fgs/CliffordService.kt         Watchdog daemon
  core/llm/NpuClient.kt          model+vision daemon client
  core/stt/DaemonSttClient.kt    media daemon client (STT half)
  core/tts/DaemonTtsClient.kt    media daemon client (TTS half, contract only)
  core/shell/DaemonLauncher.kt
  core/agent/AgentLoop.kt
  uilocal/LocalHomeActivity.kt   local UI fork (session 16), additive
.github/workflows/build-apk.yml
release/debug.keystore           committed by design
```
`watchdog/` was already deleted — don't look for it. There is no
per-session handoff file (`wiki/SESSION{N}-HANDOFF.md`) or standalone
`wiki/APP-SOTU-AUDIT.md`/`wiki/FAILURE_LOG.md` anymore — consolidated
into this file's SOTU and `wiki/JOB_EXECUTION_LOG.md` respectively.
`.github/workflows/build-apk.yml`'s publish-target TODO could not be
confirmed still real (no foreign repo found hardcoded anywhere in its
history) — verify against a live release page before assuming it needs
work.

---

## Hard Rules

- **`HomeGrid.kt` IS FROZEN at commit `984b061`** (2026-07-27, "ROUTER plate
  down 24dp") — operator-confirmed as the final, correct layout. No agent or
  session touches `horizons/src/main/java/com/horizons/ui/HomeGrid.kt` for ANY
  reason — not a layout tweak, not a "small" fix, not even a build-critical or
  CI-breaking fix — without the operator's explicit go-ahead. If it is
  implicated in a failure: stop, report, wait for sign-off.
  Icebox copies (blob `618cf4b6`): `FROZEN-correct-home-screen-984b0610`,
  `claude/homegrid-v5-tuned`, `RELEASE-correct-home-screen-984b0610`.
  Earlier snapshot: `claude/homegrid-v5-SNAPSHOT-good-1837dc2` (`725306d`).
- Never push `main` without explicit user permission
- Never `--no-verify`, `push --force`, `reset --hard` without confirming
- No CPU fallback in the Qwen3.5-9B path (NPU or nothing for that model)
- No in-process tensor runtime — every model runs via its own uploadable daemon binary
- `M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti` is REFERENCE-ONLY
- Don't trigger the dormant compile pipeline pre-emptively — see
  `wiki/COMPILE-PIPELINE.md` for its own hard rules (`SKIP_VISION`,
  `max_dynamic_tensor_size_mib`), which only matter if/when that pipeline
  actually runs

---

## Build / CI

- AGP 8.8.0 · Kotlin 2.1.0 · compileSdk 35 · minSdk 31 · JDK 17 · arm64-v8a only
- Signing: `release/debug.keystore` (committed by design)
- `build-apk.yml` cross-compiles `ort_engine` (daemon/) via CMake/NDK,
  builds the APK, publishes both plus `libonnxruntime.so` to a
  `latest-debug` GitHub Release. Publish target already defaults to
  this repo (`softprops/action-gh-release@v2` has no `repository:`
  override) — an old TODO here claiming otherwise could not be verified
  as still real; if a CI run's release step actually misfires, check
  the repo's Settings → Actions → General → Workflow permissions first
  (that's what broke it once this session, not the publish-target).

---

## Brand

- Background `#222C34` · Surface `#35414A` · Primary teal `#2DD4D9`
- Highlight teal `#4FE7EC` · Icon backplate `#050709` · Action yellow `#F5C518`
- Backdrop: pure Compose `Brush.radialGradient` — NOT XML shape

---

## Termux / Mobile Rules

**Device:** Motorola Razr Ultra 2025 · SM8750 · 16GB · Hexagon HTP v79. **Phone only. No laptop.**

- No tokens or long URLs in paste-able commands
- Shell variables: short alias then `$VAR`
- Every paste-able command under ~50 chars where possible

---

## What Was Ripped Out — Do NOT Reference

Scoped to the Qwen3.5-9B build path. Other model families ship their own
runtime binaries; this table is not a constraint on future runtimes.

| Old | Replaced by |
|---|---|
| Track 1 / Track 2 (for Qwen3.5-9B) | single path: ONNX → QNN context binary → Hexagon HTP |
| LiteRT / LiteRT-LM (for Qwen3.5-9B) | ort_engine daemon |
| genie_engine (for Qwen3.5-9B) | ort_engine (ORT + QNN EP) |
| Separate Watchdog | CliffordService (CLIFFORD == Watchdog) |
| Nexa SDK, OmniNeural | dead |
| Cloud failover in app LLM | HttpFetch agent tool |
