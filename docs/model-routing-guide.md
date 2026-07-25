# Model Routing & Selection Guide

## Quick Decision Tree

```
User query arrives
    ↓
Agent 1 (Qwen 2B) receives query
    ↓
Agent 1 checks MEMO (last 7 days)
    ├─→ MEMO has answer? → Respond immediately
    ├─→ Simple fact lookup? → Respond immediately
    ├─→ JSON formatting needed? → Respond immediately
    └─→ Deep reasoning needed?
         ↓
         Write _handoff.md
         ↓
         Agent 2 (Qwen 9B) wakes
         ↓
         Synthesize with deep context
         ↓
         Write _result.md
         ↓
         Drop cache, free 5.74GB
```

## Agent Selection Matrix

| Query Type | Agent 1 (2B) | Agent 2 (9B) | Notes |
|-----------|--------------|--------------|-------|
| "What time is it?" | ✓ Immediate | ✗ | Trivial, no reasoning needed |
| "What did I do on 2026-07-20?" | MEMO lookup | ✓ If novel | Check MEMO first; 2B answers if recent |
| "Write a 50-line function" | ✗ | ✓ Handoff | Complex code needs 9B |
| "Summarize last week's events" | ✗ | ✓ Handoff | Timeline synthesis = deep reasoning |
| "Is X a word?" | ✓ Immediate | ✗ | Factual, no synthesis |
| "How do I refactor this pattern?" | ✗ | ✓ Handoff | Requires architectural thinking |
| "Set a timer for 5 min" | ✓ Immediate | ✗ | Action, no reasoning |
| "Cross-reference event X with Y" | ✗ | ✓ Handoff | Multi-source synthesis |

## Complexity Heuristics

**Agent 1 can handle:**
- Questions answerable from MEMO + common sense
- JSON formatting / data reshaping
- Simple routing / delegation
- Fact lookups
- Direct instructions (timers, app controls)

**Handoff to Agent 2:**
- "Compare X and Y" (multi-source analysis)
- "What happened between dates A and B?" (complex timeline)
- "Why did Z fail?" (root-cause synthesis)
- "Generate new code" (creative synthesis)
- "Teach me X" (structured knowledge building)

## Agent 1 System Prompt (Pseudo-Code)

```
You are the Qwen 3.5 2B router. Your job: answer quickly or delegate.

ALWAYS check the MEMO layer first:
jq '.[] | select(.timestamp > (now - 604800))' ~/openwiki/MEMO/users/me/timeline.json

If you can answer from MEMO + context: respond immediately with JSON.

If you CANNOT answer:
1. Write _handoff.md with the query, memory snapshot, and reasoning
2. Signal Agent 2: echo "HANDOFF" > /tmp/agent2-wakeup
3. Wait for _result.md to appear (or timeout after 5 seconds)
4. Return the result to user

Do NOT overthink. Delegate fast.
```

## Agent 2 System Prompt (Pseudo-Code)

```
You are the Qwen 3.5 9B deep thinker. Your job: reason deeply, then exit.

You receive a payload in _handoff.md:
{
  "query": "user's original question",
  "memory_snapshot": {
    "recent_events": [...],
    "user_preferences": {...},
    "temporal_context": "2026-07-25 12:30:00"
  },
  "token_budget": 2048,
  "reasoning_type": "timeline_synthesis|code_gen|novel_problem"
}

Process the handoff payload with full reasoning depth. Output your response to _result.md.

After response is written:
- Close all file handles
- Drop all context cache
- Signal ready for next handoff

Do NOT remain loaded. Purge aggressively.
```

## Fallback Strategy

If dual-agent system unavailable (e.g., during boot, WebSocket bridge down):

1. **Gemma 4 12B IT** (single-agent mode)
   - Slower routing (no Agent 1 filter)
   - Same functionality, higher latency
   - Useful for testing / shell mode

2. **Gemma 4 E4B** (if 12B fails)
   - Lighter weight, slower reasoning
   - Better than nothing

3. **Qwen 0.8B** (emergency only)
   - Very fast, very dumb
   - Factual lookups only
   - Use only if out of RAM

## Memory Impact

**Always-hot (Agent 1):**
- 1.6GB RAM used constantly
- No performance penalty for queries
- Device can handle this at 6GB ceiling

**On-demand (Agent 2):**
- 5.74GB loaded only when handoff triggered
- Auto-purge after response (1-2 seconds)
- Requires active cache-drop script

**Critical:** If device memory drops below 6GB during Agent 2 operation:
- Handoff script fails to load Agent 2
- System falls back to Agent 1 + offline MEMO lookups
- No crash, but degraded (expected under memory pressure)

## Testing Your Routing

```bash
# Test 1: Simple query (Agent 1 should answer)
echo "What is 2+2?" | nc localhost 8080

# Test 2: MEMO lookup (Agent 1 checks cache)
echo "Remind me what happened on 2026-07-22" | nc localhost 8080

# Test 3: Deep reasoning (should trigger handoff)
echo "Synthesize my entire week and suggest improvements" | nc localhost 8080

# Test 4: Code generation (definitely Agent 2)
echo "Write a Python script that fetches and caches model weights" | nc localhost 8080
```

Watch for handoff signals:
```bash
# Terminal 2:
tail -f ~/.openwiki/_handoff.md
tail -f ~/.openwiki/_result.md
```

If `_handoff.md` appears but `_result.md` doesn't: Agent 2 handoff failed. Check device memory + logs.
