package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent.BitConverter;
import io.openems.edge.bridge.modbus.api.ChannelMetaInfoBit;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.WriteChannel;

/**
 * A BitsWordElement is an {@link UnsignedWordElement} where every bit
 * represents a Boolean value.
 */
public class BitsWordElement extends UnsignedWordElement {

	private final Logger log = LoggerFactory.getLogger(BitsWordElement.class);

	private final AbstractOpenemsModbusComponent component;

	/*
	 * Holds the ChannelWrapper. 'null' if not explicitly defined!
	 */
	private final ChannelWrapper[] channels = new ChannelWrapper[16];

	public BitsWordElement(int address, AbstractOpenemsModbusComponent component) {
		super(address);
		this.component = component;
	}

	@Override
	protected BitsWordElement self() {
		return this;
	}

	/**
	 * Adds a mapping for a given bit.
	 *
	 * @param bitIndex  the index of the bit; a number between 0 and 15
	 * @param channelId the Channel-ID
	 * @param converter the Bit-Converter
	 * @return myself for builder pattern
	 */
	public BitsWordElement bit(int bitIndex, ChannelId channelId, BitConverter converter) {
		return this.bit(bitIndex, channelId, converter, new ChannelMetaInfoBit(this.getStartAddress(), bitIndex));
	}

	/**
	 * Adds a mapping for a given bit.
	 *
	 * @param bitIndex           the index of the bit; a number between 0 and 15
	 * @param channelId          the Channel-ID
	 * @param channelMetaInfoBit an object that holds meta information about the
	 *                           Channel
	 * @return myself for builder pattern
	 */
	public BitsWordElement bit(int bitIndex, ChannelId channelId, ChannelMetaInfoBit channelMetaInfoBit) {
		return this.bit(bitIndex, channelId, BitConverter.DIRECT_1_TO_1, channelMetaInfoBit);
	}

	/**
	 * Adds a mapping for a given bit.
	 *
	 * @param bitIndex           the index of the bit; a number between 0 and 15
	 * @param channelId          the Channel-ID
	 * @param converter          the Bit-Converter
	 * @param channelMetaInfoBit an object that holds meta information about the
	 *                           Channel
	 * @return myself for builder pattern
	 */
	public BitsWordElement bit(int bitIndex, ChannelId channelId, BitConverter converter,
			ChannelMetaInfoBit channelMetaInfoBit) {
		if (bitIndex < 0 || bitIndex > 15) {
			throw new IllegalArgumentException("Bit-Index must be between 0 and 15 for Channel-ID [" + channelId + "]");
		}

		Channel<?> channel = this.component.channel(channelId);
		if (channel.getType() != OpenemsType.BOOLEAN) {
			throw new IllegalArgumentException("Channel [" + channelId + "] must be of type [BOOLEAN] for bit-mapping");
		}
		@SuppressWarnings("unchecked")
		var booleanChannel = (Channel<Boolean>) channel;

		var channelWrapper = new ChannelWrapper(booleanChannel, converter);

		// Set Channel-Source
		channel.setMetaInfo(channelMetaInfoBit);

		// Add Modbus Address and Bit-Index to Channel Source
		if (channel instanceof WriteChannel<?>) {
			// Handle Writes to Bit-Channels
			var booleanWriteChannel = (WriteChannel<Boolean>) booleanChannel;
			booleanWriteChannel.onSetNextWrite(value -> {
				// Listen on Writes to the BooleanChannel and store the value
				channelWrapper.setWriteValue(value);
			});
		}

		this.channels[bitIndex] = channelWrapper;
		return this;
	}

	/**
	 * Adds a mapping for a given bit.
	 *
	 * @param bitIndex  the index of the bit; a number between 0 and 15
	 * @param channelId the Channel-ID
	 * @return myself for builder pattern
	 */
	public BitsWordElement bit(int bitIndex, ChannelId channelId) {
		return this.bit(bitIndex, channelId, BitConverter.DIRECT_1_TO_1);
	}

	/**
	 * Sets the individual BooleanChannel-Values from an InputRegister.
	 *
	 * @param registers the InputRegisters
	 */
	@Override
	public void setInput(InputRegister register) {
//		if (registers.length != 1) {
//			throw new IllegalArgumentException("Expected only one Register instead of [" + registers.length
//					+ "] for Component [" + this.component.id() + "] on address [" + this.getStartAddress() + "]");
//		}

		// convert Register to int
		var buff = ByteBuffer.allocate(2).order(this.getByteOrder());
		buff.put(register.toBytes());
		var value = Short.toUnsignedInt(buff.getShort(0));

		for (var bitIndex = 0; bitIndex < 16; bitIndex++) {
			// Get Wrapper
			var wrapper = this.channels[bitIndex];
			if (wrapper == null) {
				continue;
			}

			// Get Value
			var setValue = value << ~bitIndex < 0;

			// Apply Bit-Conversion
			switch (wrapper.converter) {
			case DIRECT_1_TO_1:
				break;
			case INVERT:
				setValue = !setValue;
				break;
			}

			// Set Value to Channel
			wrapper.channel.setNextValue(setValue);
		}
	}

