# GenieX Unified Orchestration — Dual-Agent Model Routing

## Overview

GenieX (part of QAIRT SDK v2.48.0.260626) coordinates two agent models under a single C SDK, preventing Hexagon HTP context-switch thrashing that would occur if running separate `llama.cpp` and `QAIRT` processes simultaneously.

## The Two Agents

### Agent 1: Qwen 3.5 2B (Router/Gatekeeper)
- **Runtime:** llama.cpp plugin (GGUF wrapper)
- **State:** Always-hot in RAM (~1.6GB)
- **Latency:** <100ms response time
- **Role:** Intercept queries, query MEMO layer, decide: immediate response OR handoff

### Agent 2: Qwen 3.5 9B Q4_0 (Deep Thinker)
- **Runtime:** llama.cpp plugin (GGUF wrapper)
- **State:** On-demand, auto-purge after response
- **Latency:** 1-5 second response time (deep work)
- **Role:** Heavy reasoning, synthesis, complex timeline analysis

## Model Loading Configuration

GenieX loads BOTH models into a unified context:

```yaml
# ~/.geniex/config.yaml
engines:
  qairt:
    models:
      - path: ~/models/qwen-2b-compiled/qwen-2b.gguf
        alias: "agent-1-router"
        runtime: llama_cpp
        context_size: 1024
        n_gpu_layers: 999
        threads: -1
        always_loaded: true  # stays hot

      - path: ~/models/qwen-9b-q4_0/qwen-9b-q4_0.gguf
        alias: "agent-2-thinker"
        runtime: llama_cpp
        context_size: 2048
        n_gpu_layers: 999
        threads: -1
        on_demand_load: true
        auto_unload_after_response: true

scheduling:
  isolation: "none"  # GenieX handles coordination
  tensor_mode: "shared"  # Single HTP access point
  context_switch_delay_ms: 50
  max_concurrent_models: 1  # HTP processes one model at a time
```

## Handoff Logic (Agent 1 → Agent 2)

### When Agent 1 Decides to Handoff

Agent 1 analyzes the incoming query:

```python
# Pseudo-logic in Agent 1's system prompt
if query_requires_deep_reasoning():
    # Write structured handoff to file
    write_handoff_signal(_handoff.md, {
        "query": user_query,
        "memory_context": lookup_memo(),
        "token_budget": 2048,
        "reasoning_type": "timeline_synthesis|code_gen|novel_problem"
    })
    # Signal Agent 2
    trigger_agent_2_wakeup()
    return "Thinking deeply..."
else:
    # Answer immediately
    return immediate_response
```

### Agent 2 Wake Trigger

Background script monitors `~/.openwiki/_handoff.md`:

```bash
#!/bin/bash
while true; do
    if [ -s ~/.openwiki/_handoff.md ]; then
        # Load Agent 2 with handoff payload
        payload=$(cat ~/.openwiki/_handoff.md)
        
        # Call Agent 2 via GenieX with deep reasoning params
        curl -s http://localhost:8080/v1/messages \
            -H "Content-Type: application/json" \
            -d "{
                \"model\": \"agent-2-thinker\",
                \"max_tokens\": $(jq -r '.token_budget' ~/.openwiki/_handoff.md),
                \"messages\": [{
                    \"role\": \"user\",
                    \"content\": $(echo "$payload" | jq -c '.memory_snapshot | @json')
                }]
            }" > ~/.openwiki/_result.md
        
        # Clean up signal files
        rm ~/.openwiki/_handoff.md
        
        # Force cache drop (free 5.74GB)
        sync && echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || true
    fi
    
    sleep 1
done
```

## MEMO Layer Integration

Agent 1 queries MEMO on every invocation:

```bash
# Agent 1 lookup pattern
jq '.[] | select(.timestamp > (now - 604800))' \
    ~/openwiki/MEMO/users/me/timeline.json
```

Returns last 7 days of context. If MEMO hit answers the query, Agent 1 responds immediately. If MEMO is silent or query is novel, handoff to Agent 2.

## NPU Context Management

**Critical:** Hexagon HTP is a shared resource with NO asymmetric multitasking.

- **GenieX serializes** all tensor operations
- When Agent 1 is running, Agent 2 cannot access HTP (and vice versa)
- On handoff, Agent 1 releases HTP → Agent 2 acquires HTP
- After response, Agent 2 drops context cache → frees HTP + RAM

**Memory Budget:**
- Agent 1 always-hot: 1.6GB
- Agent 2 on-demand: 5.74GB
- Total: 7.44GB (within 9GB ceiling)
- Device reboots daily: keep swaps small, purges aggressive

## Fallback Strategy

If dual-agent unavailable:
1. Use Gemma 4 12B IT (QAT) as single-agent
2. Loses routing efficiency, but operational
3. Good for shell mode / WebUI mode during development

## Testing the Orchestration

```bash
# Terminal 1: Start GenieX with both models
geniex-server --config ~/.geniex/config.yaml --port 8080

# Terminal 2: Monitor handoff signals
tail -f ~/.openwiki/_handoff.md

# Terminal 3: Ask Agent 1 a question
echo "What did I do on 2026-07-20?" > ~/.openwiki/_input.md

# Terminal 4: Watch result come back
tail -f ~/.openwiki/_result.md
```

Expected flow: query → Agent 1 MEMO lookup → Agent 2 handoff → synthesis → result file updated.

## Tuning Parameters

| Parameter | Value | Impact |
|-----------|-------|--------|
| `context_size` (Agent 1) | 1024 | Faster, less context reuse |
| `context_size` (Agent 2) | 2048 | More context window for synthesis |
| `auto_unload_delay` | 100ms | How quickly Agent 2 purges after response |
| `tensor_mode` | "shared" | Single HTP access point (do NOT change) |
| `n_gpu_layers` | 999 | All layers to HTP (optimal for Snapdragon) |

If device memory drops below 6GB: reduce Agent 2 context to 1024, reduce max_tokens to 512.
