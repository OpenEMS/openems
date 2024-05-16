package io.openems.edge.controller.fnnstb.mqtt;

import static io.openems.edge.controller.fnnstb.mqtt.MqttUtils.createSslSocketFactory;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;

public class MqttConnectionManager {

	private String broker;
	private String clientId;
	private String username;
	private String password;
	private String certPem;
	private String privateKeyPem;
	private String trustStorePem;

	public MqttClient client;

	public MqttConnectionManager(String broker, String clientId) {
		this.broker = broker;
		this.clientId = clientId;
	}

	public MqttConnectionManager(String broker, String clientId, String username, String password, String certPem,
			String privateKeyPem, String trustStorePem) {
		this.broker = broker;
		this.clientId = clientId;
		this.username = username;
		this.password = password;
		this.certPem = certPem;
		this.privateKeyPem = privateKeyPem;
		this.trustStorePem = trustStorePem;
	}

	
	/**
	 * Establishes a connection to the MQTT broker.
	 *
	 * @throws MqttException if an error occurs during the connection process.
	 */
	public void connect() throws MqttException {
		this.client = new MqttClient(this.broker, this.clientId);
		System.out.println(this.client.getClientId());
		MqttConnectionOptions connOpts = new MqttConnectionOptions();

		connOpts.setAutomaticReconnect(true);
		connOpts.setCleanStart(true);
		connOpts.setConnectionTimeout(10000);

		if (this.username != null && !this.username.isBlank()) {
			connOpts.setUserName(this.username);
		}

		if (this.password != null && !this.password.isBlank()) {
			connOpts.setPassword(this.password.getBytes(StandardCharsets.UTF_8));
		}

		if (this.certPem != null && !this.certPem.isBlank() //
				&& this.privateKeyPem != null && !this.privateKeyPem.isBlank() //
				&& this.trustStorePem != null && !this.trustStorePem.isBlank()) {
			connOpts.setSocketFactory(createSslSocketFactory(this.certPem, this.privateKeyPem, this.trustStorePem));
		}

		System.out.println(this.client.getClientId());
		this.client.connect(connOpts);
		System.out.println("Return");
	}

	/**
	 * Disconnects from the MQTT broker.
	 *
	 * @throws MqttException if an error occurs during the disconnection process.
	 */
	public void disconnect() throws MqttException {
		if (this.client != null && this.client.isConnected()) {
			this.client.disconnect();
		}
	}

	public MqttClient getClient() {
		return this.client;
	}
}
