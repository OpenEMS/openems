#!/bin/bash

# Halt on any error
set -e

# Initialize package name variable. Fill in with the name found in your DEBIAN package.
export PACKAGE_NAME=""

echo "Initiating update process for $PACKAGE_NAME"

# Initial flag for forced updates
SHOULD_FORCE_UPDATE=false
# URL to the .deb package for download
DOWNLOAD_URL=""

# Processing command line arguments
while getopts "fb:" option; do
    case ${option} in
        f)
            echo "Force update mode activated"
            SHOULD_FORCE_UPDATE=true
            ;;
        ?)
            echo "Unrecognized option: -${OPTARG}. Aborting."
            exit 2
            ;;
    esac
done

if [ "$SHOULD_FORCE_UPDATE" = true ]; then
    # Handling forced update logic
    # Removing any dpkg-diverts
    for diverted_file in $(dpkg-divert --list '*openems*' | sed 's/.* diversion of \(.*\) to.*/\1/'); do
        echo "# Eliminating dpkg-divert for $diverted_file"
        rm $diverted_file
        dpkg-divert --remove --rename --local $diverted_file
    done

else
    # Skip updating SNAPSHOT versions
    if [ $(curl --max-time 10 --silent http://x:user@localhost:8084/rest/channel/_meta/Version 2>&1 |
        grep '_meta/Version' |
        sed 's/^.*\"value\":\"\(.*\)\".*$/\1/' |
        grep -c '-') -ge 1 ]; then
        echo "Cannot update SNAPSHOT versions"
        exit 1
    fi
fi

echo "Verifying current version of $PACKAGE_NAME"
export CURRENT_VERSION=$(curl --max-time 10 --silent http://x:user@localhost:8084/rest/channel/_meta/Version 2>&1 |
    grep '_meta/Version' |
    sed 's/^.*\"value\":\"\(.*\)\".*$/\1/')
echo "The current version of $PACKAGE_NAME is $CURRENT_VERSION"

export NEWEST_VERSION="$(wget -qO- ""#"" --no-check-certificate)"  # Replace ""#"" with your URL to a text file containing the latest version.
echo "The latest available version for $PACKAGE_NAME is $NEWEST_VERSION"

if [ "$CURRENT_VERSION" = "$NEWEST_VERSION" ] && [ "$SHOULD_FORCE_UPDATE" = false ]; then
    echo "$PACKAGE_NAME is already up to date with version $NEWEST_VERSION."
else
    echo "Proceeding to update $PACKAGE_NAME from $CURRENT_VERSION to $NEWEST_VERSION"

    echo "Retrieving $PACKAGE_NAME package..."
    wget $DOWNLOAD_URL -O /tmp/${PACKAGE_NAME}.deb --no-check-certificate && echo "Successfully retrieved $PACKAGE_NAME package" || (echo "Package retrieval failed"; exit 1)

    if [ -e /usr/lib/openems/openems.jar ]; then
        echo "# Creating backup of /usr/lib/openems/openems.jar"
        cp /usr/lib/openems/openems.jar /usr/lib/openems/openems.jar.bak
    fi

    echo "Installing new version of $PACKAGE_NAME..."
    dpkg -i /tmp/${PACKAGE_NAME}.deb && echo "Installation successful" || (echo "Installation failed"; exit 1)
    
    echo "Cleaning up installation files"
    rm -f /tmp/${PACKAGE_NAME}.deb && echo "Cleanup complete"

    echo "Restarting openems service..."
    systemctl restart openems.service && echo "openems service restarted."

    echo "Update process for $PACKAGE_NAME has completed."

fi

# Final check to ensure package is installed
if ! dpkg -l | grep -qw $PACKAGE_NAME; then
    echo "Failed to detect $PACKAGE_NAME after update."
    exit 1
fi
