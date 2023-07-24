package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class BitsWordElement extends ModbusElement<BitsWordElement, Register, Boolean[]> {

	private final AbstractOpenemsModbusComponent component;

	private static record ChannelWrapper(Channel<Boolean> channel, BitConverter converter) {
	}

	/** Holds the ChannelWrapper; 'null' if not explicitly defined. */
	private final ChannelWrapper[] channels = new ChannelWrapper[16];

	public BitsWordElement(int address, AbstractOpenemsModbusComponent component) {
		super(OpenemsType.INTEGER, address, 1);
		this.component = component;

		// On Value Update: set the individual BooleanChannel-Values
		this.onUpdateCallback(value -> {
			for (var bitIndex = 0; bitIndex < 16; bitIndex++) {
				// Get Wrapper
				var wrapper = this.channels[bitIndex];
				if (wrapper == null) {
					continue;
				}

				final Boolean setValue;
				if (value == null) {
					setValue = null;
				} else {
					var bit = value[bitIndex];
					setValue = switch (wrapper.converter) {
					case DIRECT_1_TO_1 -> bit;
					case INVERT -> !bit;
					};
				}

				// Set Value to Channel
				wrapper.channel().setNextValue(setValue);
			}
		});
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
		return this.bit(bitIndex, channelId, converter, new ChannelMetaInfoBit(this.startAddress, bitIndex));
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

		// Add Modbus Address and Bit-Index to Channel Source
		channel.setMetaInfo(channelMetaInfoBit);

		// Handle Writes to Bit-Channels
		if (channel instanceof WriteChannel<?>) {
			var booleanWriteChannel = (WriteChannel<Boolean>) booleanChannel;
			booleanWriteChannel.onSetNextWrite(value -> {
				// Listen on Writes to the BooleanChannel and store the value
				this.getNextWriteValue()[bitIndex] = value;
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

	@Override
	protected Register valueToRaw(Boolean[] values) {
		// Check if any Boolean is set; if none: return null immediately
		if (Stream.of(values) //
				.allMatch(Objects::isNull)) {
			return null;
		}

		// Get value of each Channel
		var b0 = (byte) 0;
		var b1 = (byte) 0;
		List<ChannelAddress> channelsWithMissingWriteValue = new ArrayList<>();
		for (var bitIndex = 0; bitIndex < this.channels.length; bitIndex++) {
			var wrapper = this.channels[bitIndex];
			if (wrapper == null) {
				continue;
			}
			var value = values[bitIndex];

			if (value == null) {
				// Value is missing
				channelsWithMissingWriteValue.add(wrapper.channel.address());
				continue;
			}

			// Value exists
			final boolean setValue;
			setValue = switch (wrapper.converter) {
			case DIRECT_1_TO_1 -> value;
			case INVERT -> !value;
			};

			if (setValue) {
				// Value is true -> set the bit of the byte
				if (bitIndex < 8) {
					b0 |= 1 << bitIndex;
				} else {
					b1 |= 1 << bitIndex - 8;
				}
			}
		}

		// If at least one value was missing: Error + return null
		if (!channelsWithMissingWriteValue.isEmpty()) {
			new IllegalArgumentException(
					"The following BooleanWriteChannels have no Write-Value: " + channelsWithMissingWriteValue.stream() //
							.map(ChannelAddress::toString) //
							.collect(Collectors.joining(",")))
					.printStackTrace();
			return null;
		}

		// create Register - always BIG_ENDIAN
		return new SimpleRegister(b1, b0);
	}

	@Override
	protected Boolean[] rawToValue(Register register) {
		// convert Register to int
		var buff = ByteBuffer.allocate(2);
		buff.put(register.toBytes());
		var value = Short.toUnsignedInt(buff.getShort(0));

		var result = new Boolean[16];
		for (var bitIndex = 0; bitIndex < 16; bitIndex++) {
			// Get Wrapper
			var wrapper = this.channels[bitIndex];
			if (wrapper == null) {
				continue;
			}

			// Get Value
			var bit = value << ~bitIndex < 0;

			// Apply Bit-Conversion
			result[bitIndex] = switch (wrapper.converter) {
			case DIRECT_1_TO_1 -> bit;
			case INVERT -> !bit;
			};
		}

		return result;
	}

	@Override
	protected void resetNextWriteValue() {
		// Clear all Write-Values
		Stream.of(this.channels) //
				.filter(Objects::nonNull) //
				.map(ChannelWrapper::channel) //
				.filter(WriteChannel.class::isInstance) //
				.map(WriteChannel.class::cast) //
				.forEach(WriteChannel::getNextWriteValueAndReset);
	}
}
