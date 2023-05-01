# Prepares the next SNAPSHOT development version
#
# - Sets SNAPSHOT version number
#
#   E.g. changes 2020.2.0 to 2020.3.0-SNAPSHOT

# Basic definitions
openems_constants="io.openems.common/src/io/openems/common/OpenemsConstants.java"
package_json="ui/package.json"
package_lock="ui/package-lock.json"
user_component="ui/src/app/user/user.component.html"

# Reset files
git checkout $openems_constants
git checkout $package_json
git checkout $package_lock
git checkout $user_component

# Find new Version
major=$(grep 'VERSION_MAJOR =' $openems_constants | sed 's/^.*= \([0-9]\+\);/\1/')
minor=$(grep 'VERSION_MINOR =' $openems_constants | sed 's/^.*= \([0-9]\+\);/\1/')
patch=$(grep 'VERSION_PATCH =' $openems_constants | sed 's/^.*= \([0-9]\+\);/\1/')
new_minor=$(echo $minor | awk '{print $0+1}')
new_version="${major}.${new_minor}.${patch}-SNAPSHOT"

echo "# SNAPSHOT version: $new_version"

echo "# Update $openems_constants"
sed --in-place "s/\(VERSION_MINOR = \)\([0-9]\+\)\(.*\)/\1$new_minor\3/" $openems_constants
sed --in-place 's/\(public .* VERSION_STRING = "\)\(".*$\)/\1SNAPSHOT\2/' $openems_constants

echo "# Update $package_json" 
sed --in-place "s/\(\"version\": \"\).*\(\".*$\)/\1$new_version\2/" $package_json

echo "# Update $package_lock" 
sed --in-place "s/\(^  \"version\": \"\).*\(\".*$\)/\1$new_version\2/" $package_lock

echo "# Update $user_component"
sed --in-place "s/\(<a .* routerLink=\"\/changelog\">\).*\(<\/a>\)/\1$new_version\2/" $user_component

echo "# Finished"

echo ""
echo "# Ready for commit: \"Start development of version $new_version\""
