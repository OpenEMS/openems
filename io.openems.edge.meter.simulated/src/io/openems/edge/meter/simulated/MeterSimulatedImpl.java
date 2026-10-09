package io.openems.edge.meter.simulated;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * MeterSimulatedImpl — Grid Meter for PPC simulation via Node-RED Modbus slave.
 *
 * All registers consolidated into Priority.HIGH tasks so every channel
 * updates in the same cycle — eliminates the SAC/PAC lag seen in logs.
 *
 * ┌────────┬──────────┬──────────────┬───────────────────────────┬──────────────────────────────┐
 * │ Reg    │ CSV Type │ PPC Channel  │ OpenEMS Channel           │ Node-RED sends / converter   │
 * ├────────┼──────────┼──────────────┼───────────────────────────┼──────────────────────────────┤
 * │ 2000   │ S32,FC16 │ PAC          │ ACTIVE_POWER              │ W          DIRECT_1_TO_1     │
 * │ 2002   │ S32,FC16 │ QAC          │ REACTIVE_POWER            │ var        DIRECT_1_TO_1     │
 * │ 2004   │ S32,FC16 │ UAC          │ VOLTAGE                   │ mV(V×1000) DIRECT_1_TO_1     │
 * │ 2006   │ U16,FC6  │ FAC          │ FREQUENCY                 │ mHz        DIRECT_1_TO_1     │
 * │ 2007   │ U16,FC6  │ UAC12        │ VOLTAGE_L1                │ V integer  SCALE_FACTOR_3    │
 * │ 2008   │ U16,FC6  │ UAC23        │ VOLTAGE_L2                │ V integer  SCALE_FACTOR_3    │
 * │ 2009   │ U16,FC6  │ UAC31        │ VOLTAGE_L3                │ V integer  SCALE_FACTOR_3    │
 * │ 2010   │ U16,FC6  │ IAC1         │ CURRENT_L1                │ A integer  SCALE_FACTOR_3    │
 * │ 2011   │ U16,FC6  │ IAC2         │ CURRENT_L2                │ A integer  SCALE_FACTOR_3    │
 * │ 2012   │ U16,FC6  │ IAC3         │ CURRENT_L3                │ A integer  SCALE_FACTOR_3    │
 * │ 2013   │ S16,FC6  │ PF           │ POWER_FACTOR              │ PF×1000    DIRECT_1_TO_1     │
 * │ 2014   │ S32,FC16 │ SAC          │ APPARENT_POWER            │ VA         DIRECT_1_TO_1     │
 * │ 2016   │ U16,FC6  │ THD          │ THD                       │ %×10       DIRECT_1_TO_1     │
 * │ 2017   │ S32,FC16 │ PAC_L1       │ ACTIVE_POWER_L1           │ W          DIRECT_1_TO_1     │
 * │ 2019   │ S32,FC16 │ PAC_L2       │ ACTIVE_POWER_L2           │ W          DIRECT_1_TO_1     │
 * │ 2021   │ S32,FC16 │ PAC_L3       │ ACTIVE_POWER_L3           │ W          DIRECT_1_TO_1     │
 * │ 2023   │ S32,FC16 │ QAC_L1       │ REACTIVE_POWER_L1         │ var        DIRECT_1_TO_1     │
 * │ 2025   │ S32,FC16 │ QAC_L2       │ REACTIVE_POWER_L2         │ var        DIRECT_1_TO_1     │
 * │ 2027   │ S32,FC16 │ QAC_L3       │ REACTIVE_POWER_L3         │ var        DIRECT_1_TO_1     │
 * │ 2029   │ U16,FC6  │ IAC          │ CURRENT                   │ A integer  SCALE_FACTOR_3    │
 * └────────┴──────────┴──────────────┴───────────────────────────┴──────────────────────────────┘
 *
 * SCALE_FACTOR_3 = ×1000  (exponent notation: 10^3)
 * SCALE_FACTOR_2 = ×100
 * SCALE_FACTOR_1 = ×10
 *
 * IAC at reg 2029: UAC is S32 occupying regs 2004+2005. IAC must not share 2005.
 * Add to Node-RED CSV: 2029,1,6,1,U16
 */
