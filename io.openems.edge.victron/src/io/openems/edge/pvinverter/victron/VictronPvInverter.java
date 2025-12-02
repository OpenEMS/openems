package io.openems.edge.pvinverter.victron;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.victron.enums.Position;

public interface VictronPvInverter extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		POSITION(Doc.of(Position.values()) //
				.accessMode(AccessMode.READ_ONLY)), //
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)), //
		MAXIMUM_POWER_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOWATT)), //
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
