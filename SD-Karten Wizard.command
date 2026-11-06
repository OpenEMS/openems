#!/bin/bash
cd "$(dirname "$0")" || exit 1
if ! python3 -c 'import tkinter; import hashlib; assert hasattr(hashlib, "file_digest")' 2>/dev/null; then
  printf 'Benötigt Python 3.11 oder neuer mit Tkinter. Bitte Python für macOS installieren.\n'
  read -r -p 'Enter zum Schliessen …'
  exit 1
fi
python3 neutrava/scripts/sd_wizard.py
