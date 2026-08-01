# CLAUDE.md — Novus Agenti / Omni Claw

> **RESUME PROMPT — COPY THIS BLOCK VERBATIM TO START ANY NEW SESSION**
>
> ```
> Project: Novus Agenti (Omni Claw) — on-device agentic AI assistant.
> App repo: c10vis-poem/Horizons-UI   Vault: c10vis-poem/OBSIDIAN-Master_Wiki
> Runtime:  c10vis-poem/aesop         Termux CLI: c10vis-poem/claude-code-android
> Device:   Motorola Razr Ultra 2025 · SM8750 · 16GB · Hexagon HTP v79. PHONE ONLY.
>
> ### 0. HOW TO NOT WASTE THE OPERATOR'S TIME
> They have ~2,000 pages of Qualcomm/stack documentation and have forked or
> downloaded everything this project needs. If you think something is missing or
> undocumented, YOU HAVEN'T FOUND IT YET. Go look. Do not report their own
> inventory back to them as a discovery.
>  - VENDOR DOCS OUTRANK EVERY SUMMARY IN THESE REPOS, INCLUDING THIS FILE.
>  - DO NOT STOP AT THE FIRST FILE. The QAIRT manual alone is 7 sections
>    (Overview, Api, Backend, Context, Graph, Tensor, HTP). knowledge/qairt-sdk/
>    has 5; Overview+Tensor are only in the vault at
>    #AESOP_HORIZONS-UI_Master/(AESOP.]build/#QAIRT_main/#QAIRT/.
>  - READ THE FOLDER, NOT THE FILE. Siblings go deeper on different parts;
>    numbered folders are conversation series. Same-named files ACROSS folders
>    are byte-identical (verified) — depth is in the siblings BESIDE a file.
>  - The PDFs list their own library tree at the top. If you spot a path to a
>    doc that isn't here, ASK — the operator can download it in minutes.
>  - Terse operator replies are NOT confirmation of whatever you just wrote.
>    Confirm what they're answering before building on it.
>
> ### 1. RUNTIME TRUTH — read before saying ANYTHING about backends
> SOURCE OF TRUTH: the GenieX README, in Drive at #QAIRT_main/#GenieX/ as a
> 7-page PDF (pages 3 and 6). The vault .md of that page is the repo FILE TREE
> ONLY — the README body with this table is not in it. Read the PDF.
>
> GenieX is ONE runtime with TWO backends. The fork is INSIDE GenieX; it is not
> GenieX-versus-something-else:
>   |              | llama.cpp (`llama_cpp`)   | AI Engine Direct (`qairt`) |
>   | models from  | Hugging Face, ~any GGUF   | Qualcomm AI Hub, precompiled|
>   | format       | GGUF                      | per-chipset bundle         |
>   | compute      | NPU · GPU · CPU           | NPU ONLY                   |
>   | best for     | bring your own GGUF       | highest NPU performance    |
>   geniex infer google/gemma-4-E4B-it-qat-q4_0-gguf   -> llama_cpp
>   geniex infer ai-hub-models/Qwen2.5-VL-7B-Instruct  -> qairt
> Q4_0 is the recommended precision: "best Hexagon NPU support."
> The README says "almost any GGUF" — not literally any. Don't overclaim it.
>
> THE MAGIC SAUCE IS THE GGML/HTP KERNELS. The format is inert and the runtime
> wrapper is plumbing; what puts a GGUF on the NPU is having ggml ops that
> execute on Hexagon. That is why the libs are compiled PER DSP ARCH, in the
> vault at #AESOP.../##LLM-WIKI_OPEN-WIKI.main_/llm-wiki/ :
> libggml-hexagon.so + libggml-htp-v73/v75/v79/v81.so (this device = v79).
> GenieX does the same thing: third-party/ vendors llama.cpp b10019 and notes/
> tracks `overlay-htp` — the overlay IS the kernel layer over stock llama.cpp.
> Qualcomm's own AI Engine Direct stack diagram has a "Kernels" row per column
> (col 3 = HTP Core -> HMX/HVX -> HTP).
> Corollaries that keep getting broken:
>  - "ggml" DOES NOT MEAN "CPU". Reading it that way is THE most repeated error
>    in this project. DEVICE-INVENTORY.md says it plainly: GenieX ships dual
>    backends, llama.cpp (ggml, HTP v68-v81 + CPU + OpenCL) AND QAIRT.
>  - NOT everything is compile-free. The GGUF/llama_cpp side needs no compile.
>    But AI Hub ships only SMALLER Qwen variants, NOT the 9B — so Qwen3.5-9B on
>    the qairt backend requires BYOM-compiling via the QAI Hub workbench
>    (compile/compile_qwen3_5_9b.py). That is what the "dormant" pipeline is for.
>  - QAT != QAIRT. QAT is how weights were quantized (gemma-4-E4B-it-qat-q4_0-
>    gguf). QAIRT is the runtime. A QAT GGUF still lands on HTP.
>  - ORT/ONNX IS THE VOICE LAYER ONLY. It was never the LLM path. Do not
>    describe the LLM stack in terms of ORT.
>  - GenieX ships as a Maven Central Android SDK with prebuilt arm64-v8a native
>    libs — NO NDK, NO CMake. minSdk 27, requires SM8750/SM8850. It pulls
>    weights from HF/AI Hub itself. Reference app: qualcomm/ai-hub-apps (forked).
>
> ### 2. WHAT THIS IS — the Four Rooms (spec, not paraphrase)
> A manual, modular workbench, NOT a black box. Boots EMPTY and stable.
> THE SPEC IS "Horizons Project: Architecture & Inference Build Map", in
> (AESOP) REPO.data_bank/(1a)-Horizons.Ui-defined/1. brainstorming and
> conceptualization master document. Copy of 2026-07-18.md — READ IT, don't
> paraphrase it from here. Flow is strictly ONE-WAY: DEFINE -> VALIDATE -> EXECUTE.
>   Terminal (Mod Garage)  — where a runtime is DEFINED. The fuse. Pure params.
>                            Exports to Settings/Archives or ships to Monitor.
>   Settings (Pantry/VAULT)— landing zone for raw ingredients: binaries, keys,
>                            tokens, .gguf. THE SETTINGS TILE *IS* THE VAULT.
>   Monitor  (Library)     — THE CHECKPOINT. Static greenLight checklist,
>                            NO network calls, NO side effects. "ALL GREEN"/"N RED".
>   Router   (Plate/Fuse)  — completed configs get plated. Refuses to turn on
>                            unless every param is satisfied AT execution time.
>   Archives               — real file manager, ArchiveStore on filesDir/archive.
> RUNTIME DEFINITION PROTOCOL — the operator's OWN four, in plain English.
> These were deliberately SIMPLIFIED down from five to four. Do not re-expand
> them, do not re-split #4, do not drop #3. Quote them as written:
>   1. THE ENGINE (what makes it go) — `ort_engine`, `geniex`, OR a custom script.
>   2. THE FUEL & CARGO (the assets) — model, extra scripts, SDK libs.
>      Sometimes ZERO items, sometimes a whole list.
>   3. THE ROAD & WEIGHT LIMIT (hardware/memory) — can this survive on THIS
>      device? Too heavy for RAM? Wrong architecture? This is the amperage
>      limit, and it exists specifically to stop OOM crashes. greenLight() NOW
>      CHECKS THIS (arch + weight), and it is ADVISORY — it reports the real
>      numbers (config bytes · free · device total) and never holds the switch
>      shut. Per the master doc the hard red banner gets stripped out; the Router
>      "just does it's going to try to connect whatever you put on there."
>   4. THE COMMUNICATION (syntax AND handshake — ONE parameter, on purpose)
>      "there is no need to separate the syntax and the endpoint — they both
>      just represent how we talk to the engine and how it talks back."
>      argsTemplate is how we instruct it; a port/endpoint like :8080/health or
>      :18181/v1/models is one way it replies. An in-process SDK call is
>      another. #4 does NOT imply a daemon or a port.
> Assets land PASSIVELY. Monitor acknowledges and waits for the user to PLUG IN.
> SLOT COUNTS ARE NOT FIXED AT 3: cloud API = 3 (endpoint/key/model), terminal
> script = 1, on-device CLI = 0. EVERY slot in EVERY room must offer a manual
> escape hatch (add / upload / plug in / write a script).
> A config carries its own target: file-backed (path) · localhost (port) ·
> cloud (endpoint + key ref). Only file-backed needs storage; localhost and
> cloud need no download, no registry, and never touch the WebView.
>
> ### 3. THE FUSE METAPHOR GOT BUILT — this is a defect, not a feature
> "Flip a fuse" was DESCRIPTIVE LANGUAGE about how the system is organized. A
> past session implemented it literally: RouterPane has onRun -> switchOn(), and
> CliffordService's idle notification literally instructs the user to "flip a
> fuse in the Router." Per the operator's own model the Router has NO authority
> to run anything. Do not build further on the literal reading.
> Worse, switchOn() (:79-93) doesn't start anything anyway — it re-runs
> greenLight, calls setStatus(RUNNING), then llmRuntime.preWarm(), and
> preWarm() (:28) is an EMPTY STUB. The flip writes a flag to router_configs.json
> and returns. CliffordService discovers it up to 15s later on a poll loop.
> Nothing dispatches. EXECUTIONS.md 1.1/1.2 graded this MISSING EXECUTION long
> ago and it was never built.
>
> ### 4. THE CRASH
> Symptom: crashes ~90s after landing, worse each launch, "still trying to
> connect." FIXED + CI green, STILL UNVERIFIED ON DEVICE:
>  - FailureMonitor.install() built a full report SYNCHRONOUSLY ON THE MAIN
>    THREAD in Application.onCreate(), walking every crash + log file.
>  - Every "tail" was really readText()/readLines() on the WHOLE file.
>  - crash.log had NO cap and NO rotation.
>    => closed loop: crash -> bigger log -> heavier boot -> more crashes.
>  - CliffordService runs in :clifford, so its RouterConfigStore was a DIFFERENT
>    INSTANCE that never re-read => a Router flip was invisible to the launcher.
>    Fixed via reloadIfChanged().
> STILL UNPROVEN: the trigger of the FIRST ~90s crash.
> READ THE LOG IN THE APP: Artifacts tile (ArtifactsPane.kt:223 renders
> Breadcrumb.readAll()). DO NOT tell the operator to tail it from Termux —
> VERIFIED DEAD 2026-07-31: Android 11+ blocks /sdcard/Android/data/<pkg>/ to
> every other app and MANAGE_EXTERNAL_STORAGE is explicitly carved out.
> boot.log lines are tagged [main]/[clifford].
>
> ### 5. PHASE 1 SCOPE (operator): browser · voice · Termux backend
> VOICE LAYER = ONE ONNX PLANE, three parts: Silero VAD (endpointing) +
> Moonshine STT (missing) + Kokoro TTS (works, in-process on sherpa AAR).
> SILERO VAD IS ALREADY WIRED: VoiceLoopController takes vad: VadDetector,
> LiveChatService/ScreenShareService build it via VadFactory.create(). No
> push-to-talk, no fixed window, no timeout — continuous endpointing, MIT,
> sub-1ms per 30ms chunk.
> THE INTERACTION MODEL, exactly (operator): TAP the mic button, talk, and when
> you STOP TALKING it engages. Silence is the send signal, decided by the VAD.
> NOT a hotword. NOT always-listening. NOT hold-the-button-down. One tap opens
> the mic; the VAD closes it. Do not build any of the other three.
> The loop already knows WHEN you speak. It cannot turn that into TEXT.
> That is the whole gap. This is also why ORT/ONNX is the voice
> layer and never the LLM path.
> NOT about hosted models.
>  - Browser: DONE. In Monitor, shared component, new tabs + OAuth popups work.
>  - Monitor pop-out tabs CONSOLE/TERMINAL/BROWSER: DONE.
>  - Voice: BROKEN BY DESIGN, NOT BY BUG. There is no STT component. Every voice
>    path calls app.llmRuntime.streamAudio(), and LlmRuntime's default impl
>    DISCARDS THE WAV and pipes the prompt string into the text stream. Call
>    sites: VoiceLoopController, LiveChatService, ScreenShareService,
>    HorizonsApplication, HorizonsRecognitionService (which replaces the SYSTEM
>    recognizer, so this breaks other apps' voice input too). :8091 is bound by
>    NOTHING — not AESOP, not the daemon; it was never anyone's port.
>    FIX = Moonshine STT in-process on the sherpa AAR already shipping, as its
>    own client. TTS already proves the in-process pattern works.
>  - Termux backend: THE TERMUX SIDE ALREADY EXISTS. AESOP's aesopd is a
>    WebSocket bridge on :8765 routing llm.generate to GGML (:8081) or NPU
>    (:8080) by a `backend` field, with a wire spec at protocol/bridge-protocol.md.
>    What's missing is the HORIZONS end of that bridge. Much smaller than
>    "build a backend."
>
> ### 6. KNOWN GAPS
>  - THE APP IS THE WORKBENCH — everything gets DEFINED AND COMPILED INSIDE THE
>    APP RUNTIME. The app's job is to make PRIMITIVES available and let a runtime
>    definition compose them: daemons · sockets · websockets · inbound listeners
>    /webhooks · file pickers · in-process SDK calls · cloud endpoints. It is NOT
>    to hardcode one welded path per capability. That welding is exactly why
>    capabilities can only be TRIGGERED by the one thing wired to them and never
>    ADDRESSED — the reason STT is a method on LlmRuntime instead of a component.
>    STATE OF THE PALETTE:
>      daemon spawn      — EXISTS (DaemonLauncher, sh -T- detach)
>      loopback HTTP     — EXISTS (NpuClient -> :8080)
>      websocket client  — MISSING (needed for AESOP aesopd :8765)
>      inbound listener  — MISSING (app has ZERO; this is the "Termux backend")
>      file picker (SAF) — MISSING (ACTION_OPEN_DOCUMENT, no permission needed)
>      browser download  — EXISTS but lands in PUBLIC /Download, app doesn't own it
>      in-process SDK    — MISSING (GenieX Maven SDK, no NDK required)
>      cloud endpoint    — partial (CloudLlmRuntime), bypasses the gate
>    RuntimeDef today expresses ONLY "spawn a binary, poll a port". That single
>    shape is why cloud/localhost/PWA configs fall out of the launcher's logic
>    and why switchOn() skips the gate when no RuntimeDef matches.
>  - THE DUAL-AGENT PAIR IS THE TARGET, both resident: Qwen3.5-0.8B as the
>    always-on QUERY model (decides whether to wake the big one) + Qwen3.5-9B
>    Q4_0 (~5.4GB) as the EXECUTIVE. Operator has 10-12GB free routinely; any
>    cap under ~9-9.5GB total is not feasible. RuntimeDef has NO concept of a
>    companion model yet — one KEY_ACTIVE_MODEL pin only.
>  - greenLight's RUNTIME_HEADROOM_BYTES (3.5GB) is a PLACEHOLDER I picked, not
>    a measurement. Operator's call: the app should COMPUTE this — sum what is
>    actually plugged in, compare to real availMem — not carry a constant.
>  - Advisory vs blocking AssetCheck is not rendered differently in
>    MonitorPane/RouterPane. A red advisory currently looks like a hard failure.
>  - temperature hardcoded (NpuClient:101, CloudLlmRuntime:122). verbosity has a
>    Settings slider NOTHING READS; debugLogLevel likewise. Cores don't exist.
>    Must become real RuntimeDef params BEFORE the runtime-agnostic launcher.
>  - resolveNpuModelPath() AUTO-SCANS models/, filesDir, /Download for the newest
>    LLM file. EXECUTIONS.md 0.1 grades this CONTRADICTS "boots empty". It should
>    not exist — a config carries its own target.
>  - greenLight() checks 2 of 4 boxes. switchOn() SKIPS THE GATE ENTIRELY when no
>    RuntimeDef matches, so cloud/PWA/terminal configs bypass it — same wrong
>    assumption that everything is a file on disk.
>  - BrowserPane's download listener writes to PUBLIC /Download via
>    setDestinationInExternalPublicDir — app doesn't own it, and it lands in a
>    directory the auto-scanner sweeps. Nothing registers for DownloadManager
>    completion, so the app never learns a file arrived.
>  - HomeGrid.kt:69 computes npuReady from startsWith("Adreno 830"), which the
>    NO-BACKEND fallback string also matches. FROZEN — report, don't fix.
>  - Two launcher icons: .MainActivity AND .uilocal.LocalHomeActivity both carry
>    MAIN/LAUNCHER, running different code. Removing one is a pending operator call.
>
> ### 7. AESOP — the app is the EDGE NODE, and the protocol names it
> AESOP (c10vis-poem/aesop) is a PROTOCOL, not a daemon set. Note: aesop-wiki.md
> in the vault describes llamad/aesopd/protocol/bridge-protocol.md — NONE of that
> exists in the repo. Six files: README, ARCHITECTURE, RESUME, protocol/tiers.md,
> profiles/{nav,_example}.yaml. Read those, not the wiki page.
> ROLES: query/tool-exec · executive · librarian/switchboard (the flywheel) ·
> auditor (red). MEMORY: declarative (wiki markdown, canonical) · recall (OB1
> vector) · strategic (ReasoningBank, fed by the auditor). Vector indexes are
> DERIVED and rebuildable from the markdown — markdown is the source of truth.
> profiles/nav.yaml: `phone: tiers:[edge], client: omni-claw` — THIS APP IS THE
> EDGE CLIENT. Bindings: query on phone, executive {home: jetson, away: cloud},
> librarian on jetson, auditor on cloud with edge_behavior: defer.
> EDGE TIER CONTRACT (tiers.md) — to claim `edge` a node MUST provide:
>   local small model or passthrough connector · camera/mic access ·
>   A DURABLE OFFLINE ACTION QUEUE FOR TIER-2 DEFERRALS  ← NOT BUILT
> The queue is not optional polish; it is why the audit boundary works:
>   tier0 none (read/reason) · tier1 self (reversible local writes, self-pass +
>   log) · tier2 independent (irreversible/outbound + MEMORY COMMITS need the
>   auditor) · offline_tier2 QUEUE for audit-on-reconnect.
> OPERATOR (2026-08-01): when NOT tailscaled into the home network, the phone
> runs BOTH models locally — it cannot reach the jetson executive. The profile
> does NOT express this: executive fallback is [cloud, personal], and `personal`
> is the computer. THERE IS NO EDGE FALLBACK FOR EXECUTIVE. Flag to the operator
> before building around it. Consequence for this app:
>   - HOME (tailnet reachable): query only on-device. No NPU contention.
>   - AWAY: query + executive BOTH on this NPU → turn-taking required. Use
>     QAIRT's own primitives (Graph Priority, Multi-Graph Switching, Yielding
>     and Pre-Emption, VTCM Sharing, Init/Execute Cancellation) — do NOT
>     hand-roll a Kotlin mutex above the layer that knows HMX/HVX contention.
>     NOTE: llama.cpp-npu uses a SINGLE NPU session and says QNN uses multiple
>     sessions to avoid that limit — co-residency is a QAIRT capability.
> So NPU arbitration is a MODE, not a permanent condition. Nothing in the Four
> Rooms owns it: Terminal defines, Settings supplies, Monitor validates (static,
> no side effects), Router carries current. Execution-time arbitration between
> two live runtimes is an UNOWNED role — do not staple it onto the Monitor.
>
> ### 8. THE WIDER STACK (this app is one node)
> Three-device Tailscale mesh: Razr (mobile orchestrator) · Jetson Orin Nano
> Super (heavy inference, 500GB SSD, OB1, OmniRoute) · Rubik Pi 3 (UI/scripting
> hub + Red Agent auditor). WebSocket router on the Jetson, not polling REST.
> Memory layers are FOUR DISTINCT THINGS, not competitors:
>   OB1 = persistence layer/protocol · mem0 = live extraction+pruning
>   LLM-Wiki = conceptual knowledge graph · Reasoning Bank = execution history
> OmniRoute = single OpenAI-compatible gateway (:20128/v1) + failover + token
> compression. Red Agent gates TWICE: before code executes, and before
> trajectories feed recursive KAG training. AESOP is the operator's
> implementation tying these together. See the vault; do not re-derive.
>
> HF_TOKEN / QAI_HUB_API_TOKEN come from the environment. Never hardcode them.
> ```

