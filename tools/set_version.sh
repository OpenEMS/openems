#!/bin/bash
#
# Sets a version

set -e

main() {
    initialize_environment
    VERSION_MAJOR=2023
    VERSION_MINOR=9
    VERSION_PATCH=1
    VERSION_STRING="SNAPSHOT"
    VERSION="$VERSION_MAJOR.$VERSION_MINOR.$VERSION_PATCH-$VERSION_STRING"
    common_update_version_in_code
}

initialize_environment() {
    # Set working directory
    SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
    cd ${SCRIPT_DIR}/..

    # Include commons
    source $SCRIPT_DIR/common.sh
    common_initialize_environment
}

main; exit
