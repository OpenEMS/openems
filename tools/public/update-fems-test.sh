#!/bin/bash

set -e

# Avoid "apt-listchanges: Can't set locale" error
export LANG=C

export PACKAGE="fems"
FORCE_UPDATE=false
DEB_PATH="http://fenecon.de/debian-test/${PACKAGE}-latest.deb"

# Parse command line paramaters
while getopts "fb:" arg; do
    case ${arg} in
        f)
            echo "# Activate FORCE_UPDATE mode"
            FORCE_UPDATE=true
            ;;
        b)
            echo "# Install SNAPSHOT from branch \"${OPTARG}\""
            DEB_PATH="https://dev.intranet.fenecon.de/${OPTARG}/${PACKAGE}.deb"
            ;;
        ?)
            echo "Invalid option: -${OPTARG}."
            exit 2
            ;;
    esac
done

if [ "$FORCE_UPDATE" = true ]; then
    # FORCE_UPDATE mode
    # Remove dpkg-diverts
    for file in $(dpkg-divert --list '*openems*' | sed 's/.* diversion of \(.*\) to.*/\1/'); do
        echo "# Remove dpkg-divert for $file"
        rm $file
        dpkg-divert --remove --rename --local $file
    done

else
    # Do not update SNAPSHOT versions
    if [ $(curl --max-time 10 --silent http://x:user@localhost:8084/rest/channel/_meta/Version 2>&1 |
        grep '_meta/Version' | # Get JSON
        sed 's/^.*\"value\":\"\(.*\)\".*$/\1/' | # Get full Version
        grep -c '-') -ge 1 ]; then # Contains hyphen = SNAPSHOT; count >= 1
        echo "SNAPSHOT versions cannot be updated"
        exit 1
    fi
fi

function add_apt_key_wheezy {
        cat <<EOL |
-----BEGIN PGP PUBLIC KEY BLOCK-----

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
=GN3Z
-----END PGP PUBLIC KEY BLOCK-----
EOL
apt-key add -
}

function is_installed {
    dpkg-query -W -f='${Status}' $1 2>/dev/null | grep "ok installed" >/dev/null
    return $?
}

function apt_install {
    if ! is_installed $1; then
        echo "Install '$1'"
        apt-get install $1 --assume-yes || true
    fi
}

function apt_remove {
    if is_installed $1; then
        echo "Remove '$1'"
        apt-get remove $1 --assume-yes || true
    fi
}

if [ -d "/home/imodcloud" ]; then
    echo "# Do not apt update Techbase"
