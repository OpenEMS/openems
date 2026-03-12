package io.openems.edge.io.shelly.shellypro3em;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBase;

public interface IoShellyPro3Em extends IoGen2ShellyBase {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Apparent Power.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT_AMPERE}
		 * </ul>
		 */
		APPARENT_POWER(new IntegerDoc()//
				.unit(Unit.VOLT_AMPERE)), //

		/**
		 * Apparent Power L1.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT_AMPERE}
		 * </ul>
		 */
		APPARENT_POWER_L1(new IntegerDoc()//
				.unit(Unit.VOLT_AMPERE)), //

		/**
		 * Apparent Power L2.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT_AMPERE}
		 * </ul>
		 */
		APPARENT_POWER_L2(new IntegerDoc()//
				.unit(Unit.VOLT_AMPERE)), //

		/**
		 * Apparent Power L3.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT_AMPERE}
		 * </ul>
		 */
		APPARENT_POWER_L3(new IntegerDoc()//
				.unit(Unit.VOLT_AMPERE)); //

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
