package io.openems.edge.ess.saxpower.ess;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.modbus.api.*;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.openems.edge.common.sum.GridMode;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

@Designate(ocd = io.openems.edge.ess.saxpower.ess.Config.class, factory = true)
@Component(//
    name = "Ess.SaxPower", //
    immediate = true, //
    configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class SaxPowerImpl extends AbstractOpenemsModbusComponent
        implements SaxPower, ManagedSinglePhaseEss, OpenemsComponent, ModbusComponent, ModbusSlave {

    @Reference
    private ConfigurationAdmin cm;
    @Reference
    private Power power;

    private Config config;

    private SinglePhase phase;

    @Override
    @Reference(//
            name = "Modbus", //
            policy = STATIC, //
            policyOption = GREEDY, //
            cardinality = MANDATORY //
    )
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private final UnsignedWordElement activePowerElement = new UnsignedWordElement(41);
    private final UnsignedWordElement cosPhiElement = new UnsignedWordElement(42);
    private final UnsignedWordElement maxDischargePowerElement = new UnsignedWordElement(43);
    private final UnsignedWordElement maxChargePowerElement = new UnsignedWordElement(44);
    private final UnsignedWordElement operatingStateElement = new UnsignedWordElement(45);

    public SaxPowerImpl() {
        super(//
                OpenemsComponent.ChannelId.values(), //
                ModbusComponent.ChannelId.values(), //
                SinglePhaseEss.ChannelId.values(), //
                ManagedSinglePhaseEss.ChannelId.values(), //
                SaxPower.ChannelId.values() //
        );
    }

    private final Logger log = LoggerFactory.getLogger(SaxPowerImpl.class);

    @Activate
    private void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, //
                "Modbus", config.modbus_id())) {
            return;
        }

        this.getGridModeChannel().setNextValue(GridMode.ON_GRID);

        this.config = config;
        SinglePhaseEss.initializeCopyPhaseChannel(this, config.phase());

        try {
            this.setOperatingState(2);
            this.cosPhiElement.setNextWriteValue(1000);
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Failed to set SAX Power state to ON: {}", e.getMessage());
        }
    }


    @Override
    @Deactivate
    protected void deactivate() {
        try {
            this.setOperatingState(1);
            this.activePowerElement.setNextWriteValue(16384);
        } catch (OpenemsError.OpenemsNamedException e) {
            throw new RuntimeException(e);
        }

        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {

        return new ModbusProtocol(this,
                new FC3ReadRegistersTask(45, Priority.HIGH,

                        m(SaxPower.ChannelId.OPERATING_STATE, operatingStateElement),

                        m(SymmetricEss.ChannelId.ACTIVE_POWER, new UnsignedWordElement(47),
                                new ElementToChannelConverter(val -> {
                                    if (val == null) return null;
                                    return ((Number) val).intValue() - 16384;
                                })
                        )
                ),

                new FC6WriteRegisterTask(41, m(SaxPower.ChannelId.ACTIVE_POWER_SET_POINT, activePowerElement)),
                new FC6WriteRegisterTask(42, m(SaxPower.ChannelId.COS_PHI_SET_POINT, cosPhiElement)),
                new FC6WriteRegisterTask(43, m(SaxPower.ChannelId.MAX_DISCHARGE_POWER, maxDischargePowerElement)),
                new FC6WriteRegisterTask(44, m(SaxPower.ChannelId.MAX_CHARGE_POWER, maxChargePowerElement)),
                new FC6WriteRegisterTask(45, m(SaxPower.ChannelId.OPERATING_STATE, operatingStateElement))
        );
    }

    @Override
    public void applyPower(int activePower, int reactivePower) throws OpenemsError.OpenemsNamedException {
        this.getAllowedChargePowerChannel().setNextValue(-3500); //5,8 Version: 2,5kW, 7,7: 3,5 kW
        this._setAllowedDischargePower(4600);

        this.getSetActivePowerEqualsChannel().setNextWriteValue(activePower);
        this.getSetReactivePowerEqualsChannel().setNextWriteValue(reactivePower);

        int rawValue = activePower + 16384;

        this.activePowerElement.setNextWriteValue(rawValue);

        this.maxDischargePowerElement.setNextWriteValue(4600);
        this.maxChargePowerElement.setNextWriteValue(3500);
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
    public SinglePhase getPhase() {
        return this.config.phase();
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
