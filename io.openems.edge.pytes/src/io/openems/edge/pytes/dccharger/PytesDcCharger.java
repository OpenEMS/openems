package io.openems.edge.pytes.dccharger;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.pytes.enums.DcInputType;

public interface PytesDcCharger extends EssDcCharger, OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
	        // -----------------------------------------------------------------------
	        // PV Energy Counters (reg 33029..33039)
	        // All counters are cumulative totals from the inverter lifetime.
	        // Priority LOW – historical totals, slow-changing.
	        // -----------------------------------------------------------------------

	        /**
	         * Total PV energy generated since installation (reg 33029, U32).
	         * For Hybrid models: shows PV generation of this inverter.
	         * For AC Coupled models: shows generation of parallel PV inverters.
	         * Datasheet: 1 kWh resolution → no converter needed.
	         * Unit: kWh
	         */
	        PV_ENERGY_TOTAL_KWH(Doc.of(LONG) //
	        	.unit(Unit.KILOWATT_HOURS) //
	        	.accessMode(READ_ONLY)),

	        /**
	         * PV energy generated in the current month (reg 33031, U32).
	         * Datasheet: 1 kWh resolution → no converter needed.
	         * Unit: kWh
	         */
	        PV_ENERGY_MONTH_KWH(Doc.of(LONG) //
	        	.unit(Unit.KILOWATT_HOURS) //
	        	.accessMode(READ_ONLY)),

	        /**
	         * PV energy generated in the previous month (reg 33033, U32).
	         * Datasheet: 1 kWh resolution → no converter needed.
	         * Unit: kWh
	         */
	        PV_ENERGY_LAST_MONTH_KWH(Doc.of(LONG) //
	        	.unit(Unit.KILOWATT_HOURS) //
	        	.accessMode(READ_ONLY)),

	        /**
	         * PV energy generated today (reg 33035, U16).
	         * For Hybrid models: shows PV generation of this inverter.
	         * For AC Coupled models: shows generation of parallel PV inverters.
	         * Datasheet: 0.1 kWh resolution → SCALE_FACTOR_2 → Wh.
	         * Unit: Wh
	         */
	        PV_ENERGY_TODAY(Doc.of(INTEGER) //
	        	.unit(Unit.WATT_HOURS) //
	        	.accessMode(READ_ONLY)),

	        /**
	         * PV energy generated yesterday (reg 33036, U16).
	         * Datasheet: 0.1 kWh resolution → SCALE_FACTOR_2 → Wh.
	         * Unit: Wh
	         */
	        PV_ENERGY_YESTERDAY(Doc.of(INTEGER) //
	        	.unit(Unit.WATT_HOURS) //
	        	.accessMode(READ_ONLY)),

	        /**
	         * PV energy generated in the current year (reg 33037, U32).
	         * Datasheet: 1 kWh resolution → no converter needed.
	         * Unit: kWh
	         */
	        PV_ENERGY_YEAR_KWH(Doc.of(LONG) //
	        	.unit(Unit.KILOWATT_HOURS) //
	        	.accessMode(READ_ONLY)),

	        /**
	         * PV energy generated in the previous year (reg 33039, U32).
	         * Datasheet: 1 kWh resolution → no converter needed.
	         * Unit: kWh
	         */
	        PV_ENERGY_LAST_YEAR_KWH(Doc.of(LONG) //
	        	.unit(Unit.KILOWATT_HOURS) //
	        	.accessMode(READ_ONLY)),

	        // -----------------------------------------------------------------------
	        // DC Input Configuration (reg 33048)
	        // Priority LOW – set by hardware, never changes at runtime.
	        // -----------------------------------------------------------------------

	        /**
	         * Number of DC input strings (MPPT channels) connected (reg 33048, U16).
	         * See {@link DcInputType} for possible values (1–8 inputs).
	         * Read-only — determined by the hardware configuration of the inverter model.
	         */
	        DC_INPUT_TYPE(Doc.of(DcInputType.values()) //
	        	.accessMode(READ_ONLY)),

	        // -----------------------------------------------------------------------
	        // DC Voltages and Currents per input string (reg 33049..33066)
	        // All voltages: 0.1 V resolution → SCALE_FACTOR_2 (×100) → mV
	        // All currents: 0.1 A resolution → SCALE_FACTOR_2 (×100) → mA
	        // Priority HIGH – real-time PV string monitoring.
	        // -----------------------------------------------------------------------

	        /**
	         * DC input string 1 voltage (reg 33049, U16).
	         * Datasheet: 0.1 V resolution → SCALE_FACTOR_2 → mV.
	         * Unit: mV
	         */
	        DC_VOLTAGE_1(Doc.of(INTEGER).unit(Unit.MILLIVOLT).accessMode(READ_ONLY)),

	        /**
	         * DC input string 1 current (reg 33050, U16).
	         * Datasheet: 0.1 A resolution → SCALE_FACTOR_2 → mA.
	         * Unit: mA
	         */
	        DC_CURRENT_1(Doc.of(INTEGER).unit(Unit.MILLIAMPERE).accessMode(READ_ONLY)),

	        /**
	         * DC input string 2 voltage (reg 33051, U16).
	         * Datasheet: 0.1 V resolution → SCALE_FACTOR_2 → mV.
	         * Unit: mV
	         */
	        DC_VOLTAGE_2(Doc.of(INTEGER).unit(Unit.MILLIVOLT).accessMode(READ_ONLY)),

	        /**
	         * DC input string 2 current (reg 33052, U16).
	         * Datasheet: 0.1 A resolution → SCALE_FACTOR_2 → mA.
	         * Unit: mA
	         */
	        DC_CURRENT_2(Doc.of(INTEGER).unit(Unit.MILLIAMPERE).accessMode(READ_ONLY)),

	        /**
	         * DC input string 3 voltage (reg 33053, U16).
	         * Datasheet: 0.1 V resolution → SCALE_FACTOR_2 → mV.
	         * Unit: mV
	         */
	        DC_VOLTAGE_3(Doc.of(INTEGER).unit(Unit.MILLIVOLT).accessMode(READ_ONLY)),

	        /**
	         * DC input string 3 current (reg 33054, U16).
	         * Datasheet: 0.1 A resolution → SCALE_FACTOR_2 → mA.
	         * Unit: mA
	         */
	        DC_CURRENT_3(Doc.of(INTEGER).unit(Unit.MILLIAMPERE).accessMode(READ_ONLY)),

	        /**
	         * DC input string 4 voltage (reg 33055, U16).
	         * Datasheet: 0.1 V resolution → SCALE_FACTOR_2 → mV.
	         * Unit: mV
	         */
	        DC_VOLTAGE_4(Doc.of(INTEGER).unit(Unit.MILLIVOLT).accessMode(READ_ONLY)),

	        /**
	         * DC input string 4 current (reg 33056, U16).
	         * Datasheet: 0.1 A resolution → SCALE_FACTOR_2 → mA.
	         * Unit: mA
	         */
	        DC_CURRENT_4(Doc.of(INTEGER).unit(Unit.MILLIAMPERE).accessMode(READ_ONLY)),

	        /**
	         * DC input string 5 voltage (reg 33059, U16).
	         * Datasheet: 0.1 V resolution → SCALE_FACTOR_2 → mV.
	         * Unit: mV
	         */
	        DC_VOLTAGE_5(Doc.of(INTEGER).unit(Unit.MILLIVOLT).accessMode(READ_ONLY)),

	        /**
	         * DC input string 5 current (reg 33060, U16).
	         * Datasheet: 0.1 A resolution → SCALE_FACTOR_2 → mA.
	         * Unit: mA
	         */
	        DC_CURRENT_5(Doc.of(INTEGER).unit(Unit.MILLIAMPERE).accessMode(READ_ONLY)),

	        /**
	         * DC input string 6 voltage (reg 33061, U16).
	         * Datasheet: 0.1 V resolution → SCALE_FACTOR_2 → mV.
	         * Unit: mV
	         */
	        DC_VOLTAGE_6(Doc.of(INTEGER).unit(Unit.MILLIVOLT).accessMode(READ_ONLY)),

	        /**
	         * DC input string 6 current (reg 33062, U16).
	         * Datasheet: 0.1 A resolution → SCALE_FACTOR_2 → mA.
	         * Unit: mA
	         */
	        DC_CURRENT_6(Doc.of(INTEGER).unit(Unit.MILLIAMPERE).accessMode(READ_ONLY)),

	        /**
	         * DC input string 7 voltage (reg 33063, U16).
	         * Datasheet: 0.1 V resolution → SCALE_FACTOR_2 → mV.
	         * Unit: mV
	         */
	        DC_VOLTAGE_7(Doc.of(INTEGER).unit(Unit.MILLIVOLT).accessMode(READ_ONLY)),

	        /**
	         * DC input string 7 current (reg 33064, U16).
	         * Datasheet: 0.1 A resolution → SCALE_FACTOR_2 → mA.
	         * Unit: mA
	         */
	        DC_CURRENT_7(Doc.of(INTEGER).unit(Unit.MILLIAMPERE).accessMode(READ_ONLY)),

	        /**
	         * DC input string 8 voltage (reg 33065, U16).
	         * Datasheet: 0.1 V resolution → SCALE_FACTOR_2 → mV.
	         * Unit: mV
	         */
	        DC_VOLTAGE_8(Doc.of(INTEGER).unit(Unit.MILLIVOLT).accessMode(READ_ONLY)),

	        /**
	         * DC input string 8 current (reg 33066, U16).
	         * Datasheet: 0.1 A resolution → SCALE_FACTOR_2 → mA.
	         * Unit: mA
	         */
	        DC_CURRENT_8(Doc.of(INTEGER).unit(Unit.MILLIAMPERE).accessMode(READ_ONLY));

	        private final Doc doc;

	        private ChannelId(Doc doc) {
	        	this.doc = doc;
	        }

	        @Override
	        public Doc doc() {
	        	return this.doc;
	        }
        }


	// -----------------------------------------------------------------------
	// Accessor methods – PV Energy Counters
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#PV_ENERGY_TOTAL_KWH}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getPvEnergyTotalKwhChannel() {
		return this.channel(ChannelId.PV_ENERGY_TOTAL_KWH);
	}

	/**
	 * Total PV energy generated [kWh]. See {@link ChannelId#PV_ENERGY_TOTAL_KWH}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getPvEnergyTotalKwh() {
		return this.getPvEnergyTotalKwhChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#PV_ENERGY_MONTH_KWH}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getPvEnergyMonthKwhChannel() {
		return this.channel(ChannelId.PV_ENERGY_MONTH_KWH);
	}

	/**
	 * PV energy generated this month [kWh]. See {@link ChannelId#PV_ENERGY_MONTH_KWH}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getPvEnergyMonthKwh() {
		return this.getPvEnergyMonthKwhChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#PV_ENERGY_LAST_MONTH_KWH}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getPvEnergyLastMonthKwhChannel() {
		return this.channel(ChannelId.PV_ENERGY_LAST_MONTH_KWH);
	}

	/**
	 * PV energy generated last month [kWh]. See {@link ChannelId#PV_ENERGY_LAST_MONTH_KWH}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getPvEnergyLastMonthKwh() {
		return this.getPvEnergyLastMonthKwhChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#PV_ENERGY_TODAY}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPvEnergyTodayChannel() {
		return this.channel(ChannelId.PV_ENERGY_TODAY);
	}

	/**
	 * PV energy generated today [Wh]. See {@link ChannelId#PV_ENERGY_TODAY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPvEnergyToday() {
		return this.getPvEnergyTodayChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#PV_ENERGY_YESTERDAY}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPvEnergyYesterdayChannel() {
		return this.channel(ChannelId.PV_ENERGY_YESTERDAY);
	}

	/**
	 * PV energy generated yesterday [Wh]. See {@link ChannelId#PV_ENERGY_YESTERDAY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPvEnergyYesterday() {
		return this.getPvEnergyYesterdayChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#PV_ENERGY_YEAR_KWH}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getPvEnergyYearKwhChannel() {
		return this.channel(ChannelId.PV_ENERGY_YEAR_KWH);
	}

	/**
	 * PV energy generated this year [kWh]. See {@link ChannelId#PV_ENERGY_YEAR_KWH}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getPvEnergyYearKwh() {
		return this.getPvEnergyYearKwhChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#PV_ENERGY_LAST_YEAR_KWH}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getPvEnergyLastYearKwhChannel() {
		return this.channel(ChannelId.PV_ENERGY_LAST_YEAR_KWH);
	}

	/**
	 * PV energy generated last year [kWh]. See {@link ChannelId#PV_ENERGY_LAST_YEAR_KWH}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getPvEnergyLastYearKwh() {
		return this.getPvEnergyLastYearKwhChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – DC Input Configuration
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#DC_INPUT_TYPE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcInputTypeChannel() {
		return this.channel(ChannelId.DC_INPUT_TYPE);
	}

	/**
	 * Number of DC input strings connected. See {@link ChannelId#DC_INPUT_TYPE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcInputType() {
		return this.getDcInputTypeChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – DC Voltages and Currents
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#DC_VOLTAGE_1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcVoltage1Channel() {
		return this.channel(ChannelId.DC_VOLTAGE_1);
	}

	/**
	 * DC string 1 voltage [mV]. See {@link ChannelId#DC_VOLTAGE_1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcVoltage1() {
		return this.getDcVoltage1Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_CURRENT_1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcCurrent1Channel() {
		return this.channel(ChannelId.DC_CURRENT_1);
	}

	/**
	 * DC string 1 current [mA]. See {@link ChannelId#DC_CURRENT_1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcCurrent1() {
		return this.getDcCurrent1Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_VOLTAGE_2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcVoltage2Channel() {
		return this.channel(ChannelId.DC_VOLTAGE_2);
	}

	/**
	 * DC string 2 voltage [mV]. See {@link ChannelId#DC_VOLTAGE_2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcVoltage2() {
		return this.getDcVoltage2Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_CURRENT_2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcCurrent2Channel() {
		return this.channel(ChannelId.DC_CURRENT_2);
	}

	/**
	 * DC string 2 current [mA]. See {@link ChannelId#DC_CURRENT_2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcCurrent2() {
		return this.getDcCurrent2Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_VOLTAGE_3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcVoltage3Channel() {
		return this.channel(ChannelId.DC_VOLTAGE_3);
	}

	/**
	 * DC string 3 voltage [mV]. See {@link ChannelId#DC_VOLTAGE_3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcVoltage3() {
		return this.getDcVoltage3Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_CURRENT_3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcCurrent3Channel() {
		return this.channel(ChannelId.DC_CURRENT_3);
	}

	/**
	 * DC string 3 current [mA]. See {@link ChannelId#DC_CURRENT_3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcCurrent3() {
		return this.getDcCurrent3Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_VOLTAGE_4}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcVoltage4Channel() {
		return this.channel(ChannelId.DC_VOLTAGE_4);
	}

	/**
	 * DC string 4 voltage [mV]. See {@link ChannelId#DC_VOLTAGE_4}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcVoltage4() {
		return this.getDcVoltage4Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_CURRENT_4}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcCurrent4Channel() {
		return this.channel(ChannelId.DC_CURRENT_4);
	}

	/**
	 * DC string 4 current [mA]. See {@link ChannelId#DC_CURRENT_4}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcCurrent4() {
		return this.getDcCurrent4Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_VOLTAGE_5}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcVoltage5Channel() {
		return this.channel(ChannelId.DC_VOLTAGE_5);
	}

	/**
	 * DC string 5 voltage [mV]. See {@link ChannelId#DC_VOLTAGE_5}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcVoltage5() {
		return this.getDcVoltage5Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_CURRENT_5}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcCurrent5Channel() {
		return this.channel(ChannelId.DC_CURRENT_5);
	}

	/**
	 * DC string 5 current [mA]. See {@link ChannelId#DC_CURRENT_5}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcCurrent5() {
		return this.getDcCurrent5Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_VOLTAGE_6}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcVoltage6Channel() {
		return this.channel(ChannelId.DC_VOLTAGE_6);
	}

	/**
	 * DC string 6 voltage [mV]. See {@link ChannelId#DC_VOLTAGE_6}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcVoltage6() {
		return this.getDcVoltage6Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_CURRENT_6}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcCurrent6Channel() {
		return this.channel(ChannelId.DC_CURRENT_6);
	}

	/**
	 * DC string 6 current [mA]. See {@link ChannelId#DC_CURRENT_6}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcCurrent6() {
		return this.getDcCurrent6Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_VOLTAGE_7}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcVoltage7Channel() {
		return this.channel(ChannelId.DC_VOLTAGE_7);
	}

	/**
	 * DC string 7 voltage [mV]. See {@link ChannelId#DC_VOLTAGE_7}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcVoltage7() {
		return this.getDcVoltage7Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_CURRENT_7}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcCurrent7Channel() {
		return this.channel(ChannelId.DC_CURRENT_7);
	}

	/**
	 * DC string 7 current [mA]. See {@link ChannelId#DC_CURRENT_7}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcCurrent7() {
		return this.getDcCurrent7Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_VOLTAGE_8}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcVoltage8Channel() {
		return this.channel(ChannelId.DC_VOLTAGE_8);
	}

	/**
	 * DC string 8 voltage [mV]. See {@link ChannelId#DC_VOLTAGE_8}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcVoltage8() {
		return this.getDcVoltage8Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_CURRENT_8}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcCurrent8Channel() {
		return this.channel(ChannelId.DC_CURRENT_8);
	}

	/**
	 * DC string 8 current [mA]. See {@link ChannelId#DC_CURRENT_8}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcCurrent8() {
		return this.getDcCurrent8Channel().value();
	}

}
