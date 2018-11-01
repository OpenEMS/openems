package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;

public class ModbusRecordChannel extends ModbusRecord {

	private final Logger log = LoggerFactory.getLogger(ModbusRecordChannel.class);

	private final ChannelId channelId;

	protected Consumer<Object> onWriteValueCallback = null;

	/**
	 * this is used to buffer calls to writeValue(). Once the buffer is full, the
	 * value is actually forwarded to the channel.
	 */
	private final Byte[] writeValueBuffer;

	public ModbusRecordChannel(int offset, ModbusType type, ChannelId channelId) {
		super(offset, type);
		this.channelId = channelId;

		// initialize buffer
		int byteLength = 0;
		switch (this.getType()) {
		case FLOAT32:
			byteLength = ModbusRecordFloat32.BYTE_LENGTH;
			break;
		case STRING16:
			byteLength = ModbusRecordString16.BYTE_LENGTH;
			break;
		case UINT16:
			byteLength = ModbusRecordUint16.BYTE_LENGTH;
			break;
		}
		this.writeValueBuffer = new Byte[byteLength];
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
			return ModbusRecordFloat32.toByteArray(value);
		case STRING16:
			return ModbusRecordString16.toByteArray(value);
		case UINT16:
			return ModbusRecordUint16.toByteArray(value);
		}
		assert true;
		return new byte[0];
	}

	public void onWriteValue(Consumer<Object> onWriteValueCallback) {
		this.onWriteValueCallback = onWriteValueCallback;
	}

	@Override
	public void writeValue(OpenemsComponent component, int index, byte byte1, byte byte2) {
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
		case FLOAT32:
			value = buff.getFloat();
			break;
		case STRING16:
			value = ""; // TODO implement String conversion
			break;
		case UINT16:
			value = buff.getShort();
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
		return ""; // TODO get some meaningful text from Doc(), like 'between 0 and 100 %'
	}

}