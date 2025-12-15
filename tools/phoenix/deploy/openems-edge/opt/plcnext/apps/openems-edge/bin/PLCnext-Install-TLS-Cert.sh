#/bin/sh

### Config ###
PATH_JRE_TRUSTSTORE="/usr/lib/jvm/temurin-21-jre-armhf/lib/security/cacerts"
PATH_PXC_PLCNEXT_CERT="/opt/plcnext/Security/IdentityStores/HTTPS-self-signed/certificate.pem"

TRUSTSTORE_PASSWD="changeit"

### Add cert to default truststore ###
echo "Adding PxC PLCnext self-signed certificate to default truststore. Hope you have a backup!"
keytool -import -alias PxC:PLCnext-SlfSgndCert.pem \
	-file "$PATH_PXC_PLCNEXT_CERT" \
	-noprompt \
	-storepass "$TRUSTSTORE_PASSWD" \
	-keystore "$PATH_JRE_TRUSTSTORE"