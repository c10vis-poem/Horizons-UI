# Novus-Agenti Model Stack — Configuration & Deployment

This directory contains the **operational specs** for the on-device dual-agent AI stack. Actual GGUF weights live on HuggingFace; this repo maintains the routing logic, orchestration configs, and documentation.

## Directory Structure

```
novus-agenti/
├── models/
│   ├── qwen-9b-q4_0/              # Agent 2 (Deep Thinker)
│   │   ├── geniex-config.yaml     # Inference parameters
│   │   └── hf-download-link.txt   # HF URL for wget
│   ├── qwen-2b-compiled/          # Agent 1 (Router)
│   │   ├── geniex-config.yaml
│   │   └── hf-download-link.txt
│   ├── gemma-12b-it-qat/          # Fallback (single-agent)
│   │   ├── geniex-config.yaml
│   │   └── hf-download-link.txt
│   ├── gemma-4-e4b/               # Backup swap
│   ├── gemma-4-e2b/               # Emergency tiny
│   └── qwen-0.8b-backup/          # Ultimate fallback
│
├── runtimes/
│   ├── geniex-orchestration.md    # How dual-agent routing works
│   ├── qairt-params.yaml          # HTP optimization settings
│   └── agent-dispatch-logic.md    # Handoff logic details
│
├── docs/
│   ├── MODELS-README.md           # This file
│   ├── model-routing-guide.md     # When to use which agent
│   ├── qai-hub-reference.md       # QAI Hub model info
│   └── unsloth-reference.md       # Training / quantization notes
│
└── scripts/
    ├── download-models.sh         # One-shot bootstrap (wget only)
    └── bootstrap-agent-runtime.sh # Full setup script
```

## Quick Start

### 1. Bootstrap Models (First Run Only)

```bash
bash ~/novus-agenti/scripts/download-models.sh
```

This downloads all 6 models from HuggingFace to `~/models/` using `wget`. No dependencies. Cached locally after first run.

**Time:** ~60 minutes (depends on connection; models are 5-12GB each)

### 2. Start GenieX with Dual Agents

```bash
geniex-server --config ~/.geniex/config.yaml --port 8080
```

GenieX loads both Agent 1 (2B, always-hot) and Agent 2 (9B, on-demand) under one coordinator.

### 3. Test the Routing

```bash
# Simple query (Agent 1)
curl http://localhost:8080/v1/messages \
  -d '{"model":"agent-1-router","messages":[{"role":"user","content":"What time is it?"}]}'

# Deep reasoning (Agent 1 → Agent 2 handoff)
curl http://localhost:8080/v1/messages \
  -d '{"model":"agent-1-router","messages":[{"role":"user","content":"Synthesize my entire week"}]}'
```

Watch handoff signals:
```bash
tail -f ~/.openwiki/_handoff.md
tail -f ~/.openwiki/_result.md
```

## Model Specs at a Glance

| Agent | Model | Size | RAM | Role | Latency | State |
|-------|-------|------|-----|------|---------|-------|
| **1** | Qwen 3.5 2B | 1.2GB | 1.6GB | Router / JSON formatter | <100ms | Always-hot |
| **2** | Qwen 3.5 9B Q4_0 | 5.0GB | 5.74GB | Deep reasoning / synthesis | 1-5s | On-demand |
| **Fallback** | Gemma 4 12B IT | 5.1GB | 5.1GB | Single-agent (no routing) | 2-3s | Active if dual unavailable |
| **Backup** | Gemma 4 E4B | 5.1GB | 5.1GB | Emergency swap (development) | — | Fallback |
| **Emergency** | Qwen 0.8B | 0.5GB | 0.8GB | Ultimate fallback (factual only) | — | Last resort |

**Total Budget:** 7.44GB (within 9GB ceiling for Snapdragon Elite with margin)

## Architecture: AESOP Harness + NOVUS-AGENTI Weights

- **AESOP** = The operational harness (memory layer, voice pipeline, permissions, protocols)
- **NOVUS-AGENTI** = The agent weights and routing logic (this repo)
- **HORIZONS** = The Kotlin UI (WebSocket bridge to hardware)

Flow: User speaks → Kotlin UI → WebSocket → Termux LLM → local inference → result back to UI → TTS/NPU via Kotlin

## Configuration Files

Each model directory contains:

1. **geniex-config.yaml** — Runtime parameters (context size, GPU layers, timeout, etc.)
2. **hf-download-link.txt** — HuggingFace URL for wget download
3. (Optional) **routing-spec.md** — Model-specific routing logic

