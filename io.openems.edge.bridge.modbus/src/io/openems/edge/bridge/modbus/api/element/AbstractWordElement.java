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

public abstract class AbstractWordElement<T> extends AbstractModbusRegisterElement<T> {

	private final static ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.BIG_ENDIAN;

	private final Logger log = LoggerFactory.getLogger(AbstractWordElement.class);

	protected ByteOrder byteOrder = DEFAULT_BYTE_ORDER;

	public AbstractWordElement(OpenemsType type, int startAddress) {
		super(type, startAddress);
	}

	@Override
	public final int getLength() {
		return 1;
	}

	@Override
	protected final void _setInputRegisters(InputRegister... registers) {
		// convert registers
		ByteBuffer buff = ByteBuffer.allocate(2).order(getByteOrder());
		buff.put(registers[0].toBytes());
		T value = fromByteBuffer(buff);
		// set value
		super.setValue(value);
	}

	/**
	 * Converts a 2-byte ByteBuffer to the the current OpenemsType
	 * 
	 * @param buff
	 * @return
	 */
	protected abstract T fromByteBuffer(ByteBuffer buff);

	@Override
	public final void _setNextWriteValue(Optional<T> valueOpt) throws OpenemsException {
		if (this.isDebug()) {
			log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
		}
		if (valueOpt.isPresent()) {
			ByteBuffer buff = ByteBuffer.allocate(2).order(this.getByteOrder());
			buff = this.toByteBuffer(buff, valueOpt.get());
			byte[] b = buff.array();
			this.setNextWriteValueRegisters(Optional.of(new Register[] { //
					new SimpleRegister(b[0], b[1]) }));
		} else {
			this.setNextWriteValueRegisters(Optional.empty());
		}
	}

	/**
	 * Converts the current OpenemsType to a 2-byte ByteBuffer
	 * 
	 * @param buff
	 * @return
	 */
	protected abstract ByteBuffer toByteBuffer(ByteBuffer buff, T value);
}
