package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import io.openems.edge.battery.soltaro.cluster.versionc.ClusterVersionC;
import io.openems.edge.battery.soltaro.cluster.versionc.Config;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<ClusterVersionC> {

	protected final Config config;

	public Context(ClusterVersionC parent, Config config) {
		super(parent);
		this.config = config;
	}
}