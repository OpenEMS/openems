package io.openems.edge.common.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractDummyOpenemsComponent<SELF extends AbstractDummyOpenemsComponent<?>>
		extends AbstractOpenemsComponent implements OpenemsComponent {

	protected AbstractDummyOpenemsComponent(String id,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		this(id, "", firstInitialChannelIds, furtherInitialChannelIds);
	}

	protected AbstractDummyOpenemsComponent(String id, String alias,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		super.activate(new DummyComponentContext(), id, alias, true);
	}

	protected abstract SELF self();

}
