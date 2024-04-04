package io.openems.edge.deye.common.charger;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.timedata.api.TimedataProvider;

public interface DeyeSunPv extends EssDcCharger, OpenemsComponent, TimedataProvider {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		DEBUG_SET_PV_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),

		/**
		 * Curtail PV. Careful: this channel is shared between both Chargers.
		 */
		SET_PV_POWER_LIMIT(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_SET_PV_POWER_LIMIT)),

		// LongReadChannel
		BMS_DCDC_OUTPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		BMS_DCDC_INPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		BMS_DCDC_INPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		BMS_DCDC_INPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		BMS_DCDC_OUTPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		BMS_DCDC_OUTPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		PV_DCDC_INPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		PV_DCDC_OUTPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		PV_DCDC_INPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		PV_DCDC_INPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC_OUTPUT_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		PV_DCDC_OUTPUT_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //

		// IntegerReadChannel
		BMS_DCDC_OUTPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BMS_DCDC_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		BMS_DCDC_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BMS_DCDC_INPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		BMS_DCDC_INPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		BMS_DCDC_INPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BMS_DCDC_REACTOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BMS_DCDC_IGBT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		PV_DCDC_OUTPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		PV_DCDC_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		PV_DCDC_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PV_DCDC_INPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		PV_DCDC_INPUT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIWATT)), //
		PV_DCDC_INPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PV_DCDC_REACTOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		PV_DCDC_IGBT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)); //

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
