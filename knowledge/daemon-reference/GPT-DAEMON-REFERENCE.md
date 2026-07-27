# GPT Daemon & Architecture Reference (Distilled)

> **Source:** GPT-generated guides posted session 9 (2026-06-28).
> **Status:** Distilled — wrong specifics stripped, useful patterns kept.
> Raw material was ~15K words across 5 documents covering compile pipeline,
> daemon architecture, NPU governance, sampling, Game SDK, and deployment.

---

## What Was Wrong (Do Not Use)

| Claim | Reality |
|-------|---------|
| `transformers==4.40.2` | Qwen3.5 requires transformers 5.x+ |
| `optimum-cli --rope-fuse` | No Qwen3.5 export config in optimum; our manual M-RoPE fold is required |
| Output `.hex` / `.qpc` | QAI Hub produces `qnn_context_binary` (.bin) |
| Hexagon v68 | SM8750 = Hexagon HTP v79 |
| SNPE SDK / `snpe-profiler` | We use QNN EP (ORT or Genie SDK), not SNPE |
| System UID daemon (`init.rc`, `android.uid.system`) | Requires root/custom ROM; we have neither |
| `/dev/adsprpc` direct access | Requires root |
| Power HAL temperature reads | Requires system UID |
| Google Play Games SDK (`gms.games`) | We use `android.app.GameManager` API 31+ |
| AIDL/Binder daemon IPC | We use HTTP at `127.0.0.1:8080` (NpuClient) |

---

## What Was Useful (Patterns to Keep)

### 1. TracerWarning Root Cause

ONNX export fails when the tracer hits dynamic Python branches that depend on
tensor values or shapes. Key offenders in Qwen3.5:

- `if position_ids.ndim == 3 and position_ids.shape[0] == 4`
- `torch.all(attention_mask == 1)` in `_update_linear_attn_mask`
- `torch_compilable_check(n_image_tokens * ... == image_features.numel())`

**Fix:** Pre-expand position_ids to 4D `[4,B,S]`, provide complete dummy inputs
with correct image tokens and mm_token_type_ids, use `use_cache=False`.

### 2. NPU Governor Pattern

Monitor DSP thermal state + per-step memory, auto-throttle or cloud-fallback:

```
Governor thread (200ms poll):
  1. Read DSP temperature (sysfs: /sys/class/thermal/thermal_zone*/temp)
  2. Read per-step tensor memory from inference loop
  3. If temp > 80C OR tensor > budget:
     - Lower Hexagon clock (performance mode LOW)
     - Set cloud_fallback = true
  4. Otherwise: restore HIGH performance mode
```

**Maps to:** CliffordService.kt (watchdog loop) + future thermal monitor.
No root needed for sysfs thermal reads on most devices.

### 3. Warm-Up Pass

Run one dummy forward pass after model load to prime Hexagon SRAM caches.
First inference is ~30% slower without this. Add to daemon startup.

### 4. Sampling Strategies (for ort_engine daemon)

All run on CPU after logits return from DSP — negligible overhead (~1ms):

- **Temperature:** `logits / tau` before softmax
- **Top-k:** Partial sort via heap, softmax over truncated set, sample
- **Top-p (nucleus):** Sort descending, cumsum until threshold, renormalize

Reference implementations (Python + Kotlin) in GPT docs — adapt for C++ daemon.

### 5. Cloud Fallback Trigger

When governor detects thermal/memory breach:
1. Serialize current prompt + generated tokens
2. Call cloud endpoint (gRPC or HTTPS)
3. Append returned tokens to buffer, continue local generation when clear

**Maps to:** Agent layer HttpFetch tool (neuromesh contract).

### 6. Circular Buffer Context Management

Fixed-size token buffer (max_seq_len) with:
- `buffer_ids[0, cur_len] = next_token_id`
- `buffer_attn[0, cur_len] = 1`
- `buffer_pos[0, cur_len] = cur_len`
- Stop when `cur_len >= max_len`

RAM per conversation: `max_len * 4 bytes` (int32) = ~8KB for 2048 tokens.

### 7. Performance Targets

| Model | Quant | Target latency | Peak RAM |
|-------|-------|----------------|----------|
| Qwen3.5-9B | W4A16 (int4 weights) | ~10-15 ms/token | ~5.5 GB |
| Qwen3.5-9B | int8 | ~12 ms/token | ~8 GB (too large) |

W4A16 is our target — fits the 5.5-7.0 GB envelope.

### 8. Game SDK Boost (Correct API for Our Target)

The GPT docs used the Play Games SDK (wrong). Correct approach for API 31+:

```kotlin
// HorizonsApplication.onCreate()
val gameManager = getSystemService(GameManager::class.java)
gameManager.setGameMode(GameMode.PERFORMANCE)
```

```xml
<uses-feature android:name="android.hardware.game" android:required="true" />
<uses-permission android:name="android.permission.HIGH_PERFORMANCE" />
```

Gives scheduler boost for UI processes. NPU still runs without it, but
GPU sub-graphs throttle to background frequency without the boost.

---

## Architecture Comparison

| GPT Doc Approach | Our Approach | Why Ours Works on Stock |
|---|---|---|
| System daemon via `init.rc` | `sh -T-` detach + oom_score_adj | No root needed |
| AIDL Binder IPC | HTTP localhost:8080 SSE-JSON | Standard protocol, no privileges |
| QPC + SNPE runtime | qnn_context_binary + ORT/Genie | Matches QAI Hub output directly |
| Power HAL governor | CliffordService FGS + sysfs thermal | Works without HAL access |
| `/dev/adsprpc` direct | QNN EP abstraction layer | Handled by runtime |

---

## Decision Matrix: Genie vs ORT

| Criterion | Genie SDK | ORT + QNN EP |
|-----------|-----------|--------------|
| Input format | `.bin` (qnn_context_binary) | `.bin` (qnn_context_binary) or `.onnx` |
| Performance | Optimized for Qualcomm, lower latency | Slightly higher overhead from EP bridge |
| Flexibility | Qualcomm models only | Any ONNX model, multiple backends |
| Maturity | Newer, less documented | Mature, well-documented |
| Our status | Not built | Scaffolded (`daemon/src/`, CI-built) |

**SUPERSEDED (2026-07-27):** this line said "ort_engine built first, Genie
explored later" — that plan flipped. Per CLAUDE.md's session-19 Standing
Decisions: GenieX on QAIRT/HTP is now the actual path; ort_engine is legacy,
still in the repo, not the way forward. Read CLAUDE.md for the current
decision, not this line.
