#!/bin/bash
#
# This script downloads the fems.deb package from Woodpecker CI build 
# (https://dev.intranet.fenecon.de/feature/main/fems.deb) and prepares
# it for update-fems script.

set -e

main() {
    initialize_environment
    download_debian_package
    parse_version
    print_header
    archive_old_packages
    release
    echo "# FINISHED"
}

initialize_environment() {
    # Set working directory
    SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
    cd ${SCRIPT_DIR}

    ROOT_DIR="fems-debian"
    DEPLOY_DIR="$ROOT_DIR/_test"; mkdir -p ${DEPLOY_DIR}
    ARCHIVE_DIR="$ROOT_DIR/_archive"; mkdir -p ${ARCHIVE_DIR}

    BRANCH="main"
    PACKAGE_NAME="fems"
    PACKAGE_URL="https://dev.intranet.fenecon.de/${BRANCH}/${PACKAGE_NAME}.deb"
    PACKAGE_FILE="/tmp/${PACKAGE_NAME}.deb"
    LINK_LATEST="${PACKAGE_NAME}-latest.deb"
    VERSION_FILE="${PACKAGE_NAME}-latest.version"
}

download_debian_package() {
    echo "# Download debian package"
    wget $PACKAGE_URL -O $PACKAGE_FILE
}

parse_version() {
    VERSION=$(dpkg-deb --info $PACKAGE_FILE | grep 'Version: ' | cut -d ' ' -f3)
    PACKAGE_FULL_NAME="${PACKAGE_NAME}-${VERSION}.deb"
}

print_header() {
    echo "#"
    echo "# Releasing version: $VERSION"
    echo "#"
    echo
}

archive_old_packages() {
    if ls ${DEPLOY_DIR}/${PACKAGE_NAME}-*.deb 1>/dev/null 2>&1; then
        echo "# Archiving old files to ${ARCHIVE_DIR}/"
        mv ${DEPLOY_DIR}/${PACKAGE_NAME}-*.deb ${ARCHIVE_DIR}/
    fi
}

release() {
    cd "$DEPLOY_DIR"

    # Move debian package into place
    echo "# Create package:        $PACKAGE_FULL_NAME"
    mv "$PACKAGE_FILE" "$PACKAGE_FULL_NAME"
    echo "# Update symbolic link:  $LINK_LATEST"
    ln -sf "$PACKAGE_FULL_NAME" "$LINK_LATEST"

    # Update .version file
    echo "# Update latest version: $VERSION_FILE"
    echo $VERSION > $VERSION_FILE
    cd ${SCRIPT_DIR}

    # Finished
    echo "# Enjoy brand new release $VERSION! (fingers crossed...)"
    echo "#   wget http://fenecon.de/debian-test/${LINK_LATEST} -O ${LINK_LATEST} && dpkg -i ${LINK_LATEST} && rm ${LINK_LATEST} && systemctl restart openems --no-block; journalctl -lfu openems"
}

main; exit
