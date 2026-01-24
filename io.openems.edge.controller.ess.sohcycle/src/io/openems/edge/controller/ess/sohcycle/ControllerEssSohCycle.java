package io.openems.edge.controller.ess.sohcycle;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.sohcycle.statemachine.StateMachine;

public interface ControllerEssSohCycle extends Controller, OpenemsComponent {
	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(StateMachine.State.values())
				.text("The current state of the State Machine.")),
		MEASURED_CAPACITY(Doc.of(OpenemsType.LONG)
				.unit(Unit.WATT_HOURS)
				.text("The last measured capacity of the ESS in Wh.")),
		VOLTAGE_DELTA(Doc.of(OpenemsType.INTEGER)
				.unit(Unit.VOLT)
				.text("The voltage delta used to detect a full charge.")),
		IS_BATTERY_BALANCED(Doc.of(BatteryBalanceStatus.values())
				.initialValue(BatteryBalanceStatus.NOT_MEASURED)
				.text("Indicates whether the battery cells are sufficiently balanced."));

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#MEASURED_CAPACITY}.
	 *
	 * @return the Channel
	 */
	default Channel<Long> getMeasuredCapacityChannel() {
		return this.channel(ChannelId.MEASURED_CAPACITY);
	}
}
