package io.openems.edge.bridge.modbus.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface BridgeModbus extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CYCLE_TIME_IS_TOO_SHORT(Doc.of(Level.INFO) //
				.debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		EXECUTION_DURATION(Doc.of(OpenemsType.LONG) //
				.unit(Unit.MILLISECONDS)//
				.persistencePriority(PersistencePriority.HIGH));

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
	 * Gets the Channel for {@link ChannelId#CYCLE_TIME_IS_TOO_SHORT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getCycleTimeIsTooShortChannel() {
		return this.channel(ChannelId.CYCLE_TIME_IS_TOO_SHORT);
	}

	/**
	 * Gets the Cycle-Time-is-too-short State. See
	 * {@link ChannelId#CYCLE_TIME_IS_TOO_SHORT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCycleTimeIsTooShort() {
		return this.getCycleTimeIsTooShortChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CYCLE_TIME_IS_TOO_SHORT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCycleTimeIsTooShort(boolean value) {
		this.getCycleTimeIsTooShortChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EXECUTION_DURATION}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getExecutionDurationChannel() {
		return this.channel(ChannelId.EXECUTION_DURATION);
	}

	/**
	 * Gets the Execution Duration in [ms], see
	 * {@link ChannelId#EXECUTION_DURATION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getExecutionDuration() {
		return this.getExecutionDurationChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EXECUTION_DURATION} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setExecutionDuration(long value) {
		this.getExecutionDurationChannel().setNextValue(value);
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

	/**
	 * The Modbus Bridge marks defective Components, e.g. if there are communication
	 * failures. If a component is marked as defective, reads and writes are paused
	 * for an increasing waiting time. This method resets the waiting time, causing
	 * the Modbus Bridge to retry if a Component is not anymore defective.
	 * 
	 * <p>
	 * Use this method if there is good reason that a Modbus Component should be
	 * available again 'now', e.g. because it was turned on manually.
	 * 
	 * @param sourceId the unique source identifier
	 */
	public void retryModbusCommunication(String sourceId);

}
