# llama.cpp Hexagon Backend — NDK Cross-Compile & JNI Bridge

> **Source caveat:** Extracted from Gemini brainstorming transcripts (July 2025).
> Content is unverified LLM output — useful as implementation guidelines,
> alternative architectural paths, and troubleshooting reference, but treat
> specific API signatures and version claims as "check before shipping."
> Original transcripts preserved in this folder for provenance.

---

## Status in This Project

The Horizons app uses a **detached daemon architecture** (daemon binary
communicates over HTTP, not in-process JNI). The primary path is GenieX
(`geniex serve` on `:18181/v1`); the legacy path is `ort_engine` on
`:8080`.

This document covers the **alternative JNI approach** — embedding
llama.cpp directly inside the Android app via NDK. It's preserved as
reference for:
- Troubleshooting the Hexagon backend behavior
- Understanding what the community llama.cpp-npu fork does under the hood
- A possible fallback if the detached daemon approach hits blockers
- Termux-side CLI execution (no JNI needed, just the compiled binary)

---

## 1. Proprietary Driver Libraries

The Hexagon HTP backend requires two Qualcomm libraries that bridge
userspace to the NPU hardware via FastRPC:

| Library | Purpose |
|---|---|
| `libhtp_ops.so` | HTP operator implementations (CPU-side stub) |
| `libhtp_ops_skel.so` | HTP operator skeletons (DSP-side, loaded onto Hexagon) |

### How to obtain

- Extract from a Snapdragon 8 Elite device firmware dump
- Download via the [Qualcomm AI Stack SDK](https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai)
- May already be on-device — check `/vendor/lib64/` and
  `/data/local/tmp/` (see `knowledge/device-inventory/DEVICE-INVENTORY.md`)

---

## 2. NDK Cross-Compilation

### Standard llama.cpp (CPU only — baseline)

```bash
mkdir build-android && cd build-android
cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
      -DANDROID_ABI=arm64-v8a \
      -DANDROID_PLATFORM=android-34 \
      ..
make -j4
```

### With Hexagon/Snapdragon NPU backend

```bash
mkdir build-android && cd build-android
cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
      -DANDROID_ABI=arm64-v8a \
      -DANDROID_PLATFORM=android-34 \
      -DLLAMA_SNAPDRAGON=ON \
      -DLLAMA_SNAPDRAGON_HTP=ON \
      ..
make -j4
```

**Note:** The `-DLLAMA_SNAPDRAGON=ON` flag may be named differently
depending on the llama.cpp fork/version. The `haozixu/llama.cpp-npu`
fork and the upstream `ggml-hexagon` backend may use different CMake
variable names. Check the fork's CMakeLists.txt before building.

### Output

Place into `src/main/jniLibs/arm64-v8a/`:
- `libllama.so` (compiled llama.cpp)
- `libhtp_ops.so` (Qualcomm driver)
- `libhtp_ops_skel.so` (Qualcomm driver)

---

## 3. JNI Bridge (Alternative to HTTP Daemon)

If embedding llama.cpp in-process rather than using a detached daemon:

### C++ side (`llama-jni.cpp`)

```cpp
#include <jni.h>
#include <string>
#include "llama.h"

extern "C"
JNIEXPORT jlong JNICALL
Java_com_horizons_engine_LlamaEngine_loadModelNative(
        JNIEnv *env, jobject thiz, jstring model_path) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);

    llama_model_params model_params = llama_model_default_params();

    // Steer to Hexagon NPU — verify this enum exists in your
    // llama.cpp version before using
    model_params.devices[0] = {
        .type = LLAMA_DEVICE_TYPE_HEXAGON,
        .index = 0
    };

    llama_model *model = llama_load_model_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    return reinterpret_cast<jlong>(model);
}
```

**Caveat:** `LLAMA_DEVICE_TYPE_HEXAGON` appeared in the Gemini transcript
as a real enum. It's consistent with llama.cpp's naming conventions
(`LLAMA_DEVICE_TYPE_*`) but may only exist in specific forks (e.g.
`haozixu/llama.cpp-npu`) or under a different name upstream. Verify
against the actual header before compiling.

