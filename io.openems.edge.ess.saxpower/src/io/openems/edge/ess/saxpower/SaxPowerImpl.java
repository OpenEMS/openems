package io.openems.edge.ess.saxpower;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.modbus.api.*;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

@Designate(ocd = Config.class, factory = true)
@Component(//
    name = "Ess.SaxPower", //
    immediate = true, //
    configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class SaxPowerImpl extends AbstractOpenemsModbusComponent
        implements SaxPower, ManagedSymmetricEss, SymmetricEss, OpenemsComponent, ModbusComponent, ModbusSlave {

    @Reference
    private ConfigurationAdmin cm;
    @Reference
    private Power power;

    @Override
    @Reference(//
            policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
            target = "(&(id={config.modbus_id})(enabled=true))")
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private Config config = null;

    private final UnsignedWordElement activePowerElement = new UnsignedWordElement(41);

    public SaxPowerImpl() {
        super(//
                OpenemsComponent.ChannelId.values(), //
                ModbusComponent.ChannelId.values(), //
                SymmetricEss.ChannelId.values(), //
                ManagedSymmetricEss.ChannelId.values(), //
                SaxPower.ChannelId.values()
        );
    }

    @Activate
    private void activate(ComponentContext context, Config config) throws OpenemsException {
        if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbus_id())) {
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
        return new ModbusProtocol(this, //
                new FC3ReadRegistersTask(41, Priority.HIGH, //
                        m(SaxPower.ChannelId.ACTIVE_POWER_SET_POINT, this.activePowerElement),

                        m(SaxPower.ChannelId.COS_PHI_SET_POINT, new UnsignedWordElement(42)),

                        m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new UnsignedWordElement(43)),

                        m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new UnsignedWordElement(44)),

                        m(SaxPower.ChannelId.OPERATING_STATE, new UnsignedWordElement(45)),

                        m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(46)),

                        m(SymmetricEss.ChannelId.ACTIVE_POWER, new UnsignedWordElement(47),
                                new ElementToChannelConverter(val -> {
                                    if (val == null) return null;
                                    return ((Number) val).intValue() - 16384;
                                })),

                        m(SaxPower.ChannelId.METER_POWER, new UnsignedWordElement(48),
                                new ElementToChannelConverter(val -> {
                                    if (val == null) return null;
                                    return ((Number) val).intValue() - 16384;
                                }))
                ),

                new FC6WriteRegisterTask(41, this.activePowerElement)
        );
    }

    @Override
    public void applyPower(int activePower, int reactivePower) throws OpenemsException {
        int rawValue = activePower + 16384;

        if (rawValue < 0) {
            rawValue = 0;
        } else if (rawValue > 65535) {
            rawValue = 65535;
        }

        this.activePowerElement.setNextWriteValue(rawValue);
    }

    @Override
    public Power getPower() {
        return this.power;
    }

    @Override
    public int getPowerPrecision() {
        return 1;
    }


    @Override
    public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
        return new ModbusSlaveTable(
                OpenemsComponent.getModbusSlaveNatureTable(accessMode),
                SymmetricEss.getModbusSlaveNatureTable(accessMode),
                ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode),
                SaxPower.getModbusSlaveNatureTable(accessMode)
        );
    }

    @Override
    public String debugLog() {
        return "SoC:" + this.getSoc().asString() + "|L:" + this.getActivePower().asString();
    }
}