@Designate(ocd = Config.class, factory = true)
@Component(
        name = "Meter.Simulated",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class MeterSimulatedImpl extends AbstractOpenemsModbusComponent
        implements MeterSimulated, ElectricityMeter, OpenemsComponent, ModbusComponent {

    @Reference
    private ConfigurationAdmin cm;

    @Reference(
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY,
            cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private Config config = null;

    public MeterSimulatedImpl() {
        super(
            OpenemsComponent.ChannelId.values(),
            ModbusComponent.ChannelId.values(),
            ElectricityMeter.ChannelId.values(),
            MeterSimulated.ChannelId.values()
        );
    }

    @Activate
    private void activate(ComponentContext context, Config config) throws OpenemsException {
        if (super.activate(context, config.id(), config.alias(), config.enabled(),
                config.modbusUnitId(), this.cm, "Modbus", config.modbus_id())) {
            return;
        }
        this.config = config;
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {
        return new ModbusProtocol(this,

            // ── Block 1 HIGH: PAC, QAC, UAC, FAC (regs 2000–2006) ────────────
            // All core totals in one HIGH-priority contiguous read.
            // UAC is S32 → occupies regs 2004 AND 2005. Next register is FAC at 2006.
            new FC3ReadRegistersTask(2000, Priority.HIGH,

                // Reg 2000–2001: PAC Active Power [W, S32]
                m(ElectricityMeter.ChannelId.ACTIVE_POWER,
                    new SignedDoublewordElement(2000),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                // Reg 2002–2003: QAC Reactive Power [var, S32]
                m(ElectricityMeter.ChannelId.REACTIVE_POWER,
                    new SignedDoublewordElement(2002),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                // Reg 2004–2005: UAC Voltage avg [mV, S32]
                // Node-RED: V × 1000. 415 V → 415000 mV. S32 holds it cleanly.
                m(ElectricityMeter.ChannelId.VOLTAGE,
                    new SignedDoublewordElement(2004),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                // Reg 2006: FAC Frequency [mHz, U16]
                // Node-RED: Hz × 1000. 50 Hz → 50000.
                m(ElectricityMeter.ChannelId.FREQUENCY,
                    new UnsignedWordElement(2006),
                    ElementToChannelConverter.DIRECT_1_TO_1)
            ),

            // ── Block 2 HIGH: Per-phase voltage + current (regs 2007–2012) ───
            // Promoted to HIGH so they update in the same cycle as PAC/QAC.
            new FC3ReadRegistersTask(2007, Priority.HIGH,

                // Reg 2007: UAC12 → VOLTAGE_L1 [U16, integer V → mV ×1000]
                // Node-RED sends 240 V. SCALE_FACTOR_3 (×1000) → 240000 mV.
                m(ElectricityMeter.ChannelId.VOLTAGE_L1,
                    new UnsignedWordElement(2007),
                    ElementToChannelConverter.SCALE_FACTOR_3),

                // Reg 2008: UAC23 → VOLTAGE_L2 [U16, V → mV ×1000]
                m(ElectricityMeter.ChannelId.VOLTAGE_L2,
                    new UnsignedWordElement(2008),
                    ElementToChannelConverter.SCALE_FACTOR_3),

                // Reg 2009: UAC31 → VOLTAGE_L3 [U16, V → mV ×1000]
                m(ElectricityMeter.ChannelId.VOLTAGE_L3,
                    new UnsignedWordElement(2009),
                    ElementToChannelConverter.SCALE_FACTOR_3),

                // Reg 2010: IAC1 → CURRENT_L1 [U16, integer A → mA ×1000]
                // Node-RED sends integer amps (~3467 A for 7.5 MW / 3 phases).
                m(ElectricityMeter.ChannelId.CURRENT_L1,
                    new UnsignedWordElement(2010),
                    ElementToChannelConverter.SCALE_FACTOR_3),

                // Reg 2011: IAC2 → CURRENT_L2 [U16, A → mA ×1000]
                m(ElectricityMeter.ChannelId.CURRENT_L2,
                    new UnsignedWordElement(2011),
                    ElementToChannelConverter.SCALE_FACTOR_3),

                // Reg 2012: IAC3 → CURRENT_L3 [U16, A → mA ×1000]
                m(ElectricityMeter.ChannelId.CURRENT_L3,
                    new UnsignedWordElement(2012),
                    ElementToChannelConverter.SCALE_FACTOR_3)
            ),

            // ── Block 3 HIGH: PF, SAC, THD (regs 2013–2016) ─────────────────
            // SAC promoted to HIGH — was lagging PAC by 1–2 cycles in logs.
            new FC3ReadRegistersTask(2013, Priority.HIGH,

                // Reg 2013: PF Power Factor [S16, PF×1000, 1:1]
                // 0.95 → 950 ‰
                m(MeterSimulated.ChannelId.POWER_FACTOR,
                    new SignedWordElement(2013),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                // Reg 2014–2015: SAC Apparent Power [VA, S32, 1:1]
                // FIX: was Priority.LOW → updated 1–2 cycles behind PAC.
                m(MeterSimulated.ChannelId.APPARENT_POWER,
                    new SignedDoublewordElement(2014),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                // Reg 2016: THD [U16, %×10, 1:1]
                m(MeterSimulated.ChannelId.THD,
                    new UnsignedWordElement(2016),
                    ElementToChannelConverter.DIRECT_1_TO_1)
            ),

            // ── Block 4 HIGH: Per-phase active power (regs 2017–2022) ────────
            new FC3ReadRegistersTask(2017, Priority.HIGH,

                m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1,
                    new SignedDoublewordElement(2017),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2,
                    new SignedDoublewordElement(2019),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3,
                    new SignedDoublewordElement(2021),
                    ElementToChannelConverter.DIRECT_1_TO_1)
            ),

            // ── Block 5 HIGH: Per-phase reactive power (regs 2023–2028) ──────
            new FC3ReadRegistersTask(2023, Priority.HIGH,

                m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1,
                    new SignedDoublewordElement(2023),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2,
                    new SignedDoublewordElement(2025),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3,
                    new SignedDoublewordElement(2027),
                    ElementToChannelConverter.DIRECT_1_TO_1)
            ),

            // ── Block 6 HIGH: IAC total (reg 2029) ────────────────────────────
            // At 2029 (not 2005) — UAC S32 occupies 2004+2005.
            // Node-RED sends integer amps. SCALE_FACTOR_3 → milliamps.
            new FC3ReadRegistersTask(2029, Priority.HIGH,

                m(ElectricityMeter.ChannelId.CURRENT,
                    new UnsignedWordElement(2029),
                    ElementToChannelConverter.SCALE_FACTOR_3)
            )
        );
    }

    @Override
    public MeterType getMeterType() {
        return this.config.type();
    }

    @Override
    public String debugLog() {
        return "PAC:"  + this.getActivePower().asString()
             + "|QAC:" + this.getReactivePower().asString()
             + "|UAC:" + this.getVoltage().asString()
             + "|IAC:" + this.getCurrent().asString()
             + "|FAC:" + this.getFrequency().asString()
             + "|PF:"  + this.channel(MeterSimulated.ChannelId.POWER_FACTOR).value().asString()
             + "|SAC:" + this.channel(MeterSimulated.ChannelId.APPARENT_POWER).value().asString()
             + "|THD:" + this.channel(MeterSimulated.ChannelId.THD).value().asString();
    }
}