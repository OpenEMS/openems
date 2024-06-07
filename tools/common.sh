#!/bin/bash -e
#
# Provides commonly used functions and variables

common_initialize_environment() {
    # Code files relevant for version
    SRC_OPENEMS_CONSTANTS="io.openems.common/src/io/openems/common/OpenemsConstants.java"
    SRC_PACKAGE_JSON="ui/package.json"
    SRC_PACKAGE_LOCK_JSON="ui/package-lock.json"
    SRC_CHANGELOG_CONSTANTS="ui/src/app/changelog/view/component/changelog.constants.ts"

    # Set environment variables
    THEME="openems"
    PACKAGE_NAME="openems-edge"
    VERSION_STRING=""
    VERSION="$(cd ui && node -p "require('./package.json').version" && cd ..)"
    local tmp_version=$(echo $VERSION | cut -d'-' -f1)
    VERSION_MAJOR=$(echo $tmp_version | cut -d'.' -f1)
    VERSION_MINOR=$(echo $tmp_version | cut -d'.' -f2)
    VERSION_PATCH=$(echo $tmp_version | cut -d'.' -f3)
    VERSION_STRING=$(echo $VERSION | cut -s -d'-' -f2)
}

common_build_snapshot_version() {
    if [[ "$VERSION" == *"-SNAPSHOT" ]]; then
        # Replace unwanted characters with '.', compliant with Debian version
        # Ref: https://unix.stackexchange.com/a/23673
        VERSION_DEV_BRANCH="$(git branch --show-current)"
        VERSION_DEV_COMMIT=""
        git diff --exit-code --quiet;
        if [ $? -ne 0 ]; then
            VERSION_DEV_COMMIT="dirty"
        else
            VERSION_DEV_COMMIT="$(git rev-parse --short HEAD)"
        fi
        VERSION_DEV_BUILD_TIME=$(date "+%Y%m%d.%H%M")
        # Compliant with https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-version
        VERSION_STRING="$(echo $VERSION_DEV_BRANCH | tr -cs 'a-zA-Z0-9\n' '.').${VERSION_DEV_BUILD_TIME}.${VERSION_DEV_COMMIT}"
        VERSION="${VERSION/-SNAPSHOT/"-${VERSION_STRING}"}"
    fi
}

# Inserts the version number into the Code
common_update_version_in_code() {
    echo "# Update version in Code"
    echo "## Update $SRC_OPENEMS_CONSTANTS"
    sed --in-place "s#\(VERSION_MAJOR = \)\([0-9]\+\);#\1$VERSION_MAJOR;#" $SRC_OPENEMS_CONSTANTS
    sed --in-place "s#\(VERSION_MINOR = \)\([0-9]\+\);#\1$VERSION_MINOR;#" $SRC_OPENEMS_CONSTANTS
    sed --in-place "s#\(VERSION_PATCH = \)\([0-9]\+\);#\1$VERSION_PATCH;#" $SRC_OPENEMS_CONSTANTS
    sed --in-place "s#\(VERSION_STRING = \)\"\(.*\)\";#\1\"$VERSION_STRING\";#" $SRC_OPENEMS_CONSTANTS
    sed --in-place "s#\(VERSION_DEV_BRANCH = \)\"\(.*\)\";#\1\"${VERSION_DEV_BRANCH}\";#" $SRC_OPENEMS_CONSTANTS
    sed --in-place "s#\(VERSION_DEV_COMMIT = \)\"\(.*\)\";#\1\"$VERSION_DEV_COMMIT\";#" $SRC_OPENEMS_CONSTANTS
    sed --in-place "s#\(VERSION_DEV_BUILD_TIME = \)\"\(.*\)\";#\1\"$VERSION_DEV_BUILD_TIME\";#" $SRC_OPENEMS_CONSTANTS

    echo "## Update $SRC_PACKAGE_JSON"
    sed --in-place "s#^\(  \"version\": \"\).*\(\".*$\)#\1$VERSION\2#" $SRC_PACKAGE_JSON

    echo "## Update $SRC_PACKAGE_LOCK_JSON"
    sed --in-place "s#^\(  \"version\": \"\).*\(\".*$\)#\1$VERSION\2#" $SRC_PACKAGE_LOCK_JSON

    echo "## Update $SRC_CHANGELOG_CONSTANTS"
    sed --in-place "s#\(UI_VERSION = \"\).*\(\";\)#\1$VERSION\2#" $SRC_CHANGELOG_CONSTANTS
}

# Build OpenEMS Backend
common_build_backend() {
    echo "# Build OpenEMS Backend"
    ./gradlew $@ --build-cache build buildBackend resolve.BackendApp
    git diff --exit-code io.openems.backend.application/BackendApp.bndrun
}

# Build OpenEMS Edge and UI in parallel
common_build_edge_and_ui_in_parallel() {
    # TODO use 'parallel' tool for reliable implementation
    common_build_edge
    common_build_ui
}

# Build OpenEMS Edge
common_build_edge() {
    echo "# Build OpenEMS Edge"
    ./gradlew $@ --build-cache build buildEdge resolve.EdgeApp resolve.BackendApp
    git diff --exit-code io.openems.edge.application/EdgeApp.bndrun io.openems.backend.application/BackendApp.bndrun
}

# Run OpenEMS Checkstyle
common_run_checkstyle() {
    echo "# Run Checkstyle"
    ./gradlew $@ checkstyleAll
}

# Build OpenEMS UI
common_build_ui() {
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

common_save_environment() {
    local file=${1:-build.environment}
    echo "
    export VERSION=\"$VERSION\"
    export VERSION_MAJOR=\"$VERSION_MAJOR\"
    export VERSION_MINOR=\"$VERSION_MINOR\"
    export VERSION_PATCH=\"$VERSION_PATCH\"
    export VERSION_STRING=\"$VERSION_STRING\"
    export VERSION_DEV_BRANCH=\"$VERSION_DEV_BRANCH\"
    export VERSION_DEV_COMMIT=\"$VERSION_DEV_COMMIT\"
    export VERSION_DEV_BUILD_TIME=\"$VERSION_DEV_BUILD_TIME\"
    " | tee $file
}