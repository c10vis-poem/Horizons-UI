# Router · Monitor · Terminal — Operator Spec

> **Status: ADOPTED.** Dictated by the operator, 2026-08-04, with four reference
> screenshots. This supersedes earlier descriptions of the Router as a gate,
> fuse box, or breaker switch wherever they conflict.
>
> A previous session was given this and never wrote it down. That is why it is
> here.

---

## 0. The correction that matters

**The Router is not a gate.** It does not verify, refuse, or "blow a fuse."
There is no flip-the-switch step in the Router.

The Router is a **load bay**. You load things into it, adjust them, and you are
done. The **Monitor** is the last step and the thing that actually engages a
run.

Earlier documents describe a `greenLight()` gate in the Router that throws a red
FUSE BOX banner. That was a literalization of metaphor language the operator used
to make the concept legible — it was never a specification. See §5.

---

## 1. Flow of authority

```
  TERMINAL  ──────►  MONITOR  ──────►  (run)
  defines            verifies
  the package        + dispatches
      │                  ▲
      │                  │
      └──►  ROUTER  ─────┘
            loads / preps / holds
```

1. **Terminal** configures a runtime package — this is where the four questions
   get answered and the four boxes get *defined*. Exported **as a file**.
2. **Router** holds the hardware: model weights in the changer, parameters on the
   display, runtime files in the tape deck. You select which of the loaded set is
   current. This *preps* a model. It does not run it.
3. **Monitor** takes the package file from the Terminal, looks at what the Router
   currently has loaded, and asks: *do they check all the boxes?* If yes, it runs.
   The Monitor is what ensures what you run through the Router will not crash the
   system, using the information the Terminal gathered.

**All six tiles push to the Router.** The Router is the destination; it is where
everything ends up.

---

## 2. The Router — stereo stack

Visual reference: **Aiwa NSX-V20** compact stereo (screenshot 1).

### Layout

Roughly **60–70% of the screen** is the stereo itself; the remaining **30–40%**
is a submenu strip at the bottom, about the height of a small keyboard. Touching
a component up top pops it open, and the options in the lower strip change to
match. What you touch above, you work with below.

### The CD changer (top) — model weights

- Tap the changer button and the tray **visibly animates open**, showing slots.
- **Six slots.** (Three, as on the real unit, is too few.)
- Load model weights here.
- **Open question:** whether the voice layer also lives here, so its parameters
  can be adjusted or the whole layer swapped out. Operator is undecided.

### The display window (middle) — parameters

The stereo's display is an **interactive panel with touch buttons**. This is
where runtime parameters are set, and it is where they *should* be set — not
baked in before the APK is ever built.

- Voice pitch
- Voice speed
- Temperature
- Cores / NPU targeting
- Verbosity — **open question**, operator unsure whether this is still adjustable

### The tape deck (bottom) — runtime files

- Scripts and preloaded commands that travel *with* a model.
- Behaves like a folder you add files to — open it, add, remove.
- Import and export runtimes here, as files.

### Cycling / hot-swap

The channel-change control in the middle cycles between **up to six loaded
runtimes**. Keep several in the Router, cycle to the one you want, select it.
That is the hot-swap.

### Terminal tab

The Router carries a **terminal tab in the upper corner**. Tapping it opens a
terminal **window at the bottom** of the Router, so the Terminal is reachable
without leaving the tile.

---

## 3. The Monitor — arcade cabinet

Visual reference: **Video-Sonic stand-up arcade cabinet** (screenshot 2).
Screensaver: the glass HUD waveform panel (screenshot 3).

- The tile renders as a retro upright cabinet, joystick and buttons interactive.
- On engaging, **the cabinet screen animates and jumps out to fill the display.**

### What the Monitor does

- Holds the runtime package **file** produced by the Terminal.
- Shows what the Router currently has loaded.
- Shows what is in Settings (the Vault).
- Scroll the library.
- Assemble a runtime file package directly, here.
- **Checks the boxes and runs it.** This is the dispatch point.

### Tabs

- **Browser tab** in the corner — opens a full interactive Chromium WebView.
  Missing a piece of the package? Fetch it here.
- **Terminal tab at the top** — opens a terminal window at the top of the Monitor.

