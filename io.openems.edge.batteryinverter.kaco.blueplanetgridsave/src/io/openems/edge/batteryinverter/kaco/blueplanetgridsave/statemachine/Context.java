package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.Config;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoBlueplanetGridsave;

public class Context {
	protected final KacoBlueplanetGridsave component;
	protected final Battery battery;
	protected final Config config;
	protected final int setActivePower;
	protected final int setReactivePower;

	public Context(KacoBlueplanetGridsave component, Battery battery, Config config, int setActivePower,
			int setReactivePower) {
		this.component = component;
		this.battery = battery;
		this.config = config;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
	}
}