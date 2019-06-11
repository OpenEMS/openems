#!/bin/bash
#
# This updates the i18n/translate.pot file. This file is the reference for all translations.
#
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ${DIR}/../modules/ROOT/pages

echo "Updating ${DIR}/../modules/ROOT/pages/i18n/translate.pot"

po4a-gettextize --package-name OpenEMS -f asciidoc -M UTF8 -p i18n/translate.pot $(find -L . -name \*.adoc -printf '-m %P ')