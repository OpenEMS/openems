package io.openems.edge.ruhfass.battery.batcon;

import static io.openems.common.utils.ConfigUtils.generateReferenceTargetFilter;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.ruhfass.battery.batcon.enums.BatteryType;
import io.openems.edge.ruhfass.battery.batcon.enums.RemainingBusSimulationCommand;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private String modbusId = null;
		private int modbusUnitId;
		private StartStopConfig startStop;
		private RemainingBusSimulationCommand remainingBusSimulationCommand;
		private BatteryType batteryType;

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

		public Builder setStartStop(StartStopConfig startStop) {
			this.startStop = startStop;
			return this;
		}

		public Builder setRemainingBusSimulationCommand(RemainingBusSimulationCommand remainingBusSimulationCommand) {
			this.remainingBusSimulationCommand = remainingBusSimulationCommand;
			return this;
		}

		public Builder setBatteryType(BatteryType batteryType) {
			this.batteryType = batteryType;
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
	public String Modbus_target() {
		return generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

	@Override
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public StartStopConfig startStop() {
		return this.builder.startStop;
	}

	@Override
	public RemainingBusSimulationCommand remainingBusSimulation() {
		return this.builder.remainingBusSimulationCommand;
	}

	@Override
	public BatteryType batteryType() {
		return this.builder.batteryType;
	}
}