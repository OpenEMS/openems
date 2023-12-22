package io.openems.edge.controller.api.mqtt;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String uri;
		private PersistencePriority persistencePriority;
		private boolean debugMode;
		private String clientId;
		private String username;
		private String password;
		private String certPem;
		private String privateKeyPem;
		private String trustStorePem;
		private String trustStorePassword;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
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
		
		public Builder setCertPem(String certPem) {
			this.certPem = certPem;
			return this;
		}
		
		public Builder setPrivateKeyPem(String privateKeyPem) {
			this.privateKeyPem = privateKeyPem;
			return this;
		}
		
		public Builder setTrustStorePath(String trustStorePem) {
			this.trustStorePem = trustStorePem;
			return this;
		}
		
		public Builder setTrustStorePassword(String trustStorePassword) {
			this.trustStorePassword = trustStorePassword;
			return this;
		}

		public Builder setPersistencePriority(PersistencePriority persistencePriority) {
			this.persistencePriority = persistencePriority;
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
	public String uri() {
		return this.builder.uri;
	}

	@Override
	public PersistencePriority persistencePriority() {
		return this.builder.persistencePriority;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
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
	public String certPem() {
		return this.builder.certPem;
	}
	
	@Override
	public String privateKeyPem() {
		return this.builder.privateKeyPem;
	}
	
	@Override
	public String trustStorePem() {
		return this.builder.trustStorePem;
	}
	
	@Override
	public String trustStorePassword() {
		return this.builder.trustStorePassword;
	}
}