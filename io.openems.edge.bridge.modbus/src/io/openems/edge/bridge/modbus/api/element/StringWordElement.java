package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;

/**
 * An StringWordElement represents a String value. Each Register (= 2 bytes)
 * represents two characters.
 */
public class StringWordElement extends ModbusRegisterElement<StringWordElement, String> {

	private static final ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.BIG_ENDIAN;

	private final Logger log = LoggerFactory.getLogger(AbstractWordElement.class);

	protected ByteOrder byteOrder = DEFAULT_BYTE_ORDER;
	private final int length;

	public StringWordElement(int startAddress, int length) {
		super(OpenemsType.STRING, startAddress);
		this.length = length;
	}

	@Override
	public final int getLength() {
		return this.length;
	}

	@Override
	protected final void _setInputRegisters(InputRegister... registers) {
		// convert registers
		var buff = ByteBuffer.allocate(this.length * 2).order(this.getByteOrder());
		for (InputRegister r : registers) {
			var bs = r.toBytes();
			for (var i = 0; i < bs.length; i++) {
				if (bs[i] == 0) {
					bs[i] = 32; // replace '0' with ASCII space
				}
			}
			buff.put(bs);
		}

		var value = this.fromByteBuffer(buff);
		// set value
		super.setValue(value);
	}

	@Override
	public void _setNextWriteValue(Optional<String> valueOpt) throws OpenemsException {
		if (this.isDebug()) {
			this.log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
		}
		if (valueOpt.isPresent()) {
			var buff = ByteBuffer.allocate(2 * this.length).order(this.getByteOrder());
			buff = this.toByteBuffer(buff, valueOpt.get());
			var b = buff.array();

			var registers = new Register[this.length];
			for (var i = 0; i < this.length; i = i + 1) {
				registers[i] = new SimpleRegister(b[i * 2], b[i * 2 + 1]);
			}

			this.setNextWriteValueRegisters(Optional.of(registers));
		} else {
			this.setNextWriteValueRegisters(Optional.empty());
		}
		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
	}

	protected String fromByteBuffer(ByteBuffer buff) {
		return new String(buff.array()).trim();
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, String value) {
		return ByteBuffer.wrap(value.getBytes());
	}

	@Override
	protected StringWordElement self() {
		return this;
	}

}
