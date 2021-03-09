package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.Config;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoBlueplanetGridsave;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<KacoBlueplanetGridsave> {

	protected final Battery battery;
	protected final Config config;
	protected final int setActivePower;
	protected final int setReactivePower;

	public Context(KacoBlueplanetGridsave parent, Battery battery, Config config, int setActivePower,
			int setReactivePower) {
		super(parent);
		this.battery = battery;
		this.config = config;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
	}
}