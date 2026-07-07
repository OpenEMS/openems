package io.openems.edge.controller.ess.balancing;

import java.util.Optional;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssBalancing extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		DEBUG_SET_GRID_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		SET_GRID_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_GRID_ACTIVE_POWER)) //
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
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_GRID_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	default Channel<Integer> getDebugSetGridActivePowerChannel() {
		return this.channel(ChannelId.DEBUG_SET_GRID_ACTIVE_POWER);
	}

	/**
	 * Gets the {@link Value} for {@link ChannelId#DEBUG_SET_GRID_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Integer> getDebugSetGridActivePower() {
		return this.getDebugSetGridActivePowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_GRID_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	default WriteChannel<Integer> getSetGridActivePowerChannel() {
		return this.channel(ChannelId.SET_GRID_ACTIVE_POWER);
	}

	/**
	 * Gets the {@link Value} for {@link ChannelId#SET_GRID_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Optional<Integer> getSetGridActivePowerNextWriteValue() {
		return this.getSetGridActivePowerChannel().getNextWriteValue();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SET_GRID_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	default void _setGridActivePower(int value) {
		this.getSetGridActivePowerChannel().setNextValue(value);
	}

}