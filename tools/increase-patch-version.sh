#!/bin/bash
#
# Increases the Patch version, i.e. changes 2023.8.1 to 2023.8.2

set -e

main() {
    initialize_environment
    common_update_version_in_code
    common_build_ui
    echo "# FINISHED"
}

initialize_environment() {
    # Set working directory
    SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
    cd ${SCRIPT_DIR}/..

    # Include commons
    source $SCRIPT_DIR/common.sh
    common_initialize_environment

    # Target version
    VERSION_PATCH="$(echo $VERSION_PATCH | awk '{print $0+1}')"
    VERSION="$VERSION_MAJOR.$VERSION_MINOR.$VERSION_PATCH-$VERSION_STRING"

    # Reset files
    git checkout $SRC_OPENEMS_CONSTANTS 2>/dev/null
    git checkout $SRC_PACKAGE_JSON 2>/dev/null
    git checkout $SRC_CHANGELOG_CONSTANTS 2>/dev/null
}

main; exit