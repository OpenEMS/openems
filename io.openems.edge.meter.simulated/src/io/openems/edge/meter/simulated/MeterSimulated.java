package io.openems.edge.meter.simulated;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * MeterSimulated interface — PPC Grid Meter custom channels.
 *
 * OpenEMS ElectricityMeter does NOT have APPARENT_POWER, POWER_FACTOR, or THD
 * built-in. We add them here as custom ChannelIds.
 *
 * PPC Grid parameter sheet (14 channels) → channel mapping:
 *   1  PAC   → ElectricityMeter.ACTIVE_POWER         (W,   built-in)
 *   2  QAC   → ElectricityMeter.REACTIVE_POWER       (var, built-in)
 *   3  UAC   → ElectricityMeter.VOLTAGE              (mV,  built-in)  ← kV from PPC
 *   4  IAC   → ElectricityMeter.CURRENT              (mA,  built-in)  ← A  from PPC
 *   5  UAC12 → ElectricityMeter.VOLTAGE_L1           (mV,  built-in)
 *   6  UAC23 → ElectricityMeter.VOLTAGE_L2           (mV,  built-in)
 *   7  UAC31 → ElectricityMeter.VOLTAGE_L3           (mV,  built-in)
 *   8  IAC1  → ElectricityMeter.CURRENT_L1           (mA,  built-in)
 *   9  IAC2  → ElectricityMeter.CURRENT_L2           (mA,  built-in)
 *  10  IAC3  → ElectricityMeter.CURRENT_L3           (mA,  built-in)
 *  11  FAC   → ElectricityMeter.FREQUENCY            (mHz, built-in)
 *  12  PF    → MeterSimulated.POWER_FACTOR           (THOUSANDTH, custom)
 *  13  SAC   → MeterSimulated.APPARENT_POWER         (VA,  custom)
 *  14  THD   → MeterSimulated.THD                    (THOUSANDTH, custom)
 *
 * Unit scaling (Node-RED → OpenEMS):
 *   Voltage   : kV  × 1_000_000 = µV? No — send mV (V × 1000). 415 V LL → 415000
 *   Current   : A   × 1000      = mA.   10 A → 10000
 *   Frequency : Hz  × 1000      = mHz.  50 Hz → 50000
 *   Power     : kW/kVAR × 1000  = W/var (Node-RED must convert kW → W)
 *   PF        : dimensionless × 1000.  0.95 → 950
 *   SAC       : kVA × 1000      = VA
 *   THD       : % × 10.          3.5% → 35
 */
public interface MeterSimulated extends ElectricityMeter, OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Power Factor (PF) — PPC Grid parameter #12.
         *
         * Unit: THOUSANDTH (PF × 1000).
         * Node-RED: send PF × 1000.  0.95 → 950,  1.00 → 1000.
         * Signed: negative = leading (capacitive), positive = lagging (inductive).
         * Modbus Reg 2013 (16-bit signed).
         */
        POWER_FACTOR(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.THOUSANDTH)
                .text("Power Factor PF (×1000; 1000=PF 1.00, 950=PF 0.95 lag)")),

        /**
         * Apparent Power (SAC) — PPC Grid parameter #13.
         *
         * Unit: VOLT_AMPERE (VA).
         * Node-RED: send in VA (kVA × 1000). 500 kVA → 500000.
         * Modbus Reg 2014–2015 (32-bit signed).
         */
        APPARENT_POWER(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.VOLT_AMPERE)
                .text("Apparent Power SAC [VA] (kVA × 1000 from PPC)")),

        /**
         * Total Harmonic Distortion (THD) — PPC Grid parameter #14.
         *
         * Unit: THOUSANDTH (THD% × 10 for 0.1% resolution).
         * Node-RED: send THD% × 10.  3.5% → 35.
         * Modbus Reg 2016 (16-bit unsigned).
         */
        THD(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.THOUSANDTH)
                .text("Total Harmonic Distortion THD (THD% × 10; 35 = 3.5%)"));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }
}