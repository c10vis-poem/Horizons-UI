---
name: termux-mobile-dev
description: >-
  Set up and troubleshoot the on-device Termux mobile dev environment — phone
  as TigerVNC + XFCE host, Samsung Tab S9 FE+ as AVNC client, plus the Matrix
  (zsh + tmux + cmatrix + Termux:Float) phone terminal. Also covers the
  three-mode AI architecture: APK Active (dual Qwen via GenieX), Agentic Shell
  Mode (APK exposes localhost OpenAI API to Termux), and Bare Metal Mode
  (Termux owns hardware, Gemma 4 12B QAT). Includes mem0 local memory setup,
  model downloads, and SELinux constraints.
---

# Termux Mobile Dev Environment

**Device:** Motorola Razr Ultra 2025 · SM8750 · 16GB RAM · Hexagon HTP v79

---

## Three-Mode AI Architecture

Termux and the Horizons APK share one NPU. They cannot both use it simultaneously.
The three modes define who owns the hardware at any given time.

### Mode A — APK Active (primary)
The Horizons APK controls the NPU via GenieX. Two Qwen models run inside a
single GenieX context with serialized execution:
- **Agent 1 (Gatekeeper):** Qwen 3.5 2B · ~1.21 GB · QAIRT backend (pre-compiled .dlc) · always hot
- **Agent 2 (Thinker):** Qwen 3.5 9B Q4_0 · ~5.74 GB · llama_cpp backend · loaded on demand, unloaded immediately after

Combined footprint: ~6.95 GB. Serialized execution — GenieX freezes the 2B's compute
state, hands 100% HTP to the 9B, then unfreezes after. `serialize_execution_blocks: true`
prevents NPU thrashing. Termux in this mode runs a sub-1B model or cloud connector only.

### Mode B — Agentic Shell Mode
APK loads Gemma 4 12B QAT on the NPU and exposes a localhost OpenAI-compatible
endpoint. Termux scripts call it directly:
```bash
curl -s http://127.0.0.1:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model":"gemma-shell","messages":[{"role":"user","content":"hello"}]}'
```
This is the bridge for Termux Python scripts to get NPU-accelerated inference
without touching NPU drivers directly (which SELinux blocks — see below).

### Mode C — Bare Metal Mode (APK killed)
APK is dead. Termux owns 100% of the hardware. Run Gemma 4 12B QAT GGUF via
GenieX's llama_cpp backend directly:
```bash
geniex serve --model ~/models/gemma-4-12b-it-qat-q4_k_xl.gguf \
  --backend llamacpp --port 8080
```
Use this for heavy offline power work when the APK is not needed.

---

## Critical Constraint: Termux Cannot Hit the NPU Directly

**SELinux blocks Termux from accessing `/vendor/lib64` NPU drivers.** Running
GenieX or llama.cpp inside Termux will fall back to CPU — it cannot reach the
Hexagon HTP. This is not a configuration issue; it is Android's mandatory security
policy. The APK (with `foregroundServiceType="specialUse"`) owns the privileged
path to the NPU via `CliffordService`. Termux gets NPU access only through the
APK's localhost bridge (Mode B/Agentic Shell Mode above).

Killed guesses:
- ❌ "compile GenieX in Termux and it'll hit the NPU" — SELinux denies it.
- ❌ "run llama-cli in Termux against HTP" — same sandbox, same block.

---

## Model Files — What's Already on Device

As of 2026-07-13 snapshot (`knowledge/device-inventory/DEVICE-INVENTORY.md`):
```
/storage/emulated/0/Download/
  geniex-bench-android-arm64    # v0.3.14, both GGML + QAIRT backends
  qwen3.5-9b-q4_0.gguf          # ~5.74 GB
  HTP_79_libs/                  # HTP v79 runtime libs
```
Re-verify before downloading anything: `ls ~/Download/` or `ls /sdcard/Download/`.

### Download Gemma 4 12B QAT (Mode C / Bare Metal)
```bash
# requires HF egress in this session — test first:
huggingface-cli whoami

# download the Q4_K_XL quant (~6.72 GB):
huggingface-cli download unsloth/gemma-4-12b-it-qat-GGUF \
  --include "gemma-4-12b-it-qat-q4_k_xl.gguf" \
  --local-dir ~/models/
```
Correct size is ~6.72 GB. If HF egress is blocked in this session
(`403` / `Host not in allowlist`), trigger from Termux on-device instead.

### Download Qwen 3.5 2B (Agent 1 router)
```bash
huggingface-cli download Qwen/Qwen3.5-2B-Instruct-GGUF \
  --include "qwen3.5-2b-instruct-q4_k_m.gguf" \
  --local-dir ~/models/
```
The pre-compiled .dlc for QAIRT backend is produced by QAI Hub — check
`compile/manifest.yaml` for the current status. If not yet compiled, the
2B runs through llama_cpp backend temporarily.

