package io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine;

import java.time.Clock;
import java.util.List;

import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.battery.fenecon.f2b.cluster.common.BatteryFeneconF2bCluster;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryFeneconF2bCluster> {

	protected final Clock clock;
	protected final List<BatteryFeneconF2b> batteries;

	public Context(BatteryFeneconF2bCluster component, Clock clock, List<BatteryFeneconF2b> batteries) {
		super(component);
		this.clock = clock;
		this.batteries = batteries;
	}
}