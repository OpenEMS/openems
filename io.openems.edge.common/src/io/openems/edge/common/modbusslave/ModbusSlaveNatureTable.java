package io.openems.edge.common.modbusslave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.openems.edge.common.channel.doc.ChannelId;

public final class ModbusSlaveNatureTable {

	public static class Builder {
		private final short natureHash;
		private final int length;
		private final List<ModbusRecord> maps = new ArrayList<>();

		private int nextOffset = 0;

		public Builder(Class<?> nature, int length) {
			this.natureHash = (short) nature.getSimpleName().hashCode();
			this.length = length;
		}

		public Builder channel(int offset, ChannelId channelId, ModbusType type) {
			this.add(new ModbusRecordChannel(offset, type, channelId));
			return this;
		}

		public Builder uint16(int offset, short value) {
			this.add(new ModbusRecordUint16(offset, value));
			return this;
		}

		public Builder uint16Reserved(int offset) {
			this.add(new ModbusRecordUint16Reserved(offset));
			return this;
		}

		public Builder float32(int offset, float value) {
			this.add(new ModbusRecordFloat32(offset, value));
			return this;
		}

		public Builder float32Reserved(int offset) {
			this.add(new ModbusRecordFloat32Reserved(offset));
			return this;
		}

		public Builder string16(int offset, String value) {
			this.add(new ModbusRecordString16(offset, value));
			return this;
		}

		private void add(ModbusRecord record) throws IllegalArgumentException {
			if (record.getOffset() != this.nextOffset) {
				throw new IllegalArgumentException("Expected offset [" + this.nextOffset + "] but got ["
						+ record.getOffset() + "] for Record [" + record + "]");
			}
			this.nextOffset += record.getType().getWords();
			// TODO validate that this 'offset' is not already used (e.g. by float32)
			this.maps.add(record);
		}

		public ModbusSlaveNatureTable build() {
			Collections.sort(this.maps, (m1, m2) -> {
				return Integer.compare(m1.getOffset(), m2.getOffset());
			});
			return new ModbusSlaveNatureTable(natureHash, length,
					this.maps.toArray(new ModbusRecord[this.maps.size()]));
		}
	}

	public static Builder of(Class<?> nature, int length) {
		return new Builder(nature, length);
	}

	private final short natureHash;
	private final int length;
	private final ModbusRecord[] modbusRecords;

	private ModbusSlaveNatureTable(short natureHash, int length, ModbusRecord[] modbusRecords) {
		this.natureHash = natureHash;
		this.length = length;
		this.modbusRecords = modbusRecords;
	}

	public short getNatureHash() {
		return natureHash;
	}

	public int getLength() {
		return length;
	}

	public ModbusRecord[] getModbusRecords() {
		return modbusRecords;
	}
}
