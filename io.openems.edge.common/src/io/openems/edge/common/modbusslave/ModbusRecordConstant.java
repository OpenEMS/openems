package io.openems.edge.common.modbusslave;

import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class ModbusRecordConstant extends ModbusRecord {

	private final Logger log = LoggerFactory.getLogger(ModbusRecordConstant.class);

	private final String name;
	private final byte[] value;

	public ModbusRecordConstant(int offset, String name, ModbusType type, byte[] value) {
		super(offset, type);
		this.name = name;
		this.value = value;
	}

	@Override
	public String toString() {
		return "ModbusRecordConstant [offset=" + this.getOffset() + ", type=" + this.getType() + "]";
	}

	public byte[] getValue() {
		return this.value;
	}

	@Override
	public byte[] getValue(OpenemsComponent component) {
		return this.getValue();
	}

	@Override
	public void writeValue(int index, byte byte1, byte byte2) {
		this.log.warn("Writing to Read-Only Modbus Record is not allowed! [" + this.getOffset() + ", " + this.getType()
				+ "]");
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public AccessMode getAccessMode() {
		return AccessMode.READ_ONLY;
	}

	/**
	 * Generates a common toString() method for implementations of
	 * {@link ModbusRecordConstant}.
	 * 
	 * @param <T>         the type of the value
	 * @param name        the name of the implementation class
	 * @param callback    a {@link StringBuilder} callback
	 * @param value       the actual value
	 * @param toHexString the toHexString() method
	 * @return a {@link String}
	 */
	protected <T> String generateToString(String name, Consumer<StringBuilder> callback, T value,
			Function<T, String> toHexString) {
		var b = new StringBuilder() //
				.append(name) //
				.append(" [");
		if (callback != null) {
			callback.accept(b);
		}
		b.append("value=");
		if (value != null) {
			b.append(value);
			if (toHexString != null) {
				b.append("/0x").append(toHexString.apply(value));
			}
		} else {
			b.append("UNDEFINED");
		}
		return b.append(", type=").append(this.getType()) //
				.append("]") //
				.toString();
	}

	protected <T> String generateToString(String name, T value, Function<T, String> toHexString) {
		return this.generateToString(name, null, value, toHexString);
	}

	protected <T> String generateToString(String name, T value) {
		return this.generateToString(name, null, value, null);
	}
}
