package io.openems.edge.goodwe.stsbox;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.MultiplexingMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String modbusId;
		private int modbusUnitId;
		private String gensetId;
		private MultiplexingMode portMultiplexingMode;
		private int ratedPower;
		private int preheatingTimeSeconds;
		private int runtime;
		private EnableDisable enableCharge;
		private int chargeSocStart;
		private int chargeSocEnd;
		private int maxPowerPercent;
		private int voltageUpperLimit;
		private int voltageLowerLimit;
		private int frequencyUpperLimit;
		private int frequencyLowerLimit;

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

		public Builder setGensetId(String gensetId) {
			this.gensetId = gensetId;
			return this;
		}

		public Builder setPortMultiplexingMode(MultiplexingMode portMultiplexingMode) {
			this.portMultiplexingMode = portMultiplexingMode;
			return this;
		}

		public Builder setRatedPower(int ratedPower) {
			this.ratedPower = ratedPower;
			return this;
		}

		public Builder setPreheatingTimeSeconds(int preheatingTimeSeconds) {
			this.preheatingTimeSeconds = preheatingTimeSeconds;
			return this;
		}

		public Builder setRuntime(int runtime) {
			this.runtime = runtime;
			return this;
		}

		public Builder setEnableCharge(EnableDisable enableCharge) {
			this.enableCharge = enableCharge;
			return this;
		}

		public Builder setChargeSocStart(int chargeSocStart) {
			this.chargeSocStart = chargeSocStart;
			return this;
		}

		public Builder setChargeSocEnd(int chargeSocEnd) {
			this.chargeSocEnd = chargeSocEnd;
			return this;
		}

		public Builder setMaxPowerPercent(int maxPowerPercent) {
			this.maxPowerPercent = maxPowerPercent;
			return this;
		}

		public Builder setVoltageUpperLimit(int voltageUpperLimit) {
			this.voltageUpperLimit = voltageUpperLimit;
			return this;
		}

		public Builder setVoltageLowerLimit(int voltageLowerLimit) {
			this.voltageLowerLimit = voltageLowerLimit;
			return this;
		}

		public Builder setFrequencyUpperLimit(int frequencyUpperLimit) {
			this.frequencyUpperLimit = frequencyUpperLimit;
			return this;
		}

		public Builder setFrequencyLowerLimit(int frequencyLowerLimit) {
			this.frequencyLowerLimit = frequencyLowerLimit;
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
	public String genset_id() {
		return this.builder.gensetId;
	}

	@Override
	public MultiplexingMode portMultiplexingMode() {
		return this.builder.portMultiplexingMode;
	}

	@Override
	public int ratedPower() {
		return this.builder.ratedPower;
	}

	@Override
	public int preheatingTime() {
		return this.builder.preheatingTimeSeconds;
	}

	@Override
	public int runtime() {
		return this.builder.runtime;
	}

	@Override
	public EnableDisable enableCharge() {
		return this.builder.enableCharge;
	}

	@Override
	public int chargeSocStart() {
		return this.builder.chargeSocStart;
	}

	@Override
	public int chargeSocEnd() {
		return this.builder.chargeSocEnd;
	}

	@Override
	public int maxPowerPercent() {
		return this.builder.maxPowerPercent;
	}

	@Override
	public int voltageUpperLimit() {
		return this.builder.voltageUpperLimit;
	}

	@Override
	public int voltageLowerLimit() {
		return this.builder.voltageLowerLimit;
	}

	@Override
	public int frequencyUpperLimit() {
		return this.builder.frequencyUpperLimit;
	}

	@Override
	public int frequencyLowerLimit() {
		return this.builder.frequencyLowerLimit;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}
}
