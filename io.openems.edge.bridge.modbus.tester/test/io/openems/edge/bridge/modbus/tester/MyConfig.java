package io.openems.edge.bridge.modbus.tester;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String modbusId = null;
		private int modbusUnitId = 1;
		private int registerAddress = 0;
		private RegisterType registerType = RegisterType.FC3_READ_HOLDING_REGISTERS;
		private int registerCount = 1;
		private ModbusProtocolType modbusProtocolType = ModbusProtocolType.TCP;

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

		public Builder setModbusUnitId(int modbusUnitId) {
			this.modbusUnitId = modbusUnitId;
			return this;
		}

		public Builder setRegisterAddress(int registerAddress) {
			this.registerAddress = registerAddress;
			return this;
		}

		public Builder setRegisterType(RegisterType registerType) {
			this.registerType = registerType;
			return this;
		}

		public Builder setRegisterCount(int registerCount) {
			this.registerCount = registerCount;
			return this;
		}

		public Builder setModbusProtocolType(ModbusProtocolType modbusProtocolType) {
			this.modbusProtocolType = modbusProtocolType;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
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

	@Override
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public int registerAddress() {
		return this.builder.registerAddress;
	}

	@Override
	public RegisterType registerType() {
		return this.builder.registerType;
	}

	@Override
	public int registerCount() {
		return this.builder.registerCount;
	}

	@Override
	public ModbusProtocolType modbusProtocolType() {
		return this.builder.modbusProtocolType;
	}

}
