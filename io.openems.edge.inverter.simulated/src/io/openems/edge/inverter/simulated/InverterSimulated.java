package io.openems.edge.inverter.simulated;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

/**
 * InverterSimulated interface — PPC PV Plant (15 channels).
 *
 * ManagedSymmetricPvInverter already provides:
 *   ACTIVE_POWER_LIMIT  (W, RW)  ← PV_PAC_LIMIT   setpoint  [Reg 3000, WRITE]
 *   MAX_ACTIVE_POWER    (W, R)   ← PV_PAC_MAX_LIMIT capacity [Reg 1000, READ]
 *
 * ElectricityMeter (via ManagedSymmetricPvInverter) already provides:
 *   ACTIVE_POWER        (W)     ← PV_PAC          [Reg 1002, READ]
 *   REACTIVE_POWER      (var)   ← PV_QAC          [Reg 1004, READ]
 *   VOLTAGE_L1/L2/L3    (mV)
 *   CURRENT_L1/L2/L3    (mA)
 *   FREQUENCY           (mHz)
 *
 * PPC PV Plant parameter sheet → 15 channels total:
 *
 *  S.No │ PPC Channel       │ R/W   │ Unit  │ Notes
 * ──────┼───────────────────┼───────┼───────┼──────────────────────────────
 *   1   │ PV_PAC            │ READ  │ kW    │ → ElectricityMeter.ACTIVE_POWER (built-in)
 *   2   │ PV_QAC            │ READ  │ kVAR  │ → ElectricityMeter.REACTIVE_POWER (built-in)
 *   3   │ PV_PAC_MAX_LIMIT  │ READ  │ kW    │ → ManagedSymmetricPvInverter.MAX_ACTIVE_POWER (built-in)
 *   4   │ PV_QAC_IND_LIMIT  │ READ  │ kVAR  │ → InverterSimulated.PV_QAC_IND_LIMIT (custom)
 *   5   │ PV_QAC_CAP_LIMIT  │ READ  │ kVAR  │ → InverterSimulated.PV_QAC_CAP_LIMIT (custom)
 *   6   │ PV_PAC_LIMIT      │ WRITE │ kW    │ → ManagedSymmetricPvInverter.ACTIVE_POWER_LIMIT (built-in)
 *   7   │ PV_QAC_LIMIT      │ WRITE │ kW    │ → InverterSimulated.QAC_LIMIT (custom)
 *   8   │ PV_HEART_BEAT     │ READ  │ sec   │ → InverterSimulated.PV_HEART_BEAT (custom)
 *   9   │ EMS_HEART_BEAT    │ WRITE │ sec   │ → InverterSimulated.EMS_HEART_BEAT (custom)
 *  10   │ PAC_LIMIT_READ    │ READ  │ kW    │ → InverterSimulated.PAC_LIMIT_READ (custom echo)
 *  11   │ QAC_LIMIT_READ    │ READ  │ kVAR  │ → InverterSimulated.QAC_LIMIT_READ (custom echo)
 *  12   │ PV_CTL            │ WRITE │ —     │ → InverterSimulated.PV_CTL (custom)
 *  13   │ PV_MODE           │ READ  │ —     │ → InverterSimulated.PV_MODE (custom)
 *  14   │ PV_PAC_CTL        │ WRITE │ —     │ → InverterSimulated.PV_PAC_CTL (custom)
 *  15   │ PV_QAC_CTL        │ WRITE │ —     │ → InverterSimulated.PV_QAC_CTL (custom)
 *
 * Modbus Register Map (Node-RED slave):
 *
 *  READ registers (FC3):
 *    1000–1001  PV_PAC            Active Power [W]           32-bit signed
 *    1002–1003  PV_QAC            Reactive Power [var]       32-bit signed
 *    1004–1005  PV_PAC_MAX_LIMIT  Active Power Capacity [W]  32-bit unsigned
 *    1006–1007  PV_QAC_IND_LIMIT  Inductive Q capacity [var] 32-bit unsigned
 *    1008–1009  PV_QAC_CAP_LIMIT  Capacitive Q capacity [var]32-bit unsigned
 *    1010       PV_MODE           Working mode               16-bit unsigned
 *    1012       PV_HEART_BEAT     PV heartbeat counter [s]   16-bit unsigned
 *    1014–1015  PAC_LIMIT_READ    Active setpoint echo [W]   32-bit signed
 *    1016–1017  QAC_LIMIT_READ    Reactive setpoint echo [var]32-bit signed
 *
 *  WRITE registers (FC6/FC16):
 *    3000–3001  PV_PAC_LIMIT      Active Power Setpoint [W]  32-bit signed
 *    3002–3003  PV_QAC_LIMIT      Reactive Power Setpoint [var] 32-bit signed
 *    3004       PV_CTL            Remote control (0=local, 1=remote)  16-bit
 *    3006       PV_PAC_CTL        Active power control enable (0/1)   16-bit
 *    3008       PV_QAC_CTL        Reactive power control enable (0/1) 16-bit
 *    3010       EMS_HEART_BEAT    EMS heartbeat counter [s]           16-bit
 *
 * Node-RED Unit Scaling Rules:
 *   POWER  : kW/kVAR × 1000 = W/var  (1 kW → 1000)
 *   VOLTAGE: kV → millivolts (kV × 1e6 / 1000 = V × 1000)
 *   PV_CTL / PV_PAC_CTL / PV_QAC_CTL: integer 0 or 1
 *   Heartbeats: integer seconds (increment each cycle)
 */
