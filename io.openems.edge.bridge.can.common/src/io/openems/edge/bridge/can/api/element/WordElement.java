package io.openems.edge.bridge.can.api.element;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.can.api.CanIoException;

/**
 * A WordElement represents a signed Integer value (16 bit) in an
 * {@link AbstractWordElement}.
 */
public class WordElement extends AbstractCanChannelElement<WordElement, Integer> {

	private final ByteOrder byteOrder;

	/**
	 * Objects describes the behavior and the position of a word value within an 8
	 * byte CAN frame.
	 *
	 * <p>
	 * Uses the BIG ENDIAN byte order by default
	 *
	 * @param bitposition 0-63 the bit position within the 8 bytes of a CAN frame
	 * @param bitcnt      0-16 the number of bits which belongs to the value
	 * @param valueoffset value to add to the received word
	 * @param valuemin    min possible value
	 * @param valuemax    max possible value
	 */
	public WordElement(int bitposition, int bitcnt, int valueoffset, int valuemin, int valuemax)
			throws OpenemsException {
		this(bitposition, bitcnt, valueoffset, valuemin, valuemax, ByteOrder.BIG_ENDIAN);
	}

	/**
	 * Objects describes the behaviour and the position of a word value within an 8
	 * byte CAN frame.
	 *
	 * @param bitposition 0-63 the bit position within the 8 bytes of a CAN frame
	 * @param bitcnt      0-16 the number of bits which belongs to the value
	 * @param valueoffset value to add to the received word
	 * @param valuemin    min possible value
	 * @param valuemax    max possible value
	 * @param byteOrder   defines the big or little endian byte order
	 */
	public WordElement(int bitposition, int bitcnt, int valueoffset, int valuemin, int valuemax, ByteOrder byteOrder)
			throws OpenemsException {
		super(OpenemsType.INTEGER, bitposition, bitcnt, valueoffset, valuemin, valuemax);

		this.byteOrder = byteOrder;
		// NOTE: we do support UnsignedWords only if they are located at full bytes
		// within the can data bytes
		if (bitposition % 8 != 0 || bitcnt != 16) {
			throw new OpenemsException("Invalid 'position' or 'len' within definition of WordElement. bitposition: "
					+ bitposition + " bitcnt: " + bitcnt);
		}
	}

	@Override
	public WordElement self() {
		return this;
	}

	// openems channel write -> send value via can
	@Override
	public void _setNextWriteValue(Optional<Integer> valueOpt) throws OpenemsException {
		if (this.isDebug()) {
			this.log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
		}
		var buff = ByteBuffer.allocate(2).order(this.getByteOrder());
		if (!valueOpt.isPresent()) {
			throw new OpenemsException("The channel value to write is invalid");
		}
		buff = this.toByteBuffer16(buff, valueOpt.get());
		var b = buff.array();
		var canData = this.canFrame.getData();

		var idx = this.bitposition / 8;
		if (canData == null || canData.length < idx + 1) {
			throw new OpenemsException("Unable to update can frame data at pos " + idx + " and " + (idx + 1));
		}
		canData[idx] = b[0];
		canData[idx + 1] = b[1];

		// TODOL validate written Channel data for constraints (min, max and offset) and
		// throw an exception if needed

		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
	}

	// received value via can -> put value in openems channel
	@Override
	public void doElementSetInput(byte[] data) throws CanIoException {

		// convert byte to int
		var idx = this.bitposition / 8;
		if (data == null || data.length <= idx + 1) {
			throw new CanIoException("Unable to update can frame data at pos " + idx + " and " + (idx + 1));
		}
		var valbytes = new byte[2];
		valbytes[0] = data[idx];
		valbytes[1] = data[idx + 1];
		var buff = ByteBuffer.allocate(2).order(this.getByteOrder());
		buff.put(valbytes);
		var value = this.fromSignedByteBuffer16(buff);
		super.setValue(value);

		// TODOM throw CanIoException if cycle time is exceeded or on any other error
	}

	private ByteOrder getByteOrder() {
		return this.byteOrder;
	}

	@Override
	public String getName() {
		return "WordElement";
	}

}
