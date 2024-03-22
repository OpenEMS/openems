package io.openems.edge.bridge.can.api.element;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.can.api.CanIoException;

/**
 * An AutoCounterElement represents an Integer value. On each CAN send it
 * automatically increases its counter value by one. When maxValue is reached it
 * starts again at minValue.
 *
 */
public class AutoCounterElement extends AbstractCanChannelElement<AutoCounterElement, Integer> {

	/**
	 * objects describes the behaviour and the position of an auto counter element
	 * within the 8 byte of a CAN frame.
	 *
	 * @param byteposition 0-7 the byte within the 8 bytes of a CAN frame
	 * @param bitposition  0-7 the byte within the given byte
	 * @param bitlength    0-7 the number of bits within the bits
	 * @param minValue     min possible value (this is also the start value)
	 * @param maxValue     max possible value
	 */
	public AutoCounterElement(int byteposition, int bitposition, int bitlength, int minValue, int maxValue)
			throws OpenemsException {
		super(OpenemsType.INTEGER, byteposition * 8 + bitposition, bitlength, 0, minValue, maxValue);

		if (byteposition > 8 || bitposition > 8 || bitlength > 8 || bitposition + bitlength > 8) {
			throw new OpenemsException("Invalid 'position' description for the AutoCounterElement");
		}
		this.invalid = false;
	}

	@Override
	public AutoCounterElement self() {
		return this;
	}

	@Override
	public void onCanFrameSuccessfullySend() {
		// auto increment the value

		var canData = this.canFrame.getData();

		// convert byte to int
		var byteidx = this.bitposition / 8;
		var bitidx = this.bitposition % 8;
		if (canData == null || canData.length <= byteidx) {
			this.log.error("Unable to auto increment frame caused by bitposition " + this.bitposition);
			return;
		}
		int val = canData[byteidx];
		if (this.bitlen != 8) {
			var bitpattern = this.getBitmask(bitidx);
			val &= bitpattern;
			val = 0xff & val >> bitidx;
		}
		// autoincrement by one
		val += 1;
		if (val < this.min || val > this.max) {
			val = this.min;
		}

		// update CAN data also
		if (this.bitlen == 8) {
			canData[byteidx] = (byte) val;
		} else {
			var newValue = canData[byteidx];
			// relevant bits go from bitidx to (bitidx+bitlen), create a special mask
			var bitpattern = this.getBitmask(bitidx);
			newValue &= ~bitpattern; // set all masked bits to 0

			// set new bits within mask to 1
			bitpattern = (byte) (bitpattern & (byte) val);
			newValue |= bitpattern;
			canData[byteidx] = newValue;
		}
		// enable if we want to inform the autocounters CHANNEL
		// Integer value = new Integer(val);
		// super.setValue(value);
	}

	// openems channel write -> send value via can
	@Override
	public void _setNextWriteValue(Optional<Integer> valueOpt) throws OpenemsException {
		; // NOTE: an AutoCounterElement does not support writing to this channel from
		// outside
	}

	// received value via can -> put value in openems channel
	@Override
	public void doElementSetInput(byte[] data) throws CanIoException {
		; // NOTE: an AutoCounterElement does not support reading data from low level CAN
		// hardware
	}

	@Override
	public String getName() {
		return "AutoCounterElement";
	}

}
