#!/data/data/com.termux/files/usr/bin/bash
# Download model weights from HuggingFace
# Runs once on first boot; models cached locally after that
# No dependencies — just wget + bash

set -e

mkdir -p ~/models/{qwen-9b-q4_0,qwen-2b-compiled,gemma-12b-it-qat,gemma-4-e4b,gemma-4-e2b,qwen-0.8b-backup}

echo "🔄 Model Bootstrap — Checking local cache..."

# Agent 2 (Qwen 9B Q4_0) — Deep Thinker
if [ ! -f ~/models/qwen-9b-q4_0/qwen-9b-q4_0.gguf ]; then
    echo "📥 Downloading Qwen 3.5 9B Q4_0 (5.74GB)..."
    wget -O ~/models/qwen-9b-q4_0/qwen-9b-q4_0.gguf \
        "$(cat models/qwen-9b-q4_0/hf-download-link.txt)"
    echo "✓ Qwen 9B cached"
else
    echo "✓ Qwen 9B already cached"
fi

# Agent 1 (Qwen 2B) — Router
if [ ! -f ~/models/qwen-2b-compiled/qwen-2b.gguf ]; then
    echo "📥 Downloading Qwen 3.5 2B (1.6GB)..."
    # NexaAI NPU-optimized or standard GGUF
    wget -O ~/models/qwen-2b-compiled/qwen-2b.gguf \
        "https://huggingface.co/NexaAI/Qwen3.5-2B-Instruct-NPU/resolve/main/qwen-2b.gguf" || \
    wget -O ~/models/qwen-2b-compiled/qwen-2b.gguf \
        "https://huggingface.co/Qwen/Qwen3.5-2B-Instruct-GGUF/resolve/main/qwen-2b-q4_0.gguf"
    echo "✓ Qwen 2B cached"
else
    echo "✓ Qwen 2B already cached"
fi

# Gemma 12B IT (Fallback) — Single-agent mode
if [ ! -f ~/models/gemma-12b-it-qat/gemma-12b-it.dlc ]; then
    echo "📥 Downloading Gemma 4 12B IT (5.1GB)..."
    wget -O ~/models/gemma-12b-it-qat/gemma-12b-it.dlc \
        "https://huggingface.co/c10vis-poem/gemma-4-12b-it-qat/resolve/main/gemma-12b-it.dlc"
    echo "✓ Gemma 12B cached"
else
    echo "✓ Gemma 12B already cached"
fi

# Gemma 4 E4B (Backup swap)
if [ ! -f ~/models/gemma-4-e4b/gemma-e4b.gguf ]; then
    echo "📥 Downloading Gemma 4 E4B (5.1GB) — backup..."
    wget -O ~/models/gemma-4-e4b/gemma-e4b.gguf \
        "https://huggingface.co/c10vis-poem/gemma-4-e4b/resolve/main/gemma-e4b.gguf"
    echo "✓ Gemma E4B cached"
else
    echo "✓ Gemma E4B already cached"
fi

# Gemma 4 E2B (Tiny backup)
if [ ! -f ~/models/gemma-4-e2b/gemma-e2b.gguf ]; then
    echo "📥 Downloading Gemma 4 E2B (2.8GB) — emergency tiny..."
    wget -O ~/models/gemma-4-e2b/gemma-e2b.gguf \
        "https://huggingface.co/c10vis-poem/gemma-4-e2b/resolve/main/gemma-e2b.gguf"
    echo "✓ Gemma E2B cached"
else
    echo "✓ Gemma E2B already cached"
fi

# Qwen 0.8B (Emergency fallback)
if [ ! -f ~/models/qwen-0.8b-backup/qwen-0.8b.gguf ]; then
    echo "📥 Downloading Qwen 0.8B (emergency fallback, 0.5GB)..."
    wget -O ~/models/qwen-0.8b-backup/qwen-0.8b.gguf \
        "https://huggingface.co/Qwen/Qwen0.5B-Chat-GGUF/resolve/main/qwen-0.5b.gguf"
    echo "✓ Qwen 0.8B cached"
else
    echo "✓ Qwen 0.8B already cached"
fi

echo ""
echo "✅ All models cached locally. Ready to boot."
echo "Total storage used:"
du -sh ~/models/
