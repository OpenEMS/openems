package io.openems.edge.meter.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.meter.api.SinglePhaseMeter;

/**
 * Provides a simple, simulated {@link SinglePhaseMeter} ElectricityMeter
 * component that can be used together with the OpenEMS Component test
 * framework.
 */
public class DummySinglePhaseElectricityMeter extends AbstractDummyElectricityMeter<DummySinglePhaseElectricityMeter>
		implements ElectricityMeter, SinglePhaseMeter {

	private SinglePhase phase = SinglePhase.L1;

	public DummySinglePhaseElectricityMeter(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SinglePhaseMeter.ChannelId.values() //
		);
	}

	@Override
	protected DummySinglePhaseElectricityMeter self() {
		return this;
	}

	/**
	 * Set the {@link SinglePhase}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummySinglePhaseElectricityMeter withPhase(SinglePhase phase) {
		this.phase = phase;
		return this.self();
	}

	@Override
	public SinglePhase getPhase() {
		return this.phase;
	}

}