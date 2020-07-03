package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import io.openems.edge.battery.soltaro.cluster.versionc.ClusterVersionC;
import io.openems.edge.battery.soltaro.cluster.versionc.Config;

public class Context {
	protected final ClusterVersionC component;
	protected final Config config;

	public Context(ClusterVersionC component, Config config) {
		super();
		this.component = component;
		this.config = config;
	}
}