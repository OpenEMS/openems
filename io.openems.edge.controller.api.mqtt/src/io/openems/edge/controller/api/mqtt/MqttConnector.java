package io.openems.edge.controller.api.mqtt;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.eclipse.paho.mqttv5.client.IMqttClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;

/**
 * This helper class wraps a connection to an MQTT broker.
 * 
 * <p>
 * One main feature of this class is to retry the initial connection to an MQTT
 * broker. A feature that is unfortunately not present in Eclipse Paho. After
 * the first successful connection, Paho reconnects on its own in case of a lost
 * connection.
 */
public class MqttConnector {

	private static final int INCREASE_WAIT_SECONDS = 5;
	private static final int MAX_WAIT_SECONDS = 60 * 5;
	private AtomicInteger waitSeconds = new AtomicInteger(0);

	/**
	 * Private inner class handles actual connection. It is executed via the
	 * ScheduledExecutorService.
	 */
	private final class MyConnector implements Runnable {

		private final CompletableFuture<IMqttClient> result = new CompletableFuture<>();
		private final IMqttClient client;
		private final MqttConnectionOptions options;

		private MyConnector(IMqttClient client, MqttConnectionOptions options) {
			this.client = client;
			this.options = options;
		}

		@Override
		public void run() {
			try {
				this.client.connect(this.options);
				this.result.complete(this.client);
			} catch (Exception e) {
				System.out.println(new Date() + ": " + e.getMessage()); // TODO
				MqttConnector.this.waitAndRetry();
			}
		}
	}

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private MyConnector connector;

	protected synchronized void deactivate() {
		this.connector = null;
		this.executor.shutdownNow();
	}

	protected synchronized CompletableFuture<IMqttClient> connect(String serverUri, String clientId, String username,
			String password) throws Exception {
		return this.connect(serverUri, clientId, username, password, null);
	}

	protected synchronized CompletableFuture<IMqttClient> connect(String serverUri, String clientId, String username,
			String password, MqttCallback callback) throws Exception {
		IMqttClient client = new MqttClient(serverUri, clientId);
		if (callback != null) {
			client.setCallback(callback);
		}

		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setUserName(username);
		if (password != null) {
			options.setPassword(password.getBytes(StandardCharsets.UTF_8));
		}
		options.setAutomaticReconnect(true);
		options.setCleanStart(true);
		options.setConnectionTimeout(10);

		String caFilePath = "/your_ssl/cacert.pem";
		String clientCrtFilePath = "/your_ssl/client.pem";
		String clientKeyFilePath = "/your_ssl/client.key";
		SSLSocketFactory socketFactory = getSocketFactory(caFilePath, clientCrtFilePath, clientKeyFilePath, "");
		options.setSocketFactory(socketFactory);

		this.connector = new MyConnector(client, options);

		this.executor.schedule(this.connector, 0 /* immediately */, TimeUnit.SECONDS);
		return this.connector.result;
	}

	private void waitAndRetry() {
		this.waitSeconds.getAndUpdate(oldValue -> Math.min(oldValue + INCREASE_WAIT_SECONDS, MAX_WAIT_SECONDS));
		this.executor.schedule(this.connector, this.waitSeconds.get(), TimeUnit.SECONDS);
	}

	private static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile,
			final String password) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		// load CA certificate
		X509Certificate caCert = null;

		FileInputStream fis = new FileInputStream(caCrtFile);
		BufferedInputStream bis = new BufferedInputStream(fis);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");

		while (bis.available() > 0) {
			caCert = (X509Certificate) cf.generateCertificate(bis);
			// System.out.println(caCert.toString());
		}

		// load client certificate
		bis = new BufferedInputStream(new FileInputStream(crtFile));
		X509Certificate cert = null;
		while (bis.available() > 0) {
			cert = (X509Certificate) cf.generateCertificate(bis);
			// System.out.println(caCert.toString());
		}

		// load client private key
		PEMParser pemParser = new PEMParser(new FileReader(keyFile));
		Object object = pemParser.readObject();
		PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
		JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
		KeyPair key;
		if (object instanceof PEMEncryptedKeyPair) {
			System.out.println("Encrypted key - we will use provided password");
			key = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
		} else {
			System.out.println("Unencrypted key - no password needed");
			key = converter.getKeyPair((PEMKeyPair) object);
		}
		pemParser.close();

		// CA certificate is used to authenticate server
		KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
		caKs.load(null, null);
		caKs.setCertificateEntry("ca-certificate", caCert);
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
		tmf.init(caKs);

		// client key and certificates are sent to server so it can authenticate
		// us
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		ks.setCertificateEntry("certificate", cert);
		ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(),
				new java.security.cert.Certificate[] { cert });
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, password.toCharArray());

		// finally, create SSL socket factory
		SSLContext context = SSLContext.getInstance("TLSv1.2");
		context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		return context.getSocketFactory();
	}

}
