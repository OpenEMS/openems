package io.openems.edge.controller.api.mqtt;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;


/**
 * This Utility class provides methods for handling MQTT-related operations.
 */
public class MqttUtils {
	/**
	 * Creates and returns an SSLSocketFactory for establishing secure connections in MQTT.
	 *
	 * <p>
	 * This method initializes an SSL context using the provided client keystore and server truststore
	 * information, allowing the creation of an SSLSocketFactory with the configured security settings.
	 *
	 * @param certPath       The file path to the client's keystore.
	 * @param privateKeyPath   The password for accessing the client's keystore. If none is required, this can be null.
	 * @param publicKeyPath     The file path to the server's truststore.
	 * @param trustStorePassword The password for accessing the server's truststore. If none is required, this can be null.
	 * @return An SSLSocketFactory configured with the specified security settings.
	 * @throws RuntimeException If there is an error during the creation of the SSLSocketFactory.
	 */

	public static SSLSocketFactory createSslSocketFactory(String certPath, String privateKeyPath, String trustStorePath, String trustStorePassword) {
        try {
        	X509Certificate clientCertificate = loadClientCertificate(certPath);

            PrivateKey privateKey = loadPrivateKey(privateKeyPath);

            // Initialize key manager factory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            clientKeyStore.load(null);
            clientKeyStore.setCertificateEntry("clientCert", clientCertificate);
            char pw[] = trustStorePassword != null ? trustStorePassword.toCharArray() : null;
            clientKeyStore.setKeyEntry("clientKey", privateKey, pw, new java.security.cert.Certificate[]{clientCertificate});
            keyManagerFactory.init(clientKeyStore, pw);

            // Initialize trust manager factory
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(loadTrustStore(trustStorePath));
            
            // Initialize SSL context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Error creating SSLSocketFactory", e);
        }
    }
	
	/**
	 * Loads a client certificate from a given path.
	 *
	 * @param certPath The path to the certificate file.
	 * @return The loaded client certificate.
	 * @throws Exception If an error occurs while loading the certificate.
	 */
	private static X509Certificate loadClientCertificate(String certPath) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate clientCertificate;
        try (InputStream certInputStream = new FileInputStream(certPath)) {
            clientCertificate = (X509Certificate) certificateFactory.generateCertificate(certInputStream);
            return clientCertificate;
        }

	}
	
	/**
	 * Loads a trust store from a given path.
	 *
	 * @param trustStorePath The path to the trust store file.
	 * @return The loaded trust store.
	 * @throws Exception If an error occurs while loading the trust store.
	 */
	private static KeyStore loadTrustStore(String trustStorePath) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        List<X509Certificate> certificates = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(trustStorePath)) {
            while (fis.available() > 0) {
                X509Certificate certificate = (X509Certificate) cf.generateCertificate(fis);
                certificates.add(certificate);
            }
        }
        
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null);
        
        int i = 0;
        for (X509Certificate certificate : certificates) {
            String alias = "cert" + i++;
            trustStore.setCertificateEntry(alias, certificate);
        }
        
        return trustStore;
	}
	
	/**
	 * Loads a private key from a given path.
	 *
	 * @param privateKeyPath The path to the private key file.
	 * @return The loaded private key.
	 * @throws Exception If an error occurs while loading the private key.
	 */
    private static PrivateKey loadPrivateKey(String privateKeyPath) throws Exception {
        try (FileInputStream keyInputStream = new FileInputStream(privateKeyPath)) {
            byte[] keyBytes = keyInputStream.readAllBytes();
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        }
    }
}
