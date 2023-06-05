package io.openems.edge.ess.fenecon.commercial40.charger;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfigPV1 extends AbstractComponentConfig implements ConfigPv1 {

	protected static class Builder {
		private String id = null;
		private String modbusId = null;
		public int maxActualPower;
		public String essId;

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

		public MyConfigPV1 build() {
			return new MyConfigPV1(this);
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

	private MyConfigPV1(Builder builder) {
		super(ConfigPv1.class, builder.id);
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