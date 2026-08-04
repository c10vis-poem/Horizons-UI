# CLAUDE.md — Horizons UI / Novus-Agenti

> **RESUME PROMPT — COPY THIS BLOCK VERBATIM TO START ANY NEW SESSION**
>
> ```
> Project: Horizons UI — on-device agentic AI assistant, shipping to ANY Android
> device as a consumer app. NOT Razr-Ultra-specific; older docs saying otherwise
> are wrong.
>
> App repo: c10vis-poem/Horizons-UI   Vault: c10vis-poem/OBSIDIAN-Master_Wiki
> Protocol: c10vis-poem/aesop         Termux CLI: c10vis-poem/claude-code-android
>
> ### READ FIRST, IN THIS ORDER
> 1. wiki/ROUTER-MONITOR-TERMINAL-SPEC.md  — ADOPTED. The tile design. Non-negotiable.
> 2. "State of the code" below            — what EXISTS vs what is only DESCRIBED.
> 3. "Open decisions" below               — do not invent answers to these.
>
> ### THE ROUTER IS NOT A GATE
> Repeated by the operator ~10-15 times across sessions before it was ever
> written down. TERMINAL defines the runtime package (a FILE) -> ROUTER loads
> and preps it -> MONITOR verifies the four boxes AND dispatches the run.
> There is no flip-the-switch in the Router.
>
> "Fuse box", "10-amp fuse", "breaker", "amperage", "green light" are the
> operator's METAPHORS, used to make the idea legible. Earlier sessions compiled
> them into ConfigStatus, greenLight() and a red FUSE BOX banner. The four
> failure classes are real; the ceremony is not. Do not build ceremony.
>
> ### THE FAILURE MODE THIS FILE EXISTS TO PREVENT
> Every document in this project describes the app as though it were built.
> Agents then cite features that were never written, and the operator — who
> reasonably trusted the docs — finds the repo full of claims with no code.
> So: NEVER record a component without its state. Built-and-verified,
> built-unverified, or designed-only. If you did not run it, say so.
>
> ### NAMING (current)
> Omni Claw was renamed NovA-Claw (2026-08-02). Settings is THE VAULT. The 7:30
> tile is ARCHIVES, not Artifacts. Anything using the old names is stale.
>
> ### CORPUS STATUS
> The operator is roughly one third through curating source material. Almost
> nothing is adopted. Treat Drive/vault documents as CANDIDATE input, not canon
> — including the big "neuro mesh" PDF, which is a reference sweep.
> Use /memory to reload. HF_TOKEN / QAI_HUB_API_TOKEN come from the environment.
> Never hardcode them.
> ```

---

## /memory — Slash Command

1. Read this file, all sections.
2. Read `wiki/ROUTER-MONITOR-TERMINAL-SPEC.md`.
3. Read `knowledge/omni-claw-defined/` — what the app is.
4. For anything else use the **`project-memory`** skill (knowledge/ → vault → Drive).
5. Produce a summary + next action; confirm before touching any file.

Markdown is always the first read. **JSONL is grep-only, never a first read.**

---

## State of the code — 2026-08-04

The only column that matters. "Compiles" means CI built it; it does **not** mean
it works on a device.

| Component | State |
|---|---|
| Home screen (`HomeGrid.kt`) | **works** · FROZEN at `984b061`, blob `618cf4b6` |
| Compose UI, 8 panels, backgrounds, theme | **works** |
| State stores (Router/RuntimeDef/Archive/SavedCommand/ChatHistory) | **works** — real file-backed persistence |
| Kokoro TTS, in-process on sherpa AAR | **works** |
| Browser (WebView, multi-window) in Monitor | **works** |
| CI: cross-compiles `ort_engine`, builds + publishes APK | **works** |
| Boot-crash fixes (bounded `FileTail`, log rotation, `reloadIfChanged`) | **works** — original ~90s crash trigger still UNPROVEN |
| Router launches the plated runtime | **compiles, unverified on device** |
| `NpuClient` honors the config's port/healthPath | **compiles, unverified** |
| Moonshine STT in-process | **compiles, unverified** — needs model files on device |
| `llama-server` built-in RuntimeDef | **compiles, unverified** — needs an arm64 binary imported |
| Stereo Router UI (changer / display / tape deck) | **designed only** — see spec §2 |
| Arcade-cabinet Monitor UI | **designed only** — see spec §3 |
| Terminal tab in Router and Monitor | **designed only** |
| Runtime package as a portable file | **designed only** — `RouterConfigStore` is internal JSON |
| Box 3 of 4: arch + RAM check | **not implemented anywhere** |
| Omni Route (context dispatcher) | **designed only, zero code** |
| Executor / Query dual-core split | **designed only, zero code** |
| Termux inbound listener | **absent** — app has no inbound sockets |
| Temperature / verbosity / cores as real params | **absent** — temperature hardcoded (`NpuClient`, `CloudLlmRuntime`); verbosity has a Settings slider nothing reads |

