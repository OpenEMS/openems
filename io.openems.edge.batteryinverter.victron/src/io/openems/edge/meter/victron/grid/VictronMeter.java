package io.openems.edge.meter.victron.grid;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

public interface VictronMeter extends SymmetricMeter, AsymmetricMeter {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ACTIVE_CONSUMPTION_ENERGY_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
		ACTIVE_CONSUMPTION_ENERGY_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
		ACTIVE_CONSUMPTION_ENERGY_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
		ACTIVE_PRODUCTION_ENERGY_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
		ACTIVE_PRODUCTION_ENERGY_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
		ACTIVE_PRODUCTION_ENERGY_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public default IntegerReadChannel getActiveConsumptionEnergyL1Channel() {
		return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
	}

	public default IntegerReadChannel getActiveConsumptionEnergyL2Channel() {
		return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
	}

	public default IntegerReadChannel getActiveConsumptionEnergyL3Channel() {
		return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);
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
