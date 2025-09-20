package io.openems.edge.common.test;

import org.osgi.service.component.ComponentContext;

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
		this(id, alias, new DummyComponentContext(), firstInitialChannelIds, furtherInitialChannelIds);
	}

	protected AbstractDummyOpenemsComponent(String id, String alias, ComponentContext context,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		super.activate(context, id, alias, true);
	}

	protected abstract SELF self();
}
