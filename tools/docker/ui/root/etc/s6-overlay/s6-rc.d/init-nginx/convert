#!/bin/bash -e
[[ "$1" != *.conf.template ]] && exit 0;

FILE_NAME=$(basename "$1");
TEMPLATE_FILE=/etc/nginx/site-templates/$FILE_NAME;
CONFIG_FILE=/etc/nginx/site-confs/${FILE_NAME::-9}

echo -e "# This file changes with every container restart\n# Don't change this file, change site-templates/$FILE_NAME instead" > $CONFIG_FILE;
eval "cat <<< \"$(<$TEMPLATE_FILE)\"" 2> /dev/null >> $CONFIG_FILE;
