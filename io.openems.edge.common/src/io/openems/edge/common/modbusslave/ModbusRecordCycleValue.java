package io.openems.edge.common.modbusslave;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * {@link ModbusRecordCycleValue}s allow to receive a {@link ModbusRecord} via a
 * {@link Function}. The Function is executed in the 'run()' method of the
 * Modbus-TCP-Api-Controller.
 *
 * @param <T> the {@link OpenemsComponent}
 */
public class ModbusRecordCycleValue<T extends OpenemsComponent> extends ModbusRecord {

	private final Logger log = LoggerFactory.getLogger(ModbusRecordCycleValue.class);

	private final Function<T, Object> function;
	private final String name;
	private final Unit unit;
	private final String valueDescription;

	private Object value;

	public ModbusRecordCycleValue(int offset, String name, Unit unit, String valueDescription, ModbusType type,
			Function<T, Object> function) {
		super(offset, type);
		this.name = name;
		this.unit = unit;
		this.valueDescription = valueDescription;
		this.function = function;
	}

	@Override
	public String toString() {
		return "ModbusRecordCycleValue [" //
				+ "offset=" + this.getOffset() + ", " //
				+ "type=" + this.getType() //
				+ "]";
	}

	/**
	 * Update the Value of this ModbusRecord.
	 * 
	 * <p>
	 * This method is called on every cycle by the run()-method of the
	 * Modbus-TCP-Api-Controller. Value is set to 'null' if provided 'component' is
	 * null.
	 * 
	 * @param component the {@link OpenemsComponent}
	 */
	public void updateValue(T component) {
		if (component == null) {
			this.value = null;
		} else {
			this.value = this.function.apply(component);
		}
	}

	@Override
	public byte[] getValue(OpenemsComponent component) {
		switch (this.getType()) {
		case FLOAT32:
			return ModbusRecordFloat32.toByteArray(this.value);
		case FLOAT64:
			return ModbusRecordFloat64.toByteArray(this.value);
		case STRING16:
			return ModbusRecordString16.toByteArray(this.value);
		case ENUM16:
		case UINT16:
			return ModbusRecordUint16.toByteArray(this.value);
		case UINT32:
			return ModbusRecordUint32.toByteArray(this.value);
		}
		assert true;
		return new byte[0];
	}

	@Override
	public void writeValue(int index, byte byte1, byte byte2) {
		this.log.warn("Writing to Read-Only CycleValue Modbus Record is not allowed! [" + this.getOffset() + ", "
				+ this.getType() + "]");
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Unit getUnit() {
		return this.unit;
	}

	@Override
	public String getValueDescription() {
		return this.valueDescription;
	}

	@Override
	public AccessMode getAccessMode() {
		return AccessMode.READ_ONLY;
	}

}