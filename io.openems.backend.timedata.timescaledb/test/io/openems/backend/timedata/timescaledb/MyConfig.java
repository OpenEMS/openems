package io.openems.backend.timedata.timescaledb;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String host;
		private int port;
		private String user;
		private String password;
		private String database;
		private boolean isReadOnly;
		private int poolSize;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setHost(String host) {
			this.host = host;
			return this;
		}

		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		public Builder setUser(String user) {
			this.user = user;
			return this;
		}

		public Builder setPassword(String password) {
			this.password = password;
			return this;
		}

		public Builder setDatabase(String database) {
			this.database = database;
			return this;
		}

		public Builder setReadOnly(boolean isReadOnly) {
			this.isReadOnly = isReadOnly;
			return this;
		}

		public Builder setPoolSize(int poolSize) {
			this.poolSize = poolSize;
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
	public String host() {
		return this.builder.host;
	}

	@Override
	public int port() {
		return this.builder.port;
	}

	@Override
	public String user() {
		return this.builder.user;
	}

	@Override
	public String password() {
		return this.builder.password;
	}

	@Override
	public String database() {
		return this.builder.database;
	}

	@Override
	public boolean isReadOnly() {
		return this.builder.isReadOnly;
	}

	@Override
	public int poolSize() {
		return this.builder.poolSize;
	}

}
