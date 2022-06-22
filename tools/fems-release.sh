#!/bin/bash
set -e

main() {
    initialize
    print_header
    update_fems_branches
    merge_from_openems
    start_release
    update_changelog
    update_version_files
    finish_release
    prepare_next_snapshot
    push_to_remote

    echo "# FINISHED"
}

initialize() {
    # Set working directory
    cd "$(dirname "$0")/.."

    # Set environment variables
    openems_constants="io.openems.common/src/io/openems/common/OpenemsConstants.java"
    package_json="ui/package.json"
    package_lock_json="ui/package-lock.json"
    user_component="ui/src/app/user/user.component.html"
    initialize_version_from_string $(grep version $package_json | cut -d'"' -f4 | cut -d'-' -f1)
    release_date=$(date --iso-8601)

    # Reset files
    git checkout $openems_constants 2>/dev/null
    git checkout $package_json 2>/dev/null
    git checkout $package_lock_json 2>/dev/null
    git checkout $user_component 2>/dev/null
}

# Initializes variables version_major, version_minor, version_patch and version_snapshot
initialize_version_from_string() {
    tmp_version=$(echo $1 | cut -d'-' -f1)
    version_major=$(echo $tmp_version | cut -d'.' -f1)
    version_minor=$(echo $tmp_version | cut -d'.' -f2)
    version_patch=$(echo $tmp_version | cut -d'.' -f3)
    version_snapshot=$(echo $1 | cut -s -d'-' -f2)
    if [ "$version_snapshot" = "" ]; then
        version_string="$version_major.$version_minor.$version_patch"
    else
        version_string="$version_major.$version_minor.$version_patch-$version_snapshot"
    fi
}

print_header() {
    echo "#"
    echo "# Releasing version: $version_string"
    echo "#              date: $release_date"    
    echo "#"
    echo
}

update_fems_branches() {
    echo "# Fetch remotes"
    git fetch

    echo "# Update branch fems/master"
    git checkout master
    git pull

    echo "# Update branch fems/develop"
    git fetch openems develop
}

merge_from_openems() {
    echo "# Merge from openems/develop"
    git merge openems/develop --no-edit

    echo "# Push to fems/develop"
    git push origin develop
}

update_changelog() {
    echo "# Update Changelog! ($(pwd)/ui/src/app/changelog/changelog.component.ts)"
    read -p ""
    git add .
    git commit --no-edit -m "Update Changelog for $version" || true
}

update_version_files() {
    echo "# Update $openems_constants"
    sed --in-place "s/\(VERSION_MAJOR = \)\([0-9]\+\);$/\1$version_major;/" $openems_constants
    sed --in-place "s/\(VERSION_MINOR = \)\([0-9]\+\);$/\1$version_minor;/" $openems_constants
    sed --in-place "s/\(VERSION_PATCH = \)\([0-9]\+\);$/\1$version_patch;/" $openems_constants
    sed --in-place "s/\(VERSION_STRING = \)\"\(.*\)\";$/\1\"$version_snapshot\";/" $openems_constants

    echo "# Update $package_json" 
    sed --in-place "s/\(\"version\": \"\).*\(\".*$\)/\1$version_string\2/" $package_json

    echo "# Update $package_lock_json" 
    sed --in-place "s/\(^  \"version\": \"\).*\(\".*$\)/\1$version_string\2/" $package_lock_json

    echo "# Update $user_component"
    sed --in-place "s/\(<a .* routerLink=\"\/changelog\">\).*\(<\/a>\)/\1$version_string \\($release_date\\)\2/" $user_component
}

start_release() {
    git checkout develop
    git flow release start "$version_string"
}

finish_release() {
    git add .
    git status
    git commit --no-edit -m "Push version to $version_string"
    git flow release finish "$version_string" -n
    git checkout develop
}

prepare_next_snapshot() {
    version_patch=$(echo $version_patch | awk '{print $0+1}')
    initialize_version_from_string "$version_major.$version_minor.$version_patch-SNAPSHOT"
    update_version_files
    git add .
    git status
    git commit --no-edit -m "Start development of version $version_string"
}

push_to_remote() {
    git push -u origin develop
    git push -u origin master
}

main; exit