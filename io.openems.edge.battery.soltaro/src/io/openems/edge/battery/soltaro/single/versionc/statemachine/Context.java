package io.openems.edge.battery.soltaro.single.versionc.statemachine;

import io.openems.edge.battery.soltaro.single.versionc.BatterySoltaroSingleRackVersionC;
import io.openems.edge.battery.soltaro.single.versionc.Config;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatterySoltaroSingleRackVersionC> {

	protected final Config config;

	public Context(BatterySoltaroSingleRackVersionC parent, Config config) {
		super(parent);
		this.config = config;
	}
}