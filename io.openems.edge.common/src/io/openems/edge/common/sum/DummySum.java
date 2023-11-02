package io.openems.edge.common.sum;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;

/**
 * Provides a simple, simulated Sum component that can be used together with the
 * OpenEMS Component test framework.
 */
public class DummySum extends AbstractDummyOpenemsComponent<DummySum> implements Sum, OpenemsComponent {

	public DummySum() {
		super(Sum.SINGLETON_COMPONENT_ID, //
				OpenemsComponent.ChannelId.values(), //
				Sum.ChannelId.values());
	}

	@Override
	protected DummySum self() {
		return this;
	}

	@Override
	public void updateChannelsBeforeProcessImage() {
		// nothing here
	}

}
