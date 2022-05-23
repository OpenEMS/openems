package io.openems.edge.evcs.generic;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.GridVoltage;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.generic.api.GenericEvcs;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Arrays;

/**
 * This Implements a Generic Modbus TCP Evcs. In order to work, it needs to have the register numbers configured. * This Component will communicate with the EVCS to provide information about the current state, and provide instructions and commands for the charging process.
 * The GenericEvcs Interface contains the raw information from the EVCS that will then be translated in the WriteHandler into the Evcs/ManagedEvcs Interface, so OpenEms can understand it.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Evcs.Generic", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class GenericEvcsImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, ManagedEvcs, GenericEvcs, Evcs, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Reference
    EvcsPower evcsPower;

    private int minPower;
    private int maxPower;
    private int[] phases;
    private GenericEvcsWriteHandler writeHandler;
    private GenericEvcsReadHandler readHandler;
    private int statusRegister;
    private boolean status;
    private int l1Register;
    private int l2Register;
    private int l3Register;
    private int chargeLimitRegister;
    private int powerRegister;
    private int gridVoltage;
    private boolean power;


    public GenericEvcsImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ManagedEvcs.ChannelId.values(),
                Evcs.ChannelId.values(),
                GenericEvcs.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
        this.minPower = config.minCurrent();
        this.maxPower = config.maxCurrent();
        this.gridVoltage = config.gridVoltage().getValue();
        this.phases = config.phases();
        if (!this.checkPhases()) {
            throw new ConfigurationException("Phase Configuration is not valid!", "Configuration must only contain 1,2 and 3.");
        }
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
        this._setMinimumHardwarePower(config.minHwCurrent() * this.gridVoltage);
        this._setMaximumPower(this.maxPower);
        this._setMaximumHardwarePower(config.maxHwCurrent() * this.gridVoltage);
        this._setMinimumPower(this.minPower);
        this._setPowerPrecision(this.gridVoltage);
        this._setIsPriority(config.priority());
        this.getRegister(config);
        this.readHandler = new GenericEvcsReadHandler(this, config.writeScaleFactor());
        this.writeHandler = new GenericEvcsWriteHandler(this, config.readScaleFactor(),this.status,this.power);
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
    }

    private void getRegister(Config config) {
        this.statusRegister = config.statusRegister();
        if (this.statusRegister != 0) {
            this.status = true;
        }
        this.powerRegister = config.powerReadRegister();
        if (this.powerRegister != 0) {
            this.power = true;
        }
        this.chargeLimitRegister = config.maxCurrentRegister();
        this.l1Register = config.l1Register();
        this.l2Register = config.l2Register();
        this.l3Register = config.l3Register();

    }

    /**
     * Checks if the Phase Configuration of the Config is valid.
     *
     * @return true if valid
     */
    private boolean checkPhases() {
        String phases = Arrays.toString(this.phases);
        return phases.contains("1") && phases.contains("2") && phases.contains("3") && this.phases.length == 3;
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        ModbusProtocol protocol = new ModbusProtocol(this,
                new FC4ReadInputRegistersTask(this.l1Register, Priority.HIGH,
                        m(GenericEvcs.ChannelId.CURRENT_L1,
                                new SignedDoublewordElement(this.l1Register),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(this.l2Register, Priority.HIGH,
                        m(GenericEvcs.ChannelId.CURRENT_L2,
                                new SignedDoublewordElement(this.l2Register),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(this.l3Register, Priority.HIGH,
                        m(GenericEvcs.ChannelId.CURRENT_L3,
                                new SignedDoublewordElement(this.l3Register),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(this.powerRegister, Priority.HIGH,
                        m(GenericEvcs.ChannelId.APPARENT_POWER,
                                new SignedDoublewordElement(this.powerRegister),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(this.chargeLimitRegister,
                        m(GenericEvcs.ChannelId.MAXIMUM_CHARGE_CURRENT,
                                new SignedWordElement(this.chargeLimitRegister),
                                ElementToChannelConverter.DIRECT_1_TO_1))
        );
        if (this.status) {
            protocol.addTask(new FC4ReadInputRegistersTask(this.statusRegister, Priority.HIGH,
                    m(GenericEvcs.ChannelId.GENERIC_STATUS,
                            new StringWordElement(this.statusRegister, 1),
                            ElementToChannelConverter.DIRECT_1_TO_1)));
        }
        return protocol;
    }

    @Override
    public String debugLog() {
        return "Total: " + this.getChargePower().get() + " W | L1 " + this.getCurrentL1() + " A | L2 " + this.getCurrentL2() + " A | L3 " + this.getCurrentL3() + " A ";
    }

    @Override
    public int[] getPhaseConfiguration() {
        return this.phases;
    }

    @Override
    public EvcsPower getEvcsPower() {
        return this.evcsPower;
    }

    @Override
    public void handleEvent(Event event) {
        this.writeHandler.run();
        try {
            this.readHandler.run();

        } catch (Throwable throwable) {
            //
        }
    }

    /**
     * Returns the minimum Software Power.
     *
     * @return minPower
     */
    public int getMinPower() {
        return this.minPower;
    }

    /**
     * Returns the maximum Software Power.
     *
     * @return maxPower
     */
    public int getMaxPower() {
        return this.maxPower;
    }

    public int getGridVoltage() {
        return gridVoltage;
    }
}
