package io.openems.edge.evcs.wallbe;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.evcs.wallbe.api.Wallbe;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

/**
 * This Provides the Wallbe EVCS Modbus TCP implementation.
 * This Component will communicate with the EVCS to provide information about the current state, and provide instructions and commands for the charging process.
 * The Wallbe Interface contains the raw information from the EVCS that will then be translated in the WriteHandler into the Evcs/ManagedEvcs Interface, so OpenEms can understand it.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Evcs.Wallbe", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class WallbeImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, ManagedEvcs, Wallbe, Evcs, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;
    private int minCurrent;
    private int maxCurrent;
    private WallbeReadHandler readHandler;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Reference
    EvcsPower evcsPower;

    /**
     * Handles charge states.
     */
    private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

    /**
     * Processes the controller's writes to this evcs component.
     */
    private final WriteHandler writeHandler = new WriteHandler(this);

    public WallbeImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Wallbe.ChannelId.values(),
                Evcs.ChannelId.values(),
                ModbusComponent.ChannelId.values(),
                ManagedEvcs.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
        this.minCurrent = config.minHwCurrent();
        this.maxCurrent = config.maxHwCurrent();
        this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
        this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());
        this.readHandler = new WallbeReadHandler(this);

        /*
         * Calculates the maximum and minimum hardware power dynamically by listening on
         * the fixed hardware limit and the phases used for charging
         */
        Evcs.addCalculatePowerLimitListeners(this);

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                new FC4ReadInputRegistersTask(100, Priority.HIGH,
                        m(Wallbe.ChannelId.WALLBE_STATUS,
                                new StringWordElement(100, 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(102, Priority.HIGH,
                        m(Wallbe.ChannelId.LOAD_TIME,
                                new UnsignedDoublewordElement(102),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(104, Priority.HIGH,
                        m(Wallbe.ChannelId.DIP_SWITCHES,
                                new UnsignedWordElement(104),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(105, Priority.HIGH,
                        m(Wallbe.ChannelId.FIRMWARE_VERSION,
                                new UnsignedDoublewordElement(105),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(114, Priority.HIGH,
                        m(Wallbe.ChannelId.CURRENT_L1,
                                new SignedDoublewordElement(114),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_5)),
                new FC4ReadInputRegistersTask(116, Priority.HIGH,
                        m(Wallbe.ChannelId.CURRENT_L2,
                                new SignedDoublewordElement(116),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_5)),
                new FC4ReadInputRegistersTask(118, Priority.HIGH,
                        m(Wallbe.ChannelId.CURRENT_L3,
                                new SignedDoublewordElement(118),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_5)),
                new FC4ReadInputRegistersTask(120, Priority.HIGH,
                        m(Wallbe.ChannelId.ACTIVE_POWER,
                                new SignedDoublewordElement(120),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_5)),
                new FC4ReadInputRegistersTask(132, Priority.HIGH,
                        m(Wallbe.ChannelId.ENERGY,
                                new UnsignedDoublewordElement(132),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(528,
                        m(Wallbe.ChannelId.MAXIMUM_CHARGE_CURRENT,
                                new SignedWordElement(528),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC5WriteCoilTask(400,
                        m(Wallbe.ChannelId.CHARGE_ENABLE,
                                new CoilElement(400),
                                ElementToChannelConverter.DIRECT_1_TO_1))

        );
    }


    @Override
    public String debugLog() {
        return "Wallbe " + this.getActivePower() + "W";
    }

    @Override
    public EvcsPower getEvcsPower() {
        return this.evcsPower;
    }

    @Override
    public int getConfiguredMinimumHardwarePower() {
        return Math.round(this.minCurrent / 1000f) * Evcs.DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
    }

    @Override
    public int getConfiguredMaximumHardwarePower() {
        return Math.round(this.maxCurrent / 1000f) * Evcs.DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
    }

    @Override
    public boolean getConfiguredDebugMode() {
        return false;
    }

    @Override
    public boolean applyChargePowerLimit(int power) throws Exception {
        var phases = this.getPhasesAsInt();
        var current = Math.round((float) power / phases / 230f);

        /*
         * Limits the charging value because Wallbe knows only values between 6 and
         * 32
         */
        current = Math.min(current, 32);

        if (current < 6) {
            current = 0;
        }
        //Wallbe wants the Charge current in 100mA steps
        this.setMaximumChargeCurrent((short) (current * 10));
        this.setEnableCharge(true);
        return true;
    }

    @Override
    public boolean pauseChargeProcess() throws Exception {
        this.setEnableCharge(false);
        return this.applyChargePowerLimit(0);

    }

    @Override
    public boolean applyDisplayText(String text) {
        return false;
    }

    @Override
    public int getMinimumTimeTillChargingLimitTaken() {
        return 30;
    }

    @Override
    public ChargeStateHandler getChargeStateHandler() {
        return this.chargeStateHandler;
    }

    @Override
    public void logDebug(String message) {
    }

    @Override
    public void handleEvent(Event event) {
        try {
            this.readHandler.run();
            this.writeHandler.run();
        } catch (Throwable throwable) {
            //
        }
    }

}