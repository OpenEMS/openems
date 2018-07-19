# Initialize default GIT settings.
# 
# This script sets some sensible defaults for GIT with OpenEMS. It needs to be executed in the base directory using "bash tools/git-settings.sh"
# - ignore .classpath files
#   See https://stackoverflow.com/questions/10417337
#
for D in *; do
    if [ -d "${D}" ]; then
        echo "${D}" 
	git update-index --assume-unchanged "${D}/.classpath"
    fi
done
