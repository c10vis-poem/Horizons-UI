---
name: project-memory
description: Retrieve Novus Agenti / Omni Claw project knowledge. Use when you need background the repo itself doesn't carry — architecture rationale, prior research, device inventory, QAIRT/SDK reference, tool docs, or "why was this decided." Covers the local knowledge/ corpus, the OBSIDIAN-Master_Wiki vault, and Google Drive (the actual source of truth).
---

# project-memory — where the knowledge actually lives

Three tiers. **Search them in this order**; stop when you have the answer.

## Tier 1 — this repo (`knowledge/`)

Fast, always present, no network. Markdown is the first read; **JSONL is
grep-only, never a first read.**

```
knowledge/omni-claw-defined/     what the app IS + workbench/ (how it works)
knowledge/daemon-reference/      GPT-DAEMON-REFERENCE.md, NPU-RUNTIME-PATHS.md
knowledge/qairt-sdk/             QNN/HTP reference
knowledge/device-inventory/      what's actually on the Razr Ultra (2026-07-13)
knowledge/research-npu/ proofs/ fragmented-qat/ google-dev-docs/ gemini-query/
```

## Tier 2 — the vault repo (`c10vis-poem/OBSIDIAN-Master_Wiki`)

**1,305 files · 363 markdown · 96 jsonl.** Far larger than `knowledge/`.

```
#AESOP_HORIZONS-UI_Master/(AESOP) REPO.data_bank/   WHAT THE APP IS
#AESOP_HORIZONS-UI_Master/(AESOP.]build/           HOW IT'S BUILT, per tool
#RESEARCH DOSSIER 1&2/  #Useful_knowledge_/        research + reference
RAG_LIBRARY/                                       JSONL chunks + BM25 index
yJSONL_data.bank_/                                 per-topic chunk files
```

`RAG_LIBRARY` ships a **BM25 index and a query script** — use it for retrieval
instead of grepping blind.

**Two documents there are AI chat transcripts, not specifications.** The
"Gemini duel" doc *retracts its own central technical claim* partway through;
`2. 2026-07-17.md` is one the operator calls a snow job. Architecture dictated
in them is canon; **claims of implementation are not.** Where transcript and
spec disagree, the spec wins.

## Tier 3 — Google Drive (the source of truth)

**The operator's material lives in Drive and is directly reachable** via
`mcp__Google_Drive__*` (`search_files`, `read_file_content`,
`download_file_content`). Load with ToolSearch, then call.

**Do not conclude something "doesn't exist" from the git repos alone.** The
master folder is `(MASTER_REPO).&WIKI.MD_VAULT`; the build tree beneath it has
~28 top-level folders (`##Mem0.AI`, `##GRAPHIFY`, `##OBSIDIAN-SKILLS_`,
`##Notebooklm-py`, `##OB1_`, `##OMNI-ROUTE_`, `#QAIRT_main`, `#QWEN_MODELS`,
`#GOOGLE _AGENTIC-AI`, `#REVERSE.ENG& RESEARCH_`, `#SKILLS`, `#CLAUDE_Ai`, …).

Convention: **each repo gets a rendered README PDF + a link `.txt`.** That pair
is what an agent needs — read the PDF's text with `read_file_content`.

To bring more Drive content into the vault, use the
**`drive-to-obsidian-migration`** skill. **Do not hand-sync file by file.**

## Hard-won rules

- **`git branch -a` first.** More real work sits on unmerged branches than on
  `main` in these repos. `main` on the app repo does not even contain the
  working home screen; the vault's `main` was reset to an *empty tree* while the
  complete 1,300-file migration sat unmerged on another branch.
- **Prefer the operator's own forks** (`c10vis-poem/*`) over upstream — for
  models, runtimes, and tools alike. There is a fork for nearly everything.
- Don't re-derive decisions already in `CLAUDE.md`; don't re-read files already
  read this session.
