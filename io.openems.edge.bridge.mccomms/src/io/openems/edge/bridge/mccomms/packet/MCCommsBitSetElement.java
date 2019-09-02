package io.openems.edge.bridge.mccomms.packet;

import com.google.common.collect.Range;
import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;

import java.util.BitSet;

/**
 * An extension of {@link MCCommsElement} that allows each bit of a collection of bytes (1 or more) to be assigned to a channel
 */
public class MCCommsBitSetElement extends MCCommsElement {
	/**
	 * The channels, in order of which bit the pertain to
	 */
	private final Channel<Boolean>[] channels;
	
	/**
	 * Constructor
	 * @param addressRange the address range of this element within the packet byte order
	 * @param channels the boolean {@link Channel}s to be bound to this element, in the order of which bit the pertain to
	 *                 bits can be skipped by supplying a null parameter; e.g:
	 *                 <p>{@code new MCCommsBitSetElement(Range.closed(1,1),
	 *                 null,
	 *                 null,
	 *                 3rdBitBooleanChannel,
	 *                 null,
	 *                 5thBitBooleanChannel)}</p>
	 * @throws OpenemsException if the number of channels specified exceed the number of available bits
	 */
	@SafeVarargs
	private MCCommsBitSetElement(Range<Integer> addressRange, Channel<Boolean>...channels) throws OpenemsException {
		super(addressRange, true, 1.0, null);
		if (((addressRange.upperEndpoint() - addressRange.lowerEndpoint()) * 8) < channels.length) {
			throw new OpenemsException("Number of channels exceeds number of bits");
		}
		this.channels = channels;
	}
	
	/**
	 * Static puplic constructor for instantiating a new MCCommsBitSetElement
	 * @param startAddress the index of the first byte of this element within the packet order
	 * @param numBytes the number of bytes this element takes up within the packet byte order
	 * @param channels the boolean {@link Channel}s to be bound to this element, in the order of which bit the pertain to
	 *                 bits can be skipped by supplying a null parameter; e.g:
	 *                 <p>{@code new MCCommsBitSetElement(Range.closed(1,1),
	 *                 null,
	 *                 null,
	 *                 3rdBitBooleanChannel,
	 *                 null,
	 *                 5thBitBooleanChannel)}</p>
	 * @return a new MCCommsBitSetElement
	 * @throws OpenemsException if the number of channels specified exceed the number of available bits
	 */
	@SafeVarargs
	public static MCCommsBitSetElement newInstanceFromChannels(int startAddress, int numBytes, Channel<Boolean>...channels) throws OpenemsException {
		return new MCCommsBitSetElement(Range.closed(startAddress, (startAddress + numBytes -1)), channels);
	}
	
	/**
	 * Not used for BitSets
	 * @param unsigned not used
	 * @return this instance
	 */
	@Override
	public MCCommsElement setUnsigned(boolean unsigned) {
		return this;
	}
	
	/**
	 * Not used for BitSets
	 * @param scaleFactor not used
	 * @return this instance
	 */
	@Override
	public MCCommsElement setScaleFactor(double scaleFactor) {
		return this;
	}
	
	/**
	 * Not applicable for this type of element
	 * @return does not return
	 * @throws NotImplementedException this method is not implemented for this type of element
	 */
	@Override
	public Channel getChannel() throws NotImplementedException {
		throw new NotImplementedException("Not supported for BitSet elements");
	}
	
	/**
	 * Not applicable for this type of element
	 * @return does not return
	 * @throws NotImplementedException this method is not implemented for this type of element
	 */
	@Override
	public MCCommsElement setChannel(Channel channel) throws NotImplementedException {
		throw new NotImplementedException("Not supported for BitSet elements");
	}
	
	/**
	 * Constructs a {@link BitSet} from the internal {@link MCCommsElement#valueBuffer} and assigns boolean values to the channels bound to this element
	 */
	@Override
	public void assignValueToChannel() {
		BitSet bitSet = BitSet.valueOf(getValueBuffer());
		for (int i = 0; i < channels.length && i < bitSet.length(); i++) {
			if (channels[i] != null) {
				channels[i].setNextValue(bitSet.get(i));
			}
		}
	}
	
	/**
	 * Pulls boolean values from the channels bound to this element and constructs the internal {@link MCCommsElement#valueBuffer} accordingly
	 */
	@Override
	public void getValueFromChannel() {
		BitSet bitSet = new BitSet(getValueBuffer().capacity());
		for (int i = 0; i < channels.length && i < bitSet.length(); i++) {
			if (channels[i] != null) {
				bitSet.set(i, channels[i].value().get());
			}
		}
		if (bitSet.length() > 0) {
			this.getValueBuffer().put(bitSet.toByteArray());
		}
	}
}
