package io.openems.edge.bridge.modbus.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;

@ProviderType
public interface BridgeModbusSerial extends BridgeModbus {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("RS485 Communication to external device failed")); //

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
	 * Gets the Port-Name (e.g. '/dev/ttyUSB0' or 'COM3').
	 * 
	 * @return the Port-Name
	 */
	public String getPortName();

	/**
	 * Gets the Baudrate (e.g. 9600).
	 * 
	 * @return the Baudrate
	 */
	public int getBaudrate();

	/**
	 * Gets the Databits (e.g. 8).
	 * 
	 * @return the Databits
	 */
	public int getDatabits();

	/**
	 * Gets the Stopbits.
	 * 
	 * @return the Stopbits
	 */
	public Stopbit getStopbits();

	/**
	 * Gets the Parity.
	 * 
	 * @return the Parity.
	 */
	public Parity getParity();

	/**
	 * Gets the Channel for {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getSlaveCommunicationFailedChannel() {
		return this.channel(ChannelId.SLAVE_COMMUNICATION_FAILED);
	}
}
