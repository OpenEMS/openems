#!/bin/bash
#
# Setup for (temporary) FEMS PageKite-Service
# Add: wget http://fenecon.de/fems-download/setup-fems-pagekite-service.sh -O /tmp/setup-fems-pagekite-service.sh --no-check-certificate && bash -x /tmp/setup-fems-pagekite-service.sh 

set -e

main() {
    create_systemd_service
    run_systemd_service
    echo "# FINISHED SUCCESSFULLY"
}

create_systemd_service() {
	cat <<EOT > /etc/systemd/system/fems-pagekite.service
[Unit]
Description=FEMS PageKite tmp
After=network.target

[Service]
User=fems
Group=fems
Type=simple
WorkingDirectory=/home/fems
ExecStart=/bin/sh -ce "/usr/bin/python /usr/bin/pagekite.py --clean --logfile=stdio --frontend=fenecon.de:2222 --service_on=raw:$(/bin/hostname).fems.fenecon.de:localhost:22:$(grep 'apikey=' /etc/fems | cut -d'=' -f2)"
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOT
}

run_systemd_service() {
    /bin/systemctl daemon-reload
    /bin/systemctl restart fems-pagekite
}

main; exit
