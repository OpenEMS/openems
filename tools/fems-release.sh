#!/bin/bash

set -xe

openems_constants="io.openems.common/src/io/openems/common/OpenemsConstants.java"


git fetch openems develop

git checkout develop

git merge openems/develop

git push origin develop

major=$(grep 'VERSION_MAJOR =' $openems_constants | sed 's/^.*= \([0-9]\+\);/\1/')
minor=$(grep 'VERSION_MINOR =' $openems_constants | sed 's/^.*= \([0-9]\+\);/\1/')
patch=$(grep 'VERSION_PATCH =' $openems_constants | sed 's/^.*= \([0-9]\+\);/\1/')
version="${major}.${minor}.${patch}"

git flow release start "$version"

bash tools/prepare-release.sh

git add .

git commit -m "Push version to $version"

git flow release finish "$version"
