package io.openems.edge.common.modbusslave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.openems.edge.common.channel.doc.ChannelId;

public final class ModbusSlaveNatureTable {

	public static class Builder {
		private final Class<?> nature;
		private final int length;
		private final List<ModbusRecord> maps = new ArrayList<>();

		private int nextOffset = 0;

		public Builder(Class<?> nature, int length) {
			this.nature = nature;
			this.length = length;
		}

		public Builder channel(int offset, ChannelId channelId, ModbusType type) {
			this.add(new ModbusRecordChannel(offset, type, channelId));
			return this;
		}

		public Builder uint16(int offset, String name, short value) {
			this.add(new ModbusRecordUint16(offset, name, value));
			return this;
		}

		public Builder uint16Hash(int offset, String text) {
			this.add(new ModbusRecordUint16Hash(offset, text));
			return this;
		}

		public Builder uint16Reserved(int offset) {
			this.add(new ModbusRecordUint16Reserved(offset));
			return this;
		}

		public Builder float32(int offset, String name, float value) {
			this.add(new ModbusRecordFloat32(offset, name, value));
			return this;
		}

		public Builder float32Reserved(int offset) {
			this.add(new ModbusRecordFloat32Reserved(offset));
			return this;
		}

		public Builder string16(int offset, String name, String value) {
			this.add(new ModbusRecordString16(offset, name, value));
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
			return new ModbusSlaveNatureTable(nature, length, this.maps.toArray(new ModbusRecord[this.maps.size()]));
		}
	}

	public static Builder of(Class<?> nature, int length) {
		return new Builder(nature, length);
	}

	private final Class<?> nature;
	private final int length;
	private final ModbusRecord[] modbusRecords;

	private ModbusSlaveNatureTable(Class<?> nature, int length, ModbusRecord[] modbusRecords) {
		this.nature = nature;
		this.length = length;
		this.modbusRecords = modbusRecords;
	}

	public Class<?> getNature() {
		return nature;
	}

	public int getLength() {
		return length;
	}

	public ModbusRecord[] getModbusRecords() {
		return modbusRecords;
	}
}
