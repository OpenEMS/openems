package io.openems.edge.io.api;

import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

@ProviderType
public interface AnalogVoltageOutput extends AnalogOutput {

	@Override
	default Consumer<Integer> setOutputChannel() {
		return (output) -> {
			try {
				this.setOutputVoltage(output);
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
		};
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Holds writes of the Relay Output for debugging in mV.
		 *
		 * <ul>
		 * <li>Type: Integer
		 * <li>Range: 0 - range().maximum()
		 * <li>Unit: mV
		 * </ul>
		 */
		DEBUG_SET_OUTPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //

		/**
		 * Set Relay Output in Voltage.
		 *
		 * <ul>
		 * <li>Type: Integer
		 * <li>Range: 0 - range().maximum()
		 * <li>Unit: mV
		 * </ul>
		 */
		SET_OUTPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.MILLIVOLT) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_SET_OUTPUT_VOLTAGE));

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
	 * Gets the Channel for {@link ChannelId#SET_OUTPUT_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetOutputVoltageChannel() {
		return this.channel(ChannelId.SET_OUTPUT_VOLTAGE);
	}

	/**
	 * Sets the voltage output value of the AnalogOutput in mV. See
	 * {@link ChannelId#SET_OUTPUT_VOLTAGE}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOutputVoltage(Integer value) throws OpenemsNamedException {
		this.getSetOutputVoltageChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_OUTPUT_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugSetOutputVoltageChannel() {
		return this.channel(ChannelId.DEBUG_SET_OUTPUT_VOLTAGE);
	}

	/**
	 * Gets the set voltage output value of the I/O. See
	 * {@link ChannelId#DEBUG_SET_OUTPUT_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDebugSetOutputVoltage() {
		return this.getDebugSetOutputVoltageChannel().value();
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(AnalogVoltageOutput.class, accessMode, 80) //
				.channel(0, ChannelId.SET_OUTPUT_VOLTAGE, ModbusType.UINT16) //
				.build();
	}
}
