package io.openems.edge.goodwe.gridmeter;

import io.openems.common.channel.Level;
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
				.unit(Unit.NONE)), //

		HAS_NO_METER(Doc.of(Level.INFO) //
				.text("This GoodWe has no Grid-Meter. Meter can be deleted or hardware must be checked.")),

		// Connect status of L1
		METER_CON_CORRECTLY_L1(Doc.of(OpenemsType.BOOLEAN)), //
		METER_CON_REVERSE_L1(Doc.of(Level.WARNING) //
				.text("L1 (Phase R) - Connected reverse")), //
		METER_CON_INCORRECTLY_L1(Doc.of(Level.WARNING) //
				.text("L1 (Phase R) - Connected incorrectly")), //

		// Connect status of L2
		METER_CON_CORRECTLY_L2(Doc.of(OpenemsType.BOOLEAN)), //
		METER_CON_REVERSE_L2(Doc.of(Level.WARNING) //
				.text("L2 (Phase T) - Connected reverse")), //
		METER_CON_INCORRECTLY_L2(Doc.of(Level.WARNING) //
				.text("L2 (Phase T) - Connected incorrectly")), //

		// Connect status of L3
		METER_CON_CORRECTLY_L3(Doc.of(OpenemsType.BOOLEAN)), //
		METER_CON_REVERSE_L3(Doc.of(Level.WARNING) //
				.text("L3 (Phase S) - Connected reverse")), //
		METER_CON_INCORRECTLY_L3(Doc.of(Level.WARNING) //
				.text("L3 (Phase S) - Connected incorrectly")); //

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
