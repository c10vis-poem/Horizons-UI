# CLAUDE.md — Novus Agenti / Omni Claw

**This is the only project doc that exists now.** As of 2026-08-08 the operator
ended the multi-repo, multi-file "read this doc but not that doc" chain for
good. There is no vault-reading requirement, no precedence ladder across
13 folders, no "canon/ + horizons-ui/ is 59 of 337 files, don't miss
pending-corpora." This file is self-contained. If something isn't in here,
it isn't settled — say so and ask, don't go spelunking through
`c10vis-poem/nova-corpus` looking for it.

The vault (`c10vis-poem/nova-corpus`, previously "OBSIDIAN-Master_Wiki" —
that name is retired) still exists and still holds detail this file
summarizes rather than reproduces in full (see "Deep reference" at the
bottom). It is not required reading. Open it only when this file explicitly
points you there for one named document.

## RULE 0 — VERIFY AGAINST LIVE STATE BEFORE TRUSTING ANY DOC (mandatory, first, every session, no exceptions)

Even this file goes stale. Before treating anything below as current fact:

1. `git fetch origin && git log --oneline -10 origin/main` — get the real tip.
2. List open PRs (GitHub MCP `list_pull_requests`, `state=open`).
3. If `origin/main` has moved past what's described below, say so out loud,
   then re-derive current state from the live repo — grep the actual
   consumer of a feature, read the actual diff — before claiming anything
   works, is broken, or is missing.
4. Never call a feature "done," "broken," or "unbuilt" from prose alone. A
   comment is not code, a PR body is not the diff, a doc from two days ago
   can already be wrong. This has happened repeatedly and cost real time.

## Branch strategy — operator directive, 2026-08-08

**Going forward: two branches, period.** `main` (live, everything merges
here fast) and the frozen HomeGrid safety copy (`FROZEN-correct-home-screen-984b0610`
/ `RELEASE-correct-home-screen-984b0610`, both at blob `618cf4b6`, do not touch).

Everything else — the 19+ branches that accumulated from sessions each
opening a draft PR and never merging — is being killed. As of tonight: 11
stale PRs closed and their branches marked for deletion (#12 #13 #14 #16 #19
#20 #22 #24 #26 #27 #30 — six of those, #20 #22 #24 #26 #27 #30, were
HomeGrid-editing attempts that would have overwritten the freeze; none of
them are coming back). **PR #35** (this branch, `claude/novus-device-file-loading-ij4k4r`)
carries real fixes — `.systemBarsPadding()` insets fix + `StorageScanner.kt`
bulk file import — and is intentionally **not touched or merged by this
commit**; that decision is the operator's, still pending. **PR #36**
(`claude/app-ui-ux-issues-epl6dg`) carries this same RULE 0 doc work plus an
app-wide font-size floor fix; the operator may or may not act on it
separately.

**No more long-lived feature branches.** A session's work either merges to
`main` same-day or gets explicitly closed. Nothing sits open "to be safe."

## Hard stops

- `HomeGrid.kt` is **FROZEN** at commit `984b061` / blob `618cf4b6`. Never
  edit it, for any reason, without explicit operator sign-off. If it's
  implicated in a failure: stop, report, wait.
- Never push `main` without explicit permission. No `--no-verify`,
  `push --force`, `reset --hard` without confirming first.
- **A skipped or unanswered question is NOT consent.** Take no action
  without an explicit order.
- Don't trigger the dormant compile pipeline pre-emptively.

## Verified build state — as of 2026-08-08, checked against live code, not prose

