package io.openems.edge.io.shelly.common.component;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.TimedataProvider;

public interface ShellyMeteredSwitch extends SinglePhaseMeter, ElectricityMeter, TimedataProvider, ShellySwitch {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		; //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public static enum ErrorChannelId implements io.openems.edge.common.channel.ChannelId {
		OVERTEMP(Doc.of(Level.WARNING), "overtemp"), //
		OVERPOWER(Doc.of(Level.WARNING), "overpower"), //
		OVERVOLTAGE(Doc.of(Level.WARNING), "overvoltage"), //
		UNDERVOLTAGE(Doc.of(Level.WARNING), "undervoltage") //
		;

		private final Doc doc;
		private final String shellyErrorCode;

		private ErrorChannelId(Doc doc, String shellyErrorCode) {
			this.doc = doc;
			this.shellyErrorCode = shellyErrorCode;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

		public String getShellyErrorCode() {
			return this.shellyErrorCode;
		}
	}

}