---

## 4. The Terminal — fakesteak matrix

Background: the operator's fork of **fakesteak** (matrix rain).
Screensaver / idle display: green oscilloscope CRT (screenshot 4).

- On typing, the dialogue surface appears: **black background, green matrix
  font.**
- Below the shell: a place for **saving scripts and commands.**

### What the Terminal is for

This is where you ask the device and find out the answers to the four questions —
**the Terminal defines the four boxes.** The result is exported **as a file**,
sent to the Monitor.

Also from here:
- Export an on-device agent already running in the Terminal.
- Pull from GitHub or HuggingFace, obtain an API, and push it toward the Router.

Definitions are expected to change over time. The package is a file, not a
hardcoded contract.

---

## 5. The four boxes

The four failure classes are real. The fuse/amperage/green-light *ceremony* is
not — that language was the operator's way of making the idea legible to an
agent, and prior sessions compiled it into a state machine.

The Terminal **defines** the four boxes for a given package. The Monitor
**checks** them. Neither is the Router's job.

| # | Class | Plain form |
|---|---|---|
| 1 | Engine | what makes it go |
| 2 | Fuel & cargo | assets — sometimes zero, sometimes many |
| 3 | Road & weight limit | architecture compatibility, available RAM |
| 4 | Communication | syntax and handshake |

Class 3 is **not implemented anywhere in the codebase.** It is the one that
matters most for shipping to arbitrary devices, since it is what prevents trying
to start a Hexagon daemon on hardware with no NPU.

---

## 6. Tiles — what each pushes to the Router

| Tile | Pushes to Router |
|---|---|
| **Horizons** | Eventually a video game, reached via an Easter egg — load it into the Router and run it. Long-term. |
| **Monitor** | Has its own sandbox. Upload or download any API; push from the browser's download folder into the Monitor or straight to the Router. |
| **Chat** | Artifacts the agent produces, links, downloadable files pulled from web search. |
| **Settings** | **Settings is the Vault.** API keys, tokens, anything imported gets a storage folder. On-device files uploaded land here. All pushable to the Router. |
| **Terminal** | Pushes to the Router *and* to the Monitor. |
| **Archives** | Bundled packages, runtime files, model files, agent files — cycled out of the Router when its slots are full. Also inert models, spent scripts, bash commands, old log files, artifacts. |

---

## 7. Settled by this document

- Settings is named **the Vault**. The Platform Armory / Systems Operations pin
  is resolved.
- The tile is **Archives**, not Artifacts.
- The runtime package is **a file** — produced by the Terminal, held by the
  Monitor, movable. Not an internal store.
- The Router **does not gate**.

## 8. Still open

- Voice layer in the Router's changer — undecided.
- Whether verbosity remains an adjustable parameter.
- Two launcher icons (`.MainActivity` + `.uilocal.LocalHomeActivity`) — unanswered.
- Whether `uilocal/` survives at all — unanswered.

---

## 9. Distance from the current code

Nothing in §2–§4 is built. Present state:

- `RouterPane` is a scrolling list of config cards with a switch. No stereo, no
  changer, no tape deck, no parameter surface, no cycling.
- The Router **does** gate today — `switchOn()` refuses on a failed check. That
  contradicts §0 and needs removing.
- The Router now launches the plated runtime (added 2026-08-04). Under this spec
  that responsibility belongs to the **Monitor**, so the launch call will move.
- `RouterConfigStore` is an internal JSON blob, not a portable file (§7).
- Temperature is hardcoded (`NpuClient`, `CloudLlmRuntime`); verbosity has a
  Settings slider nothing reads; cores do not exist. §2's parameter surface has
  nothing to bind to yet.
- No terminal tab in either the Router or the Monitor.
- Browser exists and works, in the Monitor, as §3 requires.

### Screenshots

The four reference images were supplied in-conversation and are **not yet in the
repo**. They belong in `wiki/router-monitor-img/`:

1. Aiwa NSX-V20 stereo — Router
2. Video-Sonic arcade cabinet — Monitor
3. Glass HUD waveform panel — Monitor screensaver
4. Green oscilloscope CRT — Terminal screensaver