---

## mem0 — On-Device Memory Layer

mem0 runs in Termux as a Python process, providing persistent memory across
sessions. Configuration: **local SQLite + local embeddings, zero cloud dependencies.**

### Install
```bash
pip install mem0ai sentence-transformers
```

### Config file (`~/mem0_config.yaml`)
```yaml
vector_store:
  provider: sqlite
  config:
    db_path: ~/.mem0/memories.db

embeddings:
  provider: huggingface
  config:
    model: BAAI/bge-small-en-v1.5
    # ~100 MB, runs on CPU in Termux

llm:
  provider: openai
  config:
    api_base: http://127.0.0.1:8080/v1   # APK in Agentic Shell Mode
    api_key: local
    model: gemma-shell
```

### Basic usage (Python)
```python
from mem0 import Memory
import yaml

with open("/data/data/com.termux/files/home/mem0_config.yaml") as f:
    cfg = yaml.safe_load(f)

m = Memory.from_config(cfg)
m.add("User strictly requires Float precision for Canvas math", user_id="horizons")
results = m.search("canvas precision", user_id="horizons")
```

### FastAPI bridge (expose mem0 to APK at 127.0.0.1:8081)
```bash
# ~/termux_bridge.py  — run as: python ~/termux_bridge.py
```
The APK's `LocalMemoryBridge.kt` hits `http://127.0.0.1:8081/memory/search`
via OkHttp. This lets the native Kotlin memory layer delegate to Termux mem0
without embedding Python in the APK.

---

## VNC Setup — Phone as TigerVNC + XFCE Host

### Topology
- **Host:** phone, Termux, no root → Xtigervnc + XFCE
- **Client:** Samsung Tab S9 FE+, AVNC → geometry **1280×800** (half of 2560×1600)
- **Transport:** same WiFi LAN. Phone IP is **DHCP — re-check every session.**
  `127.0.0.1` is loopback; the tablet cannot use it.
- Display `:1` = port **5901**

### First move on ANY connection failure: classify the error
- **Connection refused** → port closed / VNC bound to localhost
- **Connection timed out** → still localhost-bound OR router **AP isolation**

### Verification — `ss`/`netstat` DO NOT WORK on the phone
Unprivileged Termux denies netlink. Verify via the **VNC log** instead:
```bash
cat ~/.vnc/*.log | grep -iE "listen|interface|port"
```
| Log line | Meaning | Fix |
|----------|---------|-----|
| `Listening for VNC connections on all interface(s)` | bound to LAN; correct | still timing out → AP isolation → phone hotspot |
| `Listening ... localhost` / only `127.0.0.1` | localhost-bound | apply tigervnc.conf fix below |
| crash / no listen line | server died | read rest of log; usually dbus |

Secondary probe (netlink-free):
```bash
pkg install netcat-openbsd
nc -vz 127.0.0.1 5901         # up?
nc -vz <phone-wlan-ip> 5901   # bound to all interfaces?
```

### Make VNC listen on the LAN (persistent)
CLI flag `-localhost no` was unreliable. Use the config file:
```bash
echo '$localhost="no";' >> ~/.vnc/tigervnc.conf
echo '1;' >> ~/.vnc/tigervnc.conf
vncserver -kill :1
vncserver :1 -geometry 1280x800 -depth 24
```

### AP isolation (router blocks device-to-device WiFi)
If log says "all interface(s)" but tablet still times out: enable **phone hotspot**,
connect tablet to it, point AVNC at hotspot IP (often `192.168.43.1`).
socat fallback: `socat TCP-LISTEN:5902,bind=0.0.0.0,fork TCP:127.0.0.1:5901 &`

### XFCE/dbus won't start
Log shows `Failed to get a Console kit proxy` → no session dbus:
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
Package is `dbus`, NOT `dbus-x11` (that name does not exist in Termux).

### Stale lock cleanup (Termux tmp is `$PREFIX/tmp`, NOT `/tmp`)
```bash
vncserver -kill :1 2>/dev/null
pkill -9 Xvnc; pkill -9 Xtigervnc
rm -f ~/.vnc/*.pid ~/.vnc/*.log
rm -f $PREFIX/tmp/.X1-lock $PREFIX/tmp/.X11-unix/X1
```

### Per-session checklist
1. `ifconfig` → phone `wlan0` inet (not 127.0.0.1)
2. Clean locks if restarting
3. `vncserver :1 -geometry 1280x800 -depth 24`
4. Verify via the **log** (not `ss`): must say "all interface(s)"
5. AVNC → `<phone-ip>:5901` + vncpasswd
6. Timeout despite "all interface(s)" → AP isolation → hotspot or socat

---

