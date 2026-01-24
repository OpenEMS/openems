package io.openems.edge.bridge.mqtt;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.bridge.mqtt.api.MqttVersion;
import io.openems.edge.bridge.mqtt.api.QoS;

@ObjectClassDefinition(//
		name = "Bridge MQTT", //
		description = "Provides an MQTT connection to a broker.")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "mqtt0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "MQTT Version", description = "The MQTT protocol version to use")
	MqttVersion mqttVersion() default MqttVersion.V3_1_1;

	@AttributeDefinition(name = "Broker Host", description = "Hostname/IP of the MQTT broker (e.g., localhost, 127.0.0.1, broker.example.com)")
	String host() default "localhost";

	@AttributeDefinition(name = "Broker Port", description = "Port of the MQTT broker (e.g., 1883 or 8883)")
	int port() default 1883;

	@AttributeDefinition(name = "Use SSL/TLS", description = "Whether to use SSL/TLS for the connection")
	boolean secureConnect() default false;

	@AttributeDefinition(name = "Client ID", description = "Unique client identifier. Leave empty for auto-generated ID.")
	String clientId() default "";

	@AttributeDefinition(name = "Username", description = "Username for broker authentication (optional)")
	String username() default "";

	@AttributeDefinition(name = "Password", description = "Password for broker authentication (optional)", type = AttributeType.PASSWORD)
	String password() default "";

	@AttributeDefinition(name = "Clean Session", description = "Start with a clean session (no persistent subscriptions)")
	boolean cleanSession() default true;

	@AttributeDefinition(name = "Keep Alive Interval [s]", description = "Keep-alive interval in seconds (0 = disabled)")
	int keepAliveInterval() default 60;

	@AttributeDefinition(name = "Connection Timeout [s]", description = "Connection timeout in seconds")
	int connectionTimeout() default 30;

	@AttributeDefinition(name = "Auto Reconnect", description = "Automatically reconnect on connection loss")
	boolean autoReconnect() default true;

	@AttributeDefinition(name = "Reconnect Delay [ms]", description = "Initial delay before reconnecting in milliseconds")
	int reconnectDelayMs() default 1000;

	@AttributeDefinition(name = "Max Reconnect Delay [ms]", description = "Maximum delay between reconnect attempts in milliseconds")
	int maxReconnectDelayMs() default 30000;

	@AttributeDefinition(name = "LWT Topic", description = "Topic for Last Will and Testament message (optional)")
	String lwtTopic() default "";

	@AttributeDefinition(name = "LWT Message", description = "Message to send when connection is lost unexpectedly")
	String lwtMessage() default "";

	@AttributeDefinition(name = "LWT QoS", description = "QoS level for Last Will message")
	QoS lwtQos() default QoS.AT_LEAST_ONCE;

	@AttributeDefinition(name = "LWT Retained", description = "Whether the Last Will message should be retained")
	boolean lwtRetained() default false;

	@AttributeDefinition(name = "Trust Store Path", description = "Path to the trust store file for SSL/TLS (optional)")
	String trustStorePath() default "";

	@AttributeDefinition(name = "Trust Store Password", description = "Password for the trust store", type = AttributeType.PASSWORD)
	String trustStorePassword() default "";

	@AttributeDefinition(name = "Key Store Path", description = "Path to the key store file for client certificates (optional)")
	String keyStorePath() default "";

	@AttributeDefinition(name = "Key Store Password", description = "Password for the key store", type = AttributeType.PASSWORD)
	String keyStorePassword() default "";

	@AttributeDefinition(name = "Debug Mode", description = "Enable debug logging for MQTT communication")
	boolean debugMode() default false;

	String webconsole_configurationFactory_nameHint() default "Bridge MQTT [{id}]";

}
