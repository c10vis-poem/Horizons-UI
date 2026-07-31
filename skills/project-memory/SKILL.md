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

---

## Retrieval traps in this corpus (learned 2026-07-31, the hard way)

**1. Extensionless files are invisible.** Google Drive exports arrive with no
extension. Every converter and every `*.pdf` glob misses them. 37 PDFs sat in
the vault unconverted and unchunked, including the 193k-character QAIRT HTP
manual. Fixed, but check by magic bytes, never by suffix:
```bash
find . -type f ! -name "*.*" -exec file -b {} \; | sort | uniq -c
```

**2. A capture can keep the chrome and drop the content.** The vault's GenieX
`.md` is the GitHub repo *file tree* — the README body, with the runtime table
that answers "what will GenieX load", is not in it. The 7-page PDF in Drive has
it. If a converted doc looks thin for its subject, go find the original.

**3. Markdown extraction discards every image.** Diagrams in the Qualcomm PDFs
are unreachable by text search — both embedded rasters and vector drawings.
`fitz` `page.get_images()` only finds rasters; vector diagrams need
`page.get_drawings()` and a page render. Do not conclude a diagram is absent
because grep found nothing.

**4. Read the FOLDER, not the file.** Siblings go deeper on different parts.
Numbered folders are conversation series — pulling `3.` out of a 1-9 series
gets you a ninth of the picture. Same-named files ACROSS folders are
byte-identical (verified), so depth is always in the siblings BESIDE a file.

**5. Artifact kinds are not duplicates.** `foo.md` (prose), `foo.jsonl`
(chunks), `foo-urls.md` (sources) are three different things.

**6. Source ranking, non-negotiable.** Vendor docs > repo READMEs > this
corpus's summaries. `aesop-wiki.md` asserted "NOT on the ladder: Hexagon DSP",
which is false and misled multiple sessions; the compiled `libggml-htp-v79.so`
sitting three folders away disproved it. Any summary here can carry that rot,
including this skill.

**7. `build_rag_index.py` self-poisons.** `load_chunks` walks all of
`RAG_LIBRARY` including its own `_index/meta.jsonl`, which has a different
schema. First build succeeds; every rebuild KeyErrors on `text`. Exclude
`_index` when rebuilding.

---

## Document map — go to these, in this order

Verified in session 21. **Incomplete by construction** — the operator has
material this map doesn't name yet. Absence here is not evidence of absence.
Vault paths are relative to `c10vis-poem/OBSIDIAN-Master_Wiki`.

### What we are building (read FIRST, before touching UI code)

| Doc | Answers | Trap |
|---|---|---|
| `#AESOP_HORIZONS-UI_Master/(AESOP) REPO.data_bank/(1a)-Horizons.Ui-defined/1. brainstorming and conceptualization master document. Copy of 2026-07-18.md` | **THE spec.** "Horizons Project: Architecture & Inference Build Map" — Four Rooms, DEFINE→VALIDATE→EXECUTE, Archives. Around **line 183** is the "Grandma or 5-year-old" section with the **four fuse parameters in the operator's own words**. | The Build Map text appears **twice in the same file** (~line 296 and ~line 340). Not two versions. Also: operator speech and assistant formatting alternate — the operator's paragraphs are the canon, the tidy bullet lists are a restatement. |
| `#HORIZONS-main/Horizons UI build, [copies]/2026-07-17.md` | "Open with → Horizons" routes a file to whichever slot matches its type: `.gguf`→Model, `libgeniex.so`→Backend, binary→Runtime. | Operator calls this doc a snow job on implementation claims. Architecture only. |

**The four parameters, quoted, because they get mangled every time:**
1. **The Engine** — what makes it go. `ort_engine`, `geniex`, or a custom script.
2. **The Fuel & Cargo** — assets. Sometimes zero, sometimes a list.
3. **The Road & Weight Limit** — hardware/memory. The amperage limit. Stops OOM.
4. **The Communication** — syntax AND handshake, **merged on purpose**.

Do not re-split #4. Do not drop #3. Five became four deliberately, to strip out
redundant traps and failure loops. A prior session re-expanded them and thereby
hid the missing RAM check — the exact failure class this branch was opened for.

### Runtime / backends (vendor first, always)

