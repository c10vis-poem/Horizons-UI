---
name: termux-mobile-dev
description: >-
  Set up and troubleshoot the on-device Termux mobile dev environment — phone
  as TigerVNC + XFCE host, Samsung Tab S9 FE+ as AVNC client, plus the Matrix
  (zsh + tmux + cmatrix + Termux:Float) phone terminal. Use when a VNC
  connection from the tablet times out or is refused, when XFCE/dbus won't
  start, when stale X lock files block the server, when setting VNC geometry,
  or when configuring the Matrix waterfall split-pane look. Every step is
  sourced or trial-verified; killed guesses are recorded so they aren't
  repeated.
---

# Termux Mobile Dev Environment

This file is the full reference — there is no separate `wiki/Termux-VNC-Matrix-Environment.md`;
an earlier version of this skill linked to one that was never actually created.

## Topology
- **Host:** phone, Termux, no root → Xtigervnc + XFCE.
- **Client:** Samsung Tab S9 FE+, AVNC → geometry **1280×800** (half of 2560×1600).
- **Transport:** same WiFi LAN. Phone IP is **DHCP, it changes** — re-check `ifconfig`
  (`wlan0` inet) every session. `127.0.0.1` is loopback; the tablet cannot use it.
- Display `:1` = port **5901** (port = 5900 + display).

## First move on ANY connection failure: classify the error
- **Connection refused** → port closed / VNC bound to localhost.
- **Connection timed out** → packets reach nothing → still localhost-bound OR router
  **AP isolation**.

### Verification — `ss`/`netstat` DO NOT WORK on the phone
Unprivileged Termux denies netlink: `Cannot open netlink socket: Permission denied`
(and `/proc/net/tcp*` is permission-denied too). Verify via the **VNC log** instead:
```bash
cat ~/.vnc/*.log | grep -iE "listen|interface|port"
```
| Log line | Meaning | Fix |
|----------|---------|-----|
| `Listening for VNC connections on all interface(s)` | bound to LAN; phone is correct | still timing out → **AP isolation** → phone hotspot |
| `Listening ... localhost` / only `127.0.0.1` | localhost-bound | apply tigervnc.conf fix below |
| crash / no listen line | server died | read rest of log; usually dbus |

Secondary probe (also netlink-free):
```bash
pkg install netcat-openbsd
nc -vz 127.0.0.1 5901            # up?  (always works if server alive)
nc -vz <phone-wlan-ip> 5901      # bound to all interfaces? (refused = localhost-only)
```

## Make VNC listen on the LAN (persistent, documented method)
The CLI flag `-localhost no` was unreliable in trials. Use the config file:
```bash
echo '$localhost="no";' >> ~/.vnc/tigervnc.conf
echo '1;' >> ~/.vnc/tigervnc.conf
vncserver -kill :1
vncserver :1 -geometry 1280x800 -depth 24
```

## AP isolation (router blocks device-to-device WiFi)
If the log says "all interface(s)" / `nc` to the wlan IP succeeds but the tablet still
times out, the router is the blocker — not the phone. Enable the **phone hotspot**,
connect the tablet to it, point AVNC at the hotspot IP (often `192.168.43.1`). socat
fallback: `socat TCP-LISTEN:5902,bind=0.0.0.0,fork TCP:127.0.0.1:5901 &` → AVNC on 5902.

## XFCE/dbus won't start
Log shows `Failed to get a Console kit proxy` → no session dbus. xstartup must launch it:
```bash
cat > ~/.vnc/xstartup << 'EOF'
#!/data/data/com.termux/files/usr/bin/bash
export DISPLAY=:1
export XDG_RUNTIME_DIR=$TMPDIR
dbus-daemon --session --address=$DBUS_SESSION_BUS_ADDRESS --nofork --nopidfile --syslog-only &
sleep 1
xfce4-session &
EOF
chmod +x ~/.vnc/xstartup
```
Package is `dbus`, **not** `dbus-x11` (that name does not exist in Termux).

## Stale lock cleanup (Termux tmp is `$PREFIX/tmp`, NOT `/tmp`)
```bash
vncserver -kill :1 2>/dev/null
pkill -9 Xvnc; pkill -9 Xtigervnc
rm -f ~/.vnc/*.pid ~/.vnc/*.log
rm -f $PREFIX/tmp/.X1-lock $PREFIX/tmp/.X11-unix/X1
```

