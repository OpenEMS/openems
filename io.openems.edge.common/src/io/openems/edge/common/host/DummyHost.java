package io.openems.edge.common.host;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.TestUtils;

/**
 * Simulates a {@link Host} for the OpenEMS Component test framework.
 */
public class DummyHost extends AbstractDummyOpenemsComponent<DummyHost> implements Host {

	public DummyHost() {
		super("_host", //
				OpenemsComponent.ChannelId.values(), //
				Host.ChannelId.values());
	}

	@Override
	protected DummyHost self() {
		return this;
	}

	/**
	 * Set {@link Host.ChannelId#HOSTNAME}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyHost withHostname(String value) {
		TestUtils.withValue(this, Host.ChannelId.HOSTNAME, value);
		return this;
	}

}