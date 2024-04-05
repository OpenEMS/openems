package io.openems.edge.controller.fnnstb.mqtt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

/**
 * This Utility class provides methods for handling MQTT-related operations.
 */
public class MqttUtils {
	/**
	 * Creates and returns an SSLSocketFactory for establishing secure connections
	 * in MQTT.
	 *
	 * <p>
	 * This method initializes an SSL context using the provided certificate,
	 * private key and server truststore information, allowing the creation of an
	 * SSLSocketFactory with the configured security settings.
	 *
	 * @param cert       The client's certificate as String.
	 * @param privateKey The private key as String.
	 * @param trustStore The server's trust store as String.
	 * @return An SSLSocketFactory configured with the specified security settings.
	 * @throws RuntimeException If there is an error during the creation of the
	 *                          SSLSocketFactory.
	 */

	public static SSLSocketFactory createSslSocketFactory(String cert, String privateKey, String trustStore) {
		try {
			Security.addProvider(new BouncyCastleProvider());

			// Load client certificate
			X509Certificate clientCert = loadCertificate(cert);

			// Load client private key
			PrivateKey clientKey = loadPrivateKey(privateKey);

			// Load CA certificate
			X509Certificate caCert = loadCertificate(trustStore);

			// Create a KeyStore and add the CA certificate, client certificate, and private
			// key
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(null, null);
			keyStore.setCertificateEntry("caCert", caCert);
			keyStore.setCertificateEntry("clientCert", clientCert);
			keyStore.setKeyEntry("clientKey", clientKey, new char[0], new X509Certificate[] { clientCert });

			// Create a TrustManager that trusts the CA certificate
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);

			// Create a KeyManager that uses the client certificate and private key
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, new char[0]);

			// Create an SSLContext with the TrustManager and KeyManager
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(),
					new SecureRandom());

			return sslContext.getSocketFactory();
		} catch (Exception e) {
			throw new RuntimeException("Error creating SSLSocketFactory", e);
		}
	}

	/**
	 * Loads an X.509 certificate from a PEM-encoded string.
	 *
	 * @param cert The PEM-encoded certificate string.
	 * @return The X.509 certificate.
	 * @throws IOException          If an I/O error occurs.
	 * @throws CertificateException If an error occurs while processing the
	 *                              certificate.
	 */
	private static X509Certificate loadCertificate(String cert) throws IOException, CertificateException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		try (InputStream is = new ByteArrayInputStream(cert.getBytes())) {
			return (X509Certificate) cf.generateCertificate(is);
		}
	}

	/**
	 * Loads a private key from a PEM-encoded string.
	 *
	 * @param privateKey The PEM-encoded private key string.
	 * @return The private key.
	 * @throws IOException              If an I/O error occurs.
	 * @throws NoSuchAlgorithmException If the specified algorithm is not available.
	 * @throws InvalidKeySpecException  If the private key cannot be generated.
	 */
	private static PrivateKey loadPrivateKey(String privateKey)
			throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		try (PEMParser pemParser = new PEMParser(
				new InputStreamReader(new ByteArrayInputStream(privateKey.getBytes())))) {
			Object obj = pemParser.readObject();
			if (obj instanceof PEMKeyPair) {
				// Handle RSA private key
				PEMKeyPair pemKeyPair = (PEMKeyPair) obj;
				JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
				return converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());
			} else if (obj instanceof PrivateKeyInfo) {
				// Handle other private key formats
				PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) obj;
				JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
				return converter.getPrivateKey(privateKeyInfo);
			} else {
				throw new InvalidKeySpecException("Invalid private key format");
			}
		}
	}
}
