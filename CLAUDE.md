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
> ### PR #34 — current session's work, open draft, CI green (run #368)
> `claude/deprecated-repo-recovery-ycl0i8`. Contains: Router FUSE BOX gate
> removed (Rule 7a fix), Silero VAD CI fetch, 503 Content-Length fix,
> ModelImportActivity .so allowlist removed (libggml-hexagon.so etc. now
> accepted), MonitorPane detail truncation fixed, `manual` command in Monitor
> console, wiki/BUILD-STATUS.md (mechanical feature inventory).
> Needs operator decision to merge. Merge #33 first if possible.
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
> Settings supplies (no authority to run) · Terminal forges parameters AND
> now also configures models, fine-tunes, Termux-executes, and hosts an
> on-device agent (operator 2026-08-06 — "defines-only" is superseded) ·
> MONITOR is the switch, verifies LIVE at flip time, stores nothing,
> dispatches · Router is fuse box + breaker, carries current, DOESN'T ARGUE
> · Archives stores verified profiles, recovery restores here.
> The Router NEVER says no. A failed flip is a circuit that didn't energise,
> not an app throwing a wall. If the Router is full, the item BOUNCES back
> to its origin tile + GOAT face (operator 2026-08-06). Rule 7a: behavioural
> metaphors are NEVER compiled. Rule 7b: the supplied visual references ARE
> literal build specs.
>
> ### SPEC EXPANSION 2026-08-06 (this session)
> Canon now includes NEW cross-tile doc: canon/horizons-ui/UX-RULES.md.
> Router / Monitor / Terminal locked specs materially extended: Router gets
> animated CD tray + tap-a-disc fine-tune popup + dual-cassette Browse/Load
> role split + execution-mode swap (double-agent / single / MoA / cloud /
> terminal-agent) + STT tuning on tuner deck + cross-tile pathways +
> overflow-bounce + per-item hotkey. Terminal gets CRT-oscilloscope panel +
> Termux RUN_COMMAND_SERVICE access + describe→draft→confirm→run→auto-fix
> agent pattern (LLM-Hub reference) + port-over to Router when idle + live
> chaptered in-app user manual + provider picker. Monitor gets zoom + inset
> cropping. New standing rules: NO TYPING outside Terminal/browser;
> long-press → plain-language help popup; YOUR-FORK-FIRST for any asset
> that has a Mer0vin8ian/c10vis-poem copy (mapping table in
> canon/MASTER-BUILD-BLUEPRINT.md §14.5). Provider picker extends beyond
> OpenRouter to OpenAI / Anthropic / SambaNova / custom (§14.6).
> Executions model designated = Mer0vin8ian/Qwen3.5-0.8B — QAIRT precompiled
> from Qualcomm, ships as GitHub release-assets JSON, ONLY TERMUX CAN UNPACK
> IT. That makes Terminal Termux integration a HARD BLOCKER for executions
> model loading, not a nice-to-have. On-device 1.21 GB Qwen3.5-2B GGUF is a
> DIFFERENT model — don't conflate. Query-side pairing open. Voice stack
> UNCHANGED — sherpa-onnx stays; on-device failure is suspected Android
> accessibility issue, not stack choice (§8.3). Old duplicate launcher
> .uilocal.LocalHomeActivity → REMOVE; .MainActivity becomes real
> VoiceInteractionService.
>
> ### LEADING SUSPECT for "app can't find backend"
> SAF picker scope. Operator's GenieX SDK + Qwen 3.5-9B GGUF sit in
> /storage/emulated/0/LeGRAND_REPOSITORY/{MODELS,HARNESS}/ — NON-HIDDEN
> internal storage. If Horizons only reads context.filesDir and never
> launches ACTION_OPEN_DOCUMENT / ACTION_OPEN_DOCUMENT_TREE, those files
> are structurally invisible regardless of what the daemon does. Grep for
> ACTION_OPEN_DOCUMENT, ACTION_OPEN_DOCUMENT_TREE,
> ContentResolver.openInputStream, filesDir. This is the highest-value
> single check for the current failure — preempts the daemon-suicide theory.
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
## State of the Union — 2026-08-06 (session 22 + cont.)

