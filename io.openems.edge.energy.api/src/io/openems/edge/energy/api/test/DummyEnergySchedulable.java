package io.openems.edge.energy.api.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;

/**
 * Provides a simple, simulated {@link EnergySchedulable} component that can be
 * used together with the OpenEMS Component test framework.
 */
public class DummyEnergySchedulable extends AbstractDummyEnergySchedulable<DummyEnergySchedulable>
		implements EnergySchedulable, OpenemsComponent {

	private final EnergyScheduleHandler esh;

	public DummyEnergySchedulable(String id, EnergyScheduleHandler esh) {
		super(id, //
				OpenemsComponent.ChannelId.values() //
		);
		this.esh = esh;
	}

	@Override
	protected final DummyEnergySchedulable self() {
		return this;
	}

	@Override
	public EnergyScheduleHandler getEnergyScheduleHandler() {
		return this.esh;
	}
}
