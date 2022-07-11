package io.openems.edge.controller.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

/**
 * Provides a simple, simulated {@link Controller} component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyController extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private Runnable runCallback = null;

	protected DummyController(String id, String alias,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, alias, true);
	}

	public DummyController(String id) {
		this(id, "");
	}

	public DummyController(String id, String alias) {
		this(id, alias, //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values() //
		);
	}

	/**
	 * Set callback for applyPower() of this {@link DummyController}.
	 *
	 * @param callback the callback
	 * @return myself
	 */
	public DummyController withRunCallback(Runnable callback) {
		this.runCallback = callback;
		return this;
	}

	@Override
	public void run() {
		if (this.runCallback != null) {
			this.runCallback.run();
		}
	}
}
