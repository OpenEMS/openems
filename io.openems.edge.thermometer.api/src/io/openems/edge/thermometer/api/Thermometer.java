package io.openems.edge.thermometer.api;

import org.osgi.annotation.versioning.ProviderType;
import io.openems.common.channel.PersistencePriority;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface Thermometer extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Temperature.
		 *
		 * <ul>
		 * <li>Interface: Thermometer
		 * <li>Type: Integer
		 * <li>Unit: degree celsius
		 * </ul>
		 */
		HUMIDITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)
				.persistencePriority(PersistencePriority.HIGH)),
		TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),

		TEMPERATURE0(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)
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
	 * Gets the Channel for {@link ChannelId#TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureChannel() {
		return this.channel(ChannelId.TEMPERATURE);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperature() {
		return this.getTemperatureChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperature(Integer value) {
		this.getTemperatureChannel().setNextValue(value);
	}

}