package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.FeneconHomeBattery;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<FeneconHomeBattery> {

	public Context(FeneconHomeBattery parent) {
		super(parent);
	}

}