package io.openems.edge.bridge.mqtt;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.bridge.mqtt.api.MqttVersion;
import io.openems.edge.bridge.mqtt.api.QoS;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = "mqtt0";
		private String alias = "";
		private boolean enabled = true;
		private MqttVersion mqttVersion = MqttVersion.V3_1_1;
		private String uri = "tcp://localhost:1883";
		private String clientId = "";
		private String username = "";
		private String password = "";
		private boolean cleanSession = true;
		private int keepAliveInterval = 60;
		private int connectionTimeout = 30;
		private boolean autoReconnect = true;
		private int reconnectDelayMs = 1000;
		private int maxReconnectDelayMs = 30000;
		private String lwtTopic = "";
		private String lwtMessage = "";
		private QoS lwtQos = QoS.AT_LEAST_ONCE;
		private boolean lwtRetained = false;
		private String trustStorePath = "";
		private String trustStorePassword = "";
		private String keyStorePath = "";
		private String keyStorePassword = "";
		private boolean debugMode = false;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setAlias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder setMqttVersion(MqttVersion mqttVersion) {
			this.mqttVersion = mqttVersion;
			return this;
		}

		public Builder setUri(String uri) {
			this.uri = uri;
			return this;
		}

		public Builder setClientId(String clientId) {
			this.clientId = clientId;
			return this;
		}

		public Builder setUsername(String username) {
			this.username = username;
			return this;
		}

		public Builder setPassword(String password) {
			this.password = password;
			return this;
		}

		public Builder setCleanSession(boolean cleanSession) {
			this.cleanSession = cleanSession;
			return this;
		}

		public Builder setKeepAliveInterval(int keepAliveInterval) {
			this.keepAliveInterval = keepAliveInterval;
			return this;
		}

		public Builder setConnectionTimeout(int connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
			return this;
		}

		public Builder setAutoReconnect(boolean autoReconnect) {
			this.autoReconnect = autoReconnect;
			return this;
		}

		public Builder setReconnectDelayMs(int reconnectDelayMs) {
			this.reconnectDelayMs = reconnectDelayMs;
			return this;
		}

		public Builder setMaxReconnectDelayMs(int maxReconnectDelayMs) {
			this.maxReconnectDelayMs = maxReconnectDelayMs;
			return this;
		}

		public Builder setLwtTopic(String lwtTopic) {
			this.lwtTopic = lwtTopic;
			return this;
		}

		public Builder setLwtMessage(String lwtMessage) {
			this.lwtMessage = lwtMessage;
			return this;
		}

		public Builder setLwtQos(QoS lwtQos) {
			this.lwtQos = lwtQos;
			return this;
		}

		public Builder setLwtRetained(boolean lwtRetained) {
			this.lwtRetained = lwtRetained;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Creates a new Builder for MyConfig.
	 *
	 * @return the builder
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String id() {
		return this.builder.id;
	}

	@Override
	public String alias() {
		return this.builder.alias;
	}

	@Override
	public boolean enabled() {
		return this.builder.enabled;
	}

	@Override
	public MqttVersion mqttVersion() {
		return this.builder.mqttVersion;
	}

	@Override
	public String uri() {
		return this.builder.uri;
	}

	@Override
	public String clientId() {
		return this.builder.clientId;
	}

	@Override
	public String username() {
		return this.builder.username;
	}

	@Override
	public String password() {
		return this.builder.password;
	}

	@Override
	public boolean cleanSession() {
		return this.builder.cleanSession;
	}

	@Override
	public int keepAliveInterval() {
		return this.builder.keepAliveInterval;
	}

	@Override
	public int connectionTimeout() {
		return this.builder.connectionTimeout;
	}

	@Override
	public boolean autoReconnect() {
		return this.builder.autoReconnect;
	}

	@Override
	public int reconnectDelayMs() {
		return this.builder.reconnectDelayMs;
	}

	@Override
	public int maxReconnectDelayMs() {
		return this.builder.maxReconnectDelayMs;
	}

	@Override
	public String lwtTopic() {
		return this.builder.lwtTopic;
	}

	@Override
	public String lwtMessage() {
		return this.builder.lwtMessage;
	}

	@Override
	public QoS lwtQos() {
		return this.builder.lwtQos;
	}

	@Override
	public boolean lwtRetained() {
		return this.builder.lwtRetained;
	}

	@Override
	public String trustStorePath() {
		return this.builder.trustStorePath;
	}

	@Override
	public String trustStorePassword() {
		return this.builder.trustStorePassword;
	}

	@Override
	public String keyStorePath() {
		return this.builder.keyStorePath;
	}

	@Override
	public String keyStorePassword() {
		return this.builder.keyStorePassword;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

}
