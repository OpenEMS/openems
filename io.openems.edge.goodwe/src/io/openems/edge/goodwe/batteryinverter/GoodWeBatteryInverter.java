package io.openems.edge.goodwe.batteryinverter;

import io.openems.common.channel.Level;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.goodwe.common.GoodWe;

public interface GoodWeBatteryInverter
		extends GoodWe, ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		SMART_MODE_NOT_WORKING_WITH_PID_FILTER(Doc.of(Level.WARNING) //
				.text("SMART mode does not work correctly with active PID filter")),
		NO_SMART_METER_DETECTED(Doc.of(Level.WARNING) //
				.text("No GoodWe Smart Meter detected. Only REMOTE mode can work correctly"));

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
