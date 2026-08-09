package io.openems.edge.ess.saxpower.gridmeter;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.*;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(//
        name = "Ess.SaxPower.Grid-Meter", //
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class SaxPowerGridMeterImpl extends AbstractOpenemsModbusComponent
        implements SaxPowerEssGridMeter, ElectricityMeter, OpenemsComponent, ModbusComponent, ModbusSlave {

    private static final int ACTIVE_POWER_OFFSET = 16384;

    @Reference
    private ConfigurationAdmin cm;

    @Override
    @Reference(//
            name = "Modbus", //
            policy = ReferencePolicy.STATIC, //
            policyOption = ReferencePolicyOption.GREEDY, //
            cardinality = ReferenceCardinality.MANDATORY //
    )
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public SaxPowerGridMeterImpl() {
        super(//
                OpenemsComponent.ChannelId.values(), //
                ModbusComponent.ChannelId.values(), //
                ElectricityMeter.ChannelId.values(), //
                SaxPowerEssGridMeter.ChannelId.values() //
        );
    }

    @Override
    public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
        return new ModbusSlaveTable(
                OpenemsComponent.getModbusSlaveNatureTable(accessMode),
                ElectricityMeter.getModbusSlaveNatureTable(accessMode)
        );
    }

    @Activate
    private void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus", config.modbus_id());
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {
        return new ModbusProtocol(this,
                new FC3ReadRegistersTask(48, Priority.HIGH,
                        m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedWordElement(48),
                                new ElementToChannelConverter(val -> {
                                    if (val == null) return null;
                                    return ((Number) val).intValue() - ACTIVE_POWER_OFFSET;
                                })
                        )
                )
        );
    }

    @Override
    public String debugLog() {
        return "L:" + this.getActivePower().asString();
    }

    @Override
    public MeterType getMeterType() {
        return MeterType.GRID;
    }
}
