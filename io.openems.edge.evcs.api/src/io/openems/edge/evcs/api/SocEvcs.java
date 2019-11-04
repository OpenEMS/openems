package io.openems.edge.evcs.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface SocEvcs extends Evcs {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Current SoC
		 * 
		 * <p>
		 * The current state of charge of the car
		 * 
		 * <ul>
		 * <li>Interface: SocEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: Percent
		 * </ul>
		 */
		SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_ONLY));

		// TODO: If there are EVCSs with more informations maybe a Channel
		// TIME_TILL_CHARGING_FINISHED is possible

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