| Area | State | Where |
|---|---|---|
| HomeGrid home screen | built, frozen, correct | `HomeGrid.kt`, blob `618cf4b6` |
| Router / Monitor / Terminal / Settings panels | built, exist, render | `ui/panels/*.kt` |
| Router `loadConfig()` (replaces old `switchOn()` gate) | merged (PR #33/#34) | `RouterPane.kt` |
| Moonshine STT engine (class) | merged (PR #33), **not wired to any UI** | `core/stt/MoonshineSttEngine.kt` |
| Kokoro TTS (class) | exists, **not wired to any UI** | `core/voice/KokoroModelManager.kt` |
| `HorizonsVoiceInteractionService` | exists, 25 lines, **not called from `MainActivity`** | `assist/HorizonsVoiceInteractionService.kt` |
| **Voice, end to end** | **not reachable from the app** — classes exist, nothing turns them on | grep confirms zero `SttEngine`/`startListening`/`VoiceInteraction` refs in `MainActivity.kt` |
| Terminal panel | shells out to a **separately-installed Termux app** via `RUN_COMMAND` intent; says "Termux not installed" if it's absent | `ui/panels/TerminalPanel.kt` (1079 lines), `core/shell/TaskerBridge.kt` |
| **In-app conversational/CLI agent** | **does not exist.** No describe→draft→confirm→run→auto-fix loop, no in-process agent. Spec for one exists (see below), zero code | — |
| Cloud connectors (OpenRouter, SambaNova, custom) | built, SSE streaming | `core/llm/CloudLlmRuntime.kt` |
| `greenLight()` four-check gate | all four exist | `core/state/RuntimeDefStore.kt:119-149` |
| Silero VAD | fetched by CI now (was silently degrading to RMS before PR #34) | — |
| Font sizes app-wide | fixed on PR #36 (was 8-11sp dominant, floor now 12sp) — **not yet merged** | `ui/panels/*.kt` |
| Shared `Typography` scale | still does not exist — `HorizonsTheme.kt` only defines colors, every panel hardcodes its own sizes | `ui/theme/HorizonsTheme.kt` |
| Monitor pane zoom / expand | not built. Spec calls for pinch-to-zoom + inset padding (see below) | — |
| Wallpaper-uploadable backgrounds (Horizons/Artifacts/Settings/Chat, semi-transparent overlay) | not built, no code | — |
| Vault/not-vault visual indicator | not built | — |
| **OmniRoute** (AI gateway, 160+ providers, one endpoint) | forked to `c10vis-poem/OmniRoute`, **zero references anywhere in `horizons/src`.** Not deployed on-device, not called by `CloudLlmRuntime` or anything else | — |
| **Dual-agent memory system** (Mem0 + OB1 + reasoning-bank) | forked to `c10vis-poem/mem0`, `OB1`, `reasoning-bank` under AESOP. **Zero references anywhere in `horizons/src` or `knowledge/`.** Specced, never touched by code | — |
| Popup / long-press help UI | not built anywhere. Spec exists (long-press → description, see UX-RULES below) | — |
| Live chaptered user manual inside Terminal | not built | — |

## The three LOCKED visual tile specs — design settled, build not started

These are not concepts — the operator supplied reference images and said
"this is what it's going to be," which makes them specifications (Rule 7b:
visual references are literal build targets, not styling direction). Full
text lives in the vault (`canon/horizons-ui/{ROUTER-STEREO-STACK,MONITOR-ARCADE-CABINET,TERMINAL}-SPEC.md`)
on an **unmerged vault branch** (`claude/novus-device-file-loading-ij4k4r`,
same name as this Horizons-UI PR — a companion docs PR, also unmerged as of
tonight). Summary, since that branch is where the *current* spec text lives,
not the vault's `main`:

**Router = component stereo stack** (Aiwa NSX-V20 reference). CD deck top =
models (animated tray, spinning carousel, tap a disc → green LCD-style
fine-tune popup with `Model_`/`Engine_`/`Runtime_`/`Config._` picker rows and
`[load] swap [save] edit` — picker-only, no free text). Tuner deck middle =
runtime parameters (temperature, verbosity, cores, hardware target
npu/hybrid/gpu/cpu, cloud-vs-local toggle, voice DSP). Cassette deck bottom =
two wells, Browse (reaches Settings/Archives) and Load (plates a runtime;
execution modes: double-agent ping-pong, single chatbot, mixture-of-agents,
cloud connector, terminal agent). All six home tiles push to the Router.
Overflow bounces to origin tile + GOAT face, never a hard failure.

**Monitor = arcade cabinet** (neon upright, CRT-oscilloscope screen). Lit
marquee, CRT screen (browser/library/green-lights), instruction placard,
control deck. Pop-out tabs: CONSOLE/TERMINAL/BROWSER (already built
functionally, styling not done). **Pinch-to-zoom on the Monitor face** (or
press-to-zoom fallback on the Floating Live Tile) plus **inset padding**
against status/gesture bars — this is the fix for "Monitor stuck tiny" and
"panels running under the bars."

**Terminal = matrix-cascade console** (`c10vis-poem/fakesteak` reference,
own fork). `drawMatrixRain` already exists in code — the gap is layering: a
black console panel floating over visible rain (top edges, underneath,
through the tile below), not a flat paint. Console surface itself reads as
a CRT oscilloscope (green graticule, amber waveform, idle = flat line, active
= live trace responding to commands/mic/token-stream) — this is the answer
to "terminal's screensaver." Terminal also gets: real Termux `RUN_COMMAND_SERVICE`
integration (currently a stub that just says "not installed"), and a
**describe → draft → confirm → run → auto-fix on-device agent** (user
describes intent in natural language or picks from a menu, the model drafts
the shell command, user approves/edits, it runs with live streamed
output, failures auto-generate a corrective draft) — reference pattern:
`c10vis-poem/c10vis-llm-hub`. This is the "actual working terminal with an
actual agent" gap. Terminal can also port itself to become the Router's
active agent when the Router is idle.

## What's genuinely broken vs. what's just unbuilt

Broken (code exists, does the wrong thing): font sizes too small (fix on
PR #36, unmerged), no shared Typography scale, panels run under system
bars (fix exists on PR #35, unmerged), 20 of 44 reference images in the
vault's `HOME-REDESIGN-SPEC.md` are uploaded but never wired into the doc
(all the "Copy of ..." files, numbered 25-44).

Unbuilt (no code, spec exists): Router/Monitor/Terminal visual rebuilds,
wallpaper-uploadable panels, popup/help UI, live user manual, OmniRoute
integration, Mem0/OB1 memory system integration, Monitor zoom.

Unwired (code exists, never connected): voice (STT/TTS classes present,
`MainActivity` never calls them), Terminal's Termux bridge (present, requires
a separately-installed app, no in-process agent).

## Architecture — settled, do not re-litigate

- Runtime pathways, both live, selected by GenieX `plugin_id`: `llama_cpp`
  (GGUF Q4_0, NPU via ggml-hexagon / GPU OpenCL / CPU) and `qairt` (QAIRT
  `.bin` shards + `geniex.json`, NPU only). TFLite/LiteRT/QAT are input
  formats to the AI Hub compile, not a runtime choice. Quantization: Q4_0,
  settled.
- Model residency: every model in its own device folder, loaded by absolute
  path. APK never downloads weights. Drag-and-drop to swap.
- Voice: in-process on the sherpa-onnx AAR, on device. **Never on the NPU**
  — the actions and query models already take turns there.
- WIRED != LAUNCHED: the APK should be *capable* of everything on the
  phone; what's forbidden is loading and landing with it by default.
- Daemon split: LLM inference (+vision) separate from the watchdog/recovery
  in `:clifford` — a dying process can't report its own death.
- Authority model (series circuit): Settings supplies, no run authority.
  Terminal forges/configures/executes, pushes to Router. Monitor is the
  switch — verifies live at flip time, stores nothing, dispatches. Router
  is fuse box + breaker — carries current, never argues, never says no. A
  failed flip is a circuit that didn't energize, not a wall the app throws
  up. Archives stores verified profiles.
- No typing outside Terminal/browser; everywhere else is picker/button-driven
  (workbench UX rule). Long-press any control anywhere → plain-language
  description. Zoom on Home and Monitor. Inset-cropping on every room except
  Home, which alone draws edge-to-edge.

## Build / CI

AGP 8.8.0 · Kotlin 2.1.0 · compileSdk 35 · minSdk 31 · JDK 17 · arm64-v8a
only. Signing: `release/debug.keystore` (committed by design). `build-apk.yml`
cross-compiles `ort_engine` via CMake/NDK, builds the APK, publishes both
plus `libonnxruntime.so` to a `debug-<branch>` GitHub Release on every push
— that's how you get a sideloadable build per branch/PR.

## Brand

Background `#222C34` · Surface `#35414A` · Primary teal `#2DD4D9` ·
Highlight teal `#4FE7EC` · Icon backplate `#050709` · Action yellow
`#F5C518`. Backdrop: pure Compose `Brush.radialGradient`, not XML shape.

## Device

Motorola Razr Ultra 2025 · SM8750 · 16GB · Hexagon HTP v79. Phone only, no
laptop. No tokens/long URLs in paste-able commands; keep shell commands
short.

## Deep reference (optional, not required reading)

Full text of the LOCKED tile specs, the master build blueprint, the
parameter-packet design, and session-by-session history live in
`c10vis-poem/nova-corpus`. Open a *named* file there only when this doc
sends you to one — don't re-read the corpus "to be safe." As of tonight the
vault's own `main` is one commit behind its most current real content; the
companion branch `claude/novus-device-file-loading-ij4k4r` (unmerged) is
where the current Router/Monitor/Terminal spec text and `STATE-OF-EXISTENCE.md`
updates actually are. That vault branch needs an operator merge decision
same as this repo's PRs did — same disease, same fix.
