package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import io.openems.edge.battery.soltaro.single.versionb.Config;
import io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionBImpl;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<SingleRackVersionBImpl> {

	protected final Config config;

	public Context(SingleRackVersionBImpl parent, Config config) {
		super(parent);
		this.config = config;
	}

}