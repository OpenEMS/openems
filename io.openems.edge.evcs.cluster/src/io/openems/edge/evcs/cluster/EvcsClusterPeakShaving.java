package io.openems.edge.evcs.cluster;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.MetaEvcs;

public interface EvcsClusterPeakShaving extends MetaEvcs, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		EVCS_CLUSTER_STATUS(Doc.of(EvcsClusterStatus.values()) //
				.text("Status calculated from all given Evcss.")),
		EVCS_BLOCKED_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //
		MAXIMUM_POWER_TO_DISTRIBUTE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).text("Maximum power to distribute, for all given Evcss.")),
		MAXIMUM_AVAILABLE_ESS_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).text("Maximum available ess power.")),
		MAXIMUM_AVAILABLE_GRID_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).text("Maximum available grid power.")),
		USED_ESS_MAXIMUM_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)
				.text("Dynamic maximum discharge power, that could be limited by us to ensure the possibility to discharge the battery."));

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
	 * Gets the Channel for {@link ChannelId#EVCS_CLUSTER_STATUS}.
	 *
	 * @return the Channel
	 */
	public default Channel<EvcsClusterStatus> getEvcsClusterStatusChannel() {
		return this.channel(ChannelId.EVCS_CLUSTER_STATUS);
	}

	/**
	 * Gets the Status of the EVCS charging station. See
	 * {@link ChannelId#EVCS_CLUSTER_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default EvcsClusterStatus getEvcsClusterStatus() {
		return this.getEvcsClusterStatusChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EVCS_CLUSTER_STATUS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEvcsClusterStatus(EvcsClusterStatus value) {
		this.getEvcsClusterStatusChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EVCS_BLOCKED_CHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEvcsBlockedChargePowerChannel() {
		return this.channel(ChannelId.EVCS_BLOCKED_CHARGE_POWER);
	}

	/**
	 * Gets the Charge Power in [W]. See
	 * {@link ChannelId#EVCS_BLOCKED_CHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEvcsBlockedChargePower() {
		return this.getEvcsBlockedChargePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EVCS_BLOCKED_CHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEvcsBlockedChargePower(Integer value) {
		this.getEvcsBlockedChargePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EVCS_BLOCKED_CHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEvcsBlockedChargePower(int value) {
		this.getEvcsBlockedChargePowerChannel().setNextValue(value);
	}
}
