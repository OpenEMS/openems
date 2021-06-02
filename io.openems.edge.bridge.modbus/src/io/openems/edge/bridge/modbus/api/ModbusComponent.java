package io.openems.edge.bridge.modbus.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.bridge.modbus.jsonrpc.ModbusRegistersExportXlsxRequest;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;

/**
 * A OpenEMS Component that uses Modbus communication.
 * 
 * <p>
 * Classes implementing this interface typically inherit
 * {@link AbstractOpenemsModbusComponent}.
 * 
 * <p>
 * This {@link JsonApi} expects handling of
 * {@link ModbusRegistersExportXlsxRequest} JSON-RPC Requests.
 */
@ProviderType
public interface ModbusComponent extends OpenemsComponent, JsonApi {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// TODO
//		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
//				.debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
//				.text("RS485 Communication to external device failed")); //
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

//	/**
//	 * Gets the Channel for {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
//	 * 
//	 * @return the Channel
//	 */
//	public StateChannel getSlaveCommunicationFailedChannel();
//
//	/**
//	 * Gets the Slave Communication Failed State. See
//	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
//	 * 
//	 * @return the Channel {@link Value}
//	 */
//	public default Value<Boolean> getSlaveCommunicationFailed() {
//		return this.getSlaveCommunicationFailedChannel().value();
//	}
//
//	/**
//	 * Internal method to set the 'nextValue' on
//	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED} Channel.
//	 * 
//	 * @param value the next value
//	 */
//	public default void _setSlaveCommunicationFailed(boolean value) {
//		this.getSlaveCommunicationFailedChannel().setNextValue(value);
//	}

}
