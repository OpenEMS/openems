package io.openems.edge.battery.soltaro.single.versionc.statemachine;

import io.openems.edge.battery.soltaro.single.versionc.Config;
import io.openems.edge.battery.soltaro.single.versionc.SingleRackVersionC;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<SingleRackVersionC> {

	protected final Config config;

	public Context(SingleRackVersionC parent, Config config) {
		super(parent);
		this.config = config;
	}
}