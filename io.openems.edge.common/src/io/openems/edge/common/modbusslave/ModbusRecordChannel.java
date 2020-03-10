package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumDoc;
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
		int byteLength = 0;
		switch (this.getType()) {
		case FLOAT32:
			byteLength = ModbusRecordFloat32.BYTE_LENGTH;
			break;
		case FLOAT64:
			byteLength = ModbusRecordFloat64.BYTE_LENGTH;
			break;
		case STRING16:
			byteLength = ModbusRecordString16.BYTE_LENGTH;
			break;
		case UINT16:
			byteLength = ModbusRecordUint16.BYTE_LENGTH;
			break;
		case UINT32:
			byteLength = ModbusRecordUint32.BYTE_LENGTH;
			break;
		}
		this.writeValueBuffer = new Byte[byteLength];
	}

	/**
	 * Evaluate the AccessMode from configured Modbus-Api-AccessMode and
	 * Channel-AccessMode.
	 * 
	 * @param channelId
	 * @param channelAccessMode
	 * @return
	 */
	private static AccessMode evaluateActualAccessMode(ChannelId channelId, AccessMode modbusApiAccessMode) {
		AccessMode channelAccessMode = channelId.doc().getAccessMode();
		switch (modbusApiAccessMode) {
		case READ_ONLY:
			switch (channelAccessMode) {
			case READ_ONLY:
			case READ_WRITE:
			case WRITE_ONLY:
				return AccessMode.READ_ONLY;
			}
		case READ_WRITE:
			switch (channelAccessMode) {
			case READ_ONLY:
				return AccessMode.READ_ONLY;
			case READ_WRITE:
				return AccessMode.READ_WRITE;
			case WRITE_ONLY:
				return AccessMode.WRITE_ONLY;
			}
		case WRITE_ONLY:
			switch (channelAccessMode) {
			case READ_ONLY:
				return AccessMode.READ_ONLY;
			case READ_WRITE:
			case WRITE_ONLY:
				return AccessMode.WRITE_ONLY;
			}
		}
		// should never come here
		assert (true);
		return AccessMode.READ_ONLY;
	}

	public ChannelId getChannelId() {
		return channelId;
	}

	@Override
	public String toString() {
		return "ModbusRecordChannel [channelId=" + channelId + ", getOffset()=" + getOffset() + ", getType()="
				+ getType() + "]";
	}

	@Override
	public byte[] getValue(OpenemsComponent component) {
		Channel<?> channel = component.channel(this.channelId);
		Object value = channel.value().get();
		switch (this.getType()) {
		case FLOAT32:
			switch (this.accessMode) {
			case READ_ONLY:
			case READ_WRITE:
				return ModbusRecordFloat32.toByteArray(value);
			case WRITE_ONLY:
				return ModbusRecordFloat32Reserved.UNDEFINED_VALUE;
			}
		case FLOAT64:
			switch (this.accessMode) {
			case READ_ONLY:
			case READ_WRITE:
				return ModbusRecordFloat64.toByteArray(value);
			case WRITE_ONLY:
				return ModbusRecordFloat64Reserved.UNDEFINED_VALUE;
			}
		case STRING16:
			switch (this.accessMode) {
			case READ_ONLY:
			case READ_WRITE:
				return ModbusRecordString16.toByteArray(value);
			case WRITE_ONLY:
				return ModbusRecordString16Reserved.UNDEFINED_VALUE;
			}
		case UINT16:
			switch (this.accessMode) {
			case READ_ONLY:
			case READ_WRITE:
				return ModbusRecordUint16.toByteArray(value);
			case WRITE_ONLY:
				return ModbusRecordUint16Reserved.UNDEFINED_VALUE;
			}
		case UINT32:
			switch (this.accessMode) {
			case READ_ONLY:
			case READ_WRITE:
				return ModbusRecordUint32.toByteArray(value);
			case WRITE_ONLY:
				return ModbusRecordUint32Reserved.UNDEFINED_VALUE;
			}
		}
		assert true;
		return new byte[0];
	}

	public void onWriteValue(Consumer<Object> onWriteValueCallback) {
		this.onWriteValueCallback = onWriteValueCallback;
	}

	@Override
	public void writeValue(OpenemsComponent component, int index, byte byte1, byte byte2) {
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
		for (int i = 0; i < this.writeValueBuffer.length; i++) {
			if (this.writeValueBuffer[i] == null) {
				return; // no, it is not full
			}
		}

		// yes, it is full -> Prepare ByteBuffer
		ByteBuffer buff = ByteBuffer.allocate(this.writeValueBuffer.length);
		for (int i = 0; i < this.writeValueBuffer.length; i++) {
			buff.put(this.writeValueBuffer[i]);
		}
		buff.rewind();

		// clear buffer
		for (int i = 0; i < this.writeValueBuffer.length; i++) {
			this.writeValueBuffer[i] = null;
		}

		// Get Value-Object from ByteBuffer
		Object value = null;
		switch (this.getType()) {
		case FLOAT64:
			value = buff.getDouble();
			break;
		case FLOAT32:
			value = buff.getFloat();
			break;
		case STRING16:
			value = ""; // TODO implement String conversion
			break;
		case UINT16:
			value = buff.getShort();
			break;
		case UINT32:
			value = buff.getInt();
			break;
		}

		// Forward Value to ApiWorker
		if (this.onWriteValueCallback != null) {
			this.onWriteValueCallback.accept(value);
		} else {
			log.error("No onWriteValueCallback registered. What should I do with [" + value + "]?");
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
		Doc doc = this.channelId.doc();
		if (doc instanceof EnumDoc) {
			// List possible Options for this Enum
			EnumDoc d = (EnumDoc) doc;
			return Arrays.stream(d.getOptions()) //
					.map(option -> {
						return option.getValue() + ":" + option.getName();
					}) //
					.collect(Collectors.joining(", "));
		}

		return ""; // TODO get some meaningful text from Doc(), like 'between 0 and 100 %'
	}

	@Override
	public AccessMode getAccessMode() {
		return this.accessMode;
	}

}