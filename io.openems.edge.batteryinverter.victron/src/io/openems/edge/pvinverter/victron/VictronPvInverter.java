package io.openems.edge.pvinverter.victron;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.pvinverter.victron.enums.Position;

public interface VictronPvInverter extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		POSITION(Doc.of(Position.values()).accessMode(AccessMode.READ_ONLY)),
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
		TOTAL_POWER(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.KILOWATT)),
		MAXIMUM_POWER_CAPACITY(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.KILOWATT)),
		ACTIVE_PRODUCTION_ENERGY_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
		ACTIVE_PRODUCTION_ENERGY_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
		ACTIVE_PRODUCTION_ENERGY_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}

	}

	public default IntegerReadChannel getActiveProductionEnergyL1Channel() {
		return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY_L1);
	}

	public default IntegerReadChannel getActiveProductionEnergyL2Channel() {
		return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY_L2);
	}

	public default IntegerReadChannel getActiveProductionEnergyL3Channel() {
		return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY_L3);
	}
}
