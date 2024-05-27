package io.openems.edge.controller.api.modbus.rtu;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface ModbusRtuApi extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UNABLE_TO_START(Doc.of(Level.FAULT) //
				.text("Unable to start Modbus/TCP-Api Server")), //
		COMPONENT_NO_MODBUS_API_FAULT(Doc.of(Level.FAULT) //
				.text("A configured Component does not support Modbus-API")), //
		COMPONENT_MISSING_FAULT(Doc.of(Level.FAULT) //
				.debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("A configured Component is not available")), //
		PROCESS_IMAGE_FAULT(Doc.of(Level.FAULT) //
				.debounce(50, Debounce.FALSE_VALUES_IN_A_ROW_TO_SET_FALSE) //
				.text("Invalid Modbus Function call. Only FC3, FC4, FC6 and FC16 are supported"));

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
	 * Gets the Channel for {@link ChannelId#UNABLE_TO_START}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getUnableToStartChannel() {
		return this.channel(ChannelId.UNABLE_TO_START);
	}

	/**
	 * Gets the Unable to Start Fault State. See {@link ChannelId#UNABLE_TO_START}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getUnableToStart() {
		return this.getUnableToStartChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#UNABLE_TO_START}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setUnableToStart(boolean value) {
		this.getUnableToStartChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PROCESS_IMAGE_FAULT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getProcessImageFaultChannel() {
		return this.channel(ChannelId.PROCESS_IMAGE_FAULT);
	}

	/**
	 * Gets the Unable to Start Fault State. See
	 * {@link ChannelId#PROCESS_IMAGE_FAULT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getProcessImageFault() {
		return this.getProcessImageFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PROCESS_IMAGE_FAULT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProcessImageFault(boolean value) {
		this.getProcessImageFaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMPONENT_NO_MODBUS_API_FAULT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getComponentNoModbusApiFaultChannel() {
		return this.channel(ChannelId.COMPONENT_NO_MODBUS_API_FAULT);
	}

	/**
	 * Gets the Component No Modbus-API Fault State. See
	 * {@link ChannelId#COMPONENT_NO_MODBUS_API_FAULT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getComponentNoModbusApiFault() {
		return this.getComponentNoModbusApiFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COMPONENT_NO_MODBUS_API_FAULT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setComponentNoModbusApiFault(boolean value) {
		this.getComponentNoModbusApiFaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMPONENT_MISSING_FAULT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getComponentMissingFaultChannel() {
		return this.channel(ChannelId.COMPONENT_MISSING_FAULT);
	}

	/**
	 * Gets the Component Missing Fault State. See
	 * {@link ChannelId#COMPONENT_MISSING_FAULT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getComponentMissingFault() {
		return this.getComponentMissingFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COMPONENT_MISSING_FAULT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setComponentMissingFault(boolean value) {
		this.getComponentMissingFaultChannel().setNextValue(value);
	}
}
