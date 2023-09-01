# Provides commonly used functions and variables

common_initialize_environment() {
    # Code files relevant for version
    SRC_OPENEMS_CONSTANTS="io.openems.common/src/io/openems/common/OpenemsConstants.java"
    SRC_PACKAGE_JSON="ui/package.json"
    SRC_CHANGELOG_CONSTANTS="ui/src/app/changelog/view/component/changelog.constants.ts"

    # Set environment variables
    THEME="fenecon"
    PACKAGE_NAME="fems"
    VERSION_STRING=""
    VERSION="$(cd ui && node -p "require('./package.json').version" && cd ..)"
    tmp_version=$(echo $VERSION | cut -d'-' -f1)
    VERSION_MAJOR=$(echo $tmp_version | cut -d'.' -f1)
    VERSION_MINOR=$(echo $tmp_version | cut -d'.' -f2)
    VERSION_PATCH=$(echo $tmp_version | cut -d'.' -f3)
    VERSION_STRING=$(echo $VERSION | cut -s -d'-' -f2)
}

# Inserts the version number into the Code
common_update_version_in_code() {
    echo "# Update version in Code"
    echo "## Update $SRC_OPENEMS_CONSTANTS"
    sed --in-place "s/\(VERSION_MAJOR = \)\([0-9]\+\);$/\1$VERSION_MAJOR;/" $SRC_OPENEMS_CONSTANTS
    sed --in-place "s/\(VERSION_MINOR = \)\([0-9]\+\);$/\1$VERSION_MINOR;/" $SRC_OPENEMS_CONSTANTS
    sed --in-place "s/\(VERSION_PATCH = \)\([0-9]\+\);$/\1$VERSION_PATCH;/" $SRC_OPENEMS_CONSTANTS
    sed --in-place "s/\(VERSION_STRING = \)\"\(.*\)\";$/\1\"$VERSION_STRING\";/" $SRC_OPENEMS_CONSTANTS

    echo "## Update $SRC_PACKAGE_JSON"
    sed --in-place "s/\(\"version\": \"\).*\(\".*$\)/\1$VERSION\2/" $SRC_PACKAGE_JSON

    echo "## Update $SRC_CHANGELOG_CONSTANTS"
    sed --in-place "s/\(UI_VERSION = \"\).*\(\";\)$/\1$VERSION\2/" $SRC_CHANGELOG_CONSTANTS
}

# Build OpenEMS Edge and UI in parallel
common_build_edge_and_ui_in_parallel() {
    common_build_edge & # run in parallel
    common_build_ui & # run in parallel
    wait # wait for edge+ui
}

# Build OpenEMS Edge
common_build_edge() {
    echo "# Build OpenEMS Edge"
    ./gradlew --build-cache build buildEdge resolve.EdgeApp resolve.BackendApp
    git diff --exit-code io.openems.edge.application/EdgeApp.bndrun io.openems.backend.application/BackendApp.bndrun
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