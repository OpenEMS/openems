package io.openems.edge.victron.pvinverter;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.Unit.KILOWATT;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.victron.enums.Position;

public interface VictronPvInverter extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		POSITION(Doc.of(Position.values())//
				.accessMode(READ_ONLY)), //
		SERIAL_NUMBER(Doc.of(STRING)//
				.accessMode(READ_ONLY)), //
		MAXIMUM_POWER_CAPACITY(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(KILOWATT)), //
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
