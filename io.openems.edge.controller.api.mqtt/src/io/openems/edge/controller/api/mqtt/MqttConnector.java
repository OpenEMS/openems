package io.openems.edge.controller.api.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.mqttv5.client.IMqttClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;

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
	private final AtomicInteger waitSeconds = new AtomicInteger(0);

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
			String password) throws IllegalArgumentException, MqttException {
		// TODO	do not hard code this
		String certPath = "../aws_cert/openEMS_edge_test.cert.pem";
		String privateKeyPath = "../aws_cert/openEMS_edge_conv.private.key";
		String trustStorePath = "../aws_cert/AmazonRootCA1.der";
		String trustStorePassword = null;
		return this.connect(serverUri, clientId, username, password, certPath, privateKeyPath, trustStorePath, trustStorePassword, null);
	}
	
	protected synchronized CompletableFuture<IMqttClient> connect(String serverUri, String clientId, String username,
            String password, String certPath, String privateKeyPath, String trustStorePath, String trustStorePassword, MqttCallback callback) throws IllegalArgumentException, MqttException {
		IMqttClient client = new MqttClient(serverUri, clientId);
		if (callback != null) {
			client.setCallback(callback);
		}

		var options = new MqttConnectionOptions();
		options.setUserName(username);
		if (password != null) {
			options.setPassword(password.getBytes(StandardCharsets.UTF_8));
		}
		options.setAutomaticReconnect(true);
		options.setCleanStart(true);
		options.setConnectionTimeout(10);

		if (certPath != null && privateKeyPath != null) {
			options.setSocketFactory(MqttUtils.createSslSocketFactory(certPath, privateKeyPath, trustStorePath, trustStorePassword));
		}
		
		this.connector = new MyConnector(client, options);

		this.executor.schedule(this.connector, 0 /* immediately */, TimeUnit.SECONDS);
		return this.connector.result;
	}

	private void waitAndRetry() {
		this.waitSeconds.getAndUpdate(oldValue -> Math.min(oldValue + INCREASE_WAIT_SECONDS, MAX_WAIT_SECONDS));
		this.executor.schedule(this.connector, this.waitSeconds.get(), TimeUnit.SECONDS);
	}

}