	/**
	 * Gets the next write value from all Bits and resets them.
	 *
	 * <p>
	 * This method should be called once in every cycle on the
	 * TOPIC_CYCLE_EXECUTE_WRITE event. It makes sure, that the nextWriteValue gets
	 * initialized in every Cycle. If registers need to be written again in every
	 * cycle, next setNextWriteValue()-method needs to called on every Cycle.
	 *
	 * @return the next value as an Optional array of Registers
	 */
	@Override
	public Optional<Register[]> getNextWriteValueAndReset() {
		// Check if any BooleanWriteChannel has a Write-Value; if none: return
		// Optional.empty.
		var isAnyBooleanWriteChannelSet = false;
		for (ChannelWrapper wrapper : this.channels) {
			if (wrapper != null && wrapper.getWriteValue().isPresent()) {
				isAnyBooleanWriteChannelSet = true;
				break;
			}
		}
		if (!isAnyBooleanWriteChannelSet) {
			return Optional.empty();
		}

		// Get value of each BooleanChannel
		var b0 = (byte) 0;
		var b1 = (byte) 0;
		List<ChannelAddress> channelsWithMissingWriteValue = new ArrayList<>();
		for (var bitIndex = 0; bitIndex < this.channels.length; bitIndex++) {
			var wrapper = this.channels[bitIndex];
			if (wrapper == null) {
				continue;
			}
			var valueOpt = wrapper.getWriteValue();
			if (!valueOpt.isPresent()) {
				channelsWithMissingWriteValue.add(wrapper.channel.address());
				continue;
			}
			// Write-Value exists
			boolean value = valueOpt.get();
			if ((value ? wrapper.converter == BitConverter.DIRECT_1_TO_1 //
					: wrapper.converter == BitConverter.INVERT)) {
				// Value is true -> set the bit of the byte
				if (bitIndex < 8) {
					b0 |= 1 << bitIndex;
				} else {
					b1 |= 1 << bitIndex - 8;
				}
			}
		}

		// If at least one BooleanWriteChannel had no Write-Value: Error + return
		// Optional.empty.
		if (!channelsWithMissingWriteValue.isEmpty()) {
			new IllegalArgumentException(
					"The following BooleanWriteChannels have no Write-Value: " + channelsWithMissingWriteValue.stream() //
							.map(ChannelAddress::toString) //
							.collect(Collectors.joining(",")))
					.printStackTrace();
			return Optional.empty();
		}

		// Clear all Write-Values
		for (ChannelWrapper wrapper : this.channels) {
			if (wrapper != null) {
				wrapper.setWriteValue(null);
			}
		}

		// create Register
		Register result;
		if (this.getByteOrder() == ByteOrder.BIG_ENDIAN) {
			result = new SimpleRegister(b1, b0);
		} else {
			result = new SimpleRegister(b0, b1);
		}

		// Log Debug
		if (this.isDebug()) {
			this.log.info("BitsWordElement [" + this + "]: " //
					+ "next write value is ["
					+ String.format("%16s", Integer.toBinaryString(result.getValue())).replace(' ', '0') //
					+ "/0x" + String.format("%4s", Integer.toHexString(result.getValue())).replace(' ', '0') + "].");
		}

		return Optional.of(new Register[] { result });

	}

	@Override
	protected Integer fromByteBuffer(ByteBuffer buff) {
		throw new IllegalArgumentException("BitsWordElement.fromByteBuffer() should never be called");
	}

	@Override
	public Optional<Register[]> getNextWriteValue() {
		throw new IllegalArgumentException("BitsWordElement.getNextWriteValue() should never be called");
	}

//	@Override
//	public void _setNextWriteValue(Optional<Integer> valueOpt) throws OpenemsException {
//		throw new IllegalArgumentException("BitsWordElement._setNextWriteValue() should never be called");
//	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Integer value) {
		throw new IllegalArgumentException("BitsWordElement.toByteBuffer() should never be called");
	}

	private static class ChannelWrapper {
		private final Channel<?> channel;
		private final BitConverter converter;
		private Optional<Boolean> writeValue = Optional.empty();

		protected ChannelWrapper(Channel<Boolean> channel, BitConverter converter) {
			this.channel = channel;
			this.converter = converter;
		}

		protected void setWriteValue(Boolean value) {
			this.writeValue = Optional.ofNullable(value);
		}

		protected Optional<Boolean> getWriteValue() {
			return this.writeValue;
		}
	}
}
