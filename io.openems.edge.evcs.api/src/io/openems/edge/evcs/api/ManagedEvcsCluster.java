package io.openems.edge.evcs.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;

@ProviderType
public interface ManagedEvcsCluster extends Evcs {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Maximum power allowed to distribute for all evcs in this cluster.")),
		EVCS_COUNT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.text("Connect EVCS on this cluster.")),
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
	 * Gets the Channel for {@link ChannelId#MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getMaximumAllowedPowerToDistributeChannel() {
		return this.channel(ChannelId.MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE);
	}

	/**
	 * Gets the maximum allowed power to distribute in [W]. See
	 * {@link ChannelId#MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaximumAllowedPowerToDistribute() {
		return this.getMaximumAllowedPowerToDistributeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaximumAllowedPowerToDistribute(Integer value) {
		this.getMaximumAllowedPowerToDistributeChannel().setNextValue(value);
	}

	/**
	 * Sets maximum allowed power to distribute in [W] on
	 * {@link ChannelId#MAXIMUM_ALLOWED_POWER_TO_DISTRIBUTE} Channel.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setMaximumAllowedPowerToDistribute(Integer value) throws OpenemsNamedException {
		this.getMaximumAllowedPowerToDistributeChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EVCS_COUNT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEvcsCountChannel() {
		return this.channel(ChannelId.EVCS_COUNT);
	}

	/**
	 * Gets the evcs count. See
	 * {@link ChannelId#EVCS_COUNT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEvcsCount() {
		return this.getEvcsCountChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EVCS_COUNT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEvcsCount(Integer value) {
		this.getEvcsCountChannel().setNextValue(value);
	}

}
