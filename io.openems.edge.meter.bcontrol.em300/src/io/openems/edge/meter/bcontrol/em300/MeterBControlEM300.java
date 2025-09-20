package io.openems.edge.meter.bcontrol.em300;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface MeterBControlEM300 extends ElectricityMeter, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ACTIVE_POWER_POS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		ACTIVE_POWER_NEG(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		ACTIVE_POWER_L1_POS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		ACTIVE_POWER_L1_NEG(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		ACTIVE_POWER_L2_POS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		ACTIVE_POWER_L2_NEG(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		ACTIVE_POWER_L3_POS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		ACTIVE_POWER_L3_NEG(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		REACTIVE_POWER_POS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_NEG(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_L1_POS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_L1_NEG(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_L2_POS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_L2_NEG(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_L3_POS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_L3_NEG(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

}
