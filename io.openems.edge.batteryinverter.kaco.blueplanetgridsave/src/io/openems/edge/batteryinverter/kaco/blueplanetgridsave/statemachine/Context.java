package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import java.time.Clock;
import java.time.Instant;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.BatteryInverterKacoBlueplanetGridsaveImpl;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryInverterKacoBlueplanetGridsaveImpl> {

	protected final Battery battery;
	protected final int setActivePower;
	protected final int setReactivePower;
	protected final Clock clock;

	private static final int TIMEOUT = 240; // [s]

	public Context(BatteryInverterKacoBlueplanetGridsaveImpl parent, Battery battery, int setActivePower,
			int setReactivePower, Clock clock) {
		super(parent);
		this.battery = battery;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
		this.clock = clock;
	}

	protected boolean isTimeout(Instant now, Instant entryAt) {
		return now.minusSeconds(TIMEOUT).isAfter(entryAt);
	}
}