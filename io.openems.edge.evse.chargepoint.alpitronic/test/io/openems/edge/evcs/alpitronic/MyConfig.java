package io.openems.edge.evcs.alpitronic;

import static io.openems.common.utils.ConfigUtils.generateReferenceTargetFilter;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evse.chargepoint.alpitronic.enums.Connector;
import io.openems.edge.meter.api.PhaseRotation;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {

		private String id;
		private String modbusId;
		private int modbusUnitId;
		private int minHwPower;
		private int maxHwPower;
		private Connector connector;
		private PhaseRotation phaseRotation = PhaseRotation.L1_L2_L3;

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

		public Builder setMinHwPower(int minHwPower) {
			this.minHwPower = minHwPower;
			return this;
		}

		public Builder setMaxHwPower(int maxHwPower) {
			this.maxHwPower = maxHwPower;
			return this;
		}

		public Builder setConnector(Connector connector) {
			this.connector = connector;
			return this;
		}

		public Builder setPhaseRotation(PhaseRotation phaseRotation) {
			this.phaseRotation = phaseRotation;
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
	public boolean debugMode() {
		return false;
	}

	@Override
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public String Modbus_target() {
		return generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

	@Override
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public int minHwPower() {
		return this.builder.minHwPower;
	}

	@Override
	public int maxHwPower() {
		return this.builder.maxHwPower;
	}

	@Override
	public Connector connector() {
		return this.builder.connector;
	}

	@Override
	public PhaseRotation phaseRotation() {
		return this.builder.phaseRotation;
	}
}