package io.openems.edge.bridge.modbus.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * A OpenEMS Component that uses Modbus communication.
 *
 * <p>
 * Classes implementing this interface typically inherit
 * {@link AbstractOpenemsModbusComponent}.
 */
@ProviderType
public interface ModbusComponent extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MODBUS_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("Modbus Communication failed")) //
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
	 * Gets the Channel for {@link ChannelId#MODBUS_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getModbusCommunicationFailedChannel() {
		return this.channel(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED);
	}

	/**
	 * Gets the Modbus Communication Failed State. See
	 * {@link ChannelId#MODBUS_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getModbusCommunicationFailed() {
		return this.getModbusCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MODBUS_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setModbusCommunicationFailed(boolean value) {
		this.getModbusCommunicationFailedChannel().setNextValue(value);
	}

}
