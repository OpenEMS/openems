package io.openems.edge.battery.statemachine;

import io.openems.edge.battery.dummy.Config;
import io.openems.edge.battery.dummy.DummyBatteryImpl;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<DummyBatteryImpl> {
	public final ComponentManager componentManager;
	public final Config config;
	
	public Context(DummyBatteryImpl parent, ComponentManager componentManager, Config config) {
		super(parent);
		this.componentManager = componentManager;
		this.config = config;
	}
}