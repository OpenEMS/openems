#!/bin/bash
#
# This creates the single_document.pdf.
#
LANG="pages" # default: english
# LANG="de"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${DIR}/../modules/ROOT/${LANG}

echo "Creating ${DIR}/../modules/ROOT/${LANG}/single_document.pdf"

asciidoctor-pdf -o single_document.pdf single_document.adoc