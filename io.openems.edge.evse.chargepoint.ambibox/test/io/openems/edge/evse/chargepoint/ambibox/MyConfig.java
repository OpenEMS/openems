package io.openems.edge.evse.chargepoint.ambibox;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evse.chargepoint.ambibox.enums.Connector;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String modbusId;
		private int modbusUnitId;
		private boolean readOnly;
		private Connector connector;
		private int maxHwPower;

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

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public Builder setConnector(Connector connector) {
			this.connector = connector;
			return this;
		}

		public Builder setMaxHwPower(int maxHwPower) {
			this.maxHwPower = maxHwPower;
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
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public boolean readOnly() {
		return this.builder.readOnly;
	}

	@Override
	public Connector connector() {
		return this.builder.connector;
	}

	@Override
	public int maxHwPower() {
		return this.builder.maxHwPower;
	}
}
