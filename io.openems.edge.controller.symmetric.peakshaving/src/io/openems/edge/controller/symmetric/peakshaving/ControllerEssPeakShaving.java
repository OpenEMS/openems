package io.openems.edge.controller.symmetric.peakshaving;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.Controller.ChannelId;

public interface ControllerEssPeakShaving extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MULTI_USE_STATE(Doc.of(MultiUseState.values()) //
				.text("The current state if multi use is allowed"));
		
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
	 * Gets the Channel for {@link ChannelId#RUN_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMultiUseStateChannel() {
		return this.channel(ChannelId.MULTI_USE_STATE);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RUN_FAILED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void setMultiUseState(MultiUseState state) {
		this.getRunFailedChannel().setNextValue(state);
	}

}