else
    if [ -e /etc/apt/apt.conf.d/01-fems-setup ]; then
        echo "# Remove deprecated fiona proxy"
        rm -f /etc/apt/apt.conf.d/01-fems-setup
    fi

    if [ -e /etc/apt/sources.list.d/jessie-backports.list ]; then
        echo "# Remove deprecated jessie-backports source"
        rm -f /etc/apt/sources.list.d/jessie-backports.list
    fi

    VERSION=""
    if [ -e /etc/os-release ]; then
        source /etc/os-release
    fi

    if [ "$VERSION" = "8 (jessie)" ]; then
        echo "# Remove deprecated jessie sources"
        echo > /etc/apt/sources.list
    elif [ "$VERSION" = "9 (stretch)" ]; then
        echo "# Update stretch sources"
        echo 'deb http://archive.debian.org/debian/ stretch main' > /etc/apt/sources.list
    fi

    echo "# Setup dependencies"
    rm -f /etc/apt/trusted.gpg.d/*.lock

    if [ ! -e /usr/lib/apt/methods/https ]; then
        echo "# Fix apt method driver for https"
        ln -s http /usr/lib/apt/methods/https
    fi

    # Do not block at libc6 configure
    echo 'libc6 libraries/restart-without-asking boolean true' | sudo debconf-set-selections

    if [ $(df /boot | grep '99%\|100%' | wc -l) -eq 1 ]; then
        echo "#"
        echo "# /boot is full"
        echo "# Running kernel version: $(uname -r)"
        KERNEL_MAJOR=$(uname -r | cut -d'-' -f1)
        KERNEL_RUNNING=$(uname -r | cut -d'-' -f2)
        KERNEL_OLD=$(readlink -f /boot/vmlinuz.old | cut -d'-' -f3)
        KERNEL_NEXT=$(readlink -f /boot/vmlinuz | cut -d'-' -f3)
        for i in $(seq 1 $((KERNEL_NEXT-1))); do
            if [ $i -ne $KERNEL_RUNNING -a $i -ne $KERNEL_OLD ]; then 
                echo "# Delete kernel version $KERNEL_MAJOR-$i"
                rm -fv /boot/initrd.img-$KERNEL_MAJOR-$i-*
                rm -fv /boot/vmlinuz-$KERNEL_MAJOR-$i-*
                rm -fv /boot/System.map-$KERNEL_MAJOR-$i-*
            fi
        done
        echo "#"
    fi

    dpkg --configure -a --force-confnew || true

    apt --fix-broken install --assume-yes ||
        apt-get install -f --assume-yes || true

    apt_install dirmngr
    apt_install gnupg

    add_apt_key_wheezy

    apt autoremove --assume-yes ||
        apt-get autoremove --assume-yes || true
    apt-get --fix-broken install --assume-yes || true
    apt-get --allow-releaseinfo-change update ||
        # Jessie does not support allow-releaseinfo-change
        apt-get update || true
    apt-get dist-upgrade --assume-yes --force-yes
fi

apt_install at
apt_install ncdu

if ! is_installed temurin-17-jre; then
    if [ "$VERSION" = "8 (jessie)" ]; then
        echo "# Manually install p11-kit for Jessie"
        wget http://fenecon.de/fems-download/deb/{p11-kit_0.20.7-1_armhf,p11-kit-modules_0.20.7-1_armhf}.deb --no-check-certificate
        dpkg -i {p11-kit_0.20.7-1_armhf,p11-kit-modules_0.20.7-1_armhf}.deb
        rm -f {p11-kit_0.20.7-1_armhf,p11-kit-modules_0.20.7-1_armhf}.deb
    elif [ "$VERSION" = "9 (stretch)" ]; then
        echo "# Install p11-kit for stretch; downgrade libp11-kit0"
        apt-get install libp11-kit0=0.23.3-2 p11-kit --assume-yes --force-yes
    else
        apt_install p11-kit
    fi

    apt_install fonts-dejavu
    apt_install libasound2

    echo "# Install JRE 17"
    wget http://fenecon.de/fems-download/deb/{temurin-17-jre-armhf-latest,adoptium-ca-certificates-latest}.deb --no-check-certificate
    dpkg -i {temurin-17-jre-armhf-latest,adoptium-ca-certificates-latest}.deb
    rm -f {temurin-17-jre-armhf-latest,adoptium-ca-certificates-latest}.deb
fi

apt_remove adoptopenjdk-16-hotspot-jre
apt_remove openjdk-8-jre-headless

export CURRENT_VERSION="$(dpkg-query --showformat='${Version}' --show $PACKAGE)"
export LATEST_VERSION="$(wget -qO- http://fenecon.de/debian-test/${PACKAGE}-latest.version --no-check-certificate)"
if [ "$CURRENT_VERSION" = "$LATEST_VERSION" ]; then
    echo "# $PACKAGE: latest version [$LATEST_VERSION] is already installed"
else
    echo "# $PACKAGE: Updating from version [$CURRENT_VERSION] to [$LATEST_VERSION]"
    rm -Rf /tmp/org.osgi.framework.storage
    wget $DEB_PATH -O /tmp/${PACKAGE}.deb --no-check-certificate

    apt_remove openems-core
    apt_remove openems-core-fems

    if [ -e /usr/lib/openems/openems.jar ]; then
        echo "# Backup /usr/lib/openems/openems.jar"
        cp /usr/lib/openems/openems.jar /usr/lib/openems/openems.jar-backup-on-update
        fi
    dpkg -i /tmp/${PACKAGE}.deb
    rm -f /tmp/${PACKAGE}.deb
fi

echo "# Finished"