---

## /memory — Slash Command

**Sequence (all first-read = MARKDOWN; JSONL is grep-only, never first-read):**
1. Read this file, all sections.
2. Read `knowledge/omni-claw-defined/` — what the app IS.
3. Read `EXECUTIONS.md` — the build dock.
4. For anything about backends/HTP/QAIRT: **read `knowledge/qairt-sdk/` before
   citing any summary, including this file.**
5. For anything else, use the `project-memory` skill (knowledge/ → vault → Drive).
6. Produce a summary + next action, confirm before touching any file.

---

## Source trust

- **Vendor documentation (Qualcomm QAIRT/GenieX, Unsloth, Google) is canon.**
- **Repo summaries — including this file — are not.** `aesop-wiki.md` asserted
  "NOT on the ladder: Hexagon DSP," which is false and misled multiple sessions;
  it has been corrected in the vault. Assume other summaries carry similar rot.
- Two data-bank docs are AI transcripts, not specs (the "Gemini duel" doc
  retracts its own central claim; `2. 2026-07-17.md` the operator calls a snow
  job). **Architecture in them is canon; implementation claims are not.**
- `knowledge/device-inventory/DEVICE-INVENTORY.md` is a snapshot (2026-07-13),
  not current state. Re-verify before trusting sizes/versions.
