#/bin/sh

# Setup for JRE on PLCnext
JRE_PATH="bellsoft-jre21.0.10+10-linux-amd64.tar.gz"
JRE_VERSION="jre-21.0.10"
echo "Start JRE installation... "
echo "Create folders ..."
mkdir -p /opt/plcnext/apps/openems-edge/lib/jre
mkdir -p /opt/plcnext/apps/openems-edge/conf
mkdir -p /opt/plcnext/apps/openems-edge/bin
echo "Unzip the JRE package..."
tar zxf $JRE_PATH -C /opt/plcnext/apps/openems-edge/lib/jre

echo "Add Java to PATH."
export PATH="/opt/plcnext/apps/openems-edge/lib/jre/$JRE_VERSION/bin:$PATH"
echo "Set environment variable for JAVA_Home"
export JAVA_HOME="/opt/plcnext/apps/openems-edge/lib/jre/$JRE_VERSION"
echo "Check if the installation was successful..."
java -version
