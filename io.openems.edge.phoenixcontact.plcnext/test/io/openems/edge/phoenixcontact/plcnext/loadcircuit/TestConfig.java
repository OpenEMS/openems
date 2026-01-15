package io.openems.edge.phoenixcontact.plcnext.loadcircuit;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class TestConfig extends AbstractComponentConfig
		implements io.openems.edge.phoenixcontact.plcnext.loadcircuit.Config {

	protected static class Builder {
		private String id;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
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
	public String baseUrl() {
		return "https://jUnit";
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
	public String dataInstanceName() {
		return "OpenEmsInstance4JUnitLoadCircuit";
	}

}