**Session 22 read the corpus and verified it against live code.** Most of what it
produced is corrections. Several long-standing "facts" in these documents turned out
to be false, and two of them were actively costing every session that inherited them.

### The disease: 19 open draft PRs, none merged

Every session since roughly PR #6 branched from `main`, built something real, opened
a **draft** PR, got CI green, and stopped. `main` never accumulates, so each session
starts from a `main` missing its predecessors' work and rebuilds what already exists.

**This is why the docs and the code disagree.** The documents describe the *union of
nineteen branches*; any session sees only `main`. Both sides of most contradictions
were telling the truth about different trees. It is a merge problem, not a docs
problem, and nothing gets healthy until it is resolved. **Operator call.**

### HomeGrid freeze — branch audit (blob `618cf4b6` = correct)

**`main` already has the correct HomeGrid.** So do `RELEASE` and PR #33. Nothing
needs merging for the home screen.

**Six open PRs actively edit `HomeGrid.kt` and would overwrite the freeze:**
**#30 · #27 · #26 · #24 · #22 · #20**. The other thirteen carry an older blob only
because they branched early — git discards that on merge, so they are safe.

### Verified facts — measured this session, trust these over older prose

| Old claim | Verified reality |
|---|---|
| "`main` lacks the working home screen" | **FALSE.** `HomeGrid.kt` = `618cf4b6` on `main`, `RELEASE`, and PR #33; fonts identical |
| "Base off `RELEASE`, never `main`" | **INVERTED.** `RELEASE` is **9 commits behind** `main`, 0 ahead. Basing off it *discards* work |
| `greenLight()` checks 2 of 4 | **All four exist** (`RuntimeDefStore.kt:119-149`); exec bit at `:126-130` |
| `HomeGrid.kt:69` npuReady bug (frozen, unfixable) | **A phantom.** No `startsWith("Adreno 830")` exists anywhere; `HomeGrid.kt` never reads `backendStatus`. Consumers use `contains()` and separate Cloud from NPU correctly |
| Cloud connectors `absent`, "no compiled remote client" | **FALSE.** `CloudLlmRuntime.kt` ships OpenRouter (`:50`) + SambaNova (`:59`) + custom, SSE streaming |
| In-process Moonshine STT does not exist | **It does** — `core/stt/MoonshineSttEngine.kt`, on **PR #33**, unmerged |
| Kokoro downloads 200 MB at boot | **Already fixed.** `KokoroModelManager` is a resolver; `refresh()` is a filesystem check |
| "Moonshine is materially smaller than Whisper base" | **FALSE.** Measured: Whisper base.en **161 MB**, Moonshine base **287 MB** |

**Measured STT sizes** (sherpa-onnx int8, loadable set): Whisper tiny.en 104 MB ·
Moonshine tiny 124 MB · **Whisper base.en 161 MB** · Moonshine base 287 MB.

### What landed (PR #34, `claude/deprecated-repo-recovery-ycl0i8`)

- **The Router carries current.** `switchOn()`'s blocking `⚡ FUSE BOX` gate is gone
  (Rule 7a). It consults the Monitor, reports red lights, and attempts the flip
  regardless. Also closed the bypass — `if (def != null)` let cloud/PWA/terminal
  configs skip the Monitor entirely. Red banner → amber informational readout.
- **Silero VAD actually ships.** CI never fetched `silero_vad.onnx`, so `VadFactory`
  silently degraded to RMS on every device. Now fetched (~2 MB).
- **503 body no longer truncated** — `Content-Length: 8` for a 9-byte `not ready`.
- **Import allowlist removed** (`ModelImportActivity`). The old hardcoded list was
  rejecting `libggml-hexagon.so`, `libggml-htp-v79.so`, `libGenie.so`, and every
  custom `.so` as "Unsupported file type" — including the exact libs the GGML Hexagon
  NPU path needs. Any `.so` now passes. `geniex` binaries and extensionless executables
  also accepted. Android's `(1)` dedupe suffix stripped from filenames automatically.
- **Monitor detail string fixed** — `check.detail.takeLast(28)` was front-truncating
  path strings: `"not found in app dirs or Download"` → `"ound in app dirs or Download"`.
  Now shows tail with `…` prefix when >40 chars (`MonitorPane.kt:585`).
