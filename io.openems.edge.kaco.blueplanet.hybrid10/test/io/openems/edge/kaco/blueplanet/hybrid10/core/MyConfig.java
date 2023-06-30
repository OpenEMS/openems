package io.openems.edge.kaco.blueplanet.hybrid10.core;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String serialnumber;
		private String ip;
		private String userkey;
		private String identkey;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setIp(String ip) {
			this.ip = ip;
			return this;
		}

		public Builder setSerialnumber(String serialnumber) {
			this.serialnumber = serialnumber;
			return this;
		}

		public Builder setUserkey(String userkey) {
			this.userkey = userkey;
			return this;
		}

		public Builder setIdentkey(String identkey) {
			this.identkey = identkey;
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
	public String serialnumber() {
		return this.builder.serialnumber;
	}

	@Override
	public String ip() {
		return this.builder.ip;
	}

	@Override
	public String userkey() {
		return this.builder.userkey;
	}

	@Override
	public String identkey() {
		return this.builder.identkey;
	}

}