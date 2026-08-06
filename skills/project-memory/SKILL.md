---
name: project-memory
description: Retrieve Horizons UI / Novus-Agenti / NovA-Claw project knowledge. Use when you need background the repo doesn't carry — architecture rationale, prior research, device inventory, QAIRT/SDK reference, tool docs, or "why was this decided." Covers the local knowledge/ corpus, the OBSIDIAN-Master_Wiki vault, and Google Drive (where the operator's material actually lives).
---

# project-memory — where the knowledge actually lives

## Read these first, before searching anything

1. **`CLAUDE.md`** — especially "State of the code".
2. **`wiki/ROUTER-MONITOR-TERMINAL-SPEC.md`** — ADOPTED, operator-dictated. The
   tile design. Supersedes every earlier "fuse box / gate / breaker" description
   on conflict.

Most retrieval failures in this project were not retrieval failures. They were
agents reading a *description* of an unbuilt feature and citing it as existing.

## The one rule that matters

**Nothing in the corpus is canon unless the operator adopted it.** He is roughly
one third through curating source material. Documents are **candidate input**,
not specification — including the large "hybridized agentic neuro mesh" PDF,
which is a reference sweep he pulls sections from.

Folder and file names lie about this. `knowledge/omni-claw-defined/` says
"defined". `FEATURE-SPEC.md` says "Canonical". `CLAUDE.md` used to call itself
"architecture-of-record". All of it was mid-thought. Reading "defined" on a
folder and believing it is how a metaphor became a state machine and a protocol
became a storage tier.

When citing a document, say which it is:

- **adopted** — the operator said it directly. Rare.
- **candidate** — in the pile, not yet selected. Most things.
- **superseded** — overtaken; say by what.
- **transcript** — an AI chat log. Architecture dictated *by the operator* inside
  one is adoptable; the AI's implementation claims never are.

## Tier 1 — this repo (`knowledge/`)

Fast, always present, no network. Markdown first; **JSONL is grep-only, never a
first read.**

```
knowledge/omni-claw-defined/     core definition + workbench/ (name is stale, content is not)
knowledge/daemon-reference/      GPT-DAEMON-REFERENCE.md, NPU-RUNTIME-PATHS.md
knowledge/qairt-sdk/             QNN/HTP reference
knowledge/device-inventory/      Razr Ultra snapshot, 2026-07-13
knowledge/research-npu/ proofs/ fragmented-qat/ google-dev-docs/ gemini-query/
```

`knowledge/omni-claw-defined/workbench/01-fusebox-design-conversation.md`
**contradicts itself.** It contains the operator rejecting the hardened
gatekeeper *and*, further down, a formal-looking build map that reinstates it.
The formal section is the one every prior session believed. The rejection is the
operator's. Trust the rejection.

## Tier 2 — the vault repo (`c10vis-poem/OBSIDIAN-Master_Wiki`)

**~1,305 files · 363 markdown · 96 jsonl.** Much larger than `knowledge/`.

```
#AESOP_HORIZONS-UI_Master/(AESOP) REPO.data_bank/   what the app is
#AESOP_HORIZONS-UI_Master/(AESOP.]build/           how it's built, per tool
#RESEARCH DOSSIER 1&2/  #Useful_knowledge_/        research + reference
RAG_LIBRARY/                                       JSONL chunks + BM25 index
yJSONL_data.bank_/                                 per-topic chunk files
```

`RAG_LIBRARY` ships a **BM25 index and a query script** — use it rather than
grepping blind. Expect heavy duplication: `fraqat-paper.jsonl` appears 4×,
`automated-build-android-github-actions.jsonl` 3×, several others twice.

## Tier 3 — Google Drive (where the operator actually works)

Reachable via `mcp__Google_Drive__*` (`search_files`, `read_file_content`,
`get_file_metadata`). Load with ToolSearch, then call.

**`-MASTER-BUILD-[HOW-TO]_DataBank` is the active corpus** — being curated now,
and Priority 1 of the operator's own build order. Subfolders: `-Builders_Guide`
(how to build skills, structure a wiki, set up GitHub, and the four storage
formats: `.md` / `.jsonl` / `.pdf` / `skills.md`), `HORIZONS_UI_`, `AESOP_XI_`,
`-Pending_Corpora`, `-.ReadMe*`. The older `(MASTER_REPO).&WIKI.MD_VAULT` is what
he cherry-picks *from*.

**Do not conclude something "doesn't exist" from the git repos alone.**

### Drive tool limits, measured 2026-08-04

- `read_file_content` handles Docs, PDFs, and **images** — it OCRs screenshots
  well, which is often the fastest way to read something. It returns an **empty
  string** on very large or damaged PDFs.
- `download_file_content` has a hard **10 MB cap** and returns base64 into
  context — useless for anything binary and large.
- Google's download hosts (`drive.google.com`, `drive.usercontent.google.com`)
  are **blocked by network policy**. `www.googleapis.com` is reachable but needs
  a Drive-scoped token this environment does not have.
- A 100 MB+ PDF returning nothing is often **truncated**, not merely large.
  Running it through a PDF compressor re-renders it and usually fixes it — that
  is how the neuro-mesh PDF was eventually read.
- Oversized tool results spill to a file on disk rather than into context;
  decode them there with `python3`/`jq` and read in slices.

To bring more Drive content into the vault use the
**`drive-to-obsidian-migration`** skill. Do not hand-sync file by file.

## Other reachable sources

- **HuggingFace** (`mcp__Hugging_Face__*`) — the operator is `Mer0vin8ian`.
  `hf_fs ls hf://models/Mer0vin8ian` lists his copies; `hf://buckets/Mer0vin8ian`
  his buckets.
- **GitHub** — `mcp__Claude_Code_Remote__list_repos` is the sanctioned way to
  discover his ~60 repos; `add_repo` attaches one.

**Prefer his forks over upstream** — models, runtimes, tools alike. But **check
the artifact format matches the consumer.** His `moonshine-streaming-small-onnx`
is an Optimum export (`encoder_model.onnx` + `tokenizer.json`) and cannot load in
sherpa-onnx, which needs `preprocess/encode/uncached_decode/cached_decode` plus
`tokens.txt`; `moonshine-tflite` is TFLite. Right model, wrong artifact.

## Hard-won rules

- **`git branch -a` first.** More real work sits on unmerged branches than on
  `main`. The vault's `main` was once reset to an empty tree while the complete
  1,300-file migration sat unmerged elsewhere.
  *(Historical note: `main` on the app repo used to lack the working home screen.
  No longer true — `HomeGrid.kt` is blob `618cf4b6` on `main`, on the frozen
  branches, and on the working branch. Verified 2026-08-04.)*
- **Cite file and line.** "The docs say" is how unbuilt features become assumed
  features.
- **A document that self-describes as finished may not be.** The Four Rooms spec
  carries `[NEEDS EDIT]` in its own heading, announces five checks, and lists
  three — two of them empty.
- **Files that exist but appear in no index are invisible.** `HOME-REDESIGN-SPEC.md`
  and its image assets sat unfound for weeks for exactly this reason. When adding
  a document, add it to the file map in `CLAUDE.md` in the same commit.
- Don't re-derive decisions already in `CLAUDE.md`; don't re-read files already
  read this session.
