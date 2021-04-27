package io.openems.edge.heater.biomassheater.gilles;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC2ReadInputsTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.heater.Heater;
import io.openems.edge.heater.HeaterState;
import io.openems.edge.heater.biomassheater.gilles.api.BioMassHeater;
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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = Config.class, factory = true)
@Component(name = "WoodChipHeater",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class MassHeaterWoodChips extends AbstractOpenemsModbusComponent implements OpenemsComponent, BioMassHeater, Heater {

    private final Logger log = LoggerFactory.getLogger(MassHeaterWoodChips.class);

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    //in kW
    private int maxThermalPerformance = 1400;

    private Config config;

    public MassHeaterWoodChips() {
        super(OpenemsComponent.ChannelId.values(),
                BioMassHeater.ChannelId.values(),
                Heater.ChannelId.values());

    }

    @Activate
    public void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modBusUnitId(), this.cm, "Modbus", config.modBusBridgeId());

        if (config.maxThermicalOutput() != 0) {
            this.maxThermalPerformance = config.maxThermicalOutput();
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

        return new ModbusProtocol(this,

                new FC2ReadInputsTask(10000, Priority.HIGH,
                        m(BioMassHeater.ChannelId.DISTURBANCE, new CoilElement(10000)),
                        m(BioMassHeater.ChannelId.WARNING, new CoilElement(10001)),
                        m(BioMassHeater.ChannelId.CLEARING_ACTIVE, new CoilElement(10002)),
                        m(BioMassHeater.ChannelId.VACUUM_CLEANING_ON, new CoilElement(10003)),
                        m(BioMassHeater.ChannelId.FAN_ON, new CoilElement(10004)),
                        m(BioMassHeater.ChannelId.FAN_PRIMARY_ON, new CoilElement(10005)),
                        m(BioMassHeater.ChannelId.FAN_SECONDARY_ON, new CoilElement(10006)),
                        m(BioMassHeater.ChannelId.STOKER_ON, new CoilElement(10007)),
                        m(BioMassHeater.ChannelId.ROTARY_VALVE_ON, new CoilElement(10008)),
                        m(BioMassHeater.ChannelId.DOSI_ON, new CoilElement(10009)),
                        m(BioMassHeater.ChannelId.HELIX_1_ON, new CoilElement(10010)),
                        m(BioMassHeater.ChannelId.HELIX_2_ON, new CoilElement(10011)),
                        m(BioMassHeater.ChannelId.HELIX_3_ON, new CoilElement(10012)),
                        m(BioMassHeater.ChannelId.CROSS_CONVEYOR, new CoilElement(10013)),
                        m(BioMassHeater.ChannelId.SLIDING_FLOOR_1_ON, new CoilElement(10014)),
                        m(BioMassHeater.ChannelId.SLIDING_FLOOR_2_ON, new CoilElement(10015)),
                        m(BioMassHeater.ChannelId.IGNITION_ON, new CoilElement(10016)),
                        m(BioMassHeater.ChannelId.LS_1, new CoilElement(10017)),
                        m(BioMassHeater.ChannelId.LS_2, new CoilElement(10018)),
                        m(BioMassHeater.ChannelId.LS_3, new CoilElement(10019)),
                        m(BioMassHeater.ChannelId.LS_LATERAL, new CoilElement(10020)),
                        m(BioMassHeater.ChannelId.LS_PUSHING_FLOOR, new CoilElement(10021)),
                        m(BioMassHeater.ChannelId.HELIX_ASH_1, new CoilElement(10022)),
                        m(BioMassHeater.ChannelId.HELIX_ASH_2, new CoilElement(10023)),
                        m(BioMassHeater.ChannelId.SIGNAL_CONTACT_1, new CoilElement(10024)),
                        m(BioMassHeater.ChannelId.SIGNAL_CONTACT_2, new CoilElement(10025))

                ),
                new FC4ReadInputRegistersTask(20000, Priority.HIGH,
                        m(BioMassHeater.ChannelId.BOILER_TEMPERATURE, new UnsignedWordElement(20000)),
                        m(BioMassHeater.ChannelId.REWIND_TEMPERATURE, new UnsignedWordElement(20001)),
                        m(BioMassHeater.ChannelId.EXHAUST_TEMPERATURE, new UnsignedWordElement(20002)),
                        m(BioMassHeater.ChannelId.FIRE_ROOM_TEMPERATURE, new UnsignedWordElement(20003)),
                        m(BioMassHeater.ChannelId.SLIDE_IN_ACTIVE, new UnsignedWordElement(20004)),
                        m(BioMassHeater.ChannelId.OXYGEN_ACTIVE, new UnsignedWordElement(20005)),
                        m(BioMassHeater.ChannelId.VACUUM_ACTIVE, new UnsignedWordElement(20006)),
                        m(BioMassHeater.ChannelId.PERFORMANCE_ACTIVE, new UnsignedWordElement(20007)),
                        m(BioMassHeater.ChannelId.PERFORMANCE_WM, new UnsignedWordElement(20008)),
                        m(BioMassHeater.ChannelId.PERCOLATION, new UnsignedWordElement(20009)),
                        m(BioMassHeater.ChannelId.REWIND_GRID, new UnsignedWordElement(20010)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_1, new UnsignedWordElement(20011)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_2, new UnsignedWordElement(20012)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_3, new UnsignedWordElement(20013)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_4, new UnsignedWordElement(20014)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_5, new UnsignedWordElement(20015)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_6, new UnsignedWordElement(20016)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_7, new UnsignedWordElement(20017)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_8, new UnsignedWordElement(20018)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_9, new UnsignedWordElement(20019)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_10, new UnsignedWordElement(20020)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_11, new UnsignedWordElement(20021)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_12, new UnsignedWordElement(20022)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_13, new UnsignedWordElement(20023)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_14, new UnsignedWordElement(20024)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_15, new UnsignedWordElement(20025)),
                        m(BioMassHeater.ChannelId.BUFFER_SENSOR_16, new UnsignedWordElement(20026)),
                        m(BioMassHeater.ChannelId.BOILER_TEMPERATURE_SET_POINT_READ_ONLY, new UnsignedWordElement(20027)),
                        m(BioMassHeater.ChannelId.TEMPERATURE_FORWARD_MIN, new UnsignedWordElement(20028)),
                        m(BioMassHeater.ChannelId.SLIDE_IN_PERCENTAGE_VALUE_READ_ONLY, new UnsignedWordElement(20029)),
                        m(BioMassHeater.ChannelId.EXHAUST_PERFORMANCE_MIN, new UnsignedWordElement(20030)),
                        m(BioMassHeater.ChannelId.EXHAUST_PERFORMANCE_MAX, new UnsignedWordElement(20031)),
                        m(BioMassHeater.ChannelId.OXYGEN_PERFORMANCE_MIN_READ_ONLY, new UnsignedWordElement(20032)),
                        m(BioMassHeater.ChannelId.OXYGEN_PERFORMANCE_MAX_READ_ONLY, new UnsignedWordElement(20033)),
                        m(BioMassHeater.ChannelId.SLIDE_IN_MIN_READ_ONLY, new UnsignedWordElement(20034)),
                        m(BioMassHeater.ChannelId.SLIDE_IN_MAX_READ_ONLY, new UnsignedWordElement(20035))
                ),
                new FC3ReadRegistersTask(24576, Priority.HIGH,
                        m(BioMassHeater.ChannelId.BOILER_TEMPERATURE_SET_POINT, new UnsignedWordElement(24576)),
                        m(BioMassHeater.ChannelId.BOILER_TEMPERATURE_MINIMAL_FORWARD, new UnsignedWordElement(24577)),
                        m(BioMassHeater.ChannelId.SLIDE_IN_PERCENTAGE_VALUE_READ, new UnsignedWordElement(24578)),
                        m(BioMassHeater.ChannelId.EXHAUST_PERFORMANCE_MIN, new UnsignedWordElement(24579)),
                        m(BioMassHeater.ChannelId.EXHAUST_PERFORMANCE_MAX, new UnsignedWordElement(24580)),
                        m(BioMassHeater.ChannelId.OXYGEN_PERFORMANCE_MIN, new UnsignedWordElement(24581)),
                        m(BioMassHeater.ChannelId.OXYGEN_PERFORMANCE_MAX, new UnsignedWordElement(24582)),
                        m(BioMassHeater.ChannelId.SLIDE_IN_MIN_READ, new UnsignedWordElement(24583)),
                        m(BioMassHeater.ChannelId.SLIDE_IN_MAX_READ, new UnsignedWordElement(24584))
                ),
                new FC6WriteRegisterTask(24576,
                        m(BioMassHeater.ChannelId.BOILER_TEMPERATURE_SET_POINT, new UnsignedWordElement(24576))),
                new FC6WriteRegisterTask(24577,
                        m(BioMassHeater.ChannelId.BOILER_TEMPERATURE_MINIMAL_FORWARD, new UnsignedWordElement(24577))),
                new FC6WriteRegisterTask(24578,
                        m(BioMassHeater.ChannelId.SLIDE_IN_PERCENTAGE_VALUE, new UnsignedWordElement(24578))),
                new FC6WriteRegisterTask(24579,
                        m(BioMassHeater.ChannelId.EXHAUST_PERFORMANCE_MIN, new UnsignedWordElement(24579))),
                new FC6WriteRegisterTask(24580,
                        m(BioMassHeater.ChannelId.EXHAUST_PERFORMANCE_MAX, new UnsignedWordElement(24580))),
                new FC6WriteRegisterTask(24581,
                        m(BioMassHeater.ChannelId.OXYGEN_PERFORMANCE_MIN, new UnsignedWordElement(24581))),
                new FC6WriteRegisterTask(24582,
                        m(BioMassHeater.ChannelId.OXYGEN_PERFORMANCE_MAX, new UnsignedWordElement(24582))),
                new FC6WriteRegisterTask(24583,
                        m(BioMassHeater.ChannelId.SLIDE_IN_MIN, new UnsignedWordElement(24583))),
                new FC6WriteRegisterTask(24584,
                        m(BioMassHeater.ChannelId.SLIDE_IN_MAX, new UnsignedWordElement(24584))),
                new FC5WriteCoilTask(16387,
                        m(BioMassHeater.ChannelId.EXTERNAL_CONTROL, new CoilElement(16387)))
        );
    }

    @Override
    public boolean setPointPowerPercentAvailable() {
        return false;
    }

    @Override
    public boolean setPointPowerAvailable() {
        return false;
    }

    @Override
    public boolean setPointTemperatureAvailable() {
        return false;
    }

    @Override
    public int calculateProvidedPower(int demand, float bufferValue) throws OpenemsError.OpenemsNamedException {
        if (this.getDisturbance().value().isDefined() && this.getDisturbance().value().get()) {
            this.setOffline();
            return 0;
        }
        this.getExternalControl().setNextWriteValue(true);
        return this.maxThermalPerformance;
    }

    @Override
    public int getMaximumThermalOutput() {
        return this.maxThermalPerformance;
    }

    @Override
    public void setOffline() throws OpenemsError.OpenemsNamedException {
        this.getExternalControl().setNextWriteValue(false);
    }

    @Override
    public boolean hasError() {
        if (this.getDisturbance().value().isDefined()) {
            return this.getDisturbance().value().get();
        } else {
            return false;
        }
    }

    @Override
    public void requestMaximumPower() {
        if (this.isEnabledSignal().get()) {
            try {
                this.getExternalControl().setNextWriteValue(true);
                this.setState(HeaterState.RUNNING.name());
            } catch (OpenemsError.OpenemsNamedException e) {
                log.warn("Couldn't write in Channel" + e.getMessage());
            }
        }
    }

    @Override
    public void setIdle() {
        try {
            this.getExternalControl().setNextWriteValue(false);
            this.setState(HeaterState.AWAIT.name());
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String debugLog() {
        String out = "";
        if (this.getWarning().value().isDefined()) {
            if (this.getWarning().value().get()) {
                out = "WARNING ";
            }
        }
        if (this.getDisturbance().value().isDefined()) {
            if (this.getDisturbance().value().get()) {
                out += "DISTURBANCE";
            }
        }
        if (out.equals("")) {
            return "OK";
        } else {
            return out;
        }

    }

}

