package io.openems.edge.ess.saxpower.gridmeter;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
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

@Designate(ocd = Config.class, factory = true)
@Component(//
        name = "Ess.SaxPower.Grid-Meter", //
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class SaxPowerEssGridMeterImpl extends AbstractOpenemsModbusComponent
        implements SaxPowerEssGridMeter, ElectricityMeter, OpenemsComponent, ModbusComponent, ModbusSlave {

    public static final int GRID_POWER_WELL_KNOWN_SCALE = 1;

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

    private final UnsignedWordElement gridPowerScaleFactor = new UnsignedWordElement(40076);

    public SaxPowerEssGridMeterImpl() {
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

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    int encode_int16(int raw) {
        if (raw > 32767) {
            return raw - 65536;
        } else {
            return raw;
        }
    }

    public int getGridPowerScaleFactor() {
        IntegerReadChannel value = this.channel(SaxPowerEssGridMeter.ChannelId.GRID_POWER_SCALE_FACTOR);
        return value.getNextValue().orElse(GRID_POWER_WELL_KNOWN_SCALE);
    }
    public int scalePower(int raw) {
        return (int) (((Number) raw).intValue() * Math.pow(10, this.getGridPowerScaleFactor()));
    }

    public Object channelConverter(Object val) {
        if (val == null) {
            return null;
        }
        int value = ((Number) val).intValue();
        return encode_int16(scalePower(value));
    }

    private final ElementToChannelConverter scaleConverter =
            new ElementToChannelConverter(this::channelConverter);

    @Override
    protected ModbusProtocol defineModbusProtocol() {
        return new ModbusProtocol(this,
                new FC3ReadRegistersTask(40076, Priority.HIGH,
                        m(SaxPowerEssGridMeter.ChannelId.GRID_POWER_SCALE_FACTOR, this.gridPowerScaleFactor)
                ),
                new FC3ReadRegistersTask(40072, Priority.HIGH, //
                        m(ElectricityMeter.ChannelId.ACTIVE_POWER,    new UnsignedWordElement(40072), this.scaleConverter), //
                        m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new UnsignedWordElement(40073), this.scaleConverter), //
                        m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new UnsignedWordElement(40074), this.scaleConverter), //
                        m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new UnsignedWordElement(40075), this.scaleConverter)
                )
        );
    }

    @Override
    public String debugLog() {
        return "L:" + this.getActivePower().asString() + "|L1:" + this.getActivePowerL1() + "|L2:" + this.getActivePowerL2() + "|L3:" + this.getActivePowerL3();
    }

    @Override
    public MeterType getMeterType() {
        return MeterType.GRID;
    }
}
