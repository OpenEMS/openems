package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class ModbusRecordChannel extends ModbusRecord {

	private final Logger log = LoggerFactory.getLogger(ModbusRecordChannel.class);

	private final ChannelId channelId;
	private final AccessMode accessMode;

	protected Consumer<Object> onWriteValueCallback = null;

	/**
	 * this is used to buffer calls to writeValue(). Once the buffer is full, the
	 * value is actually forwarded to the channel.
	 */
	private final Byte[] writeValueBuffer;

	public ModbusRecordChannel(int offset, ModbusType type, ChannelId channelId, AccessMode modbusApiAccessMode) {
		super(offset, type);
		this.channelId = channelId;
		this.accessMode = evaluateActualAccessMode(channelId, modbusApiAccessMode);

		// initialize buffer
		var byteLength = switch (this.getType()) {
		case FLOAT32 -> ModbusRecordFloat32.BYTE_LENGTH;
		case FLOAT64 -> ModbusRecordFloat64.BYTE_LENGTH;
		case STRING16 -> ModbusRecordString16.BYTE_LENGTH;
		case ENUM16, UINT16 -> ModbusRecordUint16.BYTE_LENGTH;
		case UINT32 -> ModbusRecordUint32.BYTE_LENGTH;
		case UINT64 -> ModbusRecordUint64.BYTE_LENGTH;
		};
		this.writeValueBuffer = new Byte[byteLength];
	}

	/**
	 * Evaluate the AccessMode from configured Modbus-Api-AccessMode and
	 * Channel-AccessMode.
	 *
	 * @param channelId           the {@link ChannelId}
	 * @param modbusApiAccessMode the configured {@link AccessMode}
	 * @return the actual {@link AccessMode}
	 */
	private static AccessMode evaluateActualAccessMode(ChannelId channelId, AccessMode modbusApiAccessMode) {
		var channelAccessMode = channelId.doc().getAccessMode();
		return switch (modbusApiAccessMode) {
		case READ_ONLY -> AccessMode.READ_ONLY;

		case READ_WRITE -> //
			switch (channelAccessMode) {
			case READ_ONLY -> AccessMode.READ_ONLY;
			case READ_WRITE -> AccessMode.READ_WRITE;
			case WRITE_ONLY -> AccessMode.WRITE_ONLY;
			};

		case WRITE_ONLY -> //
			switch (channelAccessMode) {
			case READ_ONLY -> AccessMode.READ_ONLY;
			case READ_WRITE, WRITE_ONLY -> AccessMode.WRITE_ONLY;
			};
		};
	}

	public ChannelId getChannelId() {
		return this.channelId;
	}

	@Override
	public String toString() {
		return "ModbusRecordChannel [" //
				+ "channelId=" + this.channelId + ", " //
				+ "offset=" + this.getOffset() + ", " //
				+ "type=" + this.getType() //
				+ "]";
	}

	@Override
	public byte[] getValue(OpenemsComponent component) {
		final Object value;
		if (component != null) {
			Channel<?> channel = component.channel(this.channelId);
			if (channel != null) {
				value = switch (channel.channelDoc().getAccessMode()) {
				case WRITE_ONLY -> null; // no read allowed/possible

				case READ_ONLY, READ_WRITE -> {
					try {
						yield channel.value().get();
					} catch (IllegalArgumentException e) {
						this.log.warn("Channel [" + channel.address() + "] is not available: " + e.getMessage());
						yield null;
					}
				}
				};

			} else {
				this.log.warn("Channel [" + component.id() + "/" + this.channelId.id() + "] is not available for "
						+ this.toString());
				value = null;
			}
		} else {
			value = null;
		}

		return switch (this.getType()) {
		case FLOAT32 -> //
			switch (this.accessMode) {
			case READ_ONLY, READ_WRITE -> ModbusRecordFloat32.toByteArray(value);
			case WRITE_ONLY -> ModbusRecordFloat32.UNDEFINED_VALUE;
			};
		case FLOAT64 -> //
			switch (this.accessMode) {
			case READ_ONLY, READ_WRITE -> ModbusRecordFloat64.toByteArray(value);
			case WRITE_ONLY -> ModbusRecordFloat64.UNDEFINED_VALUE;
			};
		case STRING16 -> //
			switch (this.accessMode) {
			case READ_ONLY, READ_WRITE -> ModbusRecordString16.toByteArray(value);
			case WRITE_ONLY -> ModbusRecordString16.UNDEFINED_VALUE;
			};
		case ENUM16, UINT16 -> //
			switch (this.accessMode) {
			case READ_ONLY, READ_WRITE -> ModbusRecordUint16.toByteArray(value);
			case WRITE_ONLY -> ModbusRecordUint16.UNDEFINED_VALUE;
			};
		case UINT32 -> //
			switch (this.accessMode) {
			case READ_ONLY, READ_WRITE -> ModbusRecordUint32.toByteArray(value);
			case WRITE_ONLY -> ModbusRecordUint32.UNDEFINED_VALUE;
			};
		case UINT64 -> //
			switch (this.accessMode) {
			case READ_ONLY, READ_WRITE -> ModbusRecordUint64.toByteArray(value);
			case WRITE_ONLY -> ModbusRecordUint64.UNDEFINED_VALUE;
			};
		};
	}

	/**
	 * Add a onWriteValue callback.
	 * 
	 * @param onWriteValueCallback the callback
	 */
	public void onWriteValue(Consumer<Object> onWriteValueCallback) {
		this.onWriteValueCallback = onWriteValueCallback;
	}

	@Override
	public void writeValue(int index, byte byte1, byte byte2) {
		switch (this.accessMode) {
		case READ_ONLY:
			// Read-Only Access enabled. Do not write value.
			return;

		case READ_WRITE:
		case WRITE_ONLY:
			break;
		}

		this.writeValueBuffer[index * 2] = byte1;
		this.writeValueBuffer[index * 2 + 1] = byte2;
		// is the buffer full?
		for (Byte element : this.writeValueBuffer) {
			if (element == null) {
				return; // no, it is not full
			}
		}

		// yes, it is full -> Prepare ByteBuffer
		var buff = ByteBuffer.allocate(this.writeValueBuffer.length);
		for (Byte element : this.writeValueBuffer) {
			buff.put(element);
		}
		buff.rewind();

		// clear buffer
		Arrays.fill(this.writeValueBuffer, null);

		// Get Value-Object from ByteBuffer
		var value = switch (this.getType()) {
		case FLOAT64 -> buff.getDouble();
		case FLOAT32 -> buff.getFloat();
		case STRING16 -> ""; // TODO implement String conversion
		case ENUM16, UINT16 -> buff.getShort();
		case UINT32 -> buff.getInt();
		case UINT64 -> buff.getLong();
		};

		// Forward Value to ApiWorker
		if (this.onWriteValueCallback != null) {
			this.onWriteValueCallback.accept(value);
		} else {
			this.log.error("No onWriteValueCallback registered. What should I do with [" + value + "]?");
		}
	}

	@Override
	public String getName() {
		return new ChannelAddress(this.getComponentId(), this.getChannelId().id()).toString();
	}

	@Override
	public Unit getUnit() {
		return this.channelId.doc().getUnit();
	}

	@Override
	public String getValueDescription() {
		return this.channelId.doc().getText();
	}

	@Override
	public AccessMode getAccessMode() {
		return this.accessMode;
	}

}