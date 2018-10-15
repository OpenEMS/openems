package io.openems.edge.common.modbusslave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.openems.edge.common.channel.doc.ChannelId;

public final class ModbusSlaveNatureTable {

	public static class Builder {
		private final int natureHash;
		private final int length;
		private final List<ModbusChannelMap> maps = new ArrayList<>();

		public Builder(Class<?> nature, int length) {
			this.natureHash = nature.getSimpleName().hashCode();
			this.length = length;
		}

		public Builder record(int offset, ChannelId channelId, ModbusType type) {
			// TODO validate that this 'offset' is not already used (e.g. by float32)
			this.maps.add(new ModbusChannelMap(offset, channelId, type));
			return this;
		}

		public ModbusSlaveNatureTable build() {
			Collections.sort(this.maps, (m1, m2) -> {
				return Integer.compare(m1.offset, m2.offset);
			});
			return new ModbusSlaveNatureTable(natureHash, length,
					this.maps.toArray(new ModbusChannelMap[this.maps.size()]));
		}
	}

	public static Builder of(Class<?> nature, int length) {
		return new Builder(nature, length);
	}

	private final int natureHash;
	private final int length;
	private final ModbusChannelMap[] modbusChannelMaps;

	private ModbusSlaveNatureTable(int natureHash, int length, ModbusChannelMap[] modbusChannelMaps) {
		this.natureHash = natureHash;
		this.length = length;
		this.modbusChannelMaps = modbusChannelMaps;
	}

	public int getNatureHash() {
		return natureHash;
	}

	public int getLength() {
		return length;
	}

	public ModbusChannelMap[] getModbusChannelMaps() {
		return modbusChannelMaps;
	}
}
