#!/bin/bash
#
# Setup for (temporary) FEMS Remote-Service

set -e

main() {
    create_systemd_service
    run_systemd_service
    echo "# FINISHED SUCCESSFULLY"
}

create_systemd_service() {
	cat <<EOT > /etc/systemd/system/fems-remote-service.service
[Unit]
Description=FEMS Remote-Service
After=network.target

[Service]
User=fems
Group=fems
Type=simple
WorkingDirectory=/home/fems
ExecStart=/bin/sh -ce "/usr/bin/python /usr/bin/pagekite.py --clean --logfile=stdio --frontend=remote-service.fenecon.de:443 --service_on=raw:$(/bin/hostname).remote-service.fenecon.de:localhost:22:$(grep 'apikey=' /etc/fems | cut -d'=' -f2)"
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOT
}

run_systemd_service() {
    /bin/systemctl daemon-reload
    /bin/systemctl enable fems-remote-service
    /bin/systemctl restart fems-remote-service
}

main; exit