- **`manual` command in Monitor console** — `manual` / `manual <section>` wired into
  the Monitor's command dispatcher alongside `status`, `models`, etc.
- **`wiki/BUILD-STATUS.md`** — mechanical feature inventory, ~60 rows verified by
  grepping for consumers. Replaces unverified prose claims.

**None of it is device-verified.** CI is the only check that has run.

### ROUTER vs MONITOR — operator-confirmed 2026-08-06, independently

`wiki/ROUTER-MONITOR-TERMINAL-SPEC.md` (from PR #33) claims "ADOPTED, dictated by
the operator 2026-08-04." **That claim is now independently confirmed** — the
operator restated it unprompted this session, in his own words. Recording it here
because the spec itself notes a prior session was told this and never wrote it
down, and because an agent-authored document asserting operator authority is not
evidence on its own (Rule 5).

> **Operator, 2026-08-06:** *"once you push to the router you go to the router …
> you load what you want to load, you can set the temperature, adjust the voice
> pitch speed depth verbosity of the model. The tape deck is where you upload
> your file … your script if you had one, your hooks, environment, any kind of
> runtime that you're using, what model you're using — you're basically prepping
> your agent. Then go to monitor. Monitor will recognize [the file] because it's
> already received the initial checkboxes, the parameters that it needs to pass
> in order to run. If it satisfies those four instances then it gets the green
> light to run. **So the router's basically just always on — there's no way to
> short it out. You can't fuck it up by loading anything in the wrong sequence or
> loading too much of something or not enough. The operator switch is at the
> monitor level.**"*

**Authority runs: TERMINAL defines → ROUTER loads/preps/holds → MONITOR verifies
and dispatches.** The Router has **no gate, no verification, and no ignition**.
It cannot fail, because it decides nothing. `switchOn()` is gone; `loadConfig()`
replaces it.

This **supersedes** the fuse-box/breaker framing wherever they conflict — that
language was the operator making the concept legible, never a specification.
Note it agrees with the blueprint's own §4 diagram (`MONITOR … stores nothing ·
DISPATCHES`), which earlier sessions misread.

**Consequence for the parameter work:** the Router *is* the stereo stack —
CD changer = engaged model, tuner deck = temperature/verbosity/pitch/speed/depth,
tape deck = the loaded runtime file. So the four parameter layers are not an
abstract refactor; they are the tuner deck's controls.

### Architecture confirmed by the operator this session

- **Consumer-grade, not device-specific.** Any Android device; iOS and x86 later.
  Compute target must be **user-selectable** — strictly NPU, dispersed across
  NPU/GPU/CPU, targeted, or left to run naturally. This maps onto GenieX's existing
  `--device` aliases (`npu` / `hybrid` / `gpu` / `cpu`) and belongs in `RuntimeDef`'s
  **Runtime** layer. Note `HorizonsApplication:116` hardcodes `"Adreno 830"` — a lie
  on any other phone.
- **Both GenieX runtimes are live**, selected by `plugin_id`:
  `llama_cpp` (GGUF; NPU via `ggml-hexagon` / GPU via OpenCL / CPU) and
  `qairt` (QAIRT `.bin` shards + a required `geniex.json`; NPU only, max performance).
  **TFLite / LiteRT / QAT are input formats to the AI Hub compile**, which emits the
  bundle `qairt` loads. **"The GGML route" means the llama.cpp *runtime*** — not
  Google LiteRT, which appears in these docs only as the *rejected* in-process option.
- **Quantization + model selection: SETTLED. Q4_0. Do not reopen.**
- **Model residency:** every model gets its **own isolated device folder**, loaded by
  absolute path. The APK never downloads weights. Drag-and-drop to swap. Same storage
  either way — downloading only ever bought a boot-time failure mode.
- **Voice: in-process on the sherpa AAR, on device, through the APK. NEVER the NPU** —
  the actions model and query model already take turns there. This closes blueprint
  §8.1. Termux+proot voice was a temporary stopgap and is discarded; port its
  parameters, not its architecture.
- **WIRED ≠ LAUNCHED.** The APK should be capable of everything on the phone — sockets,
  the harness, permissions, an initial runtime for its own backend, NPU manager + file
  search, WebView/Chromium hooks, API + cloud inference, OpenRouter fallback. What is
  forbidden is *loading and landing* with it.
- **Daemon split (OOM safety).** A dying process cannot report its own death, so OOM
  recovery **requires an external observer**. Separate: **LLM inference** (+ vision
  co-located) and the **watchdog/recovery** in `:clifford`. In-process: voice, sockets,
  permissions, file search, UI. The NPU manager stays in-APK for coordination but must
  **not** own OOM detection.

### Spec expansion 2026-08-06 (this session)

Operator ran a full briefing pass; the vault got a substantial update.
Files touched in `c10vis-poem/OBSIDIAN-Master_Wiki`:

- **NEW:** `canon/horizons-ui/UX-RULES.md` — cross-tile UX rules (no typing,
  long-press help, zoom, inset-cropping, cross-tile pathways).
- **Router spec** (`canon/horizons-ui/ROUTER-STEREO-STACK-SPEC.md`) —
  animated CD tray + tap-a-disc fine-tune popup with `Model/Engine/Runtime/Config`
  overlay; dual-cassette Browse/Load role split; multi-load + execution-mode
  swap (double-agent / single / MoA / cloud / terminal-agent); tuner-deck
  extensions (hardware target, cloud/local toggle, max-tokens, STT tuning);
  cross-tile pathways; overflow-bounce; per-item on/off hotkey.
- **Terminal spec** (`canon/horizons-ui/TERMINAL-SPEC.md`) — supersedes
  "defines only"; adds configure/fine-tune models, Termux `RUN_COMMAND_SERVICE`
  access, LLM-Hub-style describe→draft→confirm→run→auto-fix agent, port-over
  to Router when Router is idle, live chaptered in-app user manual with full
  TOC, provider picker; CRT-oscilloscope panel treatment on the console.
- **Monitor spec** (`canon/horizons-ui/MONITOR-ARCADE-CABINET-SPEC.md`) —
  pinch-to-zoom (Live-Tile press-to-zoom fallback), inset-cropping.
- **Parameter packet** (`canon/aesop/PARAMETER-PACKET.md`) — Runtime layer
  gets `max_tokens` + hardware target; Communication layer gets provider
  picker; Router popup ↔ four-layer mapping recorded.
- **Master build blueprint** (`canon/MASTER-BUILD-BLUEPRINT.md`) — new
  §8.3 (voice failure = accessibility issue not stack change), §9.2
  (executions model = Mer0vin8ian/Qwen3.5-0.8B), §14.5 (your-fork-first
  standing rule + Mer0vin8ian HF mapping table + on-device folder layout),
  §14.6 (provider picker), §14.7 (commercial tiering back-burner), §16
  open list rewritten.
- **State of existence** (`canon/STATE-OF-EXISTENCE.md`) — new §4
  (SAF-picker-scope leading hypothesis for backend-not-found), new §5
  (2026-08-06 additions consolidated).
- **Feature inventory** (`canon/horizons-ui/FEATURE-INVENTORY.md`) — new
  §15 with ~40 tagged rows across Router / Terminal / Monitor / Settings /
  Voice / UX / launcher / commercial.
- **Agent brief** (`horizons-ui/AGENT-BRIEF.md`) — base-branch trap
  corrected (main IS current); SAF-picker-scope added as leading suspect;
  launcher-duplicate resolution recorded; no-typing rule added.
- **Clock face** (`canon/horizons-ui/CLOCK-FACE.md`) — CHONK timeout locked
  to 2 min (operator 2026-08-06, supersedes older "3–5 min"); launcher-tile
  question noted as not-on-the-wheel.

**Not yet pushed to the vault** — edits sit in this session's working
tree pending operator confirmation of push access + branch.

### Still open — next session priority order

**Highest-value single check (new 2026-08-06):**
0. **SAF picker scope grep.** Suspected root cause of "app can't find
   backend." Grep Horizons-UI for `ACTION_OPEN_DOCUMENT`,
   `ACTION_OPEN_DOCUMENT_TREE`, `ContentResolver.openInputStream`,
   `filesDir`. If loading only happens against `filesDir`, this is the
   bug and preempts the daemon-suicide theory. Cheap grep, one answer.

**Merge situation (operator call):** PR #34 is open draft, CI green as of run #368.
PR #33 (`claude/repo-restructure-crash-analysis-j9v9fl`) carries real work
(MoonshineSttEngine, Router loadConfig, NpuClient port/healthPath) and should be
assessed and merged before any new Router/params/voice work begins. Six PRs
(#30 #27 #26 #24 #22 #20) overwrite frozen HomeGrid and must not be merged without
operator sign-off. Remaining ~12 PRs are stale-only and safe.

**Device bugs confirmed from screenshots — spec status updated 2026-08-06:**
- Navigation bars hiding top/bottom content — **now speced in UX-RULES.md §4**
  (`WindowCompat.setDecorFitsSystemWindows` + `.systemBarsPadding()` on every
  non-Home room). Implementation pending.
- Duplicate Router configs ("Terminal: u0_a511" tiles) — `ps` output stored as model field
- Old three-button floating tile still visible — **now speced for removal**
  in FEATURE-INVENTORY §15H. `.uilocal.LocalHomeActivity` gets deleted.
- No way to edit existing Router configs — create-only, no edit flow. **Now speced
  in ROUTER-STEREO-STACK-SPEC.md §Fine-tune popup** — `[edit]` action on the
  popup handles this.
- Pop-up menus/tap-for-detail on tiles not implemented — **now speced in
  UX-RULES.md §2** (long-press → plain-language help popup).

**Functional gaps in priority order (revised 2026-08-06):**
0. **SAF picker grep** — see above. Blocks everything below if confirmed.
1. **Loading Genie SDK / models / runtimes from `/LeGRAND_REPOSITORY/…`
   actually works** — the operator's core failure. Depends on #0.
2. **The amperage check** — arch compatibility + free RAM vs declared footprint in
   `greenLight()`. Operator's own item 3; only structural defence against LMK kill.
3. **Four parameter layers** in `RuntimeDef` — Weights / Runtime / Engine /
   Communication. Now also carries `max_tokens` + hardware target
   (`npu`/`hybrid`/`gpu`/`cpu`) + provider picker. `temperature` hardcoded `0.7`
   (`NpuClient:101`, `CloudLlmRuntime:122`); `verbosity` written by
   SettingsPane, read by nothing; `cores` → GenieX `n_threads`.
4. **No-typing rule enforcement** — audit Settings, Archives, Router, Monitor
   for text fields; convert to pickers.
5. **Whisper STT engine** (`WhisperSttEngine.kt` implementing `SttEngine`) using
   `Mer0vin8ian/sherpa-onnx-whisper-base.en` per your-fork-first + engine family
   as a Runtime parameter.
6. **~60 s utterance cap** — a few lines; stops noise hanging the voice stream.
7. **Delete `DaemonSttClient`** — dead `:8091` fallback still masks STT failures.
8. **`http_server.cpp:22-29`** — single 8 KB `recv()` truncates `image_b64`.
9. **Termux `RUN_COMMAND_SERVICE` interface** for the Terminal-agent path
   (extends the older "inbound listener" gap — same underlying missing surface).
10. **Long-press help popups** wired app-wide.
11. **Remove `.uilocal.LocalHomeActivity`** launcher entry; wire `.MainActivity`
    as real `VoiceInteractionService`.

### Process failures worth not repeating

- **A comment is not code.** Twice this session an agent reported a defect that existed
  only in a stale comment (`LlmRuntime.kt:18`, and a "line 322" that was commentary
  about removed behaviour). Grep for the *consumer*, not the description.
- **Read the diff, not the PR body.** PR #33's write-up documents only the Router
  change; its STT engine is invisible from the description.
- **An uncontested assistant claim in a transcript is worth zero** (Rule 5). A Gemini
  thread's claim about CI run #353 was promoted into `STATE-OF-EXISTENCE` as fact.
- **A skipped question is NOT consent.** Take no action without an explicit order.
- Reading `canon/` + `horizons-ui/` is **59 of the vault's 337 files**. `pending-corpora/`
  is precedence rank 2 — *above* the locked visual specs — and is easy to miss entirely.

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
