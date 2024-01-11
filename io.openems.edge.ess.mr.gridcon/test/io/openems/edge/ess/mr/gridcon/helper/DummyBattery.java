package io.openems.edge.ess.mr.gridcon.helper;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.battery.test.AbstractDummyBattery;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;

public class DummyBattery extends AbstractDummyBattery<DummyBattery> implements Battery {

	public static final int DEFAULT_SOC = 50;
	public static final int DEFAULT_SOH = 99;
	public static final int DEFAULT_CAPACITY = 50_000;
	public static final int DEFAULT_MIN_CELL_VOLTAGE = 3280;
	public static final int DEFAULT_MAX_CELL_VOLTAGE = 3380;
	public static final int DEFAULT_MIN_CELL_TEMPERATURE = 25;
	public static final int DEFAULT_MAX_CELL_TEMPERATURE = 33;

	public static final int DEFAULT_VOLTAGE = 800;
	public static final int DEFAULT_CURRENT = 0;
	public static final int DEFAULT_MAX_CHARGE_CURRENT = 80;
	public static final int DEFAULT_MAX_DISCHARGE_CURRENT = 60;

	public static final int DEFAULT_MIN_VOLTAGE = 700;
	public static final int DEFAULT_MAX_VOLTAGE = 900;

	public DummyBattery() {
		super("battery0", //
				OpenemsComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Battery.ChannelId.values(), //
				BatteryProtection.ChannelId.values());

		this.withMinCellVoltage(DEFAULT_MIN_CELL_VOLTAGE);
		this.withMaxCellVoltage(DEFAULT_MAX_CELL_VOLTAGE);
		this.withMinCellTemperature(DEFAULT_MIN_CELL_TEMPERATURE);
		this.withMaxCellTemperature(DEFAULT_MAX_CELL_TEMPERATURE);
		this.withSoc(DEFAULT_SOC);
		this.withSoh(DEFAULT_SOH);
		this.withCapacity(DEFAULT_CAPACITY);
		this.withVoltage(DEFAULT_VOLTAGE);
		this.withCurrent(DEFAULT_CURRENT);
		this.withChargeMaxCurrent(DEFAULT_MAX_CHARGE_CURRENT);
		this.withDischargeMaxCurrent(DEFAULT_MAX_DISCHARGE_CURRENT);
		this.withDischargeMinVoltage(DEFAULT_MIN_VOLTAGE);
		this.withChargeMaxVoltage(DEFAULT_MAX_VOLTAGE);
	}

	@Override
	protected DummyBattery self() {
		return this;
	}

}
