#!/bin/bash
set -e

# Get directory path; source https://stackoverflow.com/a/246128
SOURCE=${BASH_SOURCE[0]}
while [ -L "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR=$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )
  SOURCE=$(readlink "$SOURCE")
  [[ $SOURCE != /* ]] && SOURCE=$DIR/$SOURCE # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR=$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )

echo $DIR
cd $DIR/../../

echo "Update repository"
echo
git reset --hard HEAD
git pull
git fetch --tags -f

echo
echo "Building Backend"
echo
./gradlew clean buildDependents export.BackendApp -x test

read -p "Press enter to continue"

echo
echo "Create backup in 'openems-backend-backup'"
rm -Rf /opt/openems-backend-backup
cp -R /opt/openems-backend /opt/openems-backend-backup


echo "Copy built openems-backend.jar"
cp io.openems.backend.application/generated/distributions/executable/BackendApp.jar /opt/openems-backend/openems-backend.jar

#echo "Copy built io.openems.backend.timedata.timescaledb.jar"
#ls -l io.openems.backend.timedata.timescaledb/generated/io.openems.backend.timedata.timescaledb.jar
#cp io.openems.backend.timedata.timescaledb/generated/io.openems.backend.timedata.timescaledb.jar /opt/openems-backend/load/

echo
echo "# Commands..."
echo "systemctl stop openems-backend"
echo "rm -R /opt/openems-backend/org.osgi.framework.storage"
echo "systemctl restart openems-backend.service"
echo "journalctl -lfu openems-backend"

echo
echo "Finished"
echo
