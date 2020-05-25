# Prepares the next SNAPSHOT development version
#
# - Sets SNAPSHOT version number
#
#   E.g. changes 2020.2.0 to 2020.2.1-SNAPSHOT

# Basic definitions
openems_constants="io.openems.common/src/io/openems/common/OpenemsConstants.java"
single_document="doc/modules/ROOT/pages/single_document.adoc"
package_json="ui/package.json"
package_lock="ui/package-lock.json"
about_component="ui/src/app/about/about.component.html"

# Reset files
git checkout $openems_constants
git checkout $single_document
git checkout $package_json
git checkout $package_lock
git checkout $about_component

# Find new Version
major=$(grep 'VERSION_MAJOR =' $openems_constants | sed 's/^.*= \([0-9]\+\);/\1/')
minor=$(grep 'VERSION_MINOR =' $openems_constants | sed 's/^.*= \([0-9]\+\);/\1/')
patch=$(grep 'VERSION_PATCH =' $openems_constants | sed 's/^.*= \([0-9]\+\);/\1/')
new_patch=$(echo $patch | awk '{print $0+1}')
new_version="${major}.${minor}.${new_patch}-SNAPSHOT"

echo "# SNAPSHOT version: $new_version"

echo "# Update $openems_constants"
sed --in-place "s/\(VERSION_PATCH = \)\([0-9]\+\)\(.*\)/\1$new_patch\3/" $openems_constants
sed --in-place 's/\(public .* VERSION_STRING = "\)\(".*$\)/\1SNAPSHOT\2/' $openems_constants

echo "# Update $single_document"
sed --in-place "s/\(^Version \).*$/\1$new_version/" $single_document

echo "# Update $package_json" 
sed --in-place "s/\(\"version\": \"\).*\(\".*$\)/\1$new_version\2/" $package_json

echo "# Update $package_lock" 
sed --in-place "s/\(^  \"version\": \"\).*\(\".*$\)/\1$new_version\2/" $package_lock

echo "# Update $about_component"
sed --in-place "s/\(.*About.build.*: \).*\(<\/a>\)/\1$new_version\2/" $about_component

echo "# Finished"

echo ""
echo "# Ready for commit: \"Start development of version $new_version\""
