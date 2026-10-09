package io.openems.edge.batteryinverter.custom;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * iPLON BatteryInverter channel definitions.
 *
 * All iPLON registers work in kW / kVar.
 * Scaling to W is done manually in BatteryInverterImpl.propagateScaledChannels().
 *
 * READ channels  — FC3 (iPLON → OpenEMS)
 * WRITE channels — FC16 (OpenEMS → iPLON)
 */
public interface BatteryInverterCustom extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // ────────────────────────────────────────────────────────────────────
        // READ — status registers
        // ────────────────────────────────────────────────────────────────────

        /**
         * Reg 1 — EMS Working Mode (R).
         * 0=Manual, 1=AutoFollow, 2=ConstantPower, 3=iPLONControl
         */
        EMS_WORKING_MODE(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_ONLY)
                .text("0=Manual;1=AutoFollow;2=ConstantPower;3=iPLONControl")),

        /**
         * Reg 2 — Charging / Discharging Status (R).
         * 0=Standby, 1=Discharge, 2=Charge
         */
        CHARGE_DISCHARGE_STATUS(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_ONLY)
                .text("0=Standby;1=Discharge;2=Charge")),

        /**
         * Reg 11 — Active Power Setpoint Return Value (R). Unit: kW.
         * What iPLON actually received and confirmed.
         */
        ACTIVE_POWER_SETPOINT_RETURN(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOWATT)
                .accessMode(AccessMode.READ_ONLY)
                .text("Active power setpoint echo from iPLON kW")),

        /**
         * Reg 12 — Reactive Power Setpoint Return Value (R). Unit: kVar.
         */
        REACTIVE_POWER_SETPOINT_RETURN(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOVOLT_AMPERE_REACTIVE)
                .accessMode(AccessMode.READ_ONLY)
                .text("Reactive power setpoint echo from iPLON kVar")),

        /**
         * Reg 29 — Actual Active Power raw (R). Unit: kW.
         * Scaled to W and written to SymmetricBatteryInverter.ACTIVE_POWER
         * by propagateScaledChannels() each cycle.
         */
        ACTUAL_ACTIVE_POWER_KW(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOWATT)
                .accessMode(AccessMode.READ_ONLY)
                .text("Actual active power from iPLON in kW (raw)")),

        /**
         * Reg 30 — Actual Reactive Power raw (R). Unit: kVar.
         */
        ACTUAL_REACTIVE_POWER_KVAR(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOVOLT_AMPERE_REACTIVE)
                .accessMode(AccessMode.READ_ONLY)
                .text("Actual reactive power from iPLON in kVar (raw)")),

        /**
         * Reg 31 — SOC raw (R). Value = % × 10.
         * 500 = 50.0%. Informational — authoritative SOC is on Battery component.
         */
        SOC_RAW(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.PERCENT)
                .accessMode(AccessMode.READ_ONLY)
                .text("SOC raw from iPLON x0.1 percent (500=50%)")),

        /**
         * Reg 32 — Daily Charge Quantity (R). Unit: kWh.
         */
        DAILY_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOWATT_HOURS)
                .accessMode(AccessMode.READ_ONLY)),

        /**
         * Reg 33 — Daily Discharge Quantity (R). Unit: kWh.
         */
        DAILY_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOWATT_HOURS)
                .accessMode(AccessMode.READ_ONLY)),

        /**
         * Reg 34 — Cumulative Charge Quantity (R). Unit: kWh.
         */
        TOTAL_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOWATT_HOURS)
                .accessMode(AccessMode.READ_ONLY)),

        /**
         * Reg 36 — Cumulative Discharge Quantity (R). Unit: kWh.
         */
        TOTAL_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOWATT_HOURS)
                .accessMode(AccessMode.READ_ONLY)),

        /**
         * Reg 38 — Max Allowed Discharge Power raw (R). Unit: kW.
         * Scaled to positive W and written to ManagedSymmetricBatteryInverter.ALLOWED_DISCHARGE_POWER
         * by propagateScaledChannels() each cycle.
         */
        MAX_DISCHARGE_POWER_KW(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOWATT)
                .accessMode(AccessMode.READ_ONLY)
                .text("Max discharge power from BMS in kW (raw)")),

        /**
         * Reg 39 — Max Allowed Charge Power raw (R). Unit: kW.
         * Scaled to negative W and written to ManagedSymmetricBatteryInverter.ALLOWED_CHARGE_POWER
         * by propagateScaledChannels() each cycle.
         */
        MAX_CHARGE_POWER_KW(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOWATT)
                .accessMode(AccessMode.READ_ONLY)
                .text("Max charge power from BMS in kW (raw)")),

        /**
         * Reg 40 — Q Limit (R). Unit: kW (as shown in iPLON register map).
         */
        Q_LIMIT(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOWATT)
                .accessMode(AccessMode.READ_ONLY)
                .text("iPLON reactive power limit")),

        /**
         * Reg 41 — Total Max Discharge Power (R). Unit: kW.
         */
        MAX_DISCHARGE_TOTAL(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.KILOWATT)
                .accessMode(AccessMode.READ_ONLY)
                .text("Total max discharge power from iPLON")),

        /**
         * Reg 42 — Fault register (R). 0=OK, 1=Fault.
         */
        FAULT(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_ONLY)
                .text("0=NoFault;1=Fault")),

        /**
         * Reg 43 — Alarm register (R). 0=OK, 1=Alarm.
         */
        ALARM(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_ONLY)
                .text("0=NoAlarm;1=Alarm")),

        /**
         * Reg 44 — EMS Communication Fault (R). 0=OK, 1=Fault.
         */
        COMM_FAULT(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_ONLY)
                .text("0=NoCommunicationFault;1=CommunicationFault")),

        // ────────────────────────────────────────────────────────────────────
        // WRITE channels — FC16 (OpenEMS → iPLON)
        // ────────────────────────────────────────────────────────────────────

        /**
         * Reg 3 — Remote/Local (W/R). 1=Remote (EMS controls), 0=Local.
         */
        SET_REMOTE_LOCAL(new IntegerDoc()
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_WRITE)
                .text("1=Remote;0=Local")),

        /**
         * Reg 4 — iPLON Allowed (W/R). 1=Allowed, 0=Not allowed.
         */
        SET_ALLOWED(new IntegerDoc()
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_WRITE)
                .text("1=Allowed;0=NotAllowed")),

        /**
         * Reg 5 — iPLON Controllable (W/R). 1=Controllable.
         */
        SET_CONTROLLABLE(new IntegerDoc()
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_WRITE)
                .text("1=Controllable;0=NotControllable")),

        /**
         * Reg 6 — iPLON Engagement (W/R). 1=Engaged, 0=Disengaged.
         */
        SET_ENGAGEMENT(new IntegerDoc()
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_WRITE)
                .text("1=Engaged;0=Disengaged")),

        /**
         * Reg 7 — Charge Lock (W/R). 1=Locked (charging blocked), 0=Unlocked.
         */
        SET_CHARGE_LOCK(new IntegerDoc()
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_WRITE)
                .text("1=ChargeLocked;0=Unlocked")),

        /**
         * Reg 8 — Discharge Lock (W/R). 1=Locked (discharging blocked), 0=Unlocked.
         */
        SET_DISCHARGE_LOCK(new IntegerDoc()
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_WRITE)
                .text("1=DischargeLocked;0=Unlocked")),

        /**
         * Reg 9 — Active Power Setpoint (W/R). Unit: kW.
         * Positive = discharge, Negative = charge.
         * PRIMARY control output — written every run() cycle.
         */
        SET_ACTIVE_POWER_SETPOINT(new IntegerDoc()
                .unit(Unit.KILOWATT)
                .accessMode(AccessMode.READ_WRITE)
                .text("Active power setpoint kW. +discharge / -charge")),

        /**
         * Reg 10 — Reactive Power Setpoint (W/R). Unit: kVar.
         */
        SET_REACTIVE_POWER_SETPOINT(new IntegerDoc()
                .unit(Unit.KILOVOLT_AMPERE_REACTIVE)
                .accessMode(AccessMode.READ_WRITE)
                .text("Reactive power setpoint kVar")),

        /**
         * Reg 13 — SOC Upper Limit (W/R). Value = % × 10. 950 = 95.0%.
         */
        SET_SOC_UPPER_LIMIT(new IntegerDoc()
                .unit(Unit.PERCENT)
                .accessMode(AccessMode.READ_WRITE)
                .text("SOC upper limit x0.1 percent (950=95%)")),

        /**
         * Reg 14 — SOC Lower Limit (W/R). Value = % × 10. 50 = 5.0%.
         */
        SET_SOC_LOWER_LIMIT(new IntegerDoc()
                .unit(Unit.PERCENT)
                .accessMode(AccessMode.READ_WRITE)
                .text("SOC lower limit x0.1 percent (50=5%)")),

        /**
         * Reg 45 — iPLON Heartbeat (W/R). Counter 0–5000.
         * Must be incremented every cycle or iPLON raises a communication fault.
         */
        SET_HEARTBEAT(new IntegerDoc()
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_WRITE)
                .text("Heartbeat counter 0-5000, incremented each cycle"));

        // ── Doc boilerplate ──────────────────────────────────────────────────

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