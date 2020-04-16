#!/bin/bash

set -xe

openems_constants="io.openems.common/src/io/openems/common/OpenemsConstants.java"


git fetch openems develop
git checkout develop
git merge openems/develop
git push origin develop

version="$(grep version ui/package.json | cut -d'"' -f4)"
git flow release start "$version"
bash tools/prepare-release.sh
git add .
git status
git commit -m "Push version to $version"

git flow release finish "$version" -n
git checkout develop
bash tools/prepare-next-snapshot-patch.sh

version="$(grep version ui/package.json | cut -d'"' -f4)"
git add .
git status
git commit -m "Start development of version $version"

git push -u origin develop
git push -u origin master

echo
echo "FINISHED"
