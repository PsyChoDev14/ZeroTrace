#!/usr/bin/env bash
set -e

# Make sure we run from the project root
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

# Ensure script is executable
chmod +x "$DIR/scripts/publish_update.py"

# Run the release automator with Python 3
python3 "$DIR/scripts/publish_update.py" "$@"
