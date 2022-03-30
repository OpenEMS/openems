package io.openems.edge.meter.algo2.algotypes;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusRegisterElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;

public abstract class Algo3Word<E, T> extends AbstractModbusRegisterElement<E, T> {

	private final Logger log = LoggerFactory.getLogger(Algo3Word.class);

	public Algo3Word(OpenemsType type, int startAddress) {
		super(type, startAddress);
	}

	/**
	 * Gets an instance of the correct subclass of myself.
	 * 
	 * @return myself
	 */
	protected abstract E self();

	@Override
	public final int getLength() {
		return 3;
	}

	@Override
	protected final void _setInputRegisters(InputRegister... registers) {
		// fill buffer
		ByteBuffer buff = ByteBuffer.allocate(8).order(this.getByteOrder());
		byte[] MSW = {0,0,1,0};
		byte[] LSW = {0,0,0,0};
		if (wordOrder == WordOrder.MSWLSW) {
			MSW[2] = registers[0].toBytes()[0];
			MSW[3] = registers[0].toBytes()[1];
			LSW[0] = registers[1].toBytes()[0];
			LSW[1] = registers[1].toBytes()[1];
			LSW[2] = registers[2].toBytes()[0];
			LSW[3] = registers[2].toBytes()[1];
			buff.put(MSW);
			buff.put(LSW);
		} else {
			
			MSW[2] = registers[2].toBytes()[0];
			MSW[3] = registers[2].toBytes()[1];
			LSW[0] = registers[1].toBytes()[0];
			LSW[1] = registers[1].toBytes()[1];
			LSW[2] = registers[0].toBytes()[0];
			LSW[3] = registers[0].toBytes()[1];
			buff.put(LSW);
			buff.put(MSW);
		}
		buff.rewind();
		// convert registers to Long
		T value = fromByteBuffer(buff);
		// set value
		super.setValue(value);
	}

	/**
	 * Converts a 3-byte ByteBuffer to the the current OpenemsType.
	 * 
	 * @param buff the ByteBuffer
	 * @return an instance of the given OpenemsType
	 */
	protected abstract T fromByteBuffer(ByteBuffer buff);

	@Override
	public final void _setNextWriteValue(Optional<T> valueOpt) throws OpenemsException {
		if (valueOpt.isPresent()) {
			if (this.isDebug()) {
				log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
			}
			ByteBuffer buff = ByteBuffer.allocate(3).order(this.getByteOrder());
			buff = this.toByteBuffer(buff, valueOpt.get());
			byte[] b = buff.array();
			if (wordOrder == WordOrder.MSWLSW) {
				this.setNextWriteValueRegisters(Optional.of(new Register[] { //
						new SimpleRegister((byte)0, b[0]), new SimpleRegister(b[1], b[2]) }));
			} else {
				this.setNextWriteValueRegisters(Optional.of(new Register[] { //
						new SimpleRegister(b[1], b[2]), new SimpleRegister(b[0], (byte)0) }));
			}
		} else {
			this.setNextWriteValueRegisters(Optional.empty());
		}
		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
	}

	/**
	 * Converts the current OpenemsType to a 4-byte ByteBuffer.
	 * 
	 * @param buff  the target ByteBuffer
	 * @param value an instance of the given OpenemsType
	 * @return the ByteBuffer
	 */
	protected abstract ByteBuffer toByteBuffer(ByteBuffer buff, T value);

	/**
	 * Sets the Word-Order. Default is "MSWLSW" - "Most Significant Word; Least
	 * Significant Word". See http://www.simplymodbus.ca/FAQ.htm#Order.
	 * 
	 * @param wordOrder the new Word-Order
	 * @return myself
	 */
	public final E wordOrder(WordOrder wordOrder) {
		this.wordOrder = wordOrder;
		return this.self();
	}

	private WordOrder wordOrder = WordOrder.MSWLSW;

}