- **FraQAT is old prior-art research.** Never reach for it over the QAIRT SDK.

---

## Hard Rules

- **`HomeGrid.kt` IS FROZEN at commit `984b061`** (2026-07-27) — operator-confirmed
  final layout. No session touches
  `horizons/src/main/java/com/horizons/ui/HomeGrid.kt` for ANY reason — not a
  layout tweak, not a "small" fix, not a build-critical or CI-breaking fix —
  without the operator's explicit go-ahead. If implicated in a failure: stop,
  report, wait. Icebox copies (blob `618cf4b6`):
  `FROZEN-correct-home-screen-984b0610`, `claude/homegrid-v5-tuned`,
  `RELEASE-correct-home-screen-984b0610`. Earlier snapshot:
  `claude/homegrid-v5-SNAPSHOT-good-1837dc2` (`725306d`).
- **Run `git branch -a` before choosing a base.** More real work sits on unmerged
  branches in these repos than on `main`.
- Never push `main` without explicit permission.
- Never `--no-verify`, `push --force`, `reset --hard` without confirming.
- No CPU fallback in the Qwen3.5-9B path.
- **The QAIRT manual outranks every summary here.** For any claim about
  backends, runtimes, HTP, graphs, or context binaries, read the manual first.
- Don't trigger the dormant compile pipeline pre-emptively — see
  `wiki/COMPILE-PIPELINE.md`.