| Doc | Answers | Trap |
|---|---|---|
| **GenieX README** — Drive `#QAIRT_main/#GenieX/`, 7-page PDF, **pages 3 and 6** | **THE runtime answer.** One runtime, two backends: `llama_cpp` (~any HF GGUF, NPU/GPU/CPU) and `qairt` (AI Hub per-chipset bundle, NPU only). Q4_0 = "best Hexagon NPU support". Page 5 = the Android SDK path. | **The vault `.md` of this page is the repo FILE TREE ONLY.** The README body is not in it. You must read the PDF. |
| `#QAIRT_main/Qualcomm user-Guides/Qualcomm AI Engine Direct SDK | Qualcomm Developer.pdf` **page 3** | The official 4-layer stack: frameworks → runtimes → AI Engine Direct backend libraries (a **Kernels** row per column) → CPU/GPU/**HTP**/cDSP/HTA. | It's a **raster image**. Text search will never find it. Extract with `page.get_images()`. |
| `#QAIRT_main/Qualcomm user-Guides/HTP - Qualcomm AI Runtime (QAIRT) SDK.pdf` | The HTP manual, 193k chars. Graph Switching, VTCM Sharing, Graph Priority, LLM native KVcache, Execute Cancellation — i.e. the primitives for resident-small + summoned-large models. | One of **7** sections. Overview and Tensor are vault-only. Never stop at the first QAIRT file. |
| `#QAIRT_main/#QAIRT/Qualcomm AI Runtime (QAIRT) Overview...mht.md` | The hierarchy: SNPE / QNN / **GENIE** are SDKs; QAIRT API sits *beneath* them; HTP is a **backend**. GenieX descends from GENIE. | GenieX/HTP are **layers of** QAIRT, not alternatives to it. |
| `#QAIRT_main/QAIRT-SDK/GenieX/Copy of GENIEX-DAEMON-PLAN.md` | `geniex serve` on `:18181/v1`, Kotlin SDK coords, and: AI Hub ships only **smaller** Qwen variants — the **9B on `qairt` needs BYOM-compiling**. | This is where the "dormant" compile pipeline actually connects. |
| `##LLM-WIKI_OPEN-WIKI.main_/llm-wiki/libggml-hexagon.so`, `libggml-htp-v73/v75/v79/v81.so` | **The magic sauce.** Physical proof ggml reaches Hexagon. Per-DSP-arch kernels; this device is **v79**. | Binaries, not docs. No grep will surface them. |
| `#Useful_knowledge_/.../GEMINI.QUERY/A 7／04／26  QWEN 3.5 9B Q4_0.docx.md` line ~159 | `llama-cli -m …gguf --device hexagon` — the GGUF→Hexagon path as a literal command. | — |
| Drive: `c10vis-poem/llama.cpp-npu` (345 KB) | haozixu research fork. Needs **HMX-layout weight conversion**, `REPACK_FOR_HVX`, quants Q4_0/IQ4_NL/Q8_0/F16, and **recommends <4B** — cDSP is 32-bit, single NPU session. | **44 bytes in the vault.** Read the Drive copy. Also: a research prototype, not the GenieX path. |

### The wider stack (this app is one node)

| Doc | Answers |
|---|---|
| `#AESOP_HORIZONS-UI_Master/(AESOP) REPO.data_bank/Personal Agentic Operating Stack (or a Cognitive Architecture Stack)./` | **A 1–9 numbered conversation series + `aesop-wiki.md`.** `1.` defines the four memory layers as distinct roles (OB1=persistence protocol, mem0=live extraction/pruning, LLM-Wiki=conceptual graph, Reasoning Bank=execution history). `3.` = Red Agent + the recursive KAG training loop. `4.`/`7.` = runtimes per node. `8.` = Red Agent filesystem spec (`/opt/red-agent/`, fail-closed, systemd). `9.` = OmniRoute as the single gateway. |
| `#HORIZONS-main/Copy of final-memory-layer-p2p-pipeline.md` | The whole mesh in one doc: Tailscale topology, three-layer agent pipeline, the auditor's two gates, OmniRoute + Open-Wiki context slicing, WebSocket wire format. |
| `#Useful_knowledge_/###Primary research and reference documentation/OMNI.CLAW_DEFINED/Omni-claw-knowledge-synthesis-architecture.md` | How OpenWiki (maintenance discipline) / OB1 (store) / reasoning-bank (retrieval strategy) layer together. §6 = Dual-Agent Talker-Reasoner = the query/executive split. |

**Reading `4.` and `5.` of the numbered series will hand you the wrong phone.**
The device is corrected mid-conversation in `6.` (Moto Razr Ultra, not RedMagic).
The wrong hardware is still sitting in the earlier docs. This is why you read
the folder, not the file.

### Known-bad — do not cite these for facts

| Doc | Status |
|---|---|
| `aesop-wiki.md` (2 copies) | **Was wrong**: "NOT on the ladder: Hexagon DSP". Corrected in place 2026-07-31 with a retraction note. Disproved by the `.so` files above. |
| `#QAIRT_main/zqnn／qairt-System Design: half Gemini slop／ half useful info/` | Operator's own label. `3.md` has the GGUF-fork orchestrator diagram — **structurally useful**, but its code imports `com.qualcomm.qairt.runtime.QnnBackend` as a Kotlin package. That API does not exist; QAIRT ships C/C++. |
| "Gemini oils up The Builder…duel" doc; `2. 2026-07-17.md` | AI transcripts. **Architecture is canon, implementation claims are not.** The duel doc retracts its own central claim mid-document. |
| `#Useful_knowledge_/.../Fragmented QAT/` (FraQAT) | Old prior-art research. Never reach for it over the QAIRT SDK. |
| `knowledge/device-inventory/DEVICE-INVENTORY.md` | Snapshot 2026-07-13. Re-verify sizes/versions. Its dual-backend line — llama.cpp (ggml, **HTP v68–v81** + CPU + OpenCL) — is the single most-misread sentence in the corpus. |
