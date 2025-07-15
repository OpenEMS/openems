package io.openems.edge.generator.api;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents a 3-Phase, symmetric CHP
 */
public interface SymmetricGenerator extends   OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		 
		GENRATOR_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		
		GENRATOR_HEAT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
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


	//
	public default IntegerReadChannel getGeneratorActivePowerChannel() {
		return this.channel(ChannelId.GENRATOR_ACTIVE_POWER);
	}

	/**
	 * Gets the Active Power  in [W]. See {@link ChannelId#ACTIVE_POWER_}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGeneratorActivePower() {
		return this.getGeneratorActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_POWER_} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGeneratorActivePower(Integer value) {
		this.getGeneratorActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_POWER_} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGeneratorActivePower(int value) {
		this.getGeneratorActivePowerChannel().setNextValue(value);
	}		
	
	



}
