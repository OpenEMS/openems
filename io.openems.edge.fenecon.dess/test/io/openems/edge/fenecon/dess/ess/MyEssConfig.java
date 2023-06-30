package io.openems.edge.fenecon.dess.ess;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyEssConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id = null;
		private String modbusId = null;

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

		public MyEssConfig build() {
			return new MyEssConfig(this);
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

	private MyEssConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

}