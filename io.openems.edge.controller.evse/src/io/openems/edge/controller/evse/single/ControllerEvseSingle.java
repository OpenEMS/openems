package io.openems.edge.controller.evse.single;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.evse.single.statemachine.StateMachine;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;

public interface ControllerEvseSingle extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(StateMachine.State.values())//
				.text("Current State of State-Machine")//
				.persistencePriority(HIGH)), //

		ACTUAL_MODE(Doc.of(Mode.Actual.values())), //
		SESSION_ENERGY(Doc.of(INTEGER)//
				.unit(WATT_HOURS)//
				.persistencePriority(HIGH)) //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Controller {@link Params}.
	 * 
	 * @return the {@link Params}
	 */
	public Params getParams();

	/**
	 * Apply {@link ChargePointActions}.
	 * 
	 * @param actions the {@link ChargePointActions}
	 */
	public void apply(ChargePointActions actions);

	/**
	 * Gets the Channel for {@link ChannelId#SESSION_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSessionEnergyChannel() {
		return this.channel(ChannelId.SESSION_ENERGY);
	}

	/**
	 * Gets the Energy that was charged during the current in [Wh]. See
	 * {@link ChannelId#SESSON_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSessionEnergy() {
		return this.getSessionEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SESSON_ENERGY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSessionEnergy(int value) {
		this.getSessionEnergyChannel().setNextValue(value);
	}
}
