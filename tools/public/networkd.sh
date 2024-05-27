path="/etc/systemd/network/eth0.network"

echo "Writing $path"

cat <<EOT > $path
[Match]
Name=eth0

[Network]
DHCP=yes
LinkLocalAddressing=yes
EOT

for f in /etc/network/interfaces.d/*; do
    echo "- parsing $f"
    original_address=$(cat $f | grep address | xargs | tr -s ' ' ' ' | cut -d ' ' -f 2)
    original_netmask=$(cat $f | grep netmask | xargs | tr -s ' ' ' ' | cut -d ' ' -f 2)
    netmask=""
    if [ "$original_netmask" = "255.255.255.0" ]; then
        netmask="24"
    elif [ "$original_netmask" = "255.255.255.252" ]; then
        netmask="30"
    fi
    if [ "$netmask" = "" ]; then
        echo "Unknown netmask $netmask"
        exit 1
    fi

    address="$original_address/$netmask"
    echo "Address=$address" >> $path
done

echo "###"
cat $path
echo "###"

echo "Removing /etc/network/interfaces.d (backup @ /opt/fems)"
mkdir -p /opt/fems
mv /etc/network/interfaces.d /opt/fems/interfaces.d.bak

echo "Replacing /etc/network/interfaces (backup @ /opt/fems)"
mv /etc/network/interfaces /opt/fems/interfaces
cat <<EOT > /etc/network/interfaces
auto lo
iface lo inet loopback
EOT

echo "Replacing /etc/resolv.conf (backup @ /opt/fems)"
cp /etc/resolv.conf /opt/fems/resolv.conf
ln -sf /run/systemd/resolve/resolv.conf /etc/resolv.conf

echo "Enabling and starting systemd-networkd"
systemctl start systemd-networkd.service
systemctl enable systemd-networkd.service

echo "Enabling and starting systemd-resolved"
systemctl start systemd-resolved.service
systemctl enable systemd-resolved.service

echo "Finished"

