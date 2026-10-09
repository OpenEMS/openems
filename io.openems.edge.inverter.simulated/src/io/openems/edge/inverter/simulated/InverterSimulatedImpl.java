package io.openems.edge.inverter.simulated;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.EventConstants;
import io.openems.edge.common.event.EdgeEventConstants;

/**
 * InverterSimulatedImpl — PV Plant for PPC simulation via Node-RED Modbus slave.
 *
 * Reads all 15 PPC PV Plant parameters and writes all control setpoints to a
 * Node-RED Modbus TCP slave server that simulates the PV inverter interface.
 *
 * ┌──────────────────────────────────────────────────────────────────────────────────┐
 * │  PV PLANT REGISTER MAP  (Node-RED Modbus TCP slave)                             │
 * ├──────────┬───────────────────┬──────────────────────────────────┬───────────────┤
 * │ Register │ PPC Channel       │ OpenEMS Channel                  │ Type/Unit     │
 * ├──────────┼───────────────────┼──────────────────────────────────┼───────────────┤
 * │ READ (FC3)                                                                       │
 * ├──────────┬───────────────────┬──────────────────────────────────┬───────────────┤
 * │ 1000–01  │ PV_PAC            │ ElectricityMeter.ACTIVE_POWER    │ 32s W         │
 * │ 1002–03  │ PV_QAC            │ ElectricityMeter.REACTIVE_POWER  │ 32s var       │
 * │ 1004–05  │ PV_PAC_MAX_LIMIT  │ MgdPvInverter.MAX_ACTIVE_POWER   │ 32u W         │
 * │ 1006–07  │ PV_QAC_IND_LIMIT  │ InverterSimulated.PV_QAC_IND_LIMIT│ 32u var      │
 * │ 1008–09  │ PV_QAC_CAP_LIMIT  │ InverterSimulated.PV_QAC_CAP_LIMIT│ 32u var      │
 * │ 1010     │ PV_MODE           │ InverterSimulated.PV_MODE        │ 16u           │
 * │ 1012     │ PV_HEART_BEAT     │ InverterSimulated.PV_HEART_BEAT  │ 16u sec       │
 * │ 1014–15  │ PAC_LIMIT_READ    │ InverterSimulated.PAC_LIMIT_READ │ 32s W         │
 * │ 1016–17  │ QAC_LIMIT_READ    │ InverterSimulated.QAC_LIMIT_READ │ 32s var       │
 * ├──────────┼───────────────────┼──────────────────────────────────┼───────────────┤
 * │ WRITE (FC16/FC6)                                                                 │
 * ├──────────┬───────────────────┬──────────────────────────────────┬───────────────┤
 * │ 3000–01  │ PV_PAC_LIMIT      │ MgdPvInverter.ACTIVE_POWER_LIMIT │ 32s W         │
 * │ 3002–03  │ PV_QAC_LIMIT      │ InverterSimulated.QAC_LIMIT      │ 32s var       │
 * │ 3004     │ PV_CTL            │ InverterSimulated.PV_CTL         │ 16u (0/1)     │
 * │ 3006     │ PV_PAC_CTL        │ InverterSimulated.PV_PAC_CTL     │ 16u (0/1)     │
 * │ 3008     │ PV_QAC_CTL        │ InverterSimulated.PV_QAC_CTL     │ 16u (0/1)     │
 * │ 3010     │ EMS_HEART_BEAT    │ InverterSimulated.EMS_HEART_BEAT │ 16u sec       │
 * └──────────┴───────────────────┴──────────────────────────────────┴───────────────┘
 *
 * Node-RED Unit Scaling:
 *   Power (kW/kVAR) → W/var: multiply by 1000 before writing to register
 *   Setpoints      → W/var: EMS writes in W; Node-RED divides by 1000 for display
 *   PV_CTL/CTL flags: 0 or 1
 *   Heartbeats: integer seconds, increment each cycle
 */
