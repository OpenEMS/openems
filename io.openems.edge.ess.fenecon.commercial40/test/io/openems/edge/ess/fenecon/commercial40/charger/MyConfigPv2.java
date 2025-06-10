package io.openems.edge.ess.fenecon.commercial40.charger;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfigPv2 extends AbstractComponentConfig implements ConfigPv2 {

	protected static class Builder {
		private String id;
		private String modbusId;
		private int maxActualPower;
		private String essId;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		public Builder setMaxActualPower(int maxActualPower) {
			this.maxActualPower = maxActualPower;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public MyConfigPv2 build() {
			return new MyConfigPv2(this);
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

	private MyConfigPv2(Builder builder) {
		super(ConfigPv2.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.builder.modbusId);
	}

	@Override
	public String Ess_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.ess_id());
	}

}