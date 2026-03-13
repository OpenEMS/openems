package io.openems.edge.bridge.modbus.ascii;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.startstop.StartStoppable;

/**
 * Interface for Modbus/ASCII Serial Bridge.
 *
 * <p>
 * Modbus ASCII uses a different frame format than RTU:
 * <ul>
 * <li>Start delimiter: ':' (colon, 0x3A)</li>
 * <li>End delimiter: CR/LF (0x0D 0x0A)</li>
 * <li>Data encoding: ASCII hex (each byte = 2 ASCII characters)</li>
 * <li>Error checking: LRC (Longitudinal Redundancy Check, 1 byte)</li>
 * </ul>
 */
@ProviderType
public interface BridgeModbusSerialAscii extends BridgeModbus, StartStoppable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Total number of bytes sent over the serial connection.
		 */
		BYTES_SENT(Doc.of(OpenemsType.LONG) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Total bytes sent")),

		/**
		 * Total number of bytes received over the serial connection.
		 */
		BYTES_RECEIVED(Doc.of(OpenemsType.LONG) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Total bytes received")),

		/**
		 * Timestamp of the last successful communication (epoch milliseconds).
		 */
		LAST_SUCCESSFUL_COMMUNICATION(Doc.of(OpenemsType.LONG) //
				.unit(Unit.MILLISECONDS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Timestamp of last successful communication")),

		/**
		 * Total count of communication errors since component activation.
		 */
		COMMUNICATION_ERRORS(Doc.of(OpenemsType.LONG) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Total communication errors")),

		/**
		 * Total count of successful transactions since component activation.
		 */
		SUCCESSFUL_TRANSACTIONS(Doc.of(OpenemsType.LONG) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Total successful transactions"));

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
	 * Gets the Channel for {@link ChannelId#BYTES_SENT}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getBytesSentChannel() {
		return this.channel(ChannelId.BYTES_SENT);
	}

	/**
	 * Gets the total bytes sent. See {@link ChannelId#BYTES_SENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getBytesSent() {
		return this.getBytesSentChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BYTES_SENT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBytesSent(long value) {
		this.getBytesSentChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BYTES_RECEIVED}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getBytesReceivedChannel() {
		return this.channel(ChannelId.BYTES_RECEIVED);
	}

	/**
	 * Gets the total bytes received. See {@link ChannelId#BYTES_RECEIVED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getBytesReceived() {
		return this.getBytesReceivedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BYTES_RECEIVED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBytesReceived(long value) {
		this.getBytesReceivedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#LAST_SUCCESSFUL_COMMUNICATION}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getLastSuccessfulCommunicationChannel() {
		return this.channel(ChannelId.LAST_SUCCESSFUL_COMMUNICATION);
	}

	/**
	 * Gets the timestamp of last successful communication. See
	 * {@link ChannelId#LAST_SUCCESSFUL_COMMUNICATION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getLastSuccessfulCommunication() {
		return this.getLastSuccessfulCommunicationChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#LAST_SUCCESSFUL_COMMUNICATION} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setLastSuccessfulCommunication(long value) {
		this.getLastSuccessfulCommunicationChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMMUNICATION_ERRORS}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getCommunicationErrorsChannel() {
		return this.channel(ChannelId.COMMUNICATION_ERRORS);
	}

	/**
	 * Gets the total communication errors. See
	 * {@link ChannelId#COMMUNICATION_ERRORS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getCommunicationErrors() {
		return this.getCommunicationErrorsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COMMUNICATION_ERRORS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCommunicationErrors(long value) {
		this.getCommunicationErrorsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SUCCESSFUL_TRANSACTIONS}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getSuccessfulTransactionsChannel() {
		return this.channel(ChannelId.SUCCESSFUL_TRANSACTIONS);
	}

	/**
	 * Gets the total successful transactions. See
	 * {@link ChannelId#SUCCESSFUL_TRANSACTIONS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getSuccessfulTransactions() {
		return this.getSuccessfulTransactionsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SUCCESSFUL_TRANSACTIONS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSuccessfulTransactions(long value) {
		this.getSuccessfulTransactionsChannel().setNextValue(value);
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
	 * Gets the ABL compatibility flag.
	 *
	 * @return the ABL compatibility flag.
	 */
	public boolean ablCompatible();
}
