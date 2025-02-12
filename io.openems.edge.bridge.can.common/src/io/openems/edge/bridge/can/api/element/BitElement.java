package io.openems.edge.bridge.can.api.element;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.can.api.CanIoException;

/**
 * A BitElement represents a single bit within the 8 bytes of CAN data.
 */
public class BitElement extends AbstractCanChannelElement<BitElement, Boolean> {

	/**
	 * objects describes the behavior and the position of an unsigned word value
	 * within an 8 byte CAN frame.
	 *
	 * @param overallBitPosition 0-63 the bit position within the 8 bytes of a CAN
	 *                           frame
	 */
	public BitElement(int overallBitPosition) throws OpenemsException {
		super(OpenemsType.BOOLEAN, overallBitPosition, 1, 0, 0, 0);
	}

	/**
	 * objects describes the behavior and the position of an unsigned word value
	 * within an 8 byte CAN frame.
	 *
	 * @param bytePosition 0-7 the byte position within the 8 bytes of a CAN frame
	 * @param bitPosition  0-7 the bit position within the given byte
	 */
	public BitElement(int bytePosition, int bitPosition) throws OpenemsException {
		this(bytePosition * 8 + bitPosition);
	}

	@Override
	public BitElement self() {
		return this;
	}

	// openems channel write -> send value via can
	@Override
	public void _setNextWriteValue(Optional<Boolean> valueOpt) throws OpenemsException {
		if (this.isDebug()) {
			this.log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
		}
		var canData = this.canFrame.getData();

		if (canData == null) {
			throw new CanIoException("Unable to update channel element. No CAN data frame set" + this);
		}
		var byteidx = this.bitposition / 8;
		var bitidx = this.bitposition % 8;
		if (canData == null || canData.length <= byteidx) {
			throw new OpenemsException("Unable to update can frame data at pos " + byteidx + " and " + byteidx);
		}

		if (valueOpt.get().booleanValue()) {
			canData[byteidx] |= 1 << bitidx;// set bit
		} else {
			canData[byteidx] &= ~(1 << bitidx);// unset bit
		}

		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
	}

	// received value via can -> put value in openems channel
	@Override
	public void doElementSetInput(byte[] data) throws CanIoException {
		var byteidx = this.bitposition / 8;
		var bitidx = this.bitposition % 8;
		if (data == null || data.length <= byteidx) {
			throw new CanIoException(
					"Unable to update channel element from can frame data at bitpos " + this.bitposition);
		}
		Boolean b;
		if ((data[byteidx] & 1 << bitidx) != 0) {
			b = Boolean.valueOf(true);
		} else {
			b = Boolean.valueOf(false);
		}
		super.setValue(b);

	}

	@Override
	public String getName() {
		return "BitElement";
	}

}
