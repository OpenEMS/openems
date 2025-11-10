package io.openems.edge.ess.generic.common;

import static io.openems.common.channel.Unit.CUMULATED_SECONDS;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface RuntimeChannels extends OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CUMULATED_TIME_OK_STATE(Doc.of(INTEGER) //
				.unit(CUMULATED_SECONDS) //
				.text("Accumulated time the ess spent in OK state")), //
		CUMULATED_TIME_INFO_STATE(Doc.of(INTEGER) //
				.unit(CUMULATED_SECONDS) //
				.text("Accumulated time the ess spent in info state")), //
		CUMULATED_TIME_WARNING_STATE(Doc.of(INTEGER) //
				.unit(CUMULATED_SECONDS) //
				.text("Accumulated time the ess spent in warning state")), //
		CUMULATED_TIME_FAULT_STATE(Doc.of(INTEGER) //
				.unit(CUMULATED_SECONDS) //
				.text("Accumulated time the ess spent in fault state")), //
		;

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
	 * Gets the Channel for
	 * {@link RuntimeChannels.ChannelId#CUMULATED_TIME_OK_STATE}.
	 *
	 * @return the Channel
	 */
	default Channel<Integer> getCumulatedTimeOkStateChannel() {
		return this.channel(ChannelId.CUMULATED_TIME_OK_STATE);
	}

	/**
	 * Gets the Channel value for
	 * {@link RuntimeChannels.ChannelId#CUMULATED_TIME_OK_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Integer> getCumulatedTimeOkStateValue() {
		return this.getCumulatedTimeOkStateChannel().value();
	}

	/**
	 * Gets the Channel for
	 * {@link RuntimeChannels.ChannelId#CUMULATED_TIME_INFO_STATE}.
	 *
	 * @return the Channel
	 */
	default Channel<Integer> getCumulatedTimeInfoStateChannel() {
		return this.channel(ChannelId.CUMULATED_TIME_INFO_STATE);
	}

	/**
	 * Gets the Channel value for
	 * {@link RuntimeChannels.ChannelId#CUMULATED_TIME_INFO_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Integer> getCumulatedTimeInfoStateValue() {
		return this.getCumulatedTimeInfoStateChannel().value();
	}

	/**
	 * Gets the Channel for
	 * {@link RuntimeChannels.ChannelId#CUMULATED_TIME_WARNING_STATE}.
	 *
	 * @return the Channel
	 */
	default StateChannel getCumulatedTimeWarningStateChannel() {
		return this.channel(ChannelId.CUMULATED_TIME_INFO_STATE);
	}

	/**
	 * Gets the StateChannel value for
	 * {@link RuntimeChannels.ChannelId#CUMULATED_TIME_WARNING_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Boolean> getCumulatedTimeWarningStateValue() {
		return this.getCumulatedTimeWarningStateChannel().value();
	}

	/**
	 * Gets the Channel for
	 * {@link RuntimeChannels.ChannelId#CUMULATED_TIME_FAULT_STATE}.
	 *
	 * @return the Channel
	 */
	default StateChannel getCumulatedTimeFaultStateChannel() {
		return this.channel(ChannelId.CUMULATED_TIME_FAULT_STATE);
	}

	/**
	 * Gets the StateChannel value for
	 * {@link RuntimeChannels.ChannelId#CUMULATED_TIME_FAULT_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Boolean> getCumulatedTimeFaultStateValue() {
		return this.getCumulatedTimeFaultStateChannel().value();
	}
}
