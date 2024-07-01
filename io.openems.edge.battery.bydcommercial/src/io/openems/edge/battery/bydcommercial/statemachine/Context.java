package io.openems.edge.battery.bydcommercial.statemachine;

import io.openems.edge.battery.bydcommercial.BydBatteryBoxCommercialC130;
import io.openems.edge.battery.bydcommercial.Config;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BydBatteryBoxCommercialC130> {

	protected final Config config;

	public Context(BydBatteryBoxCommercialC130 parent, Config config) {
		super(parent);
		this.config = config;
	}
}