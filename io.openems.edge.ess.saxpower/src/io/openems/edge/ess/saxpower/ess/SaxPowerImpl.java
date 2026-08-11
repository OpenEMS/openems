package io.openems.edge.ess.saxpower.ess;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import io.openems.edge.common.sum.GridMode;

import java.time.Duration;
import java.time.Instant;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
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
        implements SaxPower, ManagedSinglePhaseEss, SinglePhaseEss, ManagedAsymmetricEss, AsymmetricEss, ManagedSymmetricEss, SymmetricEss, OpenemsComponent, ModbusComponent, ModbusSlave {

    private static final int ACTIVE_POWER_OFFSET = 16384;

    private static final int MAX_APPARENT_POWER = 4600; //230V * 20A

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
    private final UnsignedWordElement operatingStateElement = new UnsignedWordElement(45);

    public SaxPowerImpl() {
        super(//
                OpenemsComponent.ChannelId.values(), //
                ModbusComponent.ChannelId.values(), //
                SymmetricEss.ChannelId.values(), //
                ManagedSymmetricEss.ChannelId.values(), //
                AsymmetricEss.ChannelId.values(), //
                ManagedAsymmetricEss.ChannelId.values(), //
                SinglePhaseEss.ChannelId.values(), //
                ManagedSinglePhaseEss.ChannelId.values(), //
                SaxPower.ChannelId.values() //
        );
    }

    @Activate
    private void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        this.config = config;
        this.phase = config.phase();

        if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, //
                "Modbus", config.modbus_id())) {
            return;
        }

        SinglePhaseEss.initializeCopyPhaseChannel(this, this.phase);

        this._setCapacity(this.config.capacity());
        this._setMaxApparentPower(MAX_APPARENT_POWER);

        this.getGridModeChannel().setNextValue(GridMode.ON_GRID);


        this.getSocChannel().onSetNextValue(soc -> {
            final Integer allowedCharge;
            final Integer allowedDischarge;
            if (soc.isDefined()) {
                allowedCharge = soc.get() >= 100 ? 0 : -this.config.maxChargePower();
                allowedDischarge = soc.get() <= config.minSoc() ? 0 : this.config.maxDischargePower();
            } else {
                allowedCharge = null;
                allowedDischarge = null;
            }
            setValue(this, ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, allowedCharge);
            setValue(this, ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, allowedDischarge);
        });
    }


    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {

        return new ModbusProtocol(this,
                new FC3ReadRegistersTask(45, Priority.HIGH,

                        m(SaxPower.ChannelId.OPERATING_STATE, this.operatingStateElement),
                        m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(46)),

                        m(activePowerChannelId(this.phase), new UnsignedWordElement(47),
                                new ElementToChannelConverter(val -> {
                                    if (val == null) {
                                        return null;
                                    }
                                    return ((Number) val).intValue() - ACTIVE_POWER_OFFSET;
                                })
                        )
                ),

                new FC16WriteRegistersTask(41, m(SaxPower.ChannelId.ACTIVE_POWER_SET_POINT, this.activePowerElement))
        );
    }

    private static io.openems.edge.common.channel.ChannelId activePowerChannelId(SinglePhase phase) {
        return switch (phase) {
            case L1 -> AsymmetricEss.ChannelId.ACTIVE_POWER_L1;
            case L2 -> AsymmetricEss.ChannelId.ACTIVE_POWER_L2;
            case L3 -> AsymmetricEss.ChannelId.ACTIVE_POWER_L3;
        };
    }

    private Instant lastWrite = null;

    @Override
    public void applyPower(int activePower, int reactivePower) throws OpenemsError.OpenemsNamedException {
        final var now = Instant.now();

        // Minimum write interval required by SAX battery (5 seconds)
        if (lastWrite == null || Duration.between(this.lastWrite, now).toMillis() > 5000) {
            var setPoint = (int) TypeUtils.fitWithin(0, 0xFFFF, activePower + ACTIVE_POWER_OFFSET);
            this.setActivePowerSetPoint(setPoint);

            lastWrite = now;
        }
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
                AsymmetricEss.getModbusSlaveNatureTable(accessMode),
                ManagedAsymmetricEss.getModbusSlaveNatureTable(accessMode),
                SaxPower.getModbusSlaveNatureTable(accessMode)
        );
    }

    @Override
    public String debugLog() {
        return "SoC:" + this.getSoc().asString() + "|L:" + this.getActivePower().asString();
    }
}
