package io.openems.edge.io.siemenslogo;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String alias = ""; // Default to empty string
		private boolean enabled = true; // Default to true
		private String modbusId;
		private int modbusUnitId;
		private int modbusOffsetWriteAddress;
		private int modbusOffsetReadAddress;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setAlias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		public Builder setModbusUnitId(int modbusUnitId) {
			this.modbusUnitId = modbusUnitId;
			return this;
		}

		public Builder setModbusOffsetWriteAddress(int modbusOffsetWriteAddress) {
			this.modbusOffsetWriteAddress = modbusOffsetWriteAddress;
			return this;
		}

		public Builder setModbusOffsetReadAddress(int modbusOffsetReadAddress) {
			this.modbusOffsetReadAddress = modbusOffsetReadAddress;
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
	public String id() {
		return this.builder.id;
	}

	@Override
	public String alias() {
		return this.builder.alias;
	}

	@Override
	public boolean enabled() {
		return this.builder.enabled;
	}

	@Override
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public int modbusOffsetWriteAddress() {
		return this.builder.modbusOffsetWriteAddress;
	}

	@Override
	public int modbusOffsetReadAddress() {
		return this.builder.modbusOffsetReadAddress;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}
}
