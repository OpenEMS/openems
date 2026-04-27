package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import java.time.Clock;
import java.time.Duration;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.BatteryInverterKacoBlueplanetGridsaveImpl;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.errorrestart.ErrorRestartBehaviour;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryInverterKacoBlueplanetGridsaveImpl> {

	public static final Duration TIMEOUT = Duration.ofSeconds(240);

	public final Battery battery;
	protected final int setActivePower;
	protected final int setReactivePower;
	protected final Clock clock;
	protected final ErrorRestartBehaviour errorRestartBehaviour;

	public Context(BatteryInverterKacoBlueplanetGridsaveImpl parent, Battery battery, int setActivePower,
			int setReactivePower, Clock clock, ErrorRestartBehaviour errorRestartBehaviour) {
		super(parent);
		this.battery = battery;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
		this.clock = clock;
		this.errorRestartBehaviour = errorRestartBehaviour;
	}

}