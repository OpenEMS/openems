package io.openems.edge.evcs.goe.chargerhome;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;

public interface GoeChargerHome extends ManagedEvcs, Evcs, OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ALIAS(Doc.of(OpenemsType.STRING).text("A human-readable name of this Component")),
		PRODUCT(Doc.of(OpenemsType.STRING).text("Model name (variant)")),
		SERIAL(Doc.of(OpenemsType.STRING).text("Serial number")),
		FIRMWARE(Doc.of(OpenemsType.STRING).text("Firmware version")),
		STATUS_GOE(Doc.of(Status.values()).text("Current state of the charging station")),
		ERROR(Doc.of(Errors.values()).text("")),
		CURR_USER(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).text("Current preset value of the user")),
		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).text("Voltage on L1")),
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).text("Voltage on L2")),
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).text("Voltage on L3")),
		CURRENT_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).text("Current on L1")),
		CURRENT_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).text("Current on L2")),
		CURRENT_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).text("Current on L3")),
		ACTUAL_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIWATT).text("Total real power")),
		ENERGY_TOTAL(Doc.of(OpenemsType.INTEGER).unit(Unit.CUMULATED_WATT_HOURS).text("Total power consumption")),

		CHARGINGSTATION_STATE_ERROR(Doc.of(Level.WARNING));

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
