package io.openems.edge.modbus.thermometer;



import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.PersistencePriority.LOW;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;



public interface ModbusThermometer extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
		
        TEMPERATURE_OWD01(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD02(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD03(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD04(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD05(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD06(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD07(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD08(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD09(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD10(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),

        TEMPERATURE_OWD01_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(LOW)),
        TEMPERATURE_OWD02_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(LOW)),
        TEMPERATURE_OWD03_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(LOW)),
        TEMPERATURE_OWD04_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(LOW)),
        TEMPERATURE_OWD05_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(LOW)),
        TEMPERATURE_OWD06_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(LOW)),
        TEMPERATURE_OWD07_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(LOW)),
        TEMPERATURE_OWD08_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(LOW)),
        TEMPERATURE_OWD09_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(LOW)),
        TEMPERATURE_OWD10_DEBUG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(LOW));		

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
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd1Channel() {
		return this.channel(ChannelId.TEMPERATURE_OWD01);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd1() {
		return this.getTemperatureOwd1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD1}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd1(Integer value) {
		this.getTemperatureOwd1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD1}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd1(int value) {
		this.getTemperatureOwd1Channel().setNextValue(value);
	}
	

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd2Channel() {
		return this.channel(ChannelId.TEMPERATURE_OWD02);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd2() {
		return this.getTemperatureOwd2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD2}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd2(Integer value) {
		this.getTemperatureOwd2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD2}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd2(int value) {
		this.getTemperatureOwd2Channel().setNextValue(value);
	}


	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd3Channel() {
		return this.channel(ChannelId.TEMPERATURE_OWD03);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd3() {
		return this.getTemperatureOwd3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD3}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd3(Integer value) {
		this.getTemperatureOwd3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD3}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd3(int value) {
		this.getTemperatureOwd3Channel().setNextValue(value);
	}


	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD4}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd4Channel() {
		return this.channel(ChannelId.TEMPERATURE_OWD04);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD4}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd4() {
		return this.getTemperatureOwd4Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD4}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd4(Integer value) {
		this.getTemperatureOwd4Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD4}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd4(int value) {
		this.getTemperatureOwd4Channel().setNextValue(value);
	}


	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD5}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd5Channel() {
		return this.channel(ChannelId.TEMPERATURE_OWD05);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD5}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd5() {
		return this.getTemperatureOwd5Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD5}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd5(Integer value) {
		this.getTemperatureOwd5Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD5}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd5(int value) {
		this.getTemperatureOwd5Channel().setNextValue(value);
	}


	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD6}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd6Channel() {
		return this.channel(ChannelId.TEMPERATURE_OWD06);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD6}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd6() {
		return this.getTemperatureOwd6Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD6}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd6(Integer value) {
		this.getTemperatureOwd6Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD6}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd6(int value) {
		this.getTemperatureOwd6Channel().setNextValue(value);
	}


	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD7}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd7Channel() {
		return this.channel(ChannelId.TEMPERATURE_OWD07);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD7}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd7() {
		return this.getTemperatureOwd7Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD7}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd7(Integer value) {
		this.getTemperatureOwd7Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD7}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd7(int value) {
		this.getTemperatureOwd7Channel().setNextValue(value);
	}


	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD8}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd8Channel() {
		return this.channel(ChannelId.TEMPERATURE_OWD08);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD8}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd8() {
		return this.getTemperatureOwd8Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD8}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd8(Integer value) {
		this.getTemperatureOwd8Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD8}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd8(int value) {
		this.getTemperatureOwd8Channel().setNextValue(value);
	}


	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD9}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd9Channel() {
		return this.channel(ChannelId.TEMPERATURE_OWD09);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD9}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd9() {
		return this.getTemperatureOwd9Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD9}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd9(Integer value) {
		this.getTemperatureOwd9Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD9}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd9(int value) {
		this.getTemperatureOwd9Channel().setNextValue(value);
	}


	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD10}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd10Channel() {
		return this.channel(ChannelId.TEMPERATURE_OWD10);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD10}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd10() {
		return this.getTemperatureOwd10Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD10}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd10(Integer value) {
		this.getTemperatureOwd10Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TEMPERATURE_OWD10}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureOwd10(int value) {
		this.getTemperatureOwd10Channel().setNextValue(value);
	}


	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD1_DEBUG}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd1DebugChannel() {
		return this.channel(ChannelId.TEMPERATURE_OWD01_DEBUG);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD1_DEBUG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd1Debug() {
		return this.getTemperatureOwd1DebugChannel().value();
	}

	
	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD2_DEBUG}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd2DebugChannel() {
		return this.channel(ChannelId.TEMPERATURE_OWD02_DEBUG);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD2_DEBUG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd2Debug() {
		return this.getTemperatureOwd2DebugChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD3_DEBUG}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd3DebugChannel() {
		return this.channel(ChannelId.TEMPERATURE_OWD03_DEBUG);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD3_DEBUG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd3Debug() {
		return this.getTemperatureOwd3DebugChannel().value();
	}

	
	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD4_DEBUG}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd4DebugChannel() {
		return this.channel(ChannelId.TEMPERATURE_OWD04_DEBUG);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD4_DEBUG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd4Debug() {
		return this.getTemperatureOwd4DebugChannel().value();
	}

	
	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD5_DEBUG}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd5DebugChannel() {
		return this.channel(ChannelId.TEMPERATURE_OWD05_DEBUG);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD5_DEBUG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd5Debug() {
		return this.getTemperatureOwd5DebugChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD6_DEBUG}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd6DebugChannel() {
		return this.channel(ChannelId.TEMPERATURE_OWD06_DEBUG);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD6_DEBUG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd6Debug() {
		return this.getTemperatureOwd6DebugChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD7_DEBUG}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd7DebugChannel() {
		return this.channel(ChannelId.TEMPERATURE_OWD07_DEBUG);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD7_DEBUG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd7Debug() {
		return this.getTemperatureOwd7DebugChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD8_DEBUG}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd8DebugChannel() {
		return this.channel(ChannelId.TEMPERATURE_OWD08_DEBUG);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD8_DEBUG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd8Debug() {
		return this.getTemperatureOwd8DebugChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD9_DEBUG}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd9DebugChannel() {
		return this.channel(ChannelId.TEMPERATURE_OWD09_DEBUG);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD9_DEBUG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd9Debug() {
		return this.getTemperatureOwd9DebugChannel().value();
	}
	
	
	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD10_DEBUG}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd10DebugChannel() {
		return this.channel(ChannelId.TEMPERATURE_OWD10_DEBUG);
	}

	/**
	 * Gets the Temperature in [deci degC]. See {@link ChannelId#TEMPERATURE_OWD10_DEBUG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureOwd10Debug() {
		return this.getTemperatureOwd10DebugChannel().value();
	}
	

	



	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public default ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(ModbusThermometer.class, accessMode, 100) //
				.channel(0, ChannelId.TEMPERATURE_OWD01, ModbusType.FLOAT32) //
				.channel(2, ChannelId.TEMPERATURE_OWD02, ModbusType.FLOAT32) //
				.channel(4, ChannelId.TEMPERATURE_OWD03, ModbusType.FLOAT32) //
				.channel(6, ChannelId.TEMPERATURE_OWD04, ModbusType.FLOAT32) //
				.channel(8, ChannelId.TEMPERATURE_OWD05, ModbusType.FLOAT32) //
				.channel(10, ChannelId.TEMPERATURE_OWD06, ModbusType.FLOAT32) //
				.channel(12, ChannelId.TEMPERATURE_OWD07, ModbusType.FLOAT32) //
				.channel(14, ChannelId.TEMPERATURE_OWD08, ModbusType.FLOAT32) //
				.channel(16, ChannelId.TEMPERATURE_OWD09, ModbusType.FLOAT32) //
				.channel(18, ChannelId.TEMPERATURE_OWD10, ModbusType.FLOAT32) //
				.build();
	}

	ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode);







}
