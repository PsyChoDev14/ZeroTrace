#!/usr/bin/env bash
# ZeroTrace Analytics CLI
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
python3 "$DIR/scripts/analytics.py" "$@"
