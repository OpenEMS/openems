package io.openems.edge.bridge.can.api.element;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.can.api.CanIoException;

/**
 * An UnsignedDoubleWordElement represents an Integer value (32 bit) in an
 * {@link AbstractWordElement}.
 */
public class UnsignedDoubleWordElement extends AbstractCanChannelElement<UnsignedDoubleWordElement, Integer> {

	private final ByteOrder byteOrder;

	/**
	 * Object describes the behaviour and the position of an unsigned word value
	 * within an 8 byte CAN frame.
	 *
	 * <p>
	 * Uses the BIG ENDIAN byte order by default.
	 *
	 * @param bitposition 0-63 the bit position within the 8 bytes of a CAN frame
	 */
	public UnsignedDoubleWordElement(int bitposition) throws OpenemsException {
		this(bitposition, 32, 0, 0, 0, ByteOrder.BIG_ENDIAN);
	}

	/**
	 * Object describes the behaviour and the position of an unsigned word value
	 * within an 8 byte CAN frame.
	 *
	 * <p>
	 * Uses the BIG ENDIAN byte order by default.
	 *
	 * @param bitposition 0-63 the bit position within the 8 bytes of a CAN frame
	 * @param bitcnt      0-32 the number of bits which belongs to the value
	 * @param valueoffset value to add to the received unsigned double word
	 * @param valuemin    min possible value
	 * @param valuemax    max possible value
	 */
	public UnsignedDoubleWordElement(int bitposition, int bitcnt, int valueoffset, int valuemin, int valuemax)
			throws OpenemsException {
		this(bitposition, bitcnt, valueoffset, valuemin, valuemax, ByteOrder.BIG_ENDIAN);
	}

	/**
	 * Object describes the behaviour and the position of an unsigned word value
	 * within an 8 byte CAN frame.
	 *
	 * @param bitposition 0-63 the bit position within the 8 bytes of a CAN frame
	 * @param bitcnt      0-32 the number of bits which belongs to the value
	 * @param valueoffset value to add to the received unsigned double word
	 * @param valuemin    min possible value
	 * @param valuemax    max possible value
	 * @param byteOrder   defines the big or little endian byte order
	 */
	public UnsignedDoubleWordElement(int bitposition, int bitcnt, int valueoffset, int valuemin, int valuemax,
			ByteOrder byteOrder) throws OpenemsException {
		super(OpenemsType.INTEGER, bitposition, bitcnt, valueoffset, valuemin, valuemax);

		this.byteOrder = byteOrder;
		// NOTE: we do support UnsignedWords only if they are located at full bytes
		// within the can data bytes
		if (bitposition % 8 != 0 || bitcnt != 32) {
			throw new OpenemsException(
					"Invalid 'position' or 'len' within definition of UnsignedDoubleWordElement. bitposition: "
							+ bitposition + " bitcnt: " + bitcnt);
		}
	}

	@Override
	public UnsignedDoubleWordElement self() {
		return this;
	}

	// openems channel write -> send value via can
	@Override
	public void _setNextWriteValue(Optional<Integer> valueOpt) throws OpenemsException {
		if (this.isDebug()) {
			this.log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
		}

		var buff = ByteBuffer.allocate(4).order(this.getByteOrder());
		if (!valueOpt.isPresent()) {
			throw new OpenemsException("The channel value to write is invalid");
		}
		buff = this.toByteBuffer32(buff, valueOpt.get());
		var b = buff.array();
		var canData = this.canFrame.getData();

		var idx = this.bitposition / 8;
		if (canData == null || canData.length < idx + 3) {
			throw new OpenemsException("Unable to update can frame data at pos " + idx + " and " + (idx + 3));
		}
		canData[idx] = b[0];
		canData[idx + 1] = b[1];
		canData[idx + 2] = b[2];
		canData[idx + 3] = b[3];

		// TODOL validate written Channel data for constraints (min, max and offset) and
		// throw an exception if needed

		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
	}

	@Override
	public void doElementSetInput(byte[] data) throws CanIoException {

		// convert byte to int
		var idx = this.bitposition / 8;
		if (data == null || data.length < idx + 3) {
			throw new CanIoException("Unable to update can frame data at pos " + idx + " and " + (idx + 3));
		}

		var valbytes = new byte[4];
		valbytes[0] = data[idx];
		valbytes[1] = data[idx + 1];
		valbytes[2] = data[idx + 2];
		valbytes[3] = data[idx + 3];
		var buff = ByteBuffer.allocate(4).order(this.getByteOrder());
		buff.put(valbytes);
		var value = this.fromByteBuffer32(buff);
		super.setValue(value);
	}

	public ByteOrder getByteOrder() {
		return this.byteOrder;
	}

	@Override
	public String getName() {
		return "UnsignedDoubleWordElement";
	}

}
