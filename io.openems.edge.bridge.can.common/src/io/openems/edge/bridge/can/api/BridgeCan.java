package io.openems.edge.bridge.can.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.can.LogVerbosity;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface BridgeCan extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)), //
		CYCLE_TIME_IS_TOO_SHORT(Doc.of(Level.WARNING) //
				.debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)), //
		EXECUTION_DURATION(Doc.of(OpenemsType.LONG)), //
		COUNT_CAN_MESSAGES_PER_CYCLE(Doc.of(OpenemsType.INTEGER)), //
		COUNT_PROCESSED_CAN_MESSAGES_PER_CYCLE(Doc.of(OpenemsType.INTEGER)), //
		COUNT_READ_TASK_CYCLES(Doc.of(OpenemsType.INTEGER)), //

		STATS_NATIVE_ERROR_COUNT_CYCLIC_SEND(Doc.of(OpenemsType.INTEGER)), //
		STATS_NATIVE_ERROR_COUNT_SEND(Doc.of(OpenemsType.INTEGER)), //
		STATS_NATIVE_ERROR_COUNT_RECEIVE(Doc.of(OpenemsType.INTEGER)), //
		STATS_NATIVE_FRAMES_SEND_PER_CYCLE(Doc.of(OpenemsType.INTEGER)) //

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
	 * Gets the CAN connection.
	 *
	 * @return The {@link CanConnection}
	 * @throws OpenemsException on error
	 */
	public CanConnection getCanConnection() throws OpenemsException;

	/**
	 * Closes the CAN connection.
	 */
	public abstract void closeCanConnection();

	/**
	 * Adds a Protocol with a source identifier to this CAN Bridge.
	 *
	 * @param sourceId the unique source identifier
	 * @param protocol the CAN Protocol to add
	 */
	public void addProtocol(String sourceId, CanProtocol protocol);

	/**
	 * Removes a Protocol from this CAN Bridge.
	 *
	 * @param sourceId the unique source identifier
	 */
	public void removeProtocol(String sourceId);

	/**
	 * Asks if the CAN bridge is in simulation mode.
	 *
	 * @return true, if the CAN bridge is in simulation mode
	 */
	public boolean isSimulationMode();

	/**
	 * After how many errors should a element be invalidated?.
	 *
	 * @return value
	 */
	public int invalidateElementsAfterReadErrors();

	/**
	 * Gets the log verbosity.
	 *
	 * @return The {@link LogVerbosity}
	 */
	public LogVerbosity getLogVerbosity();
}
