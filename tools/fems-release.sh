#!/bin/bash

set -xe

openems_constants="io.openems.common/src/io/openems/common/OpenemsConstants.java"

echo "Update branch fems/master"
git checkout master
git pull

echo "Update branch fems/develop"
git fetch openems develop
git checkout develop

echo "Merge from openems/develop"
git merge openems/develop --no-edit

echo "Push to fems/develop"
git push origin develop

version="$(grep version ui/package.json | cut -d'"' -f4 | cut -d'-' -f1)"

echo
echo "#"
echo "# Releasing version $version"
echo "# Update Changelog!"
echo "#"
read -p ""
git add .
git commit --no-edit -m "Update Changelog for $version"

git flow release start "$version"
bash tools/prepare-release.sh
git add .
git status
git commit --no-edit -m "Push version to $version"

git flow release finish "$version" -n
git checkout develop
bash tools/prepare-next-snapshot-patch.sh

version="$(grep version ui/package.json | cut -d'"' -f4)"
git add .
git status
git commit --no-edit -m "Start development of version $version"

git push -u origin develop
git push -u origin master

echo
echo "FINISHED"
