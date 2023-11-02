package io.openems.edge.ess.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.ess.api.SymmetricEss;

public class DummySymmetricEss extends AbstractOpenemsComponent implements SymmetricEss {

	public DummySymmetricEss(String id) {
		this(id, //
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values() //
		);
	}

	protected DummySymmetricEss(String id, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#SOC} of this {@link SymmetricEss}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummySymmetricEss withSoc(Integer value) {
		TestUtils.withValue(this, SymmetricEss.ChannelId.SOC, value);
		return this;
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#CAPACITY} of this {@link SymmetricEss}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummySymmetricEss withCapacity(Integer value) {
		TestUtils.withValue(this, SymmetricEss.ChannelId.CAPACITY, value);
		return this;
	}

}