## Matrix Phone Terminal (the "native Android over a waterfall" look)

- Pieces: **zsh + Oh My Zsh**, **tmux** (split), **cmatrix** (waterfall),
  **Termux:Float** (overlay; F-Droid only, Play build won't work)
- Colors → `~/.termux/colors.properties`: bg `#0D0D0D`, fg/cursor `#00FF41`;
  then `termux-reload-settings`
- Launcher `~/matrix-tmux.sh`: new detached session → `cmatrix -b -C green` top →
  `split-window -v -p 40` → attach. cmatrix top 60%, shell bottom 40%
- tmux prefix on Android: `Vol-Down+B` barely registers — tap **CTR** on the
  keybar then **B**. Stop waterfall without switching panes:
  `tmux send-keys -t matrix:0.0 q ""`
- Float attach needs `unset TMUX && tmux attach -t matrix` (else "sessions
  should be nested with care"). Float won't open → enable "Display over other
  apps". Resize → long-press, drag corners.

---

## On-Device Coding Agents: OpenClaude + DeepSeek V4 via OpenRouter

**Status:** Still valid for grunt work (refactors, doc gen, test scaffolding)
when the Horizons APK is not running. Primary AI path is now the APK's
dual Qwen stack via GenieX — use OpenClaude for coding tasks only.

### Install on Termux
```bash
pkg install nodejs git
npm i -g openclaude
```

### Point at DeepSeek V4 via OpenRouter
```bash
read -s OPENROUTER_KEY
export OPENROUTER_API_KEY="$OPENROUTER_KEY"
echo 'export OPENROUTER_API_KEY="<paste-in-editor>"' >> ~/.zshrc

openclaude config set baseURL  https://openrouter.ai/api/v1
openclaude config set model    deepseek/deepseek-chat-v4
openclaude config set apiKey   "$OPENROUTER_API_KEY"
```
Then `openclaude` in the repo dir.

**Cost shape (June 2026, OpenRouter):** DeepSeek V4 ~$0.27/M in, ~$1.10/M out.

**Keep architecture / multi-file reasoning on cloud Claude. OpenClaude handles
the grunt work. Architecture decisions belong to a session with full context.**

### Killed guesses
- ❌ "DeepSeek can hit the official `claude` CLI directly." Hardcoded to Anthropic's API.
- ❌ "OpenCode = OpenClaude." Different projects.
- ❌ "Termux GenieX/llama.cpp hits the NPU." SELinux blocks it — see above.

---

## Omnara (parked, not yet trialled)

YC S25 — mobile/web front-end for Claude Code. Same Anthropic Claude under the
hood, push notifications, multi-session management. Candidate for the next
multi-agent fan-out. Drop install + auth notes here after first run.

---

## Killed Guesses (do not repeat)

- ❌ "Android needs root to bind external ports." FALSE — Termux serves VNC to
  the LAN with no root.
- ❌ Relying on CLI `-localhost no`. Use `tigervnc.conf` instead.
- ❌ Using `ss`/`netstat`/`/proc/net/tcp` to verify the bind. Netlink denied.
  Use the VNC log or `nc`.
- ❌ Cleaning `/tmp`. Termux uses `$PREFIX/tmp`.
- ❌ Running GenieX/llama.cpp in Termux and expecting NPU throughput. SELinux
  blocks vendor driver access from unprivileged Termux processes.
- ❌ Gemma 4 E4B as the heavy model — PLE architecture carries ~5.1B stored
  params but activates only ~3B/token, wasting RAM. Qwen 3.5 9B gives 3x
  reasoning for the same ~5.74 GB footprint.
- ❌ `geniex_sdk` Python bindings, `GenieXEngine` Kotlin class, or
  `GenieX_SuspendModelCompute()` C function — Gemini fabricated these. Verify
  any GenieX API against actual `qualcomm/GenieX` source before using.
- ❌ LiteRT-LM as a Google package (`com.google.android.gms.ai.litert.LiteRTLM`)
  with a described API — Gemini fabricated this. LiteRT is real; that specific
  class/package is not verified to exist.

---

## Sources

- https://ivonblog.com/en-us/posts/vncserver-termux/
- https://xdaforums.com/t/guide-no-root-how-to-remotely-connect-to-your-phone-or-any-android-device-using-termux-and-a-pc.4572647/
- https://github.com/TigerVNC/tigervnc/issues/1476
- https://github.com/termux/x11-packages/issues/16
- https://docs.andronix.app/vnc/vnc-basics
- OpenRouter DeepSeek V4: https://openrouter.ai/deepseek/deepseek-chat-v4
- Omnara (YC S25): https://www.ycombinator.com/companies/omnara
- mem0 repo: https://github.com/mem0ai/mem0
- GenieX: https://github.com/qualcomm/GenieX
