package io.openems.edge.goodwe.gridmeter;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

public interface GoodWeGridMeter extends ElectricityMeter, OpenemsComponent {

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
				.text("L3 (Phase S) - Connected incorrectly")),
		EXTERNAL_METER_RATIO(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)
				.text("External meter ratio (e.g. the selected CT is 3000A:5A, the CT ratio value is 600")); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#HAS_NO_METER}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getHasNoMeterChannel() {
		return this.channel(ChannelId.HAS_NO_METER);
	}

	/**
	 * Gets the "Has-No-Meter" state. See {@link ChannelId#HAS_NO_METER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getHasNoMeter() {
		return this.getHasNoMeterChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#EXTERNAL_METER_RATIO}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getExternalMeterRatioChannel() {
		return this.channel(ChannelId.EXTERNAL_METER_RATIO);
	}
}
