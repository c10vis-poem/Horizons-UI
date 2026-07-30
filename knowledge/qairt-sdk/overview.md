# Qualcomm AI Runtime (QAIRT) Overview

> Extracted from the Qualcomm QAIRT SDK manual (`#QAIRT/` Drive capture). Official Qualcomm documentation — text extracted from the saved doc page; nav/boilerplate stripped.

Updated: Jun 04, 2026 80-63442-10 Rev: AK

Welcome to Qualcomm's AI RunTime (aka "QAIRT") documentation. QAIRT is a suite of tools that help you develop, run, and optimize AI models for Qualcomm-supported hardware.

There are several stages to go from having a trained AI model on your "host machine" to a runnable model on your "target device". QAIRT helps prepare the proper files you will need on your target device. It also provides runtime interpretters for each backend and processor to turn model instructions into runnable code.

## How to Use QAIRT

There are two primary SDKs which automate large portions of the AI build pipeline:

- Qualcomm Snapdragon Neural Processing SDK (aka "SNPE") is a simpler API and allows your model to execute using multiple processors. The tradeoff for that simplicity is that SNPE may have larger files and less granular control over how individual model operations are implemented.
- Qualcomm AI Engine Direct (aka "QNN") for granular control over how each operation in your model works. This SDK builds models to work with specific processors.
- Generative AI Inference Extensions (GENIE) SDK. GENIE extends QNN specifically for generative AI use cases (Ex. LLMs).

With both of these SDKs, you will need to:

- Get an AI model (ex. downloading a TensorFlow model).
- Use CLI tools provided in the SDKs to convert the model into a format the target device runtimes can interpret.
- Write an app in C, C++, or Java using the chosen SDK's API to execute your model.
- Transfer the built model, app executable, and QAIRT runtimes to the target device.
- Run your app to execute inferences on the target device (ex. passing images in to be classified by Inception V3).
- Benchmark and optimize your performance.

In order to use QAIRT, pick an SDK below and follow their tutorials to see the workflow in action:

- SNPE - For a simpler API which can run your model on multiple processors.
- QNN - For granular control over how your model is executed.
- GENIE (extends QNN) - For LLM or Generative AI use cases.

## QAIRT API (unified runtime API)

The QAIRT API is the low-level runtime API that sits beneath the SDKs. When you load a prepared model and run inference from an application, that application calls into QAIRT — directly if you are writing against the API, or indirectly through one of the SDKs. The SDK tools (converters, quantizers) produce artifacts that QAIRT consumes at runtime.

Use QAIRT API directly when you want a single API surface that targets all Qualcomm accelerators (CPU, GPU, HTP) through a uniform interface without the SDK-specific conventions layered on top. There are two public entry points:

- QAIRT C API — stable, ABI-compatible C interface. Preferred for systems integration, language bindings, and long-lived embedded deployments.
- QAIRT C++ API — header-only RAII wrapper over the C API. Preferred for new application code.

See the QAIRT API overview for how the C and C++ surfaces relate and guidance on which to pick.

## Additional Runtimes (aka "Delegates")

QAIRT SDKs provide runtimes that allow model operations to execute on target device processors (ex. CPU, GPU, HTP, etc.). For most situations, the runtimes provided by those SDKs will be the right ones for your use case.

There are several additional runtimes which are either optimized for specific model frameworks (ex. TFLite Delegate) or made for specific hardware components.

If these apply to your use case, you may need to follow the additional steps documented within to have your model execute in the proper environment:

- TFLite Delegate - Specifically optimized for TFLite model files.

Last Published: Jun 04, 2026
