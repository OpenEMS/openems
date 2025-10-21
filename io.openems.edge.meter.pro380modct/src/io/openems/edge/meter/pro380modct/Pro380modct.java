package io.openems.edge.meter.pro380modct;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

public interface Pro380modct extends ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        APPARENT_POWER_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE)),
        APPARENT_POWER_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE)),
        APPARENT_POWER_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE)),
        APPARENT_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE)),
        POWER_FACTOR_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),
        POWER_FACTOR_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),
        POWER_FACTOR_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),
        POWER_FACTOR(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE));

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
