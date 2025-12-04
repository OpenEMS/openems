#/bin/sh

### Config ###

OPENEMS_TRUSTSTORE_PASSWD="changeit"

PATH_JRE_TRUSTSTORE="/etc/ssl/certs/java/cacerts"
PATH_OPENEMS_EDGE_CONFIG="/mnt/c/openems/config"
PATH_OPENEMS_EDGE_JAR="/mnt/c/Users/mbreier/Development/PhoenixContact/openems-edge.jar"
PATH_OPENEMS_TRUSTSTORE="$PATH_OPENEMS_EDGE_CONFIG/pxc-plcnext-truststore.jks"
PATH_PXC_PLCNEXT_CERT="/mnt/c/Users/mbreier/Development/PhoenixContact/PLCnext_REST-API_TLS-Cert.pem"

### Prepare custom truststore ###
if [ -e $PATH_JRE_TRUSTSTORE ]; then
	echo "JRE truststore found :)"
else
	echo "JRE truststore not found!"
	ls -l "$PATH_JRE_TRUSTSTORE"
	exit 1
fi
if [ -f $PATH_OPENEMS_TRUSTSTORE ]; then
	echo "OpenEMS truststore found, recreating it"
	rm "$PATH_OPENEMS_TRUSTSTORE"
fi
cp "$PATH_JRE_TRUSTSTORE" "$PATH_OPENEMS_TRUSTSTORE"

echo "Adding PxC PLCnext self-signed certificate to OpenEMS truststore"
keytool -import -alias PxC:PLCnext-SlfSgndCert.pem \
	-file "$PATH_PXC_PLCNEXT_CERT" \
	-noprompt \
	-storepass "$OPENEMS_TRUSTSTORE_PASSWD" \
	-keystore "$PATH_OPENEMS_TRUSTSTORE"

echo "Starting OpenEMS Edge with custom truststore"
java -Djavax.net.ssl.trustStore="$PATH_OPENEMS_TRUSTSTORE" \
	-Djavax.net.ssl.trustStorePassword="OPENEMS_TRUSTSTORE_PASSWD" \
	-Dfelix.cm.dir="$PATH_OPENEMS_EDGE_CONFIG" \
	-jar "$PATH_OPENEMS_EDGE_JAR"
