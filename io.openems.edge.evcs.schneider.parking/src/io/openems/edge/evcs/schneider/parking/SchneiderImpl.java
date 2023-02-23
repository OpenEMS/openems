package io.openems.edge.evcs.schneider.parking;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
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
import io.openems.edge.evcs.schneider.parking.api.Schneider;
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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Evcs.Schneider", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics({ //
        EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, ////
})
public class SchneiderImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, Schneider, Evcs, ManagedEvcs, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;
    private int minCurrent;
    private int maxCurrent;
    private SchneiderReadHandler readHandler;

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

    public SchneiderImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Schneider.ChannelId.values(),
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
        this.readHandler = new SchneiderReadHandler(this);

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
    public String debugLog() {
        return "Schneider Ev Parking " + this.getStationPowerTotal() + "W";
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
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                //------------------Read_Only Register-------------------\\
                new FC3ReadRegistersTask(6, Priority.HIGH,
                        m(Schneider.ChannelId.CPW_STATE,
                                new SignedWordElement(6),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(9, Priority.HIGH,
                        m(Schneider.ChannelId.LAST_CHARGE_STATUS,
                                new SignedWordElement(9),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(20, Priority.HIGH,
                        m(Schneider.ChannelId.REMOTE_COMMAND_STATUS,
                                new SignedWordElement(20),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(23, Priority.HIGH,
                        m(Schneider.ChannelId.ERROR_STATUS_MSB,
                                new SignedWordElement(23),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(24, Priority.HIGH,
                        m(Schneider.ChannelId.ERROR_STATUS_LSB,
                                new SignedWordElement(24),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(30, Priority.HIGH,
                        m(Schneider.ChannelId.CHARGE_TIME,
                                new SignedWordElement(30),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(31, Priority.HIGH,
                        m(Schneider.ChannelId.CHARGE_TIME_2,
                                new SignedWordElement(31),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(301, Priority.HIGH,
                        m(Schneider.ChannelId.MAX_INTENSITY_SOCKET,
                                new UnsignedWordElement(301),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(35, Priority.HIGH,
                        m(Schneider.ChannelId.STATION_INTENSITY_PHASE_X_READ,
                                new FloatDoublewordElement(35),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(352, Priority.HIGH,
                        m(Schneider.ChannelId.STATION_INTENSITY_PHASE_2_READ,
                                new FloatDoublewordElement(352),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(354, Priority.HIGH,
                        m(Schneider.ChannelId.STATION_INTENSITY_PHASE_3_READ,
                                new FloatDoublewordElement(354),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(356, Priority.HIGH,
                        m(Schneider.ChannelId.STATION_ENERGY_MSB_READ,
                                new SignedWordElement(356),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(357, Priority.HIGH,
                        m(Schneider.ChannelId.STATION_ENERGY_LSB_READ,
                                new SignedWordElement(357),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(358, Priority.HIGH,
                        m(Schneider.ChannelId.STATION_POWER_TOTAL_READ,
                                new FloatDoublewordElement(358),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(360, Priority.HIGH,
                        m(Schneider.ChannelId.STN_METER_L1_L2_VOLTAGE,
                                new FloatDoublewordElement(360),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(362, Priority.HIGH,
                        m(Schneider.ChannelId.STN_METER_L2_L3_VOLTAGE,
                                new FloatDoublewordElement(362),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(364, Priority.HIGH,
                        m(Schneider.ChannelId.STN_METER_L3_L1_VOLTAGE,
                                new FloatDoublewordElement(364),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(366, Priority.HIGH,
                        m(Schneider.ChannelId.STN_METER_L1_N_VOLTAGE,
                                new FloatDoublewordElement(366),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(368, Priority.HIGH,
                        m(Schneider.ChannelId.STN_METER_L2_N_VOLTAGE,
                                new FloatDoublewordElement(368),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(370, Priority.HIGH,
                        m(Schneider.ChannelId.STN_METER_L3_N_VOLTAGE,
                                new FloatDoublewordElement(370),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(933, Priority.HIGH,
                        m(Schneider.ChannelId.DEGRADED_MODE,
                                new UnsignedWordElement(933),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(2004, Priority.HIGH,
                        m(Schneider.ChannelId.SESSION_TIME,
                                new SignedWordElement(2004),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(2005, Priority.HIGH,
                        m(Schneider.ChannelId.SESSION_TIME_2,
                                new SignedWordElement(2005),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                //---------------------Write Register---------------------\\
                new FC6WriteRegisterTask(150,
                        m(Schneider.ChannelId.REMOTE_COMMAND,
                                new SignedWordElement(150),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(301,
                        m(Schneider.ChannelId.MAX_INTENSITY_SOCKET,
                                new UnsignedWordElement(301),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(932,
                        m(Schneider.ChannelId.REMOTE_CONTROLLER_LIFE_BIT,
                                new SignedWordElement(932),
                                ElementToChannelConverter.DIRECT_1_TO_1))
        );
    }


    @Override
    public boolean applyChargePowerLimit(int power) throws Exception {
        var phases = this.getPhasesAsInt();
        var current = Math.round((float) power / phases / 230f);

        /*
         * Limits the charging value because Schneider knows only values between 6 and
         * 32
         */
        current = Math.min(current, 32);

        if (current < 6) {
            current = 0;
        }
        this.setMaxIntensitySocket(current);
        return true;
    }

    @Override
    public boolean pauseChargeProcess() throws Exception {
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
        if (this.getRemoteControllerLifeBitChannel().getNextValue().isDefined() && this.getRemoteControllerLifeBitChannel().getNextValue().get() == 0) {
            try {
                this.setRemoteControllerLifeBit(1);
            } catch (OpenemsError.OpenemsNamedException e) {
                e.printStackTrace();
            }
        }

        try {
            this.readHandler.run();
            this.writeHandler.run();
        } catch (Throwable throwable) {
            //
        }
    }
}