public interface InverterSimulated extends ManagedSymmetricPvInverter, OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // ── READ-back / echo channels ────────────────────────────────────────

        /**
         * PAC_LIMIT_READ — Active Power Setpoint read-back echo (PPC param #10).
         * Reg 1014–1015. Node-RED echoes back the written PV_PAC_LIMIT value.
         * Unit: Watt (Node-RED sends W; PPC unit is kW → multiply by 1000).
         */
        PAC_LIMIT_READ(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.WATT)
                .text("PV_PAC_LIMIT echo read-back [W] — confirms last written setpoint")),

        /**
         * QAC_LIMIT_READ — Reactive Power Setpoint read-back echo (PPC param #11).
         * Reg 1016–1017. Node-RED echoes back the written PV_QAC_LIMIT value.
         * Unit: VOLT_AMPERE_REACTIVE (var; PPC unit kVAR → multiply by 1000).
         */
        QAC_LIMIT_READ(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.VOLT_AMPERE_REACTIVE)
                .text("PV_QAC_LIMIT echo read-back [var] — confirms last written setpoint")),

        // ── Reactive power setpoint (WRITE) ──────────────────────────────────

        /**
         * QAC_LIMIT — Reactive Power Setpoint (PPC param #7, WRITE).
         * Reg 3002–3003. EMS writes reactive setpoint to Node-RED slave.
         * Unit: VOLT_AMPERE_REACTIVE (var).
         * ManagedSymmetricPvInverter has ACTIVE_POWER_LIMIT (PAC) built-in but
         * no reactive counterpart — added here.
         * Positive = inductive (lagging), negative = capacitive (leading).
         */
        QAC_LIMIT(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.VOLT_AMPERE_REACTIVE)
                .accessMode(AccessMode.READ_WRITE)
                .text("PV_QAC_LIMIT reactive power setpoint [var] (kVAR × 1000)")),

        // ── Capacity limits (READ) ────────────────────────────────────────────

        /**
         * PV_QAC_IND_LIMIT — Inductive (lagging) reactive power capacity (PPC param #4).
         * Reg 1006–1007. Maximum reactive power the PV plant can absorb inductively.
         * Unit: VOLT_AMPERE_REACTIVE (var; PPC unit kVAR → multiply by 1000).
         */
        PV_QAC_IND_LIMIT(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.VOLT_AMPERE_REACTIVE)
                .text("PV_QAC_IND_LIMIT inductive reactive capacity [var]")),

        /**
         * PV_QAC_CAP_LIMIT — Capacitive (leading) reactive power capacity (PPC param #5).
         * Reg 1008–1009. Maximum reactive power the PV plant can inject capacitively.
         * Unit: VOLT_AMPERE_REACTIVE (var; PPC unit kVAR → multiply by 1000).
         */
        PV_QAC_CAP_LIMIT(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.VOLT_AMPERE_REACTIVE)
                .text("PV_QAC_CAP_LIMIT capacitive reactive capacity [var]")),

        // ── Control / status channels ─────────────────────────────────────────

        /**
         * PV_CTL — Remote Control enable flag (PPC param #12, WRITE).
         * Reg 3004. EMS writes 1 to take remote control; 0 returns to local control.
         * Values: 0 = Local, 1 = Remote.
         */
        PV_CTL(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_WRITE)
                .text("PV_CTL remote control flag (0=local, 1=remote)")),

        /**
         * PV_MODE — Working Mode status (PPC param #13, READ).
         * Reg 1010. PV plant operating mode reported by inverter.
         * Typical values: 0=Idle, 1=MPPT, 2=Curtailed, 3=Fault.
         */
        PV_MODE(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.NONE)
                .text("PV_MODE working mode (0=Idle, 1=MPPT, 2=Curtailed, 3=Fault)")),

        /**
         * PV_PAC_CTL — Active Power Control Enable (PPC param #14, WRITE).
         * Reg 3006. 0 = disabled (PV runs at MPPT), 1 = enabled (EMS controls PAC).
         * Must be set to 1 before PV_PAC_LIMIT setpoint is honoured.
         */
        PV_PAC_CTL(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_WRITE)
                .text("PV_PAC_CTL active power control enable (0=disabled, 1=enabled)")),

        /**
         * PV_QAC_CTL — Reactive Power Control Enable (PPC param #15, WRITE).
         * Reg 3008. 0 = disabled (unity PF), 1 = enabled (EMS controls QAC).
         * Must be set to 1 before PV_QAC_LIMIT setpoint is honoured.
         */
        PV_QAC_CTL(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.NONE)
                .accessMode(AccessMode.READ_WRITE)
                .text("PV_QAC_CTL reactive power control enable (0=disabled, 1=enabled)")),

        // ── Heartbeat channels ─────────────────────────────────────────────────

        /**
         * PV_HEART_BEAT — PV plant heartbeat counter (PPC param #8, READ).
         * Reg 1012. Increments each second on the PV side. EMS monitors this to
         * detect loss of communication with the PV plant.
         * Unit: SECONDS (integer counter; wraps at 65535 for 16-bit register).
         */
        PV_HEART_BEAT(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.SECONDS)
                .text("PV_HEART_BEAT counter from PV plant [s] — monotonically increasing")),

        /**
         * EMS_HEART_BEAT — EMS heartbeat counter (PPC param #9, WRITE).
         * Reg 3010. EMS writes an incrementing counter each cycle. PV plant monitors
         * this to detect EMS communication loss and revert to local / safe control.
         * Unit: SECONDS (increment by 1 per cycle; wraps at 65535).
         */
        EMS_HEART_BEAT(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.SECONDS)
                .accessMode(AccessMode.READ_WRITE)
                .text("EMS_HEART_BEAT counter written by EMS each cycle [s]"));

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