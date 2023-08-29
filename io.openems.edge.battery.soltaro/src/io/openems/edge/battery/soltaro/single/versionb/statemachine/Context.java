package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import java.util.Optional;

import io.openems.edge.battery.soltaro.single.versionb.BatterySoltaroSingleRackVersionBImpl;
import io.openems.edge.battery.soltaro.single.versionb.Config;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatterySoltaroSingleRackVersionBImpl> {

	protected final Config config;
	public final Optional<Integer> numberOfModules;

	public Context(BatterySoltaroSingleRackVersionBImpl parent, Config config, Optional<Integer> numberOfSlaves) {
		super(parent);
		this.config = config;
		this.numberOfModules = numberOfSlaves;
	}

}