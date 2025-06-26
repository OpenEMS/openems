package io.openems.edge.fenecon.dess.charger;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyChargerConfig extends AbstractComponentConfig implements Config1, Config2 {

	protected static class Builder {
		private String id = null;
		private String modbusId = null;
		private String essId = null;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		public MyChargerConfig build() {
			return new MyChargerConfig(this);
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

	private MyChargerConfig(Builder builder) {
		super(Config1.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.builder.modbusId);
	}

	@Override
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public String ess_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.builder.essId);
	}

}