package io.openems.edge.controller.api.backend;

import java.net.Proxy.Type;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.controller.api.backend.api.BackendFeature;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String apikey;
		private String uri;
		private String proxyAddress;
		private int proxyPort;
		private Type proxyType;
		private int apiTimeout;
		private PersistencePriority persistencePriority;
		private PersistencePriority aggregationPriority;
		private PersistencePriority resendPriority;
		private boolean debugMode;
		private boolean useDefaultFeatures = true;
		private BackendFeature[] supportedFeatures = new BackendFeature[0];

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setApikey(String apikey) {
			this.apikey = apikey;
			return this;
		}

		public Builder setUri(String uri) {
			this.uri = uri;
			return this;
		}

		public Builder setProxyAddress(String proxyAddress) {
			this.proxyAddress = proxyAddress;
			return this;
		}

		public Builder setProxyPort(int proxyPort) {
			this.proxyPort = proxyPort;
			return this;
		}

		public Builder setProxyType(Type proxyType) {
			this.proxyType = proxyType;
			return this;
		}

		public Builder setApiTimeout(int apiTimeout) {
			this.apiTimeout = apiTimeout;
			return this;
		}

		public Builder setPersistencePriority(PersistencePriority persistencePriority) {
			this.persistencePriority = persistencePriority;
			return this;
		}

		public Builder setAggregationPriority(PersistencePriority aggregationPriority) {
			this.aggregationPriority = aggregationPriority;
			return this;
		}

		public Builder setResendPriority(PersistencePriority resendPriority) {
			this.resendPriority = resendPriority;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder setUseDefaultFeatures(boolean useDefaultFeatures) {
			this.useDefaultFeatures = useDefaultFeatures;
			return this;
		}

		public Builder setSupportedFeatures(BackendFeature... supportedFeatures) {
			this.supportedFeatures = supportedFeatures;
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
	public String apikey() {
		return this.builder.apikey;
	}

	@Override
	public String uri() {
		return this.builder.uri;
	}

	@Override
	public String proxyAddress() {
		return this.builder.proxyAddress;
	}

	@Override
	public int proxyPort() {
		return this.builder.proxyPort;
	}

	@Override
	public Type proxyType() {
		return this.builder.proxyType;
	}

	@Override
	public int apiTimeout() {
		return this.builder.apiTimeout;
	}

	@Override
	public PersistencePriority persistencePriority() {
		return this.builder.persistencePriority;
	}

	@Override
	public PersistencePriority aggregationPriority() {
		return this.builder.aggregationPriority;
	}

	@Override
	public PersistencePriority resendPriority() {
		return this.builder.resendPriority;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public boolean useDefaultFeatures() {
		return this.builder.useDefaultFeatures;
	}

	@Override
	public BackendFeature[] supportedFeatures() {
		return this.builder.supportedFeatures;
	}

}