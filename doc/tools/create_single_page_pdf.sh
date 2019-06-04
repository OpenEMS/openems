#!/bin/bash
#
# This creates the single_document.pdf.
#
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${DIR}/../modules/ROOT/pages

echo "Creating ${DIR}/../modules/ROOT/pages/single_document.pdf"

asciidoctor-pdf -o single_document.pdf single_document.adoc