---

## Repo File Map

```
CLAUDE.md                     ← THIS FILE
EXECUTIONS.md                   the build dock (canon-vs-code audit)
agents/                         build-runner.yaml · sub-agent.system.md
daemon/                         ort_engine C++ daemon (CI-built)
rules/                          AAR_DECOMPILE · AT_BAT_PROTOCOL · CACHE_PROMPT_RULES · GIT_HYGIENE
skills/
  horizons-wiki/                architecture bundle
  project-memory/               knowledge/ corpus retrieval
  termux-mobile-dev/            VNC/Matrix env · Claude Code on Android · scoped-storage limits
knowledge/
  omni-claw-defined/            ALWAYS-READ core project definition
  qairt-sdk/                    QNN HTP manual — htp/backend/context/graph/api (+htp.jsonl)
                                MISSING: Overview, Tensor (vault only)
  daemon-reference/             GPT-DAEMON-REFERENCE.md · NPU-RUNTIME-PATHS.md
  device-inventory/             on-device audit snapshot 2026-07-13
  claude-code-reference/  research-npu/  proofs/  fragmented-qat/
  google-dev-docs/  gemini-query/
compile/                        dormant compile-pipeline domain (fallback only)
wiki/                           COMPILE-PIPELINE · GENIEX-DAEMON-PLAN · JOB_EXECUTION_LOG
                                FEATURE-SPEC · BUILD-ACTION-PLAN · research/
horizons/                       Android app
  ui/browser/BrowserPane.kt     shared WebView browser (Monitor + Terminal)
  ui/panels/ArtifactsPane.kt    :223 renders the boot/crash log — the on-device log viewer
  ui/panels/RouterPane.kt       :79-93 switchOn() — see §3, does not dispatch
  fgs/CliffordService.kt        watchdog daemon, :clifford process
  core/diag/                    FileTail · FailureMonitor · Breadcrumb
  core/llm/  core/stt/  core/tts/  core/shell/  core/agent/  core/state/
  uilocal/LocalHomeActivity.kt  local UI fork (additive)
.github/workflows/build-apk.yml
release/debug.keystore           committed by design
```
`watchdog/` was deleted. There is no per-session handoff file.

