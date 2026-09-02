package io.openems.edge.phoenixcontact.plcnext.meter;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private MeterType meterType = MeterType.PRODUCTION;
		private String namespaceVariables = "";

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

		public Builder setNamespaceVariables(String namespace) {
			this.namespaceVariables = namespace;
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
	public MeterType type() {
		return this.builder.meterType;
	}

	@Override
	public String baseUrl() {
		return "https://jUnit";
	}

	@Override
	public String pathAuthApi() {
		return "/v1.3/auth";
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
		return "OpenEmsInstance4JUnitMeter.";
	}

	@Override
	public String namespaceVariables() {
		return this.builder.namespaceVariables;
	}
}