#!/bin/bash
#
# Creates a Release on main branch

set -e

main() {
    initialize_environment
    print_header
    check_dependencies
    update_fems_branches
    start_release
    common_update_version_in_code
    update_changelog
    common_build_edge_and_ui_in_parallel
    finish_release
    prepare_next_snapshot
    push_to_remote
    echo "# FINISHED"
}

initialize_environment() {
    # Set working directory
    SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
    cd ${SCRIPT_DIR}/..

    # Include commons
    source $SCRIPT_DIR/common.sh
    common_initialize_environment

    # Set environment variables
    THEME="openems"
    RELEASE_DATE=$(date --iso-8601)
    PREV_VERSION=$VERSION
    SRC_CHANGELOG_COMPONENT="ui/src/app/changelog/view/component/changelog.component.ts"

    # Target version without SNAPSHOT
    VERSION_STRING=""
    VERSION="$VERSION_MAJOR.$VERSION_MINOR.$VERSION_PATCH"

    # Reset files
    git checkout $SRC_OPENEMS_CONSTANTS 2>/dev/null
    git checkout $SRC_PACKAGE_JSON 2>/dev/null
    git checkout $SRC_CHANGELOG_CONSTANTS 2>/dev/null
}

# Check for required dependencies
check_dependencies() {
    hash code 2>/dev/null || { echo >&2 "I require 'code' but it's not installed. Aborting."; exit 1; }

    if [ $(git remote -v | grep -c '^origin') -eq 0 ]; then
        echo "# Missing Remote origin"
        echo "git remote add origin https://git.intranet.fenecon.de/FENECON/fems.git"
        exit 1
    fi

    if [ $(dpkg -l | grep -c git-flow) -eq 0 ]; then
        echo "# Missing git-flow"
        echo "apt install git-flow"
        echo "git flow init"
        echo "git config --global user.name \"...\""
        echo "git config --global user.email ..."
        exit 1
    fi
}

print_header() {
    echo "#"
    echo "# Releasing version: $VERSION"
    echo "#              date: $RELEASE_DATE"    
    echo "#"
    echo
}

update_fems_branches() {
    echo "# Update branch fems/main"
    git fetch origin main:main

    echo "# Update branch fems/develop"
    git checkout develop
    git pull
}

# Git Flow: start release branch
start_release() {
    git flow release start "$VERSION"
}

update_changelog() {
    echo "# Update Changelog! ($SRC_CHANGELOG_COMPONENT)"
    code $SRC_CHANGELOG_COMPONENT
    read -p ""
}

finish_release() {
    git add .
    git status
    git commit --no-edit -m "Push version to $VERSION"
    git flow release finish "$VERSION" -n
    git checkout develop
}

prepare_next_snapshot() {
    VERSION_STRING="SNAPSHOT"
    VERSION="$VERSION_MAJOR.$VERSION_MINOR.$VERSION_PATCH-$VERSION_STRING"
    common_update_version_in_code
    common_build_ui # updates package-lock.json
    git add .
    git status
    git commit --no-edit -m "Start development of version $VERSION"
}

push_to_remote() {
    git push -u origin develop
    git push -u origin main
}

main; exit
