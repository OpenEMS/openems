package io.openems.edge.energy.api.test;

import java.util.function.Function;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;

/**
 * Provides a simple, simulated {@link EnergySchedulable} component that can be
 * used together with the OpenEMS Component test framework.
 */
public class DummyEnergySchedulable<ESH extends EnergyScheduleHandler> extends
		AbstractDummyEnergySchedulable<DummyEnergySchedulable<ESH>> implements EnergySchedulable, OpenemsComponent {

	private final ESH esh;

	public DummyEnergySchedulable(String factoryPid, String componentId, Function<OpenemsComponent, ESH> eshFactory) {
		super(factoryPid, componentId, //
				OpenemsComponent.ChannelId.values());
		this.esh = eshFactory.apply(this);
	}

	@Override
	protected final DummyEnergySchedulable<ESH> self() {
		return this;
	}

	@Override
	public ESH getEnergyScheduleHandler() {
		return this.esh;
	}
}
