#!/bin/bash
#
# Creates a Debian package for OpenEMS Edge + UI

set -e

OUTPUT=$( realpath ${1:-.} )

DEBIAN_UI_LOCATION=tools/debian/usr/share/openems/www
DEBIAN_EDGE_LOCATION=tools/debian/usr/lib/openems/

main() {
    initialize_environment
    print_header
    check_dependencies
    common_update_version_in_code
    prepare_deb_template
    build_deb
    create_version_file
    clean_deb_template
    echo "# FINISHED"
}

initialize_environment() {
    # Set working directory
    SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
    cd ${SCRIPT_DIR}/..

    # Include commons
    source $SCRIPT_DIR/common.sh
    common_initialize_environment
    common_build_snapshot_version

    DEB_FILE="${PACKAGE_NAME}.deb"
    VERSION_FILE="${PACKAGE_NAME}.version"
}

print_header() {
    echo "#"
    echo "# Building Debian Package"
    echo "#"
    echo -e "# Theme:\t${THEME}"
    echo -e "# Version:\t${VERSION}"
    echo -e "# Destination:\t${OUTPUT}"
    echo "#"
}

# Check for required dependencies
check_dependencies() {
    hash dpkg 2>/dev/null || { echo >&2 "I require 'dpkg' but it's not installed. Aborting."; exit 1; }
}

# Prepare Debian template
prepare_deb_template() {
    echo "# Build Debian package"
    sed --in-place "s/^\(Version: \).*$/\1$VERSION/" tools/debian/DEBIAN/control

    for script in preinst postinst prerm postrm
    do
	    script="tools/debian/DEBIAN/$script"
        [ -f $script ] && chmod 755 $script
    done

    echo "## Add OpenEMS Edge"
    if [ -f "$DEBIAN_EDGE_LOCATION/openems.jar" ]; then
        echo "openems.jar exists. Skipping common_build_edge."
    else
        mkdir -p "$DEBIAN_EDGE_LOCATION"
        echo "openems.jar does not exist. Building common_build_edge."
        common_build_edge
        cp build/openems-edge.jar $DEBIAN_EDGE_LOCATION/openems.jar
    fi

    echo "## Add OpenEMS UI"
    if [ -d "$DEBIAN_UI_LOCATION" ] && [ "$(ls -A $DEBIAN_UI_LOCATION)" ] ; then
        echo "openems.ui exists. Skipping common_build_ui."
    else
        mkdir -p "$DEBIAN_UI_LOCATION"
        echo "openems.ui does not exist. Building common_build_ui."
        common_build_ui
        cp -R ui/target/* "$DEBIAN_UI_LOCATION"
    fi
}

build_deb() {
    cd tools
    dpkg-deb -Zxz --build "debian" "${OUTPUT}/${DEB_FILE}"
    echo "## Built ${DEB_FILE}"
    cd ..
}

create_version_file() {
    echo $VERSION > "${OUTPUT}/${VERSION_FILE}"
}

clean_deb_template() {
    cd tools/debian
    git clean -fd
    git checkout .
    cd ../..
}

main; exit
