package io.openems.edge.common.sum;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.TestUtils;

/**
 * Provides a simple, simulated Sum component that can be used together with the
 * OpenEMS Component test framework.
 */
public class DummySum extends AbstractDummyOpenemsComponent<DummySum> implements Sum, OpenemsComponent {

	public DummySum() {
		super(Sum.SINGLETON_COMPONENT_ID, Sum.SINGLETON_SERVICE_PID, //
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

	/**
	 * Set {@link Sum.ChannelId#PRODUCTION_AC_ACTIVE_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummySum withProductionAcActivePower(int value) {
		TestUtils.withValue(this, Sum.ChannelId.PRODUCTION_AC_ACTIVE_POWER, value);
		return this.self();
	}

}