Edit `geniex-config.yaml` to tune:
- `context_size` — Larger = more context, slower
- `max_tokens` — Limit output length
- `temperature` — 0.5 (focused) to 1.0 (creative)
- `n_gpu_layers` — 999 (all to HTP) vs lower (CPU fallback)

## Orchestration: GenieX Unified Stack

**Why not separate `llama-server` + QAIRT?**
- Hexagon HTP is shared → two runtimes = context-switch thrashing = 0 tokens/sec
- GenieX prevents this by serializing all tensor ops under one C SDK
- Both models load, but only one accesses HTP at a time

**Handoff Flow:**
1. Agent 1 receives query
2. Checks MEMO (7-day window)
3. If can answer: respond immediately
4. If cannot: write `_handoff.md` with query + context snapshot
5. Background script triggers Agent 2
6. Agent 2 loads (displaces Agent 1 from HTP)
7. Synthesizes response → writes `_result.md`
8. Purges cache → frees 5.74GB RAM + HTP access
9. Agent 1 resumes for next query

## HuggingFace Repos (Actual Weights)

All GGUF binaries live on HuggingFace under the `c10vis-poem` organization:

- `Qwen/Qwen3.5-9B-Instruct-GGUF` (Q4_0 variant)
- `NexaAI/Qwen3.5-2B-Instruct-NPU` (or fallback GGUF)
- `c10vis-poem/gemma-4-12b-it-qat`
- `c10vis-poem/gemma-4-e4b`
- `c10vis-poem/gemma-4-e2b`
- `Qwen/Qwen0.5B-Chat-GGUF`

**Note:** Don't store GGUF files in this GitHub repo. Keep weights on HuggingFace, configs here.

## Documentation

- **geniex-orchestration.md** — Complete dual-agent routing logic + tuning
- **model-routing-guide.md** — When to use which agent (decision tree + examples)
- **qai-hub-reference.md** — QAI Hub compiled model specs and QAIRT details
- **unsloth-reference.md** — Training / quantization docs (reference only — models are pre-quantized)

## Testing & Debugging

**Check memory:**
```bash
free -h
```

**Monitor Agent 2 load:**
```bash
# Agent 2 wakes up when _handoff.md appears
watch -n 1 'ls -la ~/.openwiki/_*.md'
```

**Force cache drop (if memory stuck):**
```bash
sync && echo 3 > /proc/sys/vm/drop_caches
pkill -f "agent-2" || true
```

**Fallback to single-agent:**
If Agent 2 fails to load (device at 6GB ceiling), system falls back to Gemma 12B IT or querying MEMO only. No crash, just degraded.

## Deployment Pipeline

1. **Boot script** (in aesop/deploy/phone/boot.sh):
   ```bash
   bash ~/novus-agenti/scripts/download-models.sh  # First-run only
   geniex-server --config ~/.geniex/config.yaml --port 8080 &
   # Rest of boot sequence...
   ```

2. **OpenWiki CLI** calls models via `http://localhost:8080/v1`

3. **Voice pipeline** (VAD/STT/TTS) integrates with OpenWiki → models

4. **Kotlin UI** sends queries via WebSocket → Termux → model → WebSocket → UI

## Performance Expectations

| Query Type | Agent 1 Latency | Agent 2 Total | Notes |
|-----------|-----------------|---------------|-------|
| Fact lookup | <50ms | — | MEMO hit, no reasoning |
| Simple answer | 100-200ms | — | JSON formatting |
| Medium reasoning | — | 2-4s | Handoff + synthesis |
| Deep reasoning | — | 5-10s | Complex timeline |
| Code generation | — | 8-15s | Multi-step synthesis |

Worst-case: 10s from query to response (complex synthesis on device).

## Fallback Chain

1. **Qwen 2B + 9B** (normal)
2. **Gemma 12B IT** (single-agent, if dual unavailable)
3. **Gemma 4 E4B** (lighter weight)
4. **Qwen 0.8B** (emergency, factual only)
5. **MEMO offline** (no inference, just knowledge lookups)

## What NOT to Do

- ❌ Store GGUF files in GitHub (use HuggingFace)
- ❌ Run separate `llama-cli` + QAIRT processes (use GenieX coordinator)
- ❌ Quantize models in this repo (they're pre-quantized, plug-and-play)
- ❌ Add cloud API dependencies (all local, all the time)
- ❌ Modify model weights (read-only)

## Questions / Debugging

See `model-routing-guide.md` for the decision tree + testing steps.
