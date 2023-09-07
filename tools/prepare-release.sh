#!/bin/bash
#
# Prepares a Release by removing the SNAPSHOT tag from version
#
# i.e. changes 2023.9.0-SNAPSHOT to 2023.9.0

set -e

main() {
    initialize_environment
    common_update_version_in_code
    echo "# Ready for commit: "Push version to $VERSION""
}

initialize_environment() {
    # Set working directory
    SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
    cd ${SCRIPT_DIR}/..

    # Include commons
    source $SCRIPT_DIR/common.sh
    common_initialize_environment

    # Target version
    VERSION_STRING=""
    VERSION="$VERSION_MAJOR.$VERSION_MINOR.$VERSION_PATCH"

    # Reset files
    git checkout $SRC_OPENEMS_CONSTANTS 2>/dev/null
    git checkout $SRC_PACKAGE_JSON 2>/dev/null
    git checkout $SRC_CHANGELOG_CONSTANTS 2>/dev/null
}

main; exit