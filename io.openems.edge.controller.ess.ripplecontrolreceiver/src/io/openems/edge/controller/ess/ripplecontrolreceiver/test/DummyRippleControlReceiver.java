package io.openems.edge.controller.ess.ripplecontrolreceiver.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.ripplecontrolreceiver.ControllerEssRippleControlReceiver;
import io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel;

/**
 * Dummy implementation of {@link ControllerEssRippleControlReceiver} for testing.
 */
public class DummyRippleControlReceiver extends AbstractDummyOpenemsComponent<DummyRippleControlReceiver>
		implements ControllerEssRippleControlReceiver {

	private EssRestrictionLevel restrictionLevel = EssRestrictionLevel.NO_RESTRICTION;

	public DummyRippleControlReceiver(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssRippleControlReceiver.ChannelId.values());
	}

	@Override
	protected DummyRippleControlReceiver self() {
		return this;
	}

	@Override
	public void run() {
		// Dummy implementation - does nothing
	}

	@Override
	public EssRestrictionLevel essRestrictionLevel() {
		return this.restrictionLevel;
	}

	/**
	 * Sets the restriction level for this dummy instance.
	 *
	 * @param restrictionLevel the {@link EssRestrictionLevel} to set
	 * @return myself for method chaining
	 */
	public DummyRippleControlReceiver withRestrictionLevel(EssRestrictionLevel restrictionLevel) {
		this.restrictionLevel = restrictionLevel;
		return this.self();
	}
}

