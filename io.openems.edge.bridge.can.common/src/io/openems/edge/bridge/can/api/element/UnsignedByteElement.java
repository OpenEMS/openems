package io.openems.edge.bridge.can.api.element;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.can.api.CanIoException;

/**
 * An UnsignedByteElement represents an unsigned Integer value (8 bit).
 */
public class UnsignedByteElement extends AbstractCanChannelElement<UnsignedByteElement, Integer> {

	/**
	 * Object describes the behaviour and the position of an unsigned byte value
	 * within an 8 byte CAN frame.
	 *
	 * @param byteposition 0-7 the position within the 8 bytes of a CAN frame, where
	 *                     the integer is located
	 */
	public UnsignedByteElement(int byteposition) throws OpenemsException {
		this(byteposition, 0, 8);
	}

	/**
	 * Object describes the behaviour and the position of an unsigned byte value
	 * within an 8 byte CAN frame.
	 *
	 * @param byteposition 0-7 the position within the 8 bytes of a CAN frame, where
	 *                     the integer is located
	 * @param bitposition  0-7 the position within the given byte of a CAN frame,
	 *                     where the value is located
	 * @param bitlength    0-7 the length of all the bits belonging to this value
	 * @throws OpenemsException if byte- or bit position are invalid
	 * @note: bitposition + bitlength <= 8
	 */
	public UnsignedByteElement(int byteposition, int bitposition, int bitlength) throws OpenemsException {
		super(OpenemsType.INTEGER, byteposition * 8 + bitposition, bitlength, 0, 0, 0);
		if (byteposition > 8 || bitposition + bitlength > 8) {
			throw new OpenemsException(
					"Invalid 'byteposition' or 'bitposition+bitlength > 8' within definition of UnsignedByteElement. bytepos: "
							+ byteposition + " bitpos " + bitposition);
		}
	}

	@Override
	public UnsignedByteElement self() {
		return this;
	}

	@Override
	public void _setNextWriteValue(Optional<Integer> valueOpt) throws OpenemsException {
		if (this.isDebug()) {
			this.log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
		}
		byte b = 0;
		if (!valueOpt.isPresent()) {
			throw new OpenemsException("The channel value to write is invalid");
		}
		var i = valueOpt.get().intValue();
		if (i > 0xff) {
			throw new OpenemsException(
					"The channel value " + valueOpt.get().intValue() + " is to large for this UnsignedByteElement");
		}
		b = (byte) (0xff & i);
		var canData = this.canFrame.getData();
		if (canData == null) {
			throw new CanIoException("Unable to update channel element. No CAN data frame set" + this);
		}

		var byteidx = this.bitposition / 8;
		var bitidx = this.bitposition % 8;
		if (canData == null || canData.length <= byteidx) {
			throw new OpenemsException("Unable to update can frame data at bitpos " + this.bitposition);
		}

		if (this.bitlen == 8) {
			canData[byteidx] = b;
		} else {
			var newValue = canData[byteidx];
			// relevant bits go from bitidx to (bitidx+bitlen), create a special mask
			var bitpattern = this.getBitmask(bitidx);
			newValue &= ~bitpattern; // set all masked bits to 0

			// set new bits within mask to 1
			bitpattern = (byte) (bitpattern & b << bitidx);
			newValue |= bitpattern;
			canData[byteidx] = newValue;
		}

		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
	}

	// received value via can -> put value in openems channel
	@Override
	public void doElementSetInput(byte[] data) throws CanIoException {

		// convert byte to int
		var byteidx = this.bitposition / 8;
		var bitidx = this.bitposition % 8;
		if (data == null || data.length <= byteidx) {
			throw new CanIoException("Unable to update can frame data at pos " + byteidx);
		}
		int val = data[byteidx];
		if (this.bitlen != 8) {
			var bitpattern = this.getBitmask(bitidx);
			val &= bitpattern;
			val = 0xff & val >> bitidx;
		}
		var value = Integer.valueOf(val);
		super.setValue(value);
	}

	@Override
	public String getName() {
		return "UnsignedByteElement";
	}

}
