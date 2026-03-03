package io.openems.edge.pytes.dccharger;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface PytesDcCharger extends OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        // ---------------------------------------------------------------------
        // PV energy counters (from 33029..33039)
        // ---------------------------------------------------------------------

        PV_ENERGY_TOTAL_KWH(Doc.of(OpenemsType.LONG)
                .unit(Unit.KILOWATT_HOURS)
                .accessMode(AccessMode.READ_ONLY)),

        PV_ENERGY_MONTH_KWH(Doc.of(OpenemsType.LONG)
                .unit(Unit.KILOWATT_HOURS)
                .accessMode(AccessMode.READ_ONLY)),

        PV_ENERGY_LAST_MONTH_KWH(Doc.of(OpenemsType.LONG)
                .unit(Unit.KILOWATT_HOURS)
                .accessMode(AccessMode.READ_ONLY)),

        // NOTE: register is U16 with 0.1 kWh resolution
        PV_ENERGY_TODAY_0_1KWH(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOWATT_HOURS)
                .accessMode(AccessMode.READ_ONLY)),

        // NOTE: register is U16 with 0.1 kWh resolution
        PV_ENERGY_YESTERDAY_0_1KWH(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOWATT_HOURS)
                .accessMode(AccessMode.READ_ONLY)),

        PV_ENERGY_YEAR_KWH(Doc.of(OpenemsType.LONG)
                .unit(Unit.KILOWATT_HOURS)
                .accessMode(AccessMode.READ_ONLY)),

        PV_ENERGY_LAST_YEAR_KWH(Doc.of(OpenemsType.LONG)
                .unit(Unit.KILOWATT_HOURS)
                .accessMode(AccessMode.READ_ONLY)),

        // ---------------------------------------------------------------------
        // DC input configuration (33048)
        // ---------------------------------------------------------------------

        DC_INPUT_TYPE(Doc.of(OpenemsType.INTEGER)
                .accessMode(AccessMode.READ_ONLY)),

        // ---------------------------------------------------------------------
        // DC voltages/currents per input (33049..33056, 33059..33066)
        // Voltage: 0.1 V (U16) -> mapped with SCALE_FACTOR_2
        // Current: 0.1 A (U16) -> mapped with SCALE_FACTOR_2
        // ---------------------------------------------------------------------

        DC_VOLTAGE_1(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.VOLT)
                .accessMode(AccessMode.READ_ONLY)),
        DC_CURRENT_1(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.AMPERE)
                .accessMode(AccessMode.READ_ONLY)),

        DC_VOLTAGE_2(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.VOLT)
                .accessMode(AccessMode.READ_ONLY)),
        DC_CURRENT_2(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.AMPERE)
                .accessMode(AccessMode.READ_ONLY)),

        DC_VOLTAGE_3(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.VOLT)
                .accessMode(AccessMode.READ_ONLY)),
        DC_CURRENT_3(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.AMPERE)
                .accessMode(AccessMode.READ_ONLY)),

        DC_VOLTAGE_4(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.VOLT)
                .accessMode(AccessMode.READ_ONLY)),
        DC_CURRENT_4(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.AMPERE)
                .accessMode(AccessMode.READ_ONLY)),

        DC_VOLTAGE_5(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.VOLT)
                .accessMode(AccessMode.READ_ONLY)),
        DC_CURRENT_5(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.AMPERE)
                .accessMode(AccessMode.READ_ONLY)),

        DC_VOLTAGE_6(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.VOLT)
                .accessMode(AccessMode.READ_ONLY)),
        DC_CURRENT_6(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.AMPERE)
                .accessMode(AccessMode.READ_ONLY)),

        DC_VOLTAGE_7(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.VOLT)
                .accessMode(AccessMode.READ_ONLY)),
        DC_CURRENT_7(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.AMPERE)
                .accessMode(AccessMode.READ_ONLY)),

        DC_VOLTAGE_8(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.VOLT)
                .accessMode(AccessMode.READ_ONLY)),
        DC_CURRENT_8(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.AMPERE)
                .accessMode(AccessMode.READ_ONLY));

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
