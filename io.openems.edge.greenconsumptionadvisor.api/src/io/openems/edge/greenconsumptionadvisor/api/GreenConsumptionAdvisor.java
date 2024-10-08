package io.openems.edge.greenconsumptionadvisor.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface GreenConsumptionAdvisor extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		
		/**
		 * Consumption Advice.
		 *
		 * <p>
		 * Consumption Advice based on the associated CO2 emissions per kWh taken from the grid.
		 */
		CONSUMPTION_ADVICE(Doc.of(ConsumptionAdvice.values()) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)),
		
		/**
		 * CO2-Emissions per kWh taken from the grid.
		 *
		 * <p>
		 * Associated CO2 emissions per kWh taken from the grid.
		 */
		CO2_EMISSIONS_GRAM_PER_KILOWATTHOUR(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH));

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
	 * Gets the Channel for {@link ChannelId#CONSUMPTION_ADVICE}.
	 *
	 * @return the Channel
	 */
	public default Channel<ConsumptionAdvice> getConsumptionAdviceChannel() {
		return this.channel(ChannelId.CONSUMPTION_ADVICE);
	}

	/**
	 * Gets the current consumption advice. See {@link ChannelId#CONSUMPTION_ADVICE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default ConsumptionAdvice getConsumptionAdvice() {
		return this.getConsumptionAdviceChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CONSUMPTION_ADVICE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionAdvice(ConsumptionAdvice value) {
		this.getConsumptionAdviceChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CO2_EMISSIONS_GRAM_PER_KILOWATTHOUR}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getCo2EmissionsPerKilowatthourChannel() {
		return this.channel(ChannelId.CO2_EMISSIONS_GRAM_PER_KILOWATTHOUR);
	}

	/**
	 * Gets the current conumption advice. See {@link ChannelId#CO2_EMISSIONS_GRAM_PER_KILOWATTHOUR}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCo2EmissionsPerKilowatthour() {
		return this.getCo2EmissionsPerKilowatthourChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CO2_EMISSIONS_GRAM_PER_KILOWATTHOUR} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCo2EmissionsPerKilowatthour(int value) {
		this.getCo2EmissionsPerKilowatthourChannel().setNextValue(value);
	}
}
