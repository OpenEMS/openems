package io.openems.edge.goodwe.gridmeter;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

public interface GoodWeGridMeter extends AsymmetricMeter, SymmetricMeter, OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		F_GRID_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //

		F_GRID_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //

		F_GRID_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //

		P_GRID_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		METER_POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)); //

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
