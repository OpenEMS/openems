package io.openems.edge.evse.chargepoint.smartfox;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.common.channel.Unit;
import io.openems.edge.evse.chargepoint.smartfox.enums.ChargeMode;

public interface EvseChargePointSmartfox extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Charge mode as reported by the SMARTFOX Energy Manager.
		 */
		CHARGE_MODE(Doc.of(ChargeMode.values())), //

		/**
		 * Manual charging value in percent, as configured on the SMARTFOX Energy
		 * Manager.
		 */
		MAN_CHARGING_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)), //

		/**
		 * Energy of the current/last charge session in [Wh].
		 */
		ENERGY_SESSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)) //
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
