package io.openems.edge.thermometer.esera.onewire;



import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.PersistencePriority.LOW;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.thermometer.esera.onewire.enums.OwdStatus;



public interface EseraOneWireThermometer extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		

		
		OWD_STATUS(Doc.of(OwdStatus.values()).accessMode(AccessMode.READ_ONLY)), //
		
		/**
		 * Temperature on OneWireDevice 1.
		 *
		 * <ul>
		 * <li>Interface: ModbusThermometer
		 * <li>Type: Integer
		 * <li>Unit: degree celsius
		 * </ul>
		 */
		OWD_READ_FAILED(Doc.of(OpenemsType.BOOLEAN).persistencePriority(HIGH)),		
		
		
        TEMPERATURE_OWD(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        
        

        TEMPERATURE_OWD_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(LOW));
		

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
	 * Gets the Channel for {@link ChannelId#OWD_STATUS}.
	 *
	 * @return the Channel
	 */
	public default Channel<OwdStatus> getOwdStatusChannel() {
		return this.channel(ChannelId.OWD_STATUS);
	}

	/**
	 * Gets the PCS Mode. See {@link ChannelId#OWD_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default OwdStatus getOwdStatus() {
		return this.getOwdStatusChannel().value().asEnum();
	}

	/**
	 * Set the PCS Mode. See {@link ChannelId#OWD_STATUS}.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOwdStatus(OwdStatus value) throws OpenemsNamedException {
		this.getOwdStatusChannel().setNextValue(value);
	}	
	
	/**
	 * Gets the Channel for {@link ChannelId#OWD_READ_FAILED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOwdReadFailedChannel() {
		return this.channel(ChannelId.OWD_READ_FAILED);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#OWD_READ_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getOwdReadFailed() {
		return this.getOwdReadFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#OWD_READ_FAILED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setOwdReadFailed(Boolean value) {
		this.getOwdReadFailedChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#OWD_READ_FAILED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setOwdReadFailed(boolean value) {
		this.getOwdReadFailedChannel().setNextValue(value);
	}	
	
	
	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwdChannel() {
		return this.channel(ChannelId.TEMPERATURE_OWD);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd() {
		return this.getTemperatureOwdChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd(Integer value) {
		this.getTemperatureOwdChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd(int value) {
		this.getTemperatureOwdChannel().setNextValue(value);
	}
	

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD_DEBUG}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwdDebugChannel() {
		return this.channel(ChannelId.TEMPERATURE_OWD_DEBUG);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD_DEBUG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwdDebug() {
		return this.getTemperatureOwdDebugChannel().value();
	}
	



	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public default ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(EseraOneWireThermometer.class, accessMode, 100) //
				.channel(0, ChannelId.TEMPERATURE_OWD, ModbusType.FLOAT32) //

				.build();
	}

	ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode);







}
