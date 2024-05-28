#!/bin/bash
#
# Uninstall (temporary) FEMS Remote-Service

set -e

main() {
    stop_systemd_service
    remove_systemd_service
    reload_systemd
    echo "# FINISHED SUCCESSFULLY"
}

stop_systemd_service() {
    /bin/systemctl stop fems-remote-service
    /bin/systemctl disable fems-remote-service
}

remove_systemd_service() {
    rm -f /etc/systemd/system/fems-remote-service.service
}

reload_systemd() {
    /bin/systemctl daemon-reload
}

main; exit
