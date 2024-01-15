#!/bin/bash
#
# Creates a Debian package for OpenEMS Edge + UI

set -e

main() {
    initialize_environment
    print_header
    check_dependencies
    common_update_version_in_code
    prepare_deb_template
    build_deb
    create_version_file
    clean_deb_template
    echo "# FINISHED"
}

initialize_environment() {
    # Set working directory
    SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
    cd ${SCRIPT_DIR}/..

    # Include commons
    source $SCRIPT_DIR/common.sh
    common_initialize_environment

    # Build detailed SNAPSHOT name
    if [[ "$VERSION" == *"-SNAPSHOT" ]]; then
        # Replace unwanted characters with '.', compliant with Debian version
        # Ref: https://unix.stackexchange.com/a/23673
        VERSION_DEV_BRANCH="$(git branch --show-current)"
        VERSION_DEV_COMMIT=""
        if [[ $(git diff --stat) != '' ]]; then
            VERSION_DEV_COMMIT="dirty"
        else
            VERSION_DEV_COMMIT="$(git rev-parse --short HEAD)"
        fi
        VERSION_DEV_BUILD_TIME=$(date "+%Y%m%d.%H%M")
        # Compliant with https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-version
        VERSION_STRING="$(echo $VERSION_DEV_BRANCH | tr -cs 'a-zA-Z0-9\n' '.').${VERSION_DEV_BUILD_TIME}.${VERSION_DEV_COMMIT}"
        VERSION="${VERSION/-SNAPSHOT/"-${VERSION_STRING}"}"
    fi
    DEB_FILE="${PACKAGE_NAME}.deb"
    VERSION_FILE="${PACKAGE_NAME}.version"
}

print_header() {
    echo "#"
    echo "# Building Debian Package"
    echo "#"
    echo "# Theme: ${THEME}"
    echo "# Version: ${VERSION}"
    echo "#"
}

# Check for required dependencies
check_dependencies() {
    hash dpkg 2>/dev/null || { echo >&2 "I require 'dpkg' but it's not installed. Aborting."; exit 1; }
}

# Prepare Debian template
prepare_deb_template() {
    echo "# Build Debian package"
    sed --in-place "s/^\(Version: \).*$/\1$VERSION/" tools/debian/DEBIAN/control

    echo "## Add OpenEMS Edge"
    if [ -f "tools/debian/usr/lib/openems/openems.jar" ]; then
        echo "openems.jar exists. Skipping common_build_edge."
    else
        mkdir -p tools/debian/usr/lib/openems/
        echo "openems.jar does not exist. Building common_build_edge."
        common_build_edge
    fi
    
    echo "## Add OpenEMS UI"
    if [ -f "tools/debian/usr/share/openems-fems/www" ]; then
        echo "openems.ui exists. Skipping common_build_ui."
    else
        mkdir -p tools/debian/usr/share/openems-fems/www
        echo "openems.ui does not exist. Building common_build_ui."
        common_build_ui
    fi
}

build_deb() {
    cd tools
    dpkg-deb -Zxz --build "debian" "../${DEB_FILE}"
    echo "## Built ${DEB_FILE}"
    cd ..
}

create_version_file() {
    echo $VERSION > $VERSION_FILE
}

clean_deb_template() {
    cd tools/debian
    git clean -fd
    git checkout .
    cd ../..
}

main; exit
