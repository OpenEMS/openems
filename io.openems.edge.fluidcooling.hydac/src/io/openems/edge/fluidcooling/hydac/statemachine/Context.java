package io.openems.edge.fluidcooling.hydac.statemachine;

import java.time.Clock;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.fluidcooling.hydac.HydacImpl;

public class Context extends AbstractContext<HydacImpl> {

	protected final Clock clock;

	public Context(HydacImpl parent, Clock clock) {
		super(parent);
		this.clock = clock;
	}

}