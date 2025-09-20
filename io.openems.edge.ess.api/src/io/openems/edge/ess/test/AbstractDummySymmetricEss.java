package io.openems.edge.ess.test;

import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.ess.api.SymmetricEss;

public abstract class AbstractDummySymmetricEss<SELF extends AbstractDummySymmetricEss<?>>
		extends AbstractDummyOpenemsComponent<SELF> implements SymmetricEss {

	protected AbstractDummySymmetricEss(String id, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#MAX_APPARENT_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withMaxApparentPower(int value) {
		TestUtils.withValue(this, SymmetricEss.ChannelId.MAX_APPARENT_POWER, value);
		return this.self();
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#SOC}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withSoc(Integer value) {
		TestUtils.withValue(this, SymmetricEss.ChannelId.SOC, value);
		return this.self();
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#CAPACITY}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withCapacity(Integer value) {
		TestUtils.withValue(this, SymmetricEss.ChannelId.CAPACITY, value);
		return this.self();
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#GRID_MODE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withGridMode(GridMode value) {
		TestUtils.withValue(this, SymmetricEss.ChannelId.GRID_MODE, value);
		return this.self();
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#ACTIVE_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withActivePower(Integer value) {
		TestUtils.withValue(this, SymmetricEss.ChannelId.ACTIVE_POWER, value);
		return this.self();
	}

}
