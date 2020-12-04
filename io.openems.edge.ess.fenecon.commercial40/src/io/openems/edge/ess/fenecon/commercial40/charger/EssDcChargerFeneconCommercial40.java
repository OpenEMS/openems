package io.openems.edge.ess.fenecon.commercial40.charger;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.timedata.api.TimedataProvider;

public interface EssDcChargerFeneconCommercial40 extends EssDcCharger, OpenemsComponent, TimedataProvider {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		DEBUG_SET_PV_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		SET_PV_POWER_LIMIT(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_SET_PV_POWER_LIMIT))), //

		// LongReadChannel
		BMS_DCDC0_OUTPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC0_INPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC0_INPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC0_INPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC0_OUTPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC0_OUTPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC1_INPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC1_OUTPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC1_INPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC1_INPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC1_OUTPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		BMS_DCDC1_OUTPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC0_INPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC0_OUTPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC0_INPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC0_INPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC0_OUTPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC0_OUTPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC1_INPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC1_OUTPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC1_INPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC1_INPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC1_OUTPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC1_OUTPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //

		// IntegerReadChannel
		BMS_DCDC0_OUTPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BMS_DCDC0_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		BMS_DCDC0_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BMS_DCDC0_INPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		BMS_DCDC0_INPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		BMS_DCDC0_INPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BMS_DCDC0_REACTOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BMS_DCDC0_IGBT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BMS_DCDC1_OUTPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BMS_DCDC1_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		BMS_DCDC1_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BMS_DCDC1_INPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		BMS_DCDC1_INPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		BMS_DCDC1_INPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BMS_DCDC1_REACTOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BMS_DCDC1_IGBT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		PV_DCDC0_OUTPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		PV_DCDC0_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		PV_DCDC0_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PV_DCDC0_INPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		PV_DCDC0_INPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		PV_DCDC0_INPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PV_DCDC0_REACTOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		PV_DCDC0_IGBT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		PV_DCDC1_OUTPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		PV_DCDC1_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		PV_DCDC1_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PV_DCDC1_INPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		PV_DCDC1_INPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		PV_DCDC1_INPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PV_DCDC1_REACTOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		PV_DCDC1_IGBT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}
}
