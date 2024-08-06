package io.openems.edge.bridge.mqtt;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.bridge.mqtt.api.LogVerbosity;

public class MyConfigMqttBridge extends AbstractComponentConfig implements Config {

	public MyConfigMqttBridge(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	protected static class Builder {
		private String id;
		private String brokerUrl;
		private boolean userRequired;
		private String username;
		private String password;
		private LogVerbosity verbosity;

		private Builder() {

		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setBrokerUrl(String url) {
			this.brokerUrl = url;
			return this;
		}

		public Builder setUserRequired(boolean required) {
			this.userRequired = required;
			return this;
		}

		public Builder setUser(String user) {
			this.username = user;
			return this;
		}

		public Builder setPassword(String password) {
			this.password = password;
			return this;
		}

		public Builder setLogVerbosity(LogVerbosity verbosity) {
			this.verbosity = verbosity;
			return this;
		}

		public MyConfigMqttBridge build() {
			return new MyConfigMqttBridge(this);
		}

	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	@Override
	public String brokerUrl() {
		return this.builder.brokerUrl;
	}

	@Override
	public boolean userRequired() {
		return this.builder.userRequired;
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
	public LogVerbosity logVerbosity() {
		return this.builder.verbosity;
	}

}