@Designate(ocd = InverterConfig.class, factory = true)
@Component(
        name = "Inverter.Simulated",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE }
)
public class InverterSimulatedImpl extends AbstractOpenemsModbusComponent
        implements InverterSimulated, ManagedSymmetricPvInverter,
                   ElectricityMeter, OpenemsComponent, ModbusComponent, EventHandler {

    @Reference
    private ConfigurationAdmin cm;

    @Reference(
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY,
            cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private InverterConfig config = null;

    /** EMS heartbeat counter — incremented each cycle and written to register 3010. */
    private int emsHeartbeatCounter = 0;

    public InverterSimulatedImpl() {
        super(
            OpenemsComponent.ChannelId.values(),
            ModbusComponent.ChannelId.values(),
            ElectricityMeter.ChannelId.values(),
            ManagedSymmetricPvInverter.ChannelId.values(),
            InverterSimulated.ChannelId.values()
        );
    }

    @Activate
    private void activate(ComponentContext context, InverterConfig config) throws OpenemsException {
        if (super.activate(context, config.id(), config.alias(), config.enabled(),
                config.modbusUnitId(), this.cm, "Modbus", config.modbus_id())) {
            return;
        }
        this.config = config;

        // Mirror the written Q setpoint into the channel value so QAC_LMT is visible in the log.
        ((IntegerWriteChannel) this.channel(InverterSimulated.ChannelId.QAC_LIMIT))
                .onSetNextWrite(value -> this.channel(InverterSimulated.ChannelId.QAC_LIMIT).setNextValue(value));
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {
        return new ModbusProtocol(this,

            // ── READ Block 1: PV_PAC, PV_QAC, PV_PAC_MAX_LIMIT (1000–1005) ──
            new FC3ReadRegistersTask(1000, Priority.HIGH,

                // Reg 1000–1001: PV_PAC — Active Power [W, signed 32-bit]
                // PPC unit: kW → Node-RED writes W (kW × 1000)
                m(ElectricityMeter.ChannelId.ACTIVE_POWER,
                    new SignedDoublewordElement(1000),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                // Reg 1002–1003: PV_QAC — Reactive Power [var, signed 32-bit]
                // PPC unit: kVAR → Node-RED writes var (kVAR × 1000)
                m(ElectricityMeter.ChannelId.REACTIVE_POWER,
                    new SignedDoublewordElement(1002),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                // Reg 1004–1005: PV_PAC_MAX_LIMIT — Active Power Capacity [W, unsigned 32-bit]
                // PPC unit: kW → Node-RED writes W
                m(ManagedSymmetricPvInverter.ChannelId.MAX_ACTIVE_POWER,
                    new SignedDoublewordElement(1004),
                    ElementToChannelConverter.DIRECT_1_TO_1)
            ),

            // ── READ Block 2: Q limits, mode, heartbeat (1006–1015) ──────────
            new FC3ReadRegistersTask(1006, Priority.LOW,

                // Reg 1006–1007: PV_QAC_IND_LIMIT — Inductive Q capacity [var, unsigned 32-bit]
                m(InverterSimulated.ChannelId.PV_QAC_IND_LIMIT,
                    new SignedDoublewordElement(1006),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                // Reg 1008–1009: PV_QAC_CAP_LIMIT — Capacitive Q capacity [var, unsigned 32-bit]
                m(InverterSimulated.ChannelId.PV_QAC_CAP_LIMIT,
                    new SignedDoublewordElement(1008),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                // Reg 1010: PV_MODE — Working Mode [unsigned 16-bit]
                // 0=Idle, 1=MPPT, 2=Curtailed, 3=Fault
                m(InverterSimulated.ChannelId.PV_MODE,
                    new UnsignedWordElement(1010),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                // Reg 1011: reserved / padding (skip to 1012)
                new UnsignedWordElement(1011),

                // Reg 1012: PV_HEART_BEAT — PV side heartbeat counter [s, unsigned 16-bit]
                m(InverterSimulated.ChannelId.PV_HEART_BEAT,
                    new UnsignedWordElement(1012),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                // Reg 1013: reserved / padding
                new UnsignedWordElement(1013),

                // Reg 1014–1015: PAC_LIMIT_READ — Active setpoint echo [W, signed 32-bit]
                m(InverterSimulated.ChannelId.PAC_LIMIT_READ,
                    new SignedDoublewordElement(1014),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                // Reg 1016–1017: QAC_LIMIT_READ — Reactive setpoint echo [var, signed 32-bit]
                m(InverterSimulated.ChannelId.QAC_LIMIT_READ,
                    new SignedDoublewordElement(1016),
                    ElementToChannelConverter.DIRECT_1_TO_1)
            ),

            // ── WRITE: PV_PAC_LIMIT (Reg 3000–3001, FC16) ────────────────────
            // ManagedSymmetricPvInverter.ACTIVE_POWER_LIMIT — Active Power Setpoint [W]
            // PPC param #6: PV_PAC_LIMIT. EMS writes W; PPC expects kW (Node-RED converts).
            new FC16WriteRegistersTask(
            	    3000,
            	    m(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT,
            	        new SignedDoublewordElement(3000))
            	),

            // ── WRITE: PV_QAC_LIMIT (Reg 3002–3003, FC16) ────────────────────
            // InverterSimulated.QAC_LIMIT — Reactive Power Setpoint [var]
            // PPC param #7: PV_QAC_LIMIT.
            new FC16WriteRegistersTask(3002,
                m(InverterSimulated.ChannelId.QAC_LIMIT,
                    new SignedDoublewordElement(3002),
                    ElementToChannelConverter.DIRECT_1_TO_1)
            ),

            // ── WRITE: PV_CTL (Reg 3004, FC6) ────────────────────────────────
            // Remote control flag. 0=local, 1=remote.
            new FC6WriteRegisterTask(3004,
                m(InverterSimulated.ChannelId.PV_CTL,
                    new UnsignedWordElement(3004),
                    ElementToChannelConverter.DIRECT_1_TO_1)
            ),

            // ── WRITE: PV_PAC_CTL (Reg 3006, FC6) ───────────────────────────
            // Active power control enable. 0=disabled (MPPT), 1=EMS control.
            new FC6WriteRegisterTask(3006,
                m(InverterSimulated.ChannelId.PV_PAC_CTL,
                    new UnsignedWordElement(3006),
                    ElementToChannelConverter.DIRECT_1_TO_1)
            ),

            // ── WRITE: PV_QAC_CTL (Reg 3008, FC6) ───────────────────────────
            // Reactive power control enable. 0=disabled (unity PF), 1=EMS control.
            new FC6WriteRegisterTask(3008,
                m(InverterSimulated.ChannelId.PV_QAC_CTL,
                    new UnsignedWordElement(3008),
                    ElementToChannelConverter.DIRECT_1_TO_1)
            ),

            // ── WRITE: EMS_HEART_BEAT (Reg 3010, FC6) ────────────────────────
            // EMS increments and writes this each cycle. PV plant monitors it.
            new FC6WriteRegisterTask(3010,
                m(InverterSimulated.ChannelId.EMS_HEART_BEAT,
                    new UnsignedWordElement(3010),
                    ElementToChannelConverter.DIRECT_1_TO_1)
            )
        );
    }

    /**
     * Called each cycle by OpenEMS scheduler.
     * Increments and writes the EMS heartbeat counter to the PV slave.
     */
    @Override
    public void handleEvent(Event event) {
        if (!this.isEnabled()) return;
        this.emsHeartbeatCounter = (this.emsHeartbeatCounter + 1) % 65536;
        IntegerWriteChannel hbChannel = this.channel(InverterSimulated.ChannelId.EMS_HEART_BEAT);
        try {
            hbChannel.setNextWriteValue(this.emsHeartbeatCounter);
        } catch (OpenemsNamedException e) {
            this.logWarn(this._LOGGER, "Could not write EMS_HEART_BEAT: " + e.getMessage());
        }
    }

    @Override
    public void setActivePowerLimit(int watts) throws OpenemsNamedException {
        // Take remote control and enable P control; without these the PV stays in MPPT
        IntegerWriteChannel remoteChannel = this.channel(InverterSimulated.ChannelId.PV_CTL);
        remoteChannel.setNextWriteValue(1);  // 1 = remote
        IntegerWriteChannel ctlChannel = this.channel(InverterSimulated.ChannelId.PV_PAC_CTL);
        ctlChannel.setNextWriteValue(1);  // 1 = EMS control enabled

        IntegerWriteChannel ch = this.channel(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT);
        ch.setNextWriteValue(watts);
    }
    
    
    public void setReactivePowerLimit(int vars) throws OpenemsNamedException {
        // Enable Q control
        IntegerWriteChannel ctlChannel = this.channel(InverterSimulated.ChannelId.PV_QAC_CTL);
        ctlChannel.setNextWriteValue(1);  // 1 = EMS control enabled
        
        // Write Q setpoint
        IntegerWriteChannel qChannel = this.channel(InverterSimulated.ChannelId.QAC_LIMIT);
        qChannel.setNextWriteValue(vars);  // Write in var (not kVar)
    }

    @Override
    public String debugLog() {
        return "PAC:"   + this.getActivePower().asString()
             + "|QAC:"  + this.getReactivePower().asString()
             + "|MODE:" + this.channel(InverterSimulated.ChannelId.PV_MODE).value().asString()
             + "|PAC_MAX:" + this.channel(ManagedSymmetricPvInverter.ChannelId.MAX_ACTIVE_POWER).value().asString()
             + "|PAC_LMT:" + this.channel(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT).value().asString()
             + "|QAC_LMT:" + this.channel(InverterSimulated.ChannelId.QAC_LIMIT).value().asString()
             + "|HB_PV:"  + this.channel(InverterSimulated.ChannelId.PV_HEART_BEAT).value().asString()
             + "|HB_EMS:" + this.emsHeartbeatCounter;
    }
}