package io.openems.edge.timeofusetariff.luox;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.DebugMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id;
		private String backendOAuthClientIdentifier;
		private boolean useTestApi;
		private String accessToken;
		private String refreshToken;
		private DebugMode debugMode;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setBackendOAuthClientIdentifier(String backendOAuthClientIdentifier) {
			this.backendOAuthClientIdentifier = backendOAuthClientIdentifier;
			return this;
		}

		public Builder setUseTestApi(boolean useTestApi) {
			this.useTestApi = useTestApi;
			return this;
		}

		public Builder setAccessToken(String accessToken) {
			this.accessToken = accessToken;
			return this;
		}

		public Builder setRefreshToken(String refreshToken) {
			this.refreshToken = refreshToken;
			return this;
		}
		
		public Builder setDebugMode(DebugMode debugMode) {
			this.debugMode = debugMode;
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
	public String backendOAuthClientIdentifier() {
		return this.builder.backendOAuthClientIdentifier;
	}

	@Override
	public String accessToken() {
		return this.builder.accessToken;
	}

	@Override
	public String refreshToken() {
		return this.builder.refreshToken;
	}

	@Override
	public DebugMode debugMode() {
		return this.builder.debugMode;
	}
}