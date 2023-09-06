package io.openems.edge.controller.io.channelsinglethreshold;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.CUMULATED_SECONDS;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import io.openems.common.channel.Level;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerIoChannelSingleThreshold extends Controller, OpenemsComponent {



	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		AWAITING_HYSTERESIS(Doc.of(Level.INFO) //
				.text("Would change State, but hystesis is active")),

		CUMULATED_ACTIVE_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //
		
		REGULATION_ACTIVE(Doc.of(BOOLEAN))
		; //

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
	 * Internal method to set the 'nextValue' on {@link ChannelId#REGULATION_ACTIVE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRegulationActive(boolean value) {
		this.getRegulationActiveChannel().setNextValue(value);
	}
	
	/**
	 * Gets the actual regulation state. See {@link ChannelId#REGULATION_ACTIVE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRegulationActive() {
		return this.getRegulationActiveChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#REGULATION_ACTIVE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getRegulationActiveChannel() {
		return this.channel(ChannelId.REGULATION_ACTIVE);
	}	
	
	/**
	 * Gets the Channel for {@link ChannelId#AWAITING_HYSTERESIS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getAwaitingHysteresisChannel() {
		return this.channel(ChannelId.AWAITING_HYSTERESIS);
	}

	/**
	 * Gets the Run-Failed State. See {@link ChannelId#AWAITING_HYSTERESIS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getAwaitingHysteresis() {
		return this.getAwaitingHysteresisChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AWAITING_HYSTERESIS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAwaitingHysteresis(boolean value) {
		this.getAwaitingHysteresisChannel().setNextValue(value);
	}

}
