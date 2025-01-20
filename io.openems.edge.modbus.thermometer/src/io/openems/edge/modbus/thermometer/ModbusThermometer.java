package io.openems.edge.modbus.thermometer;



import static io.openems.common.channel.PersistencePriority.HIGH;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
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
        TEMPERATURE_OWD1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD3(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD4(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD5(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD6(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD7(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD8(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD9(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH)),
        TEMPERATURE_OWD10(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).persistencePriority(HIGH));


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
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_OWD1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTemperatureOwd1Channel() {
		return this.channel(ChannelId.TEMPERATURE_OWD1);
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
		return this.channel(ChannelId.TEMPERATURE_OWD2);
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
		return this.channel(ChannelId.TEMPERATURE_OWD3);
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
		return this.channel(ChannelId.TEMPERATURE_OWD4);
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
		return this.channel(ChannelId.TEMPERATURE_OWD5);
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
		return this.channel(ChannelId.TEMPERATURE_OWD6);
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
		return this.channel(ChannelId.TEMPERATURE_OWD7);
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
		return this.channel(ChannelId.TEMPERATURE_OWD8);
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
		return this.channel(ChannelId.TEMPERATURE_OWD9);
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
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public default ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(ModbusThermometer.class, accessMode, 100) //
				.channel(0, ChannelId.TEMPERATURE_OWD1, ModbusType.FLOAT32) //
				.channel(2, ChannelId.TEMPERATURE_OWD2, ModbusType.FLOAT32) //
				.channel(4, ChannelId.TEMPERATURE_OWD3, ModbusType.FLOAT32) //
				.channel(6, ChannelId.TEMPERATURE_OWD4, ModbusType.FLOAT32) //
				.channel(8, ChannelId.TEMPERATURE_OWD5, ModbusType.FLOAT32) //
				.channel(10, ChannelId.TEMPERATURE_OWD6, ModbusType.FLOAT32) //
				.channel(12, ChannelId.TEMPERATURE_OWD7, ModbusType.FLOAT32) //
				.channel(14, ChannelId.TEMPERATURE_OWD8, ModbusType.FLOAT32) //
				.channel(16, ChannelId.TEMPERATURE_OWD9, ModbusType.FLOAT32) //
				.channel(18, ChannelId.TEMPERATURE_OWD10, ModbusType.FLOAT32) //
				.build();
	}

	ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode);







}
