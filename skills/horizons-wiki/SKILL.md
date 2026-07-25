---
name: horizons-wiki
description: |
  Provides the Novus Agenti / Omni Claw architecture-of-record. Load this
  skill when working on any Horizons module (on-device Android app,
  Qwen3.5-9B → Hexagon HTP compile pipeline, ort_engine/GenieX daemon,
  agent tools, Router/Monitor/Artifacts UI). Skill returns the stable
  architecture doc bundle so the agent can answer "where does X live" and
  "what was decided about Y" without re-searching the codebase. Current
  state (SOTU) is NOT part of this bundle — that's the SessionStart hook's
  job; this skill is architecture reference only.
version: 1.1.0
updated: 2026-07-25
license: project-private
tags: [horizons, android, qnn, hexagon, npu, anthropic-caching, obsidian]
---

# Horizons Wiki Skill

Canonical copy lives in `OBSIDIAN-Master_Wiki/skills/horizons-wiki/`;
mirrored to `Novus-Agenti/skills/horizons-wiki/`.

This skill packages the project's real architecture-of-record documents as
a single context bundle:

1. `CLAUDE.md` — stable architecture-of-record, tool/token authority,
   hard rules, current SOTU
2. `knowledge/daemon-reference/GPT-DAEMON-REFERENCE.md` — distilled
   daemon/architecture notes
3. `knowledge/daemon-reference/NPU-RUNTIME-PATHS.md` — runtime formats +
   SDK distribution model

There is no separate handoff file to load — CLAUDE.md's own
`## State of the Union` section is the single current-state source, kept
up to date in place rather than accumulating one file per session. The
skill is designed for the open SKILL.md standard (Claude Code, Codex,
Cursor, etc.) so the same wiki is consumable from any compliant tool.

## When to use

  - Starting any Horizons sub-agent (build-runner, code-review,
    diagnostics). Load this skill *first* before any user message so the
    cacheable prefix sits in the system block.
  - Answering questions about subsystem boundaries, file ownership,
    or design decisions captured in the wiki.
  - Any time you need to know "what was decided about X" or "where does
    component Y live in the codebase."

## How to use

The agent host should:

1. Read all three documents listed above, in the order listed.
2. Concatenate in that order (CLAUDE.md first — it's both the stable
   architecture-of-record and the current SOTU).
3. Pass as the `system` block with
   `cache_control: {type: "ephemeral", ttl: "1h"}` on the last entry.
4. Use this skill's name as a cache-key correlator in logs so cache
   hit/miss can be attributed.

## Architecture quick-reference (as of 2026-07-25)

### The Horizons Workbench — seven tiles + center-hub Router

The app is a manual, modular workbench. Core law: "Daemons stay dumb, the
user is the loader." Boots EMPTY and stable; nothing runs until the user
flips a fuse in the Router.

**Flow:** Runtime DEFINED in Terminal → LANDS in Settings → VALIDATED by
Monitor (greenLight) → ENGAGED by Router flip → supervised by CliffordService.

### Runtime decision
- **Primary:** GenieX on QAIRT/HTP SDK backend (`:18181/v1`, OpenAI-compat)
- **Legacy:** ort_engine (ORT + QNN EP, `:8080/api/v1/generate`)
- **Fallback compile pipeline:** DORMANT (see `wiki/COMPILE-PIPELINE.md`)

### Key files
| Component | Path |
|-----------|------|
| CliffordService | `horizons/fgs/CliffordService.kt` |
| NpuClient | `horizons/core/llm/NpuClient.kt` |
| DaemonLauncher | `horizons/core/shell/DaemonLauncher.kt` |
| AgentLoop | `horizons/core/agent/AgentLoop.kt` |
| ort_engine | `daemon/src/*.cpp` |
| Tile-hub architecture | `knowledge/omni-claw-defined/workbench/00-TILE-HUB-ARCHITECTURE.md` |

## What NOT to do

  - Do not edit any of these files mid-session — invalidates the cache
    and forces a 2x re-write.
  - Do not embed agent-specific task instructions in this skill —
    those go in the first user message so they stay out of the
    cached prefix.
  - Do not trust a prior session's claims about network reachability
    (e.g. HuggingFace egress) at face value — verify fresh per
    CLAUDE.md's HuggingFace Access section. Network policy is set
    per remote-session container, not fixed project-wide.

## Obsidian integration

This skill is part of the OBSIDIAN Master Wiki vault. The architecture
docs it references live in the Novus-Agenti repo; this skill defines
which docs to load and in what order for any Horizons-related work.

## Files referenced

  - `../../CLAUDE.md` (relative to Novus-Agenti/skills/horizons-wiki/)
  - `../../knowledge/daemon-reference/GPT-DAEMON-REFERENCE.md`
  - `../../knowledge/daemon-reference/NPU-RUNTIME-PATHS.md`

## Maintenance protocol

- Updated daily as part of the OBSIDIAN Master Wiki daily update protocol.
- Architecture quick-reference section is refreshed to match the current
  SOTU in CLAUDE.md.
- Changes are logged in `daily-updates/YYYY-MM-DD.md`.
