#!/bin/bash
#
# Creates a Debian package for FEMS (OpenEMS Edge + UI)

set -e

main() {
    initialize_environment
    print_header
    check_dependencies
    update_version_in_code
    build_edge & # run in parallel
    build_ui & # run in parallel
    wait # wait for edge+ui
    prepare_deb_template
    build_deb
    clean_deb_template
    echo "# FINISHED"
}

initialize_environment() {
    # Set working directory
    SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
    cd ${SCRIPT_DIR}/..

    # Code files relevant for version
    SRC_OPENEMS_CONSTANTS="io.openems.common/src/io/openems/common/OpenemsConstants.java"
    SRC_PACKAGE_JSON="ui/package.json"
    SRC_CHANGELOG_COMPONENT="ui/src/app/changelog/view/component/changelog.constants.ts"

    # Set environment variables
    THEME="fenecon"
    PACKAGE_NAME="fems"
    VERSION_STRING=""
    VERSION="$(cd ui && node -p "require('./package.json').version" && cd ..)"

    if [[ "$VERSION" == *"-SNAPSHOT" ]]; then
        GIT_BRANCH="$(git branch --show-current)"
        GIT_BRANCH="${GIT_BRANCH/\//"."}"
        GIT_BRANCH="${GIT_BRANCH/-/"."}"
        GIT_HASH=""
        if [[ $(git diff --stat) != '' ]]; then
            GIT_HASH="dirty"
        else
            GIT_HASH="$(git rev-parse --short HEAD)"
        fi
        DATE=$(date "+%Y%m%d.%H%M")
        # Compliant with https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-version
        VERSION_STRING="${GIT_BRANCH}.${DATE}.${GIT_HASH}"
        VERSION="${VERSION/-SNAPSHOT/"-${VERSION_STRING}"}"
    fi
    DEB_FILE="${PACKAGE_NAME}.deb"
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

# Inserts the version number into the Code
update_version_in_code() {
    echo "# Update version in Code"
    echo "## Update $SRC_OPENEMS_CONSTANTS"
    sed --in-place "s/\(VERSION_STRING = \)\"\(.*\)\";$/\1\"$VERSION_STRING\";/" $SRC_OPENEMS_CONSTANTS

    echo "## Update $SRC_PACKAGE_JSON"
    sed --in-place "s/\(\"version\": \"\).*\(\".*$\)/\1$VERSION\2/" $SRC_PACKAGE_JSON

    echo "## Update $SRC_CHANGELOG_COMPONENT"
    sed --in-place "s/\(long: \"\).*\(\"\)$/\1$VERSION\2/" $SRC_CHANGELOG_COMPONENT
}

# Build OpenEMS Edge
build_edge() {
    echo "# Build OpenEMS Edge"
    ./gradlew --build-cache build buildEdge resolve.EdgeApp resolve.BackendApp
    git diff --exit-code io.openems.edge.application/EdgeApp.bndrun io.openems.backend.application/BackendApp.bndrun
}

# Build OpenEMS UI
build_ui() {
    echo "# Build OpenEMS UI"
    if [ "${NODE_MODULES_CACHE}" != "" -a -d "$NODE_MODULES_CACHE" ]; then
        echo "## Use cached node_modules"
        mv -f "${NODE_MODULES_CACHE}" "ui/node_modules"
    fi
    cd ui

    # Install dependencies from package.json
    npm ci
    if [ "${NG_CLI_CACHE_PATH}" != "" ]; then 
        echo "## Angular Cache: $NG_CLI_CACHE_PATH"
        node_modules/.bin/ng config cli.cache.path "$NG_CLI_CACHE_PATH"
    fi
    # Lint and Build
    node_modules/.bin/ng lint
    node_modules/.bin/ng build -c "${THEME},${THEME}-edge-prod,prod"
    cd ..

    if [ "${NODE_MODULES_CACHE}" != "" ]; then
        echo "## Refresh node_modules cache"
        mv -f "ui/node_modules" "${NODE_MODULES_CACHE}"
    fi
}

# Prepare Debian template
prepare_deb_template() {
    echo "# Build Debian package"
    sed --in-place "s/^\(Version: \).*$/\1$VERSION/" tools/debian/DEBIAN/control

    echo "## Add OpenEMS Edge"
    mkdir -p tools/debian/usr/lib/openems/
    cp io.openems.edge.application/generated/distributions/executable/EdgeApp.jar tools/debian/usr/lib/openems/openems.jar

    echo "## Add OpenEMS UI"
    rm -Rf tools/debian/usr/share/openems-fems/www/*
    mkdir -p tools/debian/usr/share/openems-fems/www
    cp -R ui/target/* tools/debian/usr/share/openems-fems/www
}

build_deb() {
    cd tools
    dpkg --build "debian" "../${DEB_FILE}"
    echo "## Built ${DEB_FILE}"
    cd ..
}

clean_deb_template() {
    cd tools/debian
    git clean -fd
    git checkout .
    cd ../..
}

main; exit
