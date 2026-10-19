package io.openems.edge.controller.api.websocket;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private int port;
		private int apiTimeout;
		private boolean debugMode;
		private boolean externalAuthEnabled;
		private String[] externalAuthTrustedProxyCidrs = {};
		private String externalAuthUserIdHeader = "X-OpenEMS-User";
		private String externalAuthUserNameHeader = "X-OpenEMS-Name";
		private String externalAuthRoleHeader = "X-OpenEMS-Role";
		private String externalAuthDefaultRole = "guest";

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setApiTimeout(int apiTimeout) {
			this.apiTimeout = apiTimeout;
			return this;
		}

		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder setExternalAuthEnabled(boolean externalAuthEnabled) {
			this.externalAuthEnabled = externalAuthEnabled;
			return this;
		}

		public Builder setExternalAuthTrustedProxyCidrs(String... externalAuthTrustedProxyCidrs) {
			this.externalAuthTrustedProxyCidrs = externalAuthTrustedProxyCidrs;
			return this;
		}

		public Builder setExternalAuthDefaultRole(String externalAuthDefaultRole) {
			this.externalAuthDefaultRole = externalAuthDefaultRole;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
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

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public int port() {
		return this.builder.port;
	}

	@Override
	public int apiTimeout() {
		return this.builder.apiTimeout;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public boolean externalAuthEnabled() {
		return this.builder.externalAuthEnabled;
	}

	@Override
	public String[] externalAuthTrustedProxyCidrs() {
		return this.builder.externalAuthTrustedProxyCidrs;
	}

	@Override
	public String externalAuthUserIdHeader() {
		return this.builder.externalAuthUserIdHeader;
	}

	@Override
	public String externalAuthUserNameHeader() {
		return this.builder.externalAuthUserNameHeader;
	}

	@Override
	public String externalAuthRoleHeader() {
		return this.builder.externalAuthRoleHeader;
	}

	@Override
	public String externalAuthDefaultRole() {
		return this.builder.externalAuthDefaultRole;
	}

}
