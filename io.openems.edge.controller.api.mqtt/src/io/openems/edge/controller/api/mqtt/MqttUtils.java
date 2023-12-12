package io.openems.edge.controller.api.mqtt;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;


/**
 * This Utility class provides methods for handling MQTT-related operations.
 */
public class MqttUtils {
	public static SSLSocketFactory createSSLSocketFactory(String keyStorePath, String keyStorePassword, String trustStorePath, String trustStorePassword) {
        try {
            // Load client key store
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream keyStoreInput = new FileInputStream(keyStorePath)) {
                keyStore.load(keyStoreInput, keyStorePassword.toCharArray());
            }

            // Load trust store
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream trustStoreInput = new FileInputStream(trustStorePath)) {
                trustStore.load(trustStoreInput, trustStorePassword.toCharArray());
            }

            // Initialize key manager factory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

            // Initialize trust manager factory
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            // Initialize SSL context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Error creating SSLSocketFactory", e);
        }
    }
}
