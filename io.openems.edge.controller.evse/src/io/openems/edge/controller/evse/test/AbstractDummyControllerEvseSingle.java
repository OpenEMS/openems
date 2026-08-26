package io.openems.edge.controller.evse.test;

import static com.google.common.base.MoreObjects.toStringHelper;

import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.Params;

public abstract class AbstractDummyControllerEvseSingle<SELF extends AbstractDummyControllerEvseSingle<?>>
		extends AbstractDummyOpenemsComponent<SELF> implements ControllerEvseSingle {

	private Params params = null;

	protected AbstractDummyControllerEvseSingle(String id,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	public Params getParams() {
		return this.params;
	}

	/**
	 * Set {@link Params}.
	 *
	 * @param params the {@link Params}
	 * @return myself
	 */
	public SELF withParams(Params params) {
		this.params = params;
		return this.self();
	}

	@Override
	public String toString() {
		return toStringHelper(this) //
				.add("id", this.id()) //
				.toString();
	}
}
