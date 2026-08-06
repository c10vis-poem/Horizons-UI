# HORIZONS — OPERATOR'S GUIDE

    MØ[)u14R_ 11(
    *Pioneer_Tech (Next-Gen Certified)

Read from the Terminal:

    manual              full guide
    manual 3            chapter 3 only
    manual voice        search for a topic

---

## HOW TO READ THIS GUIDE

Horizons is a **manual, modular workbench for on-device AI**. It is not an
assistant that guesses. It boots completely empty and nothing heavy runs until
you flip a switch.

> **Core law: daemons stay dumb, the user is the loader.**

Every heavy component — the browser, the terminal, the inference engines — runs
as an independent "guest port" beneath the UI. The app's job is to be the
workbench, not to decide things for you.

**State tags used throughout.** This guide does not pretend. Anything marked
otherwise than `[WORKS]` is documented so you know where the edges are.

| Tag | Meaning |
|---|---|
| `[WORKS]` | Built and expected to function |
| `[UNVERIFIED]` | Built, never confirmed on a device |
| `[PARTIAL]` | Some of it works |
| `[PLANNED]` | Designed, no code yet |

---

# PART I — THE HOME SCREEN

## 1.1 · The clock face

Seven elements. Six tiles on a clock face, one hub at the centre.

```
                  12:00
                 MONITOR
        10:00              2:00
      HORIZONS            CHAT
              [ ROUTER ]
        8:00               4:00
      ARCHIVES          SETTINGS
                  6:00
                TERMINAL
```

| Position | Tile | Slug | Colour | Owns |
|---|---|---|---|---|
| 12:00 | **MONITOR** | `/cognito` | teal | verification, green lights, the main browser |
| 2:00 | **CHAT** | `/interface` | soft green | conversation, artifacts, the OS tool surface |
| 4:00 | **SETTINGS** | `/config` | pink | the vault — keys, tokens, assets |
| 6:00 | **TERMINAL** | `/shell` | matrix green | the mod garage — scripts, runtime definitions |
| 8:00 | **ARCHIVES** | `/logs` | amber | saved profiles, harnesses, logs |
| 10:00 | **HORIZONS** | `/about` | blue | version, credits, attribution |
| centre | **ROUTER** | `// CORE_HUB` | violet | the fuse box — ignition |

Each tile carries four lines: **TITLE**, `/slug`, a short subtitle, and a
bottom prompt line (`$_browser`, `$_model`, `$_utils`, `$_bash`, `$_files`,
`$_.home`). The Router shows `// CORE_HUB` above and `$_Statio` below.

**Tap a tile** to open it full-screen. Back returns to the wheel.

## 1.2 · The bottom status nodes

Five glowing dots beneath the wheel. They report subsystem state at a glance.

| Node | Tracks |
|---|---|
| **ASR** | speech recognition — is an STT engine loaded |
| **LLM** | the language model — is a backend alive |
| **TTS** | speech synthesis — is a voice loaded |
| **MLLM** | multi-modal — vision capability |
| **VAG** | vision-agent / screen capture |

Reading them:

| Appearance | Means |
|---|---|
| Solid bright | active and ready |
| Dim | loaded but idle |
| Pulsing | initialising |
| Dark | not configured |

On a clean install **every node is dark.** That is correct. An empty workbench
is the designed boot state, not a fault.

## 1.3 · The chat bar

The input bar at the bottom.

- **Tap** — opens a compact one-line input for a quick question.
- **Hold** — expands to a mini inference panel taking roughly the bottom third
  of the screen, without leaving the home screen. `[PLANNED]`

Conversations started here are saved into the Chat tile's history.

## 1.4 · Guardians

| Guardian | Appears when |
|---|---|
| **GOAT** | a runtime handshake fails — `// GOAT_SAYS_NO` plus a synthesized bleat. A banner **instead of** a crash. Tap the banner 7× for `// GOAT_UNLOCKED`. |
| **404 CAT** | a browser connection drops. Scoped to the browser — **not** an engine failure. |
| **CHONK** | 3–5 minutes idle. The screensaver covers the screen and pauses active processing. Tap or use the mic to wake. `[PARTIAL]` |

---

# PART II — THE SEVEN ROOMS

## 2.1 · MONITOR — 12:00 — the checkpoint

**What it is for:** looking, verifying, and browsing. The Monitor holds no data
and starts nothing. It is read-only by design.

**Operable functions**

- **Model library** — every model the app can see, with size and status.
  `[UNVERIFIED]`
- **`PLUG IN` / `UNPLUG`** — pin the model you intend to run. Nothing loads on
  plug-in; you are only declaring intent. `[WORKS]`
- **Green-light checklist** — per runtime definition, a lamp per requirement
  with **ALL GREEN** or **N RED**, naming what is missing. `[WORKS]`
- **`HAND TO ROUTER`** — plates a verified definition onto the Router.
- **Pop-out tabs** — `CONSOLE`, `TERMINAL`, `BROWSER`, each taking over the
  screen. `[UNVERIFIED]`
- **The main browser** lives here. See Chapter 6.

> The Monitor never runs anything. If you want ignition, that is the Router.

## 2.2 · CHAT — 2:00 — the stage

Multi-modal conversation against whichever backend the Router has live.

- Chat thread with selectable, copyable text `[UNVERIFIED]`
- Session history in a side panel `[UNVERIFIED]`
- The **OS tool suite** — 26 tools the model can call: app launch, alarms,
  calendar, contacts, wifi/bluetooth/volume/brightness/torch, media,
  notifications, clipboard, **shell**, `http_fetch`, `web_search`, battery,
  network, storage, tasker. `[UNVERIFIED]`
- File and image attachments `[PLANNED]`

Chat shows a backend as ready **only after you have started one.**

## 2.3 · SETTINGS — 4:00 — the armory

Where raw material lands. Nothing here runs.

- **Key vault** — API keys, tokens, bearer credentials `[UNVERIFIED]`
- **SAF file picker** — browse internal, SD and external storage `[UNVERIFIED]`
- **"Open with → Horizons"** — send a file to the app from any other app;
  it registers here, inert `[UNVERIFIED]`
- **Voice settings** — TTS voice, speed
- **Verbosity / debug sliders** `[PLANNED]` — present but not yet read by
  anything

**Files landing here do not load.** They register. That is the whole point.

## 2.4 · TERMINAL — 6:00 — the mod garage

Where runtimes get **defined** and scripts get written. **Nothing executes a
runtime here** — that is deliberate isolation between hacking and running.

- **Shell** — non-interactive `sh -c`, running as the app's own user
  `[UNVERIFIED]`
- **Runtime tab** — define a runtime as pure parameters (Chapter 1)
- **Long-press any history entry** → Copy command · Copy output · Export to
  Router · Save to Commands · Archive as a real `.sh`
- **Saved commands** — reusable, categorised
- **Browser shortcut** — the full browser lives in the Monitor

> **TUI programs do not work.** The shell is a pipe, not a pseudo-terminal, so
> `vim`, `htop`, `nano` and `tmux` will not run. Non-interactive commands are
> fine. `[PLANNED]` — a PTY layer would be needed.

**The in-app shell runs as Horizons, not as Termux.** This matters: the app's
user can reach the NPU. Termux cannot. See Chapter 4.

## 2.5 · ARCHIVES — 8:00 — the artifact vault

A real file manager over a real directory tree.

- Breadcrumb navigation, create folders and files, nest freely `[UNVERIFIED]`
- Tap a file for an inline viewer
- Stores `.sh` harnesses, exported scripts, logs
- **Verified runtime profiles** — the restore source recovery pulls from
  `[PLANNED]`

## 2.6 · HORIZONS — 10:00 — the front desk

Build version, model credits, open-source attribution, system info.
Carries a hidden game payload. `[PLANNED]`

## 2.7 · ROUTER — centre — the fuse box

**The only room that starts anything.**

- **Plated configurations** — each is Runtime · Backend · Model · Assets
- **States** — `Ready to Run` · `Running` · `Sleeping` · `Archived`
- **Flip a config on** — the Router consults the Monitor, reports any red
  lights, and **attempts the circuit anyway**

> **The Router does not say no.** It carries current. If your assets satisfy
> the requirements the engine fires; if they do not, the circuit simply fails
> to energise. It will not block you, and it will not refuse a custom binary or
> a fine-tuned weight it does not recognise. A failed flip is a circuit that
> did not close, not the app rejecting you.

---

# PART III — GETTING STARTED

## Chapter 1 · Defining a runtime in the Terminal

A **runtime definition** is pure parameters. It launches nothing. Five fields:

| Field | Is | Example |
|---|---|---|
| **Name** | what you call it | `geniex-gguf` |
| **Binary** | the executable | `geniex` |
| **Port** | where it listens | `18181` |
| **Health endpoint** | the handshake | `/v1/models` |
| **Args template** | how it starts | `serve --model {model} --port {port}` |

`{model}` and `{port}` are substituted at flip time.

**Steps**

1. Terminal → **Runtime** tab
2. Fill the five fields
3. **Define & ship to Monitor** — saves the definition, starts nothing
4. Monitor → check the green lights
5. **HAND TO ROUTER**

**Import / export / modify**

- **Export to Router** — long-press a shell entry, send it as a temporary runtime
- **Save to Commands** — keep it in the Terminal's library
- **Archive** — write it as a real `.sh` into Archives
- **Modify** — pull it back out of Archives, edit, re-ship

**The parameter packet is a plain text file.** Copy it, move it, replace it,
pull it out of the Archives. It is not a schema and it will not be validated
against a whitelist.

## Chapter 2 · Getting models onto the device

**The app never downloads weights.** Every model lives in **its own isolated
folder on the device** and loads by absolute path. Swapping a model is
drag-and-drop — no rebuild, no reinstall.

Storage cost is identical either way. Downloading only ever bought a
boot-time failure mode.

**Where to put things**

    /storage/emulated/0/Download/<model-name>/
    /storage/emulated/0/models/<model-name>/

Or pin an explicit folder in Settings.

**Language models — GGUF**

| | |
|---|---|
| Format | GGUF |
| Precision | **Q4_0** — best Hexagon NPU support |
| Source | any GGUF on Hugging Face |

**Language models — QAIRT bundles**

| | |
|---|---|
| Format | QAIRT `.bin` shards **plus a required `geniex.json`** |
| Source | Qualcomm AI Hub pre-compiled, per-chipset |
| Compute | NPU only, highest performance |

TFLite, LiteRT and QAT models are **inputs to the AI Hub compile**, which emits
the bundle. They are not loaded directly.

**Speech models** — see Chapter 3.

> **Recommended models:** maintained separately so the list can grow. Start
> from the operator's own Hugging Face buckets.

## Chapter 3 · Preparing the voice layer

**Voice runs in-process, on the CPU, inside the APK. It never touches the NPU** —
the NPU carries the two language models taking turns, and speech is not added
to that budget.

**Nothing needs compiling.** The engine (`libsherpa-onnx-jni.so`,
`libonnxruntime.so`) already ships inside the APK. Models are plain ONNX data
files. Drop them in a folder and go.

**What you need on the phone**

| Layer | Model | Size | Notes |
|---|---|---|---|
| **VAD** | Silero v5 | ~2 MB | **Already inside the APK.** Nothing to install. |
| **STT** | Whisper base.en int8 | 161 MB | encoder ~29 MB + decoder ~131 MB |
| | Whisper tiny.en int8 | 104 MB | smallest |
| | Moonshine tiny int8 | 124 MB | |
| | Moonshine base int8 | 287 MB | largest — check your budget |
| **TTS** | Kokoro multi-lang v1.0 | ~88–103 MB | 28 English voices |

**File layout — STT (Moonshine)**

    <folder>/preprocess.onnx
    <folder>/encode.int8.onnx
    <folder>/uncached_decode.int8.onnx
    <folder>/cached_decode.int8.onnx
    <folder>/tokens.txt

**File layout — STT (Whisper)**

    <folder>/base.en-encoder.int8.onnx
    <folder>/base.en-decoder.int8.onnx
    <folder>/base.en-tokens.txt

**File layout — TTS (Kokoro)**

    <folder>/model.onnx
    <folder>/voices.bin
    <folder>/tokens.txt
    <folder>/espeak-ng-data/

If a file is missing the engine stays not-ready and **tells you which one** —
it will not fail silently at transcribe time.

**Engine families are declared, not sniffed.** Whisper, Moonshine, Paraformer
and Parakeet each need a different loader, so you pick the family when you
configure the slot. Dropping files in the right folder is not enough on its own.

**Tuning**

| Parameter | Sensible default |
|---|---|
| VAD threshold | `0.5` |
| Min silence | `500 ms` (500–800 ms is the usable band) |
| Min speech | `250 ms` |
| Sample rate | `16000` |
| Max utterance | `~60 s` hard cap |

## Chapter 4 · Pushing to the Router

Every room can reach the Router. That is what makes it the hub.

| From | How | Carries |
|---|---|---|
| **Monitor** | `HAND TO ROUTER` after all-green | a verified runtime definition |
| **Terminal** | `Export to Router` on any command | a temporary runtime / script harness |
| **Settings** | export a baked asset + keys | endpoint, credentials, model pointer |
| **Archives** | restore a saved profile | a previously verified configuration |
| **Chat** | direct prompt push `[PLANNED]` | a prompt against the live backend |
| **Horizons** | the game payload `[PLANNED]` | renders on the Monitor |

**The two routes**

- **The long way** — Terminal defines → Monitor verifies → Router runs.
- **The short way** — you already know what you are doing: pick a model in the
  library and drop it straight onto the Router. It will show red lights until
  you satisfy them. That is allowed.

### NPU access — the one rule that catches people

A process launched **by Termux** runs as Termux and **cannot reach the Hexagon
DSP.** A process launched **by the app** runs as Horizons and can.

| How it starts | NPU? | Use for |
|---|---|---|
| Termux runs it; app dials `127.0.0.1:8081` | **No — CPU only** | quick tests, something already running |
| App launches it from a `RuntimeDef` | **Yes** | real NPU inference |

Both are valid. Only one gets you the NPU. If you want the NPU, the runtime
definition has to launch it.

### Choosing the compute unit `[PLANNED]`

Horizons is meant for any Android device, not one phone. The compute target is
your choice, not an assumption:

| Mode | Behaviour |
|---|---|
| `npu` | pinned to the NPU — deterministic |
| `hybrid` | scheduler spreads work across NPU/GPU/CPU per operation |
| `gpu` | GPU only |
| `cpu` | CPU only |

On a device with no NPU, `hybrid` and `cpu` degrade naturally.

## Chapter 5 · Running the Router

1. **Plate** a configuration — Runtime · Backend · Model · Assets
2. **Flip it on** — the Monitor is consulted live, at the instant of ignition.
   A series switch has no memory: if you unplugged the model after the Monitor
   approved it, the circuit will not close.
3. **Watch the readout** — an amber line reports whatever was red. It is
   information, not a refusal. The flip is attempted regardless.

**Managing what is plated**

| Action | Effect |
|---|---|
| **Run** | close the circuit |
| **Sleep** | unload from memory, keep the config on the deck |
| **Archive** | move it to Archives for later |
| **Delete** | remove it |

## Chapter 6 · Monitor and the browser

The full browser lives in the Monitor. The Terminal keeps a shortcut.

- Full chrome with a sidebar `[UNVERIFIED]`
- Multi-window — links open tabs, which is what makes OAuth popups work
  `[UNVERIFIED]`
- **Download-to-vault** — downloads land in the app's vault rather than the
  public Downloads folder `[PLANNED]`

If a page fails, the **404 CAT** appears inside the browser view. That is a
network error scoped to the browser — it does not mean an engine died.

---

# APPENDIX A · PATHS

    Diagnostics   /sdcard/Android/data/com.horizons/files/diag/
                    boot.log     tagged [main] / [clifford]
                    crash.log    stack traces

    Archives      <app files>/archive/
    Models        /storage/emulated/0/Download/<model>/

# APPENDIX B · TROUBLESHOOTING

| Symptom | Cause | Do this |
|---|---|---|
| All status nodes dark on a fresh install | Correct — the workbench boots empty | Plug something in |
| Chat says no backend | Nothing has been flipped on | Router → plate → flip |
| Flip reports red lights | Something is missing | Read the amber line; it names it |
| Model runs but never uses the NPU | Launched by Termux | Launch it from a `RuntimeDef` instead |
| Voice does nothing | Model files missing | Check the folder layout in Chapter 3 |
| `vim`/`htop` produce garbage | No PTY — the shell is a pipe | Use non-interactive commands |
| App died with no trace | Likely killed from outside | `tail -40 crash.log` — empty means external kill |

# APPENDIX C · THE LAWS

1. **Daemons stay dumb, the user is the loader.**
2. **Boots empty, boots stable.** Nothing heavy runs until you flip a fuse.
3. **The Router carries current and does not argue.** It never refuses.
4. **The Monitor holds the gate, live, at flip time.** A series switch has no
   memory.
5. **Models live in their own folders and load by path.** The APK never
   downloads weights.
6. **Wired is not launched.** The app is capable of everything on the phone. It
   just does not start with any of it.
