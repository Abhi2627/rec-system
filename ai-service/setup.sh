#!/usr/bin/env bash
# setup.sh — create a clean virtualenv and install all dependencies
# Usage: bash setup.sh   (run from the ai-service directory)

set -euo pipefail

VENV_DIR="venv"

# ── Find a usable Python (not the Xcode stub) ────────────────────────────────
find_python() {
  # Prefer explicit versioned Homebrew binaries first
  for candidate in \
      /opt/homebrew/bin/python3.13 \
      /opt/homebrew/bin/python3.12 \
      /opt/homebrew/bin/python3.11 \
      /opt/homebrew/bin/python3.10 \
      /usr/local/bin/python3.12 \
      /usr/local/bin/python3.11 \
      /usr/local/bin/python3.10 \
      python3.13 python3.12 python3.11 python3.10; do
    if command -v "$candidate" &>/dev/null 2>&1; then
      local full_path
      full_path=$(command -v "$candidate" 2>/dev/null || echo "$candidate")
      # Skip Apple / Xcode stubs — they have no pip
      if [[ "$full_path" == /usr/bin/* ]] || [[ "$full_path" == /Applications/Xcode* ]]; then
        continue
      fi
      # Confirm pip actually works
      if "$full_path" -m pip --version &>/dev/null 2>&1; then
        echo "$full_path"
        return
      fi
    fi
  done
  echo ""
}

PYTHON=$(find_python)

if [[ -z "$PYTHON" ]]; then
  echo ""
  echo "❌  No usable Python found (need 3.10+ with pip, not the Xcode stub)."
  echo ""
  echo "    Install Homebrew Python:"
  echo "      brew install python@3.11"
  echo ""
  echo "    Then re-run:  bash setup.sh"
  echo ""
  exit 1
fi

echo "✅  Using Python: $PYTHON — $($PYTHON --version)"

# ── Recreate the venv unconditionally so it always uses the right Python ─────
echo "⏳  Creating virtual environment in ./$VENV_DIR ..."
rm -rf "$VENV_DIR"
"$PYTHON" -m venv "$VENV_DIR"

# ── Install / upgrade dependencies ──────────────────────────────────────────
echo "📦  Installing dependencies from requirements.txt ..."
"$VENV_DIR/bin/pip" install --upgrade pip --quiet
"$VENV_DIR/bin/pip" install -r requirements.txt

echo ""
echo "🎉  Done."
echo "    Python: $($VENV_DIR/bin/python --version)"
echo ""
echo "    Run tests:   venv/bin/pytest"
echo "    Start app:   venv/bin/python main.py"
echo ""