### Kotlin side

```kotlin
class LlamaEngine {
    companion object {
        init {
            System.loadLibrary("htp_ops")
            System.loadLibrary("htp_ops_skel")
            System.loadLibrary("llama-jni")
        }
    }

    private var modelPointer: Long = 0

    private external fun loadModelNative(modelPath: String): Long
    private external fun generateTokensNative(
        pointer: Long, prompt: String, maxTokens: Int
    ): String

    fun initializeModel(path: String) {
        modelPointer = loadModelNative(path)
        if (modelPointer == 0L) {
            Log.e("LlamaEngine", "Failed to bind model to Hexagon NPU")
        }
    }

    fun generate(prompt: String): String {
        if (modelPointer == 0L) return "Engine not initialized"
        return generateTokensNative(modelPointer, prompt, maxTokens = 512)
    }
}
```

### Why Horizons uses a daemon instead

- Android lifecycle can force-kill activities/services under memory
  pressure — an in-process model load (~5.8 GB) is vulnerable to OOM kills
- The detached daemon (`ort_engine`, future `geniex serve`) runs as a
  separate process, keeping the model memory-mapped independently of the
  app's lifecycle
- HTTP wire contract (`POST /api/v1/generate`) decouples the app from any
  specific inference backend — swap the daemon binary, keep the same client

---

## 4. Termux CLI Execution (No JNI)

For running inference directly in Termux without the Android app layer:

### Environment setup

```bash
export LD_LIBRARY_PATH=/data/local/tmp/llama.cpp
export DSP_LIBRARY_PATH=/data/local/tmp/llama.cpp
```

### Run inference

```bash
./llama-cli \
    -m qwen3.5-9b-q4_0.gguf \
    --device hexagon \
    -p "Your prompt here"
```

### Multi-lane execution for large models

On v79, register multiple virtual HTP instances to slice across compute
lanes:

```bash
NDEV=2
D=HTP0,HTP1
```

**Note:** These environment variables are from the Gemini transcript.
Verify against the actual llama.cpp Hexagon backend docs or
`haozixu/llama.cpp-npu`'s README for correct syntax.

---

## 5. Manifest Requirements (if using in-process JNI)

```xml
<application
    android:largeHeap="true"
    android:hardwareAccelerated="true">
    ...
</application>
```

`largeHeap="true"` is essential — a 5.5 GB model file will exceed the
default Dalvik heap limit. The Horizons app already has this set.

---

## 6. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `library not found` crash at startup | Qualcomm `.so` files not in `jniLibs/arm64-v8a/` | Copy `libhtp_ops.so` + `libhtp_ops_skel.so` alongside `libllama.so` |
| Falls back to CPU (slow inference) | Hexagon backend not compiled in, or wrong quant format | Rebuild with `-DLLAMA_SNAPDRAGON=ON`; use Q4_0 not IQ4_NL |
| OOM kill during model load | KV cache too large | Cap context to 4096–8192 tokens; enable Flash Attention |
| `unhandled tensor layout` error | Non-linear quant format (IQ4_NL) on Hexagon | Re-quantize to Q4_0 — v79 only supports linear quant types |
| Thermal throttling | Sustained full-speed inference | Q4_0 should be thermally safe on v79; if throttling, check other background processes |

---

## 7. Related Resources

- [haozixu/llama.cpp-npu](https://github.com/haozixu/llama.cpp-npu) — Hexagon NPU fork
- [ggml-hexagon backend](https://github.com/jeffzhou2000/ggml-hexagon) — alternative approach
- [chraac/llama-cpp-qnn-builder](https://github.com/chraac/llama-cpp-qnn-builder) — QNN-based builder
- [SmolChat Android](https://github.com/shubham0204/SmolChat-Android) — reference GGUF Android client
- [Qualcomm AI Stack SDK](https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai)

---

## Open Questions / Future Additions

<!-- Add verified findings, benchmark results, or corrected commands below -->
