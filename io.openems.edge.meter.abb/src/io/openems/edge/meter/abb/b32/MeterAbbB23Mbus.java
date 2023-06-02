package io.openems.edge.meter.abb.b32;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

public interface MeterAbbB23Mbus extends SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		TOTAL_CONSUMED_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		MANUFACTURER_ID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)), //
		DEVICE_ID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)), //
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
