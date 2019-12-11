package io.openems.edge.thermometer.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
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
		TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS));

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
	 * Gets the Temperature in [degree celsius].
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getTemperature() {
		return this.channel(ChannelId.TEMPERATURE);
	}

}
