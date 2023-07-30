#!/bin/bash
set -e

main() {
    initialize
    update_fems_branches
    print_header
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
    changelog_constants="ui/src/app/changelog/view/component/changelog.constants.ts"
    initialize_version_from_string $(grep version $package_json | cut -d'"' -f4 | cut -d'-' -f1)
    release_date=$(date --iso-8601)

    # Validate
    if [ $(git remote -v | grep -c '^openems') -eq 0 ]; then
	    echo "# Missing Remote openems"
	    echo "git remote add openems https://github.com/OpenEMS/openems.git"
	    exit 1
    fi

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

    # Reset files
    git checkout $openems_constants 2>/dev/null
    git checkout $package_json 2>/dev/null
    git checkout $package_lock_json 2>/dev/null
    git checkout $changelog_constants 2>/dev/null
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

    echo "# Update branch fems/main"
    git checkout main
    git pull

    echo "# Update branch fems/develop"
    git checkout develop
    git pull
}

merge_from_openems() {
    echo "# Merge from openems/develop"
    git fetch openems develop
    git merge openems/develop --no-edit

    echo "# Push to fems/develop"
    git push origin develop
}

update_changelog() {
    echo "# Update Changelog! ($(pwd)/ui/src/app/changelog/changelog.component.ts)"
    read -p ""
    git add .
    git commit --no-edit -m "Update Changelog for $version_string" || true
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

    echo "# Update $changelog_constants"
    sed --in-place "s/\(UI_VERSION = \).*$/\1\"$version_string\";/" $changelog_constants
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
    git push -u origin main
}

main; exit
