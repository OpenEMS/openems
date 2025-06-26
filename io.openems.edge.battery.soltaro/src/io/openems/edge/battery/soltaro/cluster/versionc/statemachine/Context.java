package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import io.openems.edge.battery.soltaro.cluster.versionc.BatterySoltaroClusterVersionC;
import io.openems.edge.battery.soltaro.cluster.versionc.Config;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatterySoltaroClusterVersionC> {

	protected final Config config;

	public Context(BatterySoltaroClusterVersionC parent, Config config) {
		super(parent);
		this.config = config;
	}
}