### Known contradiction, recorded not hidden

`RouterPane.switchOn()` currently **gates and launches**. Per the adopted spec it
should do neither — the Monitor verifies and dispatches. The launch call needs to
move. This is the next real piece of work.

---

## Architecture

Four tiers (operator, 2026-08-02):

| Tier | Name | Role |
|---|---|---|
| 1 | **Horizons UI** (Kotlin APK) | The XI layer — Extended Interface. Captures intent. |
| 2 | **Novus-Agenti** | MoE/MoA cognitive core. The brain. |
| 3 | **NovA-Claw** (was Omni Claw) | Hardware-abstracted runtime. Daemons, SDKs, tools. |
| 4 | **AESOP XI** | Agentic Executions Split Operations Protocol — memory + audit + KAG flywheel. |

### Tile flow

All six tiles push **to** the Router. Full detail in the spec; summary:

- **Terminal** (matrix/fakesteak) — defines the runtime package, exports it as a file
- **Router** (stereo) — loads weights (6 slots), live params, runtime files; cycles between loaded runtimes
- **Monitor** (arcade cabinet) — verifies the four boxes, **dispatches the run**, hosts the browser
- **Settings** = **the Vault** — API keys, tokens, imported files
- **Archives** — bundles cycled out of the Router; inert models, spent scripts, logs
- **Chat** — artifacts, links, downloads → Router
- **Horizons** — legal/credits; a video game behind an Easter egg, long-term

### The four failure classes

Terminal **defines** them, Monitor **checks** them. Never the Router's job.

1. **Engine** — what makes it go
2. **Fuel & cargo** — assets; sometimes zero, sometimes many
3. **Road & weight limit** — architecture compatibility, available RAM ← **not built**
4. **Communication** — syntax and handshake

Class 3 matters most for a consumer app: it is what stops the code trying to
start a Hexagon daemon on a device with no NPU.

### Memory layer — operator's correction, 2026-08-04

**OB1 is Open Brain _Protocol_.** A protocol is an interface, not a destination,
so it serves **both** cores rather than hanging off one:

```
EXECUTOR CORE ─┐
               ├─→ OB1 / MCP ─→ [mem0 policy] ─→ Postgres + pgvector
QUERY ENGINE ──┘
```

The "1" is the project name (Open Brain / OB-1), **not** a tier index. Earlier
diagrams split executor→mem0 and query→OB1 as peers; that creates two stores
that drift, and mem0 needs a vector store anyway. Omni Route routes between
*cores*; OB1 is how *any* core reaches memory. Different axes.

---

## Hard Rules

- **`HomeGrid.kt` IS FROZEN at `984b061`** (blob `618cf4b6`). No agent touches
  `horizons/src/main/java/com/horizons/ui/HomeGrid.kt` for any reason —
  including a build-breaking fix — without explicit operator sign-off. If it is
  implicated: stop, report, wait.
  Icebox: `FROZEN-correct-home-screen-984b0610`, `RELEASE-correct-home-screen-984b0610`,
  `claude/homegrid-v5-SNAPSHOT-good-1837dc2`, `claude/homegrid-v4-scratch`.
  Verified intact 2026-08-04 — the blob is identical on `main`, on the frozen
  branch, and on the working branch.
- **Never claim a file was written, updated, or verified unless it was.** The
  operator was burned by exactly this. If CI has not built it, say "compiles:
  unknown". If it has not run on device, say so.
- **Do not build ceremony out of metaphors.** See the resume prompt.
- Never push `main` without explicit permission.
- Never `--no-verify`, `push --force`, `reset --hard` without confirming.
- No CPU fallback in the Qwen3.5-9B path (NPU or nothing, that model only).
- LLM inference runs via a daemon binary, not in-process. **Scoped to the LLM
  path only** — the voice layer (Kokoro TTS, Moonshine STT) runs in-process by
  design and is not a violation.
- Assets come from **the operator's own sources** — his GitHub forks, his
  HuggingFace copies, files already on device. Do not add runtime downloaders
  from upstream. (`KokoroModelManager` pulls ~200 MB at runtime; that is the
  pattern *not* to repeat.)
- Don't trigger the dormant compile pipeline pre-emptively — see
  `wiki/COMPILE-PIPELINE.md`.

---

## Open decisions — do not invent answers

