import sys
import os

# Add the ai-service root to sys.path so tests can import main, engine, etc.
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
