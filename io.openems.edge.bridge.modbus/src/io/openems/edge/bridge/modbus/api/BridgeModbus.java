package io.openems.edge.bridge.modbus.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.Level;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface BridgeModbus extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT)), //
		CYCLE_TIME_IS_TOO_SHORT(Doc.of(Level.WARNING));

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
	 * Adds a Protocol with a source identifier to this Modbus Bridge.
	 * 
	 * @param sourceId the unique source identifier
	 * @param protocol the Modbus Protocol
	 */
	public void addProtocol(String sourceId, ModbusProtocol protocol);

	/**
	 * Removes a Protocol from this Modbus Bridge.
	 * 
	 * @param sourceId the unique source identifier
	 */
	public void removeProtocol(String sourceId);

}
