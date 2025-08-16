package io.openems.edge.meter.eastron.sdm630;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

public interface MeterEastronSdm630 extends SymmetricMeter, AsymmetricMeter, OpenemsComponent, ModbusSlave {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		REACTIVE_PRODUCTION_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		REACTIVE_CONSUMPTION_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		;

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

}
