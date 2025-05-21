package io.openems.edge.controller.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.controller.api.Controller;

/**
 * Provides a simple, simulated {@link Controller} component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyController extends AbstractDummyOpenemsComponent<DummyController>
		implements Controller, OpenemsComponent {

	private Runnable runCallback = null;

	public DummyController(String id) {
		this(id, "");
	}

	public DummyController(String id, String alias) {
		super(id, alias, //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values());
	}

	@Override
	protected DummyController self() {
		return this;
	}

	/**
	 * Set callback for applyPower().
	 *
	 * @param callback the callback
	 * @return myself
	 */
	public DummyController setRunCallback(Runnable callback) {
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
