package io.openems.edge.meter.carlo.gavazzi.em100;

import static io.openems.common.channel.Unit.VOLT_AMPERE;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;

public interface MeterCarloGavazziEm100 extends ElectricityMeter, SinglePhaseMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		APPARENT_POWER(Doc.of(INTEGER) //
				.unit(VOLT_AMPERE)), //
		;

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
