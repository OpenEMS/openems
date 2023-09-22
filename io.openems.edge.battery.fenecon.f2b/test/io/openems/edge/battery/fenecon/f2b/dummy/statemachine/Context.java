package io.openems.edge.battery.fenecon.f2b.dummy.statemachine;

import io.openems.edge.battery.fenecon.f2b.dummy.BatteryFeneconF2bDummyImpl;
import io.openems.edge.battery.fenecon.f2b.dummy.Config;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryFeneconF2bDummyImpl> {
	public final ComponentManager componentManager;
	public final Config config;

	public Context(BatteryFeneconF2bDummyImpl parent, ComponentManager componentManager, Config config) {
		super(parent);
		this.componentManager = componentManager;
		this.config = config;
	}
}