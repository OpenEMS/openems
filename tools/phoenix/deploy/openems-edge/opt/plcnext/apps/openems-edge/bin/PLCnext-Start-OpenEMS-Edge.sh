#/bin/sh

### Config ###

JVM_OPTIONS=""
OPENEMS_TRUSTSTORE_PASSWD="changeit"

PATH_JRE_TRUSTSTORE="/usr/lib/jvm/temurin-21-jre-armhf/lib/security/cacerts"
PATH_OPENEMS_EDGE_CONFIG="/opt/plcnext/apps/openems-edge/conf"
PATH_OPENEMS_EDGE_JAR="/opt/plcnext/apps/openems-edge/bin/openems-edge.jar"
PATH_OPENEMS_TRUSTSTORE="$PATH_OPENEMS_EDGE_CONFIG/plcnext-truststore.jks"
PATH_PXC_PLCNEXT_CERT="/opt/plcnext/Security/IdentityStores/HTTPS-self-signed/certificate.pem"

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
	$JVM_OPTIONS \
	-jar "$PATH_OPENEMS_EDGE_JAR"
