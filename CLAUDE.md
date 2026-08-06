# CLAUDE.md — Novus Agenti / Omni Claw

> **RESUME PROMPT — COPY THIS BLOCK VERBATIM TO START ANY NEW SESSION**
>
> ```
> Project: Novus Agenti (Omni Claw) — on-device agentic AI assistant.
> App repo: c10vis-poem/Horizons-UI   Vault: c10vis-poem/OBSIDIAN-Master_Wiki
> Protocol: c10vis-poem/aesop         GenieX fork: c10vis-poem/GenieX
>
> ### SPECS LIVE IN THE VAULT, NOT HERE. READ THEM FIRST, IN FULL.
> Vault `main` is current (PR #5 merged). Start with, in order:
>   canon/SOURCE-PRECEDENCE.md      which source wins when two disagree
>   canon/STATE-OF-EXISTENCE.md     the ONLY build-state authority
>   canon/MASTER-BUILD-BLUEPRINT.md the target, W5+H, build map §12.1
>   canon/horizons-ui/WHAT-IT-IS.md + FEATURE-INVENTORY.md
>   horizons-ui/AGENT-BRIEF.md      hard stops + which local docs lie
> The vault has 337 md files across 13 top-level dirs. canon/ + horizons-ui/
> is 59 of them. pending-corpora/ is PRECEDENCE RANK 2 — higher than the
> locked visual specs — and is easy to miss. Do not claim you read the
> corpus after reading two folders. (A prior session did exactly that.)
>
> ### BRANCH REALITY — verified 2026-08-06, trust this over older docs
> `main` == 7b9e5db. It HAS the correct frozen HomeGrid (blob 618cf4b6).
> RELEASE-correct-home-screen-984b0610 is 9 commits BEHIND main, 0 ahead.
> **The old "main does NOT have the working home screen" warning is STALE.**
> Basing off RELEASE now DISCARDS work. Verify with:
>   git rev-parse origin/main:horizons/src/main/java/com/horizons/ui/HomeGrid.kt
>
> ### THE ACTUAL DISEASE: 19 OPEN DRAFT PRs, NONE MERGED
> Every session branched from main, built something real, opened a draft PR,
> got CI green, and stopped. So main never accumulates and each session
> rediscovers or rebuilds what already exists elsewhere. This is why the
> docs and the code disagree: the docs describe the UNION of 19 branches,
> any session sees only main. It is a merge problem, not a docs problem.
>
> SIX of those PRs actively edit HomeGrid.kt and would OVERWRITE THE FREEZE:
>   #30 #27 #26 #24 #22 #20  ← do not merge without operator sign-off
> PR #33 carries the CORRECT blob plus real work (see below).
>
> ### PR #33 — read it before writing anything in Router/params/voice
> `claude/repo-restructure-crash-analysis-j9v9fl`, open, draft, unmerged.
> Contains: core/stt/MoonshineSttEngine.kt (in-process STT on the sherpa
> AAR, no download, user-is-the-loader), switchOn() driving DaemonLauncher
> from the RuntimeDef, NpuClient taking port/healthPath. Its BODY documents
> only the Router change — the STT engine is invisible from the description.
> READ THE DIFF, NOT THE WRITE-UP. It explicitly did NOT touch gate
> semantics and did NOT add the arch/RAM check.
>
> ### HARD STOPS
> HomeGrid.kt is FROZEN at 984b061 / blob 618cf4b6. Never edit it, for any
> reason, without explicit operator sign-off. The home screen is DONE.
> If a doc disagrees with the screen, the doc is wrong.
> Never push main. No --no-verify / push --force / reset --hard.
> A skipped or unanswered question is NOT consent. Take no action without
> an explicit order.
>
> ### ARCHITECTURE — operator-confirmed 2026-08-06
> Runtime pathways, both LIVE, selected by GenieX `plugin_id`:
>   llama_cpp — GGUF (Q4_0), NPU via ggml-hexagon / GPU OpenCL / CPU
>   qairt     — QAIRT .bin shards + geniex.json, NPU ONLY, max performance
> TFLite / LiteRT / QAT are INPUT FORMATS to the AI Hub compile, which emits
> the per-chipset bundle qairt loads. "The GGML route" = the llama.cpp
> RUNTIME. It is NOT Google LiteRT — LiteRT appears in these docs only as
> the REJECTED in-process option.
> Quantization + model selection: SETTLED. Q4_0. Do not reopen or re-analyse.
>
> MODEL RESIDENCY: every model gets its own isolated device folder and loads
> by absolute path. The APK NEVER downloads weights. Drag-and-drop to swap.
> Same storage either way — downloading only buys a boot-time failure mode.
>
> VOICE: in-process on the sherpa-onnx AAR, on device, through the APK.
> **NEVER on the NPU** — the actions model and query model already take turns
> there. This closes blueprint §8.1. Termux+proot voice was a temporary
> stopgap and is discarded; port its parameters, not its architecture.
> Measured int8 sizes: Whisper tiny.en 104MB · Moonshine tiny 124MB ·
> Whisper base.en 161MB · Moonshine base 287MB. (STATE-OF-EXISTENCE §2's
> "Moonshine is materially smaller" is FALSE — base is 78% larger.)
>
> WIRED != LAUNCHED. The APK should be capable of everything on the phone —
> sockets, the harness, permissions, an initial runtime for its own backend,
> NPU manager + file search, WebView/Chromium hooks, API + cloud inference,
> OpenRouter fallback. What is forbidden is LOADING AND LANDING with it.
>
> DAEMON SPLIT (operator, 2026-08-06): a dying process cannot report its own
> death, so OOM recovery REQUIRES an external observer. Separate: LLM
> inference (+ vision co-located), and the watchdog/recovery in :clifford.
> In-process: voice, sockets, permissions, file search, UI. The NPU manager
> stays in-APK for coordination but must NOT own OOM detection.
>
> ### AUTHORITY MODEL — series circuit
> Settings supplies (no authority to run) · Terminal forges parameters only,
> never executes · MONITOR is the switch, verifies LIVE at flip time, stores
> nothing, dispatches · Router is fuse box + breaker, carries current,
> DOESN'T ARGUE · Archives stores verified profiles, recovery restores here.
> The Router NEVER says no. A failed flip is a circuit that didn't energise,
> not an app throwing a wall. The operator EXPLICITLY REJECTED the hardened
> gatekeeper. Rule 7a: behavioural metaphors are NEVER compiled. Rule 7b:
> the supplied visual references ARE literal build specs.
>
> Use /memory to reload. HF_TOKEN / QAI_HUB_API_TOKEN from environment.
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
## State of the Union — 2026-08-06 (session 21)

**This session did almost no coding on purpose.** It read the corpus, verified
claims against live code, and found that a large share of what the last several
sessions "knew" was wrong. The corrections matter more than the one fix that
landed.

### The disease, finally named: 19 open draft PRs, none merged

Every session since roughly PR #6 branched from `main`, built something real,
opened a **draft** PR, got CI green, and stopped. `main` therefore never
accumulates, and each new session starts from a `main` missing all of its
predecessors' work — so it rediscovers or rebuilds what already exists.

This is the root cause of the "docs lie" problem. The documents describe the
**union of nineteen branches**; any given session sees only `main`. Both sides
of every contradiction were telling the truth about different trees.

**Nothing else gets healthy until this is resolved.** It is an operator call.

### Verified facts — measured this session, trust over older docs

| Claim | Verified reality |
|---|---|
| "`main` lacks the working home screen" | **FALSE.** `HomeGrid.kt` = blob `618cf4b6` on `main`, `RELEASE`, and PR #33 alike; fonts identical |
| RELEASE is the safe base | **Inverted.** RELEASE is **9 commits behind** `main`, 0 ahead. Basing off it discards work |
| `greenLight()` checks 2 of 4 | **All four exist** (`RuntimeDefStore.kt:119-149`), exec bit at `:126-130` |
| `switchOn()` re-implements the check | **It consults** — `RouterPane.kt:82` |
| `HomeGrid.kt:69` npuReady bug (frozen, unfixable) | **A phantom.** No `startsWith("Adreno 830")` exists anywhere; `HomeGrid.kt` never reads `backendStatus`. Comment corrected at `LlmRuntime.kt` |
| Cloud connectors `absent`, "no compiled remote client" | **FALSE.** `CloudLlmRuntime.kt` ships OpenRouter (`:50`) + SambaNova (`:59`) + custom, SSE streaming |
| In-process Moonshine STT doesn't exist | **It does** — `MoonshineSttEngine.kt`, on PR #33, unmerged |
| "Moonshine is materially smaller than Whisper base" | **FALSE.** Measured: Whisper base.en 161 MB, Moonshine base **287 MB** |
| QAI Hub / QAIRT path is "dormant fallback" | **FALSE.** Both GenieX runtimes are live; `COMPILE-PIPELINE.md` is wrong |

**The genuinely missing check is the amperage gate** — architecture compatibility
and free RAM vs the model's declared footprint. Not the exec bit. That is the
operator's own item 3 from `2026-07-18.md`, and the only structural defence
against the LMK theory.

### What landed this session

- **Kokoro no longer downloads.** `KokoroModelManager` was a ~200 MB GitHub
  fetch + bzip2 + untar called unconditionally from `onCreate()`, with
  `tts.init()` loading the ONNX in-process right after. It is now a **resolver**:
  candidate device folders, an optional pinned path (`tts.kokoro_dir`), and an
  honest "missing: …" state. Mirrors `MoonshineSttEngine`'s user-is-the-loader
  pattern. This was the single largest thing the app did at boot and the prime
  suspect for the ~90 s first crash. **Unverified on device.**
- **`LlmRuntime`'s status-string comment corrected** — it was the source that kept
  regenerating the phantom `HomeGrid.kt:69` bug across sessions.

### Still open, in rough priority order

1. **The 19 unmerged PRs.** Operator call. Six of them (#30 #27 #26 #24 #22 #20)
   would overwrite the frozen `HomeGrid.kt` if merged.
2. **Assess PR #33** before writing anything in Router / params / voice — much of
   it may already be done there.
3. **`switchOn()` both ends** — delete the blocking `⚡ FUSE BOX` banner (rejected
   Rule 7a behaviour) and close the no-`RuntimeDef` bypass.
4. **The amperage check** — arch + RAM, added to `greenLight()`.
5. **Four parameter layers real in `RuntimeDef`** — Weights/Runtime/Engine/
   Communication. `temperature` still hardcoded `0.7` (`NpuClient:109`,
   `CloudLlmRuntime:122`); `verbosity` slider read by nothing; `cores` maps to
   GenieX's real `n_threads` in `geniex_ModelConfig`.
6. **Silero VAD never ships** — `VadFactory` prefers it but CI never fetches
   `silero_vad.onnx`, so it silently degrades to RMS on every device.
7. **Inbound listener** — still zero `ServerSocket`/Ktor. Blocks Termux getting
   mic/voice/WebView-OAuth. NPU access does not need it.
8. **`http_server.cpp`** — `Content-Length: 8` for a 9-byte body (`:54`); single
   8 KB `recv()` truncates `image_b64` (`:22-29`).

### Unproven

The ~90 s first-crash trigger. Diagnostics amplifiers were fixed in session 20;
Kokoro's boot fetch was removed this session. Whether either was the cause is
**unknown until someone reads the device**:
`/sdcard/Android/data/com.horizons/files/diag → tail -40 crash.log`.
Stack trace ⇒ JVM exception. Empty but the app died ⇒ killed from outside (LMK),
and no trace will ever appear.

### Process failures worth not repeating

A prior session read 59 of the vault's 337 files and reported having read
"everything" — missing `pending-corpora/`, which `SOURCE-PRECEDENCE` ranks
**above** the locked visual specs. Uncontested assistant claims inside Gemini
transcripts were promoted into `STATE-OF-EXISTENCE` as build-state facts. And a
skipped question was treated as consent to act. All three are cheap to avoid and
expensive to inherit.


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
- LLM inference runs via an uploadable daemon binary, not in-process. This is
  scoped to the **LLM path only** — the voice layer (sherpa-onnx Kokoro TTS,
  and Moonshine STT once wired) runs in-process by design and is not a
  violation of it.
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

## Superseded — historical context

What these were replaced *by*, on the Qwen3.5-9B path. This is a record of
how the build got here, **not a ban list** — if one of these turns out to be
the right tool again, that is an open question, not a rule violation.
Other model families ship their own runtimes; this table never constrained them.

| Old | Replaced by |
|---|---|
| Track 1 / Track 2 (for Qwen3.5-9B) | single path: ONNX → QNN context binary → Hexagon HTP |
| LiteRT / LiteRT-LM (for Qwen3.5-9B) | ort_engine daemon |
| genie_engine (for Qwen3.5-9B) | ort_engine (ORT + QNN EP) |
| Separate Watchdog | CliffordService (CLIFFORD == Watchdog) |
| Nexa SDK, OmniNeural | dead |
| Cloud failover in app LLM | HttpFetch agent tool |
