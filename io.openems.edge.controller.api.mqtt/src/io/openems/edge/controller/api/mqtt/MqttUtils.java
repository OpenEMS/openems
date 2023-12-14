package io.openems.edge.controller.api.mqtt;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
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

	public static SSLSocketFactory createSslSocketFactory(String certPath, String privateKeyPath, String publicKeyPath, String trustStorePath, String trustStorePassword) {
        try {
        	X509Certificate clientCertificate = loadClientCertificate(certPath);

            PrivateKey privateKey = loadPrivateKey(privateKeyPath);

            PublicKey serverPublicKey = loadPublicKey(publicKeyPath);

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
	
	private static X509Certificate loadClientCertificate(String certPath) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate clientCertificate;
        try (InputStream certInputStream = new FileInputStream(certPath)) {
            clientCertificate = (X509Certificate) certificateFactory.generateCertificate(certInputStream);
            return clientCertificate;
        }

	}
	
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
	
    private static PrivateKey loadPrivateKey(String privateKeyPath) throws Exception {
        try (FileInputStream keyInputStream = new FileInputStream(privateKeyPath)) {
            byte[] keyBytes = keyInputStream.readAllBytes();
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        }
    }

    private static PublicKey loadPublicKey(String publicKeyPath) throws Exception {
        try (FileInputStream keyInputStream = new FileInputStream(publicKeyPath)) {
            byte[] keyBytes = keyInputStream.readAllBytes();
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        }
    }
}
