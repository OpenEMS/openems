#!/bin/bash
# Deletes all .classpath files
# 
for D in *; do
    if [ -d "${D}" ]; then
        echo "${D}" 
	rm -f "${D}/.classpath"
    fi
done
