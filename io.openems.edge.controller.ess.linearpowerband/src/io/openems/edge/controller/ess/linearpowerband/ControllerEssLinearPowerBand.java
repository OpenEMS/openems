package io.openems.edge.controller.ess.linearpowerband;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssLinearPowerBand extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		TARGET_POWER(Doc.of(OpenemsType.INTEGER)) //
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
	 * Gets the Channel for {@link ChannelId#TARGET_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTargetPowerChannel() {
		return this.channel(ChannelId.TARGET_POWER);
	}

	/**
	 * Gets the Target Power in [W]. See {@link ChannelId#TARGET_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTargetPower() {
		return this.getTargetPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TARGET_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTargetPower(Integer value) {
		this.getTargetPowerChannel().setNextValue(value);
	}

}
