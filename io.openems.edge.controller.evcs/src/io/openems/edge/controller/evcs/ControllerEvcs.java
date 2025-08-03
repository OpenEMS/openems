package io.openems.edge.controller.evcs;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.types.OpenemsType.BOOLEAN;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface ControllerEvcs extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		AWAITING_HYSTERESIS(Doc.of(BOOLEAN) //
				.persistencePriority(HIGH)), //
		EVCS_IS_READ_ONLY(Doc.of(Level.INFO) //
				.translationKey(ControllerEvcs.class, "evcsIsReadOnly")); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public default void setEvcsIsReadOnlyChannel(boolean val) {
		this.getEvcsIsReadOnlyChannel().setNextValue(val);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EVCS_IS_READ_ONLY}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getEvcsIsReadOnlyChannel() {
		return this.channel(ChannelId.EVCS_IS_READ_ONLY);
	}

}
