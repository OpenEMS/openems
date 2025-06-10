#!/bin/bash
#
# This creates the German translation.
#
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${DIR}/../modules/ROOT/pages

echo "Creating German translation inside ${DIR}/../modules/ROOT/de/"

rm -R ../de
find -L . -name \*.adoc -exec bash -c 'mkdir -p ../de/$(dirname {}) && po4a-translate -v -f asciidoc -M UTF8 -k 0 -p i18n/de.po -m {} -l  ../de/{}' \;