## Killed guesses (do not repeat)
- ❌ "Android needs root to bind external ports." FALSE — Termux serves VNC to the LAN
  with no root (XDA `[NO-ROOT]` guide).
- ❌ Relying on CLI `-localhost no`. Use `tigervnc.conf` instead.
- ❌ Using `ss`/`netstat`/`/proc/net/tcp` to verify the bind. Netlink is denied on
  unprivileged Android. Use the VNC log or `nc`.
- ❌ Cleaning `/tmp`. Termux uses `$PREFIX/tmp`.

## Matrix phone terminal (the "native Android over a waterfall" look)
- Pieces: **zsh + Oh My Zsh** (the "zush"), **tmux** (split), **cmatrix** (waterfall),
  **Termux:Float** (overlay; F-Droid only, Play build won't work).
- Colors → `~/.termux/colors.properties`: bg `#0D0D0D`, fg/cursor `#00FF41`; then
  `termux-reload-settings`.
- Launcher `~/matrix-tmux.sh`: new detached session → `cmatrix -b -C green` top →
  `split-window -v -p 40` → attach. cmatrix top 60%, shell bottom 40%.
- tmux prefix on Android: `Vol-Down+B` barely registers — tap **CTR** on the keybar then
  **B**. Stop waterfall without switching panes:
  `tmux send-keys -t matrix:0.0 q ""`.
- Float attach needs `unset TMUX && tmux attach -t matrix` (else "sessions should be
  nested with care"). Float won't open → enable "Display over other apps". Resize →
  long-press, drag corners.

## Per-session checklist
1. `ifconfig` → phone `wlan0` inet (not 127.0.0.1).
2. Clean locks if restarting.
3. `vncserver :1 -geometry 1280x800 -depth 24`.
4. Verify via the **log** (not `ss`): must say "all interface(s)".
5. AVNC → `<phone-ip>:5901` + vncpasswd.
6. Timeout despite "all interface(s)" → AP isolation → hotspot or socat.

---

## On-device coding agents: OpenClaude + DeepSeek V4 via OpenRouter

**Two-pronged attack** — Anthropic Claude Code in the cloud (heavy reasoning) plus
**OpenClaude** on Termux (model-swappable, cheap parallel grunt work) hitting the same
GitHub repo / branch. You arbitrate merges.

### Why OpenClaude, not OpenCode
OpenClaude is the closer behavioural clone of the Anthropic Claude Code CLI — same
agentic tool-use loop, MCP-style integrations, runs native on Termux without an
Anthropic key. Claude Code for Android (the community port) is similar but locked to
Anthropic's API. Both are **non-Anthropic-proprietary** and run on Android; OpenClaude
wins on flexibility because you can point it at any model.

### Install on Termux (phone or tablet)
```bash
pkg install nodejs git
npm i -g openclaude          # or: git clone … && npm i -g .
```

### Point it at DeepSeek V4 via OpenRouter
```bash
# one-time: paste just the key at the prompt
read -s OPENROUTER_KEY
export OPENROUTER_API_KEY="$OPENROUTER_KEY"

# persist for future shells
echo 'export OPENROUTER_API_KEY="<paste-in-editor>"' >> ~/.zshrc

# OpenClaude config — OpenAI-compat base URL, DeepSeek V4 model id
openclaude config set baseURL  https://openrouter.ai/api/v1
openclaude config set model    deepseek/deepseek-chat-v4
openclaude config set apiKey   "$OPENROUTER_API_KEY"
```

Then `openclaude` in the repo dir. Tool-call fidelity on DeepSeek is good but not
Anthropic-tier — give it the grunt work (refactors, doc gen, test scaffolding, lint
sweeps), keep architecture / multi-file reasoning on cloud Claude.

### Cost shape (June 2026, OpenRouter)
- DeepSeek V4: ~$0.27 / M in, ~$1.10 / M out.
- Cheap enough to run multiple parallel agents on the same branch all day.

### Killed guesses
- ❌ "DeepSeek can hit the official `claude` CLI directly." It can't — that CLI is
  hardcoded to Anthropic's `/v1/messages`. You'd need a proxy shim
  (`claude-code-router`, `anyclaude`). OpenClaude sidesteps the whole problem.
- ❌ "OpenCode = OpenClaude." Different projects. OpenClaude is the one to use here.

---

## Omnara (parked, not yet trialled)

YC S25 — mobile/web front-end for Claude Code. Same Anthropic Claude under the hood,
prettier UI, push notifications, multi-session management. Candidate for the next
multi-agent fan-out (cloud Claude Code + OpenClaude-on-Termux + Omnara mobile front).
Drop install + auth notes here after first run.

---

## Claude Code Remote Control — pair Termux with claude.ai/code or the mobile app

Official Anthropic feature (research preview), verified against docs 2026-08-06.
Different problem than Omnara above: this pairs the **official** `claude` CLI
running in Termux with the web/mobile frontend as a window into it — execution
and filesystem access never leave the phone.

### Start it
```bash
cd <project-dir>          # must be a real project dir first — the workspace-trust
                            # dialog never saves for $HOME, so start there or it
                            # re-prompts every time
claude remote-control      # server mode: stays running, prints a URL, spacebar
                            # for a QR code, supports multiple concurrent sessions
# or:
claude --remote-control    # one interactive session, remote AND local input both work
# or, from inside a session already running:
/remote-control
```
Connect from elsewhere: open the printed URL, scan the QR code into the Claude
app, or find the session by name at claude.ai/code (green dot = online).

### Requirements
- **Full OAuth login. `ANTHROPIC_API_KEY` set anywhere disables Remote Control
  outright** — API keys are explicitly unsupported for it. Run `claude` then
  `/login` (or `claude auth login`) first.
- `DISABLE_TELEMETRY`, `DO_NOT_TRACK`, `CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC`,
  `DISABLE_GROWTHBOOK` each silently disable it if set anywhere — unset all four.
- Not available on Bedrock / Vertex / Foundry, or with a custom `ANTHROPIC_BASE_URL`
  (e.g. an LLM gateway) pointed at anything other than `api.anthropic.com`.

### Persistence — survives backgrounding only inside tmux/screen
The local `claude` process has to keep running; closing Termux (not just
switching apps) ends the Remote Control session outright.
```bash
tmux new -s remote
claude remote-control
# Ctrl-B, D to detach — session and connection both survive
```
See the "known conflict" below before reaching for this, though.

### Known conflict — fights the usual Termux OAuth-flakiness fix
The community workaround for Claude Code's OAuth being unreliable on Termux is
switching to `export ANTHROPIC_API_KEY=…`. **That workaround and Remote Control
are mutually exclusive** — Remote Control hard-requires OAuth and rejects API
keys outright. If OAuth itself won't stay logged in on this device, Remote
Control inherits that problem and there is no way around it via API key.

### Killed guesses
- ❌ "Use `ANTHROPIC_API_KEY` to dodge the re-auth loop, then start Remote
  Control anyway." Contradictory by design — confirmed in the official docs,
  not a bug: `ANTHROPIC_API_KEY` set anywhere is an unconditional block.
- ❌ Assuming a plain `tmux` wrapper is risk-free for this. There's an open,
  unresolved bug reporting persistent re-auth prompts **specifically inside
  tmux**, even with valid stored credentials (see Sources). If re-auth keeps
  happening, test `claude remote-control` **without** tmux first — if that's
  clean, the bug is tmux-specific and wrapping it will reintroduce the loop.

## Sources
- https://ivonblog.com/en-us/posts/vncserver-termux/
- https://xdaforums.com/t/guide-no-root-how-to-remotely-connect-to-your-phone-or-any-android-device-using-termux-and-a-pc.4572647/
- https://github.com/TigerVNC/tigervnc/issues/1476
- https://github.com/termux/x11-packages/issues/16
- https://docs.andronix.app/vnc/vnc-basics
- OpenRouter DeepSeek V4: https://openrouter.ai/deepseek/deepseek-chat-v4
- Omnara (YC S25): https://www.ycombinator.com/companies/omnara
- Claude Code Remote Control (official docs): https://code.claude.com/docs/en/remote-control
- Claude Code on the web / `--cloud` vs `--teleport` (official docs): https://code.claude.com/docs/en/claude-code-on-the-web
- Persistent re-auth prompts inside tmux, open bug: https://github.com/anthropics/claude-code/issues/9903
