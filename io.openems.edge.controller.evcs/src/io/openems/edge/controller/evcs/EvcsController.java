package io.openems.edge.controller.evcs;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.controller.api.Controller;

public interface EvcsController extends Controller, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CHARGE_MODE(Doc.of(ChargeMode.values()) //
				.initialValue(ChargeMode.FORCE_CHARGE) //
				.text("Configured Charge-Mode")), //
		FORCE_CHARGE_MINPOWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).text("Minimum value for the force charge per Phase")),
		DEFAULT_CHARGE_MINPOWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("Minimum value for a default charge")),
		PRIORITY(Doc.of(Priority.values()) //
				.initialValue(Priority.CAR) //
				.text("Which component getting preferred")), //
		ENABLED_CHARGING(Doc.of(OpenemsType.BOOLEAN) //
				.text("Activates or deactivates the Charging")); //

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
