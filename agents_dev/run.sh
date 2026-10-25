#!/usr/bin/env bash
# Script lanzador del Universal Agentic Loop con entorno virtual automático
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT_DIR="$(dirname "$DIR")"

cd "$ROOT_DIR"

if [ -f "$DIR/.venv_agente/bin/activate" ]; then
    source "$DIR/.venv_agente/bin/activate"
fi

python3 -m agents_dev "$@"
