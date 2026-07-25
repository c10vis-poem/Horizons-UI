---
name: project-memory
description: |
  The project's actual memory layer: the hand-distilled knowledge/ corpus
  (project definition, NPU/daemon research, QAIRT reference, proofs, etc.),
  not a shortcut to re-read CLAUDE.md. Use when a task needs grounding in
  accumulated project knowledge — architecture rationale, prior research
  findings, SDK reference detail — beyond what the current session already
  has in context.
version: 1.1.0
updated: 2026-07-25
tags: [memory, knowledge-retrieval, novus-agenti, obsidian]
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Project memory — the knowledge/ corpus as a real memory skill

Canonical copy lives in `OBSIDIAN-Master_Wiki/skills/project-memory/`;
mirrored to `Novus-Agenti/skills/project-memory/`.

CLAUDE.md's `## State of the Union` section is current state; that's
already surfaced by the SessionStart hook at the top of every session, so
this skill does not repeat it. This skill is specifically about
`knowledge/` — the ~18-hour hand-distilled corpus that exists precisely so
it can be ingested as memory. A skill named "project-memory" that doesn't
touch that corpus isn't project memory, it's a doc-loading shortcut with
the wrong name. This is the fix for that.

## Two-tier model

1. **Always read:** `knowledge/omni-claw-defined/` in full. Every session
   needs "what is Novus Agenti / Omni Claw, what are we actually building"
   grounded from the source, not inferred from scattered mentions.
2. **Retrieve on demand, don't preload:** everything else in `knowledge/`
   (`research-npu/`, `proofs/`, `fragmented-qat/`, `google-dev-docs/`,
   `gemini-query/`, `qairt-sdk/`, `daemon-reference/`,
   `claude-code-reference/`, `device-inventory/`). Use `Grep`/`Glob`
   against the relevant topic's `.jsonl` for what the current task actually
   touches — e.g. a QAIRT backend-config question greps
   `knowledge/qairt-sdk/htp.jsonl` for the relevant section, it doesn't
   load the whole 6000-line source. Loading a whole topic wholesale
   defeats the reason the corpus is chunked into JSONL in the first place.

## Knowledge corpus map (as of 2026-07-25)

| Folder | Content | First-read? |
|--------|---------|-------------|
| `omni-claw-defined/` | Core project definition + workbench architecture | YES |
| `omni-claw-defined/workbench/` | Tile-hub architecture + 3 workbench docs | YES |
| `daemon-reference/` | GPT-DAEMON-REFERENCE.md + NPU-RUNTIME-PATHS.md | YES |
| `research-npu/` | NPU research distillations | On-demand |
| `proofs/` | Architecture proofs | On-demand |
| `fragmented-qat/` | Quantization research | On-demand |
| `google-dev-docs/` | Google dev documentation notes | On-demand |
| `gemini-query/` | Gemini query research | On-demand |
| `qairt-sdk/` | QNN HTP manual (.md + .jsonl) | On-demand |
| `claude-code-reference/` | Claude Code knowledge (PROMPT-CACHING.md) | On-demand |
| `device-inventory/` | On-device audit snapshot (2026-07-13) | On-demand |

## How to retrieve

```
Grep(pattern="<keyword from the task>", path="knowledge/<topic>/", glob="*.jsonl")
```

Then `Read` just the matched entries' surrounding context if the JSONL
snippet alone isn't enough. Don't `Read` a full `.md` companion unless the
JSONL retrieval genuinely didn't surface what's needed.

## What this skill is NOT for

- Reloading CLAUDE.md's SOTU or session state — that's the SessionStart
  hook's job, already done before this skill would ever be invoked.
- A narrow single-file fix that doesn't touch project history or SDK
  reference material — just do the fix, no skill needed.

## Obsidian integration

This skill is part of the OBSIDIAN Master Wiki vault. On-device, the
knowledge corpus can be browsed via Obsidian or Markor at:
```
/storage/emulated/0/Documents/OBSIDIAN-Master_Wiki/
```
The Novus-Agenti repo's `knowledge/` folder is the authoritative source
for the corpus content itself; this skill defines HOW to retrieve from it.

## Maintenance protocol

- New Drive-sourced distillations land as a new `knowledge/<topic>/`
  folder (see `knowledge/README.md`'s Drive-mirror rule) — this skill
  doesn't need editing when that happens, the retrieval pattern above
  already covers any new topic folder.
- If a topic folder is missing its `.jsonl` companion (markdown-only),
  retrieval degrades to `Grep` over the `.md` directly — still usable,
  just less structured. Flag it rather than silently working around it.
- This skill is updated daily as part of the OBSIDIAN Master Wiki daily
  update protocol. Changes are logged in `daily-updates/YYYY-MM-DD.md`.
