package io.openems.edge.powercontrolunit.api;

import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface PowerControlUnit extends OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Holds the maximum active power feed of the grid. This value is defined like follows.
		 *
		 * <ul>
		 * <li>Interface: PowerControlUnit
		 * <li>Type: Long
		 * <li>Unit: Watt
		 * <li>Range: zero or positive value
		 * </ul>
		 */
        MAX_BUY_FROM_GRID_LIMIT(Doc.of(OpenemsType.LONG)//
                .unit(Unit.WATT)//
                .persistencePriority(PersistencePriority.HIGH)),
		
		/**
		 * Holds the maximum active power feed exported to the grid. This value is defined like follows.
		 *
		 * <ul>
		 * <li>Interface: PowerControlUnit
		 * <li>Type: Long
		 * <li>Unit: Watt
		 * <li>Range: zero or positive value
		 * </ul>
		 */
        MAX_SELL_TO_GRID_LIMIT(Doc.of(OpenemsType.LONG)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)),

        /**
         * Holds the maximum reactive power feed exported to the grid. This value is defined like follows.
         *
         * <ul>
         * <li>Interface: PowerControlUnit
         * <li>Type: Long
         * <li>Unit: Watt
         * <li>Range: zero or positive value
         * </ul>
         */
        MAX_REACTIVE_POWER_LIMIT(Doc.of(OpenemsType.LONG)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)//
				.persistencePriority(PersistencePriority.HIGH));



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
	 * Gets the Channel for {@link ChannelId#MAX_BUY_FROM_GRID_LIMIT}.
	 *
	 * @return the Channel
	 */
	default LongReadChannel getMaxBuyFromGridLimitChannel() {
		return this.channel(ChannelId.MAX_BUY_FROM_GRID_LIMIT);
	}


	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BUY_FROM_GRID_LIMIT} Channel.
	 *
	 * @param value the next value in {@link long}
	 */
	default void _setMaxBuyFromGridLimit(long value) {
		this.getMaxBuyFromGridLimitChannel().setNextValue(value);
	}

	default Value<Long> getMaxBuyFromGridLimit() {
		return this.getMaxBuyFromGridLimitChannel().value();
	}


	/**
	 * Gets the Channel for {@link ChannelId#MAX_BUY_FROM_GRID_LIMIT}.
	 *
	 * @return the Channel
	 */
	default LongReadChannel getMaxSellToGridLimitChannel() {
		return this.channel(ChannelId.MAX_SELL_TO_GRID_LIMIT);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BUY_FROM_GRID_LIMIT} Channel.
	 *
	 * @param value the next value in {@link long}
	 */
	default void _setMaxSellToGridLimit(long value) {
		this.getMaxSellToGridLimitChannel().setNextValue(value);
	}

	default Value<Long> getMaxSellToGridLimit() {
		return this.getMaxSellToGridLimitChannel().value();
	}

}
