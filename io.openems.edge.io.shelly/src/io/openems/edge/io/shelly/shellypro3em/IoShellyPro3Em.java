package io.openems.edge.io.shelly.shellypro3em;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface IoShellyPro3Em extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		TOTAL_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.accessMode(AccessMode.READ_ONLY)), //
		// AC L1 Apparent Power
		APPARENT_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.accessMode(AccessMode.READ_ONLY)), //
		// AC L2 Apparent Power
		APPARENT_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.accessMode(AccessMode.READ_ONLY)), //
		// AC L3 Apparent Power
		APPARENT_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.accessMode(AccessMode.READ_ONLY)), //

		// AC L1 Power Factor
		COS_PHI_L1(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_ONLY)), //
		// AC L2 Power Factor
		COS_PHI_L2(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_ONLY)), //
		// AC L3 Power Factor
		COS_PHI_L3(Doc.of(OpenemsType.FLOAT) //
				.accessMode(AccessMode.READ_ONLY)), //

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
