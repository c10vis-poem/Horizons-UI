# Qwen 3.5 9B — HTP Deployment Reference

> **Source caveat:** Extracted from Gemini brainstorming transcripts (July 2025).
> Content is unverified LLM output — useful as implementation guidelines,
> alternative architectural paths, and troubleshooting reference, but treat
> specific API signatures and version claims as "check before shipping."
> Original transcripts preserved in this folder for provenance.

---

## 1. Memory Envelope

| Metric | Value | Notes |
|---|---|---|
| Parameters | ~9 billion | Qwen 3.5 9B |
| FP16 weight size | ~18 GB | 2 bytes/param — won't fit on 16GB device |
| Q4_0 weight size | ~4.5 GB | 0.5 bytes/param |
| Expected GGUF file | ~5.5 GB | Includes metadata + quantization tables |
| Runtime footprint | ~5.8 GB | Weights + KV cache + activation buffers |
| Device RAM | 16 GB | Motorola Razr Ultra 2025 (SM8750) |

**Key constraint:** Pure FP16 is not viable on mobile — must quantize.
The HTP achieves peak efficiency with mixed precision: INT4/INT8
quantized weights + FP16 activations.

---

## 2. Why Q4_0 on Hexagon v79

1. **Hardware-matched ops:** The v79 HTP assembly instruction compiler is
   optimized for straight `MUL_MAT` operations on linear data types
   (Q4_0, Q8_0, MXFP4).
2. **No lookup table support for IQ/NL quants:** IQ4_NL works on desktop
   GPUs but the Hexagon backend's custom C++/assembly drivers don't
   implement the non-linear lookup layers. Attempting to run a Q4_NL
   model will either throw an unhandled tensor layout error or fall back
   to CPU.
3. **Thermal behavior:** Native Q4_0 on v79 tensor tiles runs at full
   speed with minimal power draw — no throttling under sustained load.

**Standing decision:** Q4_0 GGUF via GenieX's GGML backend is the
primary inference path. See `compile/manifest.yaml` and CLAUDE.md's
Standing Decisions.

---

## 3. Precision / Quantization Configuration

For QAIRT SDK / QNN toolchain compilation (the fallback pipeline):

### QNN graph conversion

```bash
qnn-pytorch-converter \
    -i /path/to/qwen3.5-9b \
    -d input_ids 1x1 \
    --input_type integer \
    -o qwen3.5_htp.json
```

### QNN mixed-precision quantization

```bash
qnn-model-quantizer \
    --input_network qwen3.5_htp.json \
    --output_precision htp_mixed_fp16_int4
```

### Quantization config (for LiteRT / ai-edge-litert path)

```json
{
    "quant_mode": "WEIGHT_ONLY_INT4",
    "activation_precision": "INT8",
    "kv_cache_precision": "INT8"
}
```

**Note:** The QNN converter/quantizer commands above are from the QAIRT
SDK CLI — verify exact flags against the SDK version installed on-device
(see `knowledge/device-inventory/DEVICE-INVENTORY.md` for what's there).

---

## 4. Three Deployment Paths

### Path A: GenieX / QNN Runtime (PRIMARY — project's chosen path)

Qualcomm's own runtime. The GenieX CLI (`geniex serve`) exposes an
OpenAI-compatible HTTP endpoint on `:18181/v1`.

- Fork: `c10vis-poem/GenieX` (already forked July 14)
- Prebuilt binary on-device: `geniex-bench-android-arm64 v0.3.14`
  (GGML + QAIRT backends) in `/storage/emulated/0/Download/`
- Wire-in work: EXECUTIONS.md P1.3 / P2.1
- Detail: `wiki/GENIEX-DAEMON-PLAN.md`

### Path B: LiteRT AOT → QNN Delegate (alternative for Gemma models)

Google's path — best suited for Gemma model family, less tested with Qwen.

```bash
litert_compile \
    --model_file=model.tflite \
    --output_file=compiled_htp.bin \
    --delegate=QNN \
    --qnn_backend=HTP
```

HTP precision flag in the delegate options:
```
TfLiteQnnDelegateHtpBackendOptions options;
options.precision = TfLiteQnnDelegateHtpPrecision.kHtpFp16;
```

This ensures FP16 internal arithmetic even with compressed weights.

### Path C: llama.cpp Hexagon Backend (community / Termux path)

Open-source, runs in Termux. See `llama-cpp-hexagon-jni.md` for the full
build guide.

```bash
./llama-cli -m qwen3.5-9b-q4_0.gguf --device hexagon -p "prompt"
```

Requires proprietary Qualcomm libraries (`libhtp_ops.so`,
`libhtp_ops_skel.so`) — extractable from device firmware or the
Qualcomm AI Stack SDK.

---

## 5. Operational Settings for Mobile

| Setting | Recommended | Why |
|---|---|---|
| Context window | 4096–8192 tokens max | KV cache grows linearly; >8k risks OOM on 16GB |
| Flash Attention | ON | Critical for bandwidth-bound mobile inference |
| CPU threads | 4 | Matches SM8750 performance core count; handles prompt ingestion while NPU does generation |
| HTP instances | NDEV=2 (HTP0, HTP1) | Slices execution across v79 compute lanes for large models |

### Environment variables for Termux/CLI execution

```bash
export LD_LIBRARY_PATH=/data/local/tmp/llama.cpp
export DSP_LIBRARY_PATH=/data/local/tmp/llama.cpp
```

---

## 6. Memory Optimization — Zero-Copy on v79

On Hexagon v79, `ion/dma-buf` memory allocations allow mapping model
weights directly into the HTP's virtual address space without an
intermediate CPU buffer copy.

At ~4.5 GB weights + ~1.3 GB runtime overhead = ~5.8 GB total, the model
fits within the Gen 4 silicon's fast-access system cache segments,
minimizing trips to physical LPDDR5X and improving tok/s.

**Relevance:** This is why the detached-daemon architecture (model loaded
once into a long-lived process with a persistent memory map) is
preferable to in-process JNI (where the Android lifecycle can force
unloads).

---

## 7. External References

These links appeared in the original Gemini transcripts. Verify before
relying on them — URLs may have moved or content may have changed.

- [QNN SDK workflow docs](https://docs.qualcomm.com/bundle/publicresource/topics/80-62010-1/qnn-workflow.html)
- [GenieX repo](https://github.com/qualcomm/GenieX)
- [LiteRT NPU compilation guide](https://github.com/google-ai-edge/litert-samples/blob/main/compiled_model_api/qualcomm/llm_chatbot_npu/NPU_COMPILATION_GUIDE.md)
- [Qualcomm QIDK deployment scripts](https://github.com/qualcomm/qidk)
- [QNN model run/verify guide](https://docs.qualcomm.com/doc/80-70014-15B/topic/qnn-run-model.html)
- [Google blog: LiteRT + Qualcomm NPU](https://developers.googleblog.com/unlocking-peak-performance-on-qualcomm-npu-with-litert/)
- [LiteRT Qualcomm integration docs](https://developers.google.com/edge/litert/next/qualcomm)
- [llama.cpp Hexagon NPU fork](https://github.com/haozixu/llama.cpp-npu)

---

## Open Questions / Future Additions

<!-- Add verified findings, benchmark results, or corrected commands below -->
