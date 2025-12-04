package io.openems.edge.io.phoenixcontact;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;

@SuppressWarnings("all")
public class TestConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private MeterType meterType;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setType(MeterType meterType) {
			this.meterType = meterType;
			return this;
		}

		public TestConfig build() {
			return new TestConfig(this);
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

	private TestConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public MeterType type() {
		return MeterType.PRODUCTION;
	}

	@Override
	public String authUrl() {
		return "https://jUnit/auth";
	}

	@Override
	public String username() {
		return "jUnit";
	}

	@Override
	public String password() {
		return "jUnit";
	}

	@Override
	public String dataUrl() {
		return "https://jUnit/data";
	}

	@Override
	public String[] dataInstanceNames() {
		return new String[]{"defaultOpenEmsInstance4JUnit"};
	}

}