---

## Build / CI

- AGP 8.8.0 · Kotlin 2.1.0 · compileSdk 35 · minSdk 31 · JDK 17 · arm64-v8a only
- Signing: `release/debug.keystore` (committed by design)
- `build-apk.yml` cross-compiles `ort_engine` via CMake/NDK, builds the APK, and
  publishes both plus `libonnxruntime.so` to a `latest-debug` GitHub Release.
  Publish target defaults to this repo. If a release step misfires, check
  Settings → Actions → General → Workflow permissions first.

---

## Brand

- Background `#222C34` · Surface `#35414A` · Primary teal `#2DD4D9`
- Highlight teal `#4FE7EC` · Icon backplate `#050709` · Action yellow `#F5C518`
- Backdrop: pure Compose `Brush.radialGradient` — NOT XML shape

---

## Termux / Mobile Rules

**Phone only. No laptop.**

- No tokens or long URLs in paste-able commands; short alias then `$VAR`
- Every paste-able command under ~50 chars where possible
- Termux CANNOT read another app's `/sdcard/Android/data/<pkg>/` on Android 11+.
  Shared storage (Music, Pictures, Downloads, DCIM) works fine — that carve-out
  is specific to other apps' private dirs. See the `termux-mobile-dev` skill.
- Termux is the operator's dev environment, **not a runtime dependency of the
  app.** The app must run with nothing connected.

---

## Superseded — historical context

How the build got here, **not a ban list**. If one of these turns out to be the
right tool again, that's an open question, not a rule violation.

| Old | Replaced by |
|---|---|
| Track 1 / Track 2 (Qwen3.5-9B) | ONNX → QNN context binary → Hexagon HTP |
| LiteRT / LiteRT-LM (Qwen3.5-9B) | ort_engine daemon |
| genie_engine (Qwen3.5-9B) | ort_engine (ORT + QNN EP) |
| Separate Watchdog | CliffordService |
| Nexa SDK, OmniNeural | dead — though GenieX carries Nexa lineage |
| Cloud failover in app LLM | HttpFetch agent tool |
