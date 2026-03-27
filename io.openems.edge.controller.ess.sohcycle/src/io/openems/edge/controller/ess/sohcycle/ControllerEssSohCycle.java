package io.openems.edge.controller.ess.sohcycle;

import static io.openems.common.channel.PersistencePriority.HIGH;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.sohcycle.statemachine.StateMachine;

public interface ControllerEssSohCycle extends Controller, OpenemsComponent {
	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(StateMachine.State.values())
				.persistencePriority(HIGH)
				.text("The current state of the State Machine.")),
		MEASURED_CAPACITY(Doc.of(OpenemsType.LONG)
				.unit(Unit.WATT_HOURS)
				.persistencePriority(HIGH)
				.text("The last measured capacity of the ESS in Wh.")),
		SOH_PERCENT(Doc.of(OpenemsType.INTEGER)
				.unit(Unit.PERCENT)
				.persistencePriority(HIGH)
				.text("The last measured State of Health (SoH) of the ESS in percent 0-100%.")),
		SOH_RAW_DEBUG(Doc.of(OpenemsType.FLOAT)
				.unit(Unit.PERCENT)
				.persistencePriority(HIGH)
				.text("The last measured State of Health (SoH) of the ESS in percent. It may be over 100%."
						+ "This value is intended for debugging purposes.")),
		VOLTAGE_DELTA(Doc.of(OpenemsType.INTEGER)
				.unit(Unit.VOLT)
				.persistencePriority(HIGH)
				.text("The voltage delta used to detect a full charge.")),
		IS_BATTERY_BALANCED(Doc.of(BatteryBalanceStatus.values())
				.initialValue(BatteryBalanceStatus.NOT_MEASURED)
				.persistencePriority(HIGH)
				.text("Indicates whether the battery cells are sufficiently balanced.")),
		IS_MEASURED(new BooleanDoc()
				.initialValue(false)
				.persistencePriority(HIGH)
				.text("Indicates whether the battery balance check was performed and a result is available.")),
		BALANCING_DELTA_MV_DEBUG(Doc.of(OpenemsType.LONG)
				.unit(Unit.MILLIVOLT)
				.persistencePriority(HIGH)
				.text("The calculated cell voltage delta (max - min) in millivolts. "
						+ "Persisted for debugging long-running cycles. Set to null if delta cannot be calculated.")),
		BALANCING_ERROR_DEBUG(Doc.of(BatteryBalanceError.values())
				.initialValue(BatteryBalanceError.NONE)
				.persistencePriority(HIGH)
				.text("Error/reason for the battery balance check result. Helps diagnose why "
						+ "NOT_MEASURED or NOT_BALANCED status occurred."));

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