- Voice layer in the Router's changer — operator undecided.
- Whether verbosity remains an adjustable parameter.
- Whether `switchOn()`'s gate is removed now or when the Monitor takes dispatch.
- Whether mem0's policy layer is adopted, or the operator writes his own.
- Repo split (one-agent-per-repo, 8 repos) — Gemini's proposal, **not** adopted.
  The *priority order* is the operator's own and stands: knowledge curation →
  skills → file-management repo + Termux closed-loop validation → APK → nodes.

---

## Build / CI

- AGP 8.8.0 · Kotlin 2.1.0 · compileSdk 35 · minSdk 31 · JDK 17 · arm64-v8a
- Signing: `release/debug.keystore` (committed by design)
- `build-apk.yml` downloads the sherpa-onnx AAR (gitignored, not vendored),
  cross-compiles `ort_engine` via CMake/NDK, builds the APK, and publishes to a
  `latest-debug` GitHub Release.
- **No Android SDK in the Claude Code environment.** Nothing compiles locally;
  CI is the only verifier. Push and read the run.

## Brand

Background `#222C34` · Surface `#35414A` · Primary teal `#2DD4D9` ·
Highlight teal `#4FE7EC` · Icon backplate `#050709` · Action yellow `#F5C518`.
Backdrop is a pure Compose `Brush.radialGradient` — not XML shape.

## Termux / Mobile

**Phone only. No laptop.** Reference device: Motorola Razr Ultra 2025 · SM8750 ·
16 GB · Hexagon HTP v79. Reference only — the app ships to any device.

- No tokens or long URLs in paste-able commands.
- Short alias then `$VAR`; keep commands under ~50 chars.
- **Never put filenames or prose in a code block unless it is meant to be typed.**
  The operator pastes code blocks into Termux verbatim.

---

## Repo File Map

```
CLAUDE.md                       this file
wiki/
  ROUTER-MONITOR-TERMINAL-SPEC.md  ** ADOPTED — read before touching any tile **
  HOME-REDESIGN-SPEC.md            home screen redesign + home-redesign-img/ assets
  MASTER-SESSION.md                older combined session log
  COMPILE-PIPELINE.md              dormant fallback pipeline
  GENIEX-DAEMON-PLAN.md            GenieX runtime plan
  JOB_EXECUTION_LOG.md             compile-job + failure ledger
  FEATURE-SPEC.md                  older UI tile spec — pre-dates the adopted spec
  BUILD-ACTION-PLAN.md
  research/                        notes on forked tools, not project architecture
knowledge/
  omni-claw-defined/               ALWAYS-READ core definition (folder name is stale)
  daemon-reference/ qairt-sdk/ device-inventory/
  research-npu/ proofs/ fragmented-qat/ google-dev-docs/ gemini-query/
skills/
  horizons-wiki/  project-memory/  termux-mobile-dev/
  (canon calls for termux-builder · aesop-protocols · horizons-ui · qairt-backend,
   each SKILL.md + references/. None of the three present match; none has a
   references/ folder; qairt-backend does not exist.)
rules/  agents/  compile/  daemon/  licenses/  tools/
horizons/src/main/java/com/horizons/
  ui/HomeGrid.kt                 FROZEN
  ui/panels/                     Router · Monitor · Settings · Terminal · Chat · Artifacts · Horizons
  ui/browser/BrowserPane.kt
  core/state/                    RouterConfig · RuntimeDef · Archive · SavedCommand · ChatHistory
  core/llm/NpuClient.kt          daemon client, port from the config
  core/stt/MoonshineSttEngine.kt in-process STT on the sherpa AAR (2026-08-04)
  core/voice/                    Kokoro TTS
  core/shell/DaemonLauncher.kt   parameterised by binaryName + args
  fgs/CliffordService.kt         watchdog, separate :clifford process
.github/workflows/build-apk.yml
```

`uilocal/` was **deleted 2026-08-04**. It was a session-16 scratch fork sold as
crash-resistant; the operator tried it during a crash and it died identically,
because both activities ran the same `HorizonsApplication.onCreate`. **Do not
recreate it** — a second Activity gives zero crash isolation. Real isolation is a
separate process (`:clifford`). One launcher icon now.

---

## Superseded — historical record, not a ban list

How the build got here on the Qwen3.5-9B path. If one of these turns out to be
the right tool again, that is an open question, not a rule violation.

| Old | Replaced by |
|---|---|
| Track 1 / Track 2 (Qwen3.5-9B) | single path: ONNX → QNN context binary → Hexagon HTP |
| LiteRT / LiteRT-LM (Qwen3.5-9B) | ort_engine daemon |
| genie_engine (Qwen3.5-9B) | ort_engine (ORT + QNN EP) |
| Separate Watchdog | CliffordService |
| Nexa SDK, OmniNeural | dead |
| Cloud failover in app LLM | HttpFetch agent tool |
