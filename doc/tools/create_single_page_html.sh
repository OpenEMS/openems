#!/bin/bash
#
# This creates the single_document.html.
#
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${DIR}/../modules/ROOT/pages

echo "Creating ${DIR}/../modules/ROOT/pages/single_document.html"

asciidoctor -b html5 -o single_document.html single_document.adoc