package io.openems.edge.bridge.sml.api;

import org.openmuc.jrxtx.SerialPort;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface BridgeSml {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)), //
		CYCLE_TIME_IS_TOO_SHORT(Doc.of(Level.WARNING) //
				.debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)), //
		EXECUTION_DURATION(Doc.of(OpenemsType.LONG));

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
	 * Add a Task.
	 * 
	 * @param sourceId the Source-ID
	 * @param task     the {@link SmlTask}
	 */
	public void addTask(String sourceId, SmlTask task);

	/**
	 * Get the {@link MBusConnection}.
	 * 
	 * @return the {@link MBusConnection}
	 */
	public SerialPort getSmlConnection();

	/**
	 * Remove the task with the given Source-ID.
	 * 
	 * @param sourceId the Source-ID
	 */
	public void removeTask(String sourceId);
}
