# Makefile — convenience targets for rec-system
# Usage: make <target>
# Requires: node, npm, and Homebrew Python (python3.11 or later)

.PHONY: setup test test-backend test-ai install-backend install-ai dev-backend dev-ai

# ── Setup ─────────────────────────────────────────────────────────────────────

setup: install-backend install-ai
	@echo ""
	@echo "✅  All dependencies installed."
	@echo "    Run 'make test' to verify everything passes."

install-backend:
	@echo "📦  Installing backend-node dependencies..."
	cd backend-node && npm install

install-ai:
	@echo "📦  Setting up ai-service virtualenv..."
	cd ai-service && bash setup.sh

# ── Tests ─────────────────────────────────────────────────────────────────────

test: test-backend test-ai
	@echo ""
	@echo "✅  All tests passed."

test-backend:
	@echo "🧪  Running backend-node tests..."
	cd backend-node && npm test

test-ai:
	@echo "🧪  Running ai-service tests..."
	cd ai-service && venv/bin/pytest

# ── Dev servers ───────────────────────────────────────────────────────────────

dev-backend:
	cd backend-node && npm run dev

dev-ai:
	cd ai-service && venv/bin/python main.py
