package io.openems.edge.io.shelly.common.gen2;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DynamicStateChannelDoc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.dynamicdoctext.ParameterProvider;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TextProvider;
import io.openems.edge.io.shelly.common.HttpBridgeShellyService;

public interface IoGen2ShellyBase extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Slave Communication Failed Fault.
		 *
		 * <ul>
		 * <li>Interface: Shelly
		 * <li>Type: State
		 * </ul>
		 */
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT)), //

		/**
		 * Model name reported by this shelly device.
		 *
		 * <ul>
		 * <li>Interface: Shelly
		 * <li>Type: String
		 * </ul>
		 */
		SHELLY_MODEL_NAME(Doc.of(OpenemsType.STRING)), //

		/**
		 * Detected wrong shelly device type.
		 *
		 * <ul>
		 * <li>Interface: Shelly
		 * <li>Type: State
		 * </ul>
		 */
		WRONG_DEVICE_TYPE(DynamicStateChannelDoc.builder(Level.FAULT)//
				.setDynamicText(//
						TextProvider.byTranslation(HttpBridgeShellyService.class, "IoShelly.WrongDeviceType"), //
						ParameterProvider.byChannel(ChannelId.SHELLY_MODEL_NAME), //
						ParameterProvider.byComponentData(IoGen2ShellyBase.class, //
								x -> String.join(", ", x.getSupportedShellyDeviceTypes()))//
				).build());

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
	 * Returns a list of supported shelly device types (checked with 'app'
	 * identifier).
	 * 
	 * @return Array of strings
	 */
	public String[] getSupportedShellyDeviceTypes();

	/**
	 * Gets the Channel for {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getSlaveCommunicationFailedChannel() {
		return this.channel(ChannelId.SLAVE_COMMUNICATION_FAILED);
	}

	/**
	 * Gets the Slave Communication Failed State. See
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getSlaveCommunicationFailed() {
		return this.getSlaveCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSlaveCommunicationFailed(boolean value) {
		this.getSlaveCommunicationFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SHELLY_MODEL_NAME}.
	 *
	 * @return the Channel
	 */
	public default StringReadChannel getShellyModelNameChannel() {
		return this.channel(ChannelId.SHELLY_MODEL_NAME);
	}

	/**
	 * Gets the discovered shelly model name. See
	 * {@link ChannelId#SHELLY_MODEL_NAME}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<String> getShellyModelName() {
		return this.getShellyModelNameChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SHELLY_MODEL_NAME}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void setShellyModelName(String value) {
		this.getShellyModelNameChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#WRONG_DEVICE_TYPE}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getWrongDeviceTypeChannel() {
		return this.channel(ChannelId.WRONG_DEVICE_TYPE);
	}

	/**
	 * Gets the Slave Communication Failed State. See
	 * {@link ChannelId#WRONG_DEVICE_TYPE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getWrongDeviceType() {
		return this.getWrongDeviceTypeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#WRONG_DEVICE_TYPE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWrongDeviceType(boolean value) {
		this.getWrongDeviceTypeChannel().setNextValue(value);
	}
}
