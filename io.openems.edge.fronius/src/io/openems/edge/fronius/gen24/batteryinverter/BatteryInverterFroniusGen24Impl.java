package io.openems.edge.fronius.gen24.batteryinverter;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Map;
import java.util.function.Consumer;

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

import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.batteryinverter.AbstractSunSpecBatteryInverter;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.fronius.gen24.battery.FroniusGen24;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(
        name = "Ess.Fronius.Gen24.Inverter",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = {
                BatteryInverterFroniusGen24.class,
                HybridManagedSymmetricBatteryInverter.class,
                ManagedSymmetricBatteryInverter.class,
                SymmetricBatteryInverter.class,
                StartStoppable.class,
                ModbusComponent.class,
                ManagedSymmetricPvInverter.class,
                OpenemsComponent.class
        }
)
@GenerateTargetsFromReferences("Modbus")
public class BatteryInverterFroniusGen24Impl extends AbstractSunSpecBatteryInverter
        implements BatteryInverterFroniusGen24,
        HybridManagedSymmetricBatteryInverter,
        ManagedSymmetricBatteryInverter,
        SymmetricBatteryInverter,
        StartStoppable,
        ModbusComponent,
        TimedataProvider,
        OpenemsComponent {

    private final Logger log =
            LoggerFactory.getLogger(BatteryInverterFroniusGen24Impl.class);

    private static final int READ_FROM_MODBUS_BLOCK = 1;

    private static final Map<SunSpecModel, Priority> ACTIVE_MODELS =
            ImmutableMap.<SunSpecModel, Priority>builder()
                    .put(DefaultSunSpecModel.S_1, Priority.LOW)
                    .put(DefaultSunSpecModel.S_103, Priority.HIGH)
                    .put(DefaultSunSpecModel.S_120, Priority.LOW)
                    .put(DefaultSunSpecModel.S_121, Priority.LOW)
                    .put(DefaultSunSpecModel.S_122, Priority.LOW)
                    .put(DefaultSunSpecModel.S_123, Priority.LOW)
                    .put(DefaultSunSpecModel.S_124, Priority.LOW)
                    .put(S160SunSpecModel.S_160, Priority.HIGH)
                    .build();

    @Reference
    protected Power power;

    @Reference(
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY,
            cardinality = ReferenceCardinality.OPTIONAL
    )
    private volatile Timedata timedata = null;

    private final CalculateEnergyFromPower calculateActiveChargeEnergy =
            new CalculateEnergyFromPower(
                    this,
                    SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY);

    private final CalculateEnergyFromPower calculateActiveDischargeEnergy =
            new CalculateEnergyFromPower(
                    this,
                    SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY);

    private final CalculateEnergyFromPower calculateDcChargeEnergy =
            new CalculateEnergyFromPower(
                    this,
                    HybridManagedSymmetricBatteryInverter.ChannelId.DC_CHARGE_ENERGY);

    private final CalculateEnergyFromPower calculateDcDischargeEnergy =
            new CalculateEnergyFromPower(
                    this,
                    HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_ENERGY);

    private final ApplyPowerHandler applyPowerHandler =
            new ApplyPowerHandler(this);

    private Config config;

    public BatteryInverterFroniusGen24Impl() throws OpenemsException {

        super(
                ACTIVE_MODELS,
                OpenemsComponent.ChannelId.values(),
                ModbusComponent.ChannelId.values(),
                SymmetricBatteryInverter.ChannelId.values(),
                ManagedSymmetricBatteryInverter.ChannelId.values(),
                HybridManagedSymmetricBatteryInverter.ChannelId.values(),
                StartStoppable.ChannelId.values(),
                new io.openems.edge.common.channel.ChannelId[] {
                        ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT
                },
                BatteryInverterFroniusGen24.ChannelId.values()
        );
    }

    @Override
    @Reference(
            policy = STATIC,
            policyOption = GREEDY,
            cardinality = MANDATORY,
            target = "(&(id=${config.modbus_id})(enabled=true))"
    )
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Activate
    private void activate(ComponentContext context, Config config)
            throws OpenemsException {

        this.config = config;

        super.activate(
                context,
                config.id(),
                config.alias(),
                config.enabled(),
                config.modbusUnitId(),
                READ_FROM_MODBUS_BLOCK
        );

        this._setGridMode(GridMode.ON_GRID);
        this._setConfiguredControlMode(config.controlMode());
        this._setInitializing(true);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected void onSunSpecInitializationCompleted() {

        this.logInfo(
                this.log,
                "SunSpec initialization finished. "
                        + this.channels().size()
                        + " Channels available."
        );

        this.mapFirstPointToChannel(
                SymmetricBatteryInverter.ChannelId.ACTIVE_POWER,
                ElementToChannelConverter.DIRECT_1_TO_1,
                DefaultSunSpecModel.S103.W
        );

        this.mapFirstPointToChannel(
                SymmetricBatteryInverter.ChannelId.REACTIVE_POWER,
                ElementToChannelConverter.DIRECT_1_TO_1,
                DefaultSunSpecModel.S103.V_AR
        );

        this.mapFirstPointToChannel(
                SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER,
                ElementToChannelConverter.DIRECT_1_TO_1,
                DefaultSunSpecModel.S121.W_MAX
        );

        this.installListeners();

        this._setInitializing(false);
    }

    @Override
    public void run(
            Battery battery,
            int setActivePower,
            int setReactivePower
    ) throws OpenemsNamedException {

        this.calculateEnergy();

        if (!(battery instanceof FroniusGen24 froniusBattery)) {

            this.log.warn(
                    "Unsupported battery type: {}",
                    battery.getClass().getSimpleName()
            );

            return;
        }

        this.applyPowerHandler.apply(
                froniusBattery,
                setActivePower,
                setReactivePower,
                this.config.controlMode()
        );
    }

    private void recalculateDcDischargePower() {

        try {

            int chargePower =
                    this.getModule3DcwChannel()
                            .getNextValue()
                            .orElse(0F)
                            .intValue();

            int dischargePower =
                    this.getModule4DcwChannel()
                            .getNextValue()
                            .orElse(0F)
                            .intValue();

            int batteryPower = dischargePower - chargePower;

            this._setDcDischargePower(batteryPower);

            if (batteryPower > 0) {

                this.log.info(
                        "Battery DISCHARGING with {} W "
                                + "[charge={} W, discharge={} W]",
                        batteryPower,
                        chargePower,
                        dischargePower
                );

            } else if (batteryPower < 0) {

                this.log.info(
                        "Battery CHARGING with {} W "
                                + "[charge={} W, discharge={} W]",
                        Math.abs(batteryPower),
                        chargePower,
                        dischargePower
                );

            } else {

                this.log.info(
                        "Battery IDLE "
                                + "[charge={} W, discharge={} W]",
                        chargePower,
                        dischargePower
                );
            }

        } catch (OpenemsException e) {

            this.log.warn(
                    "Failed to calculate DC battery power",
                    e
            );
        }
    }

    private void installListeners() {

        final Consumer<Value<Float>> calculateFloat = ignore -> {
            this.recalculateDcDischargePower();
        };

        try {

            this.getModule1DcwChannel()
                    .onSetNextValue(calculateFloat);

            this.getModule2DcwChannel()
                    .onSetNextValue(calculateFloat);

            this.getModule3DcwChannel()
                    .onSetNextValue(calculateFloat);

            this.getModule4DcwChannel()
                    .onSetNextValue(calculateFloat);

        } catch (OpenemsException e) {

            this.log.warn(
                    "Failed to install listeners",
                    e
            );
        }
    }

    private void calculateEnergy() {

        var activePower = this.getActivePower().get();

        if (activePower == null) {

            this.calculateActiveChargeEnergy.update(null);
            this.calculateActiveDischargeEnergy.update(null);

        } else if (activePower > 0) {

            this.calculateActiveChargeEnergy.update(0);
            this.calculateActiveDischargeEnergy.update(activePower);

        } else {

            this.calculateActiveChargeEnergy.update(activePower * -1);
            this.calculateActiveDischargeEnergy.update(0);
        }

        var dcPower = this.getDcDischargePower().get();

        if (dcPower == null) {

            this.calculateDcChargeEnergy.update(null);
            this.calculateDcDischargeEnergy.update(null);

        } else if (dcPower > 0) {

            this.calculateDcChargeEnergy.update(0);
            this.calculateDcDischargeEnergy.update(dcPower);

        } else {

            this.calculateDcChargeEnergy.update(dcPower * -1);
            this.calculateDcDischargeEnergy.update(0);
        }
    }

    @Override
    public Integer getSurplusPower() {
        return 0;
    }

    @Override
    public String debugLog() {
        return "|L:" + this.getActivePower().asString();
    }

    @Override
    public int getPowerPrecision() {
        return 1;
    }

    @Override
    public void setStartStop(StartStop value)
            throws OpenemsNamedException {

        this._setStartStop(value);
    }

    @Override
    public Integer getDcPvPower() {

        try {

            return this.getModule1DcwChannel()
                    .value()
                    .orElse(0F)
                    .intValue()
                    + this.getModule2DcwChannel()
                            .value()
                            .orElse(0F)
                            .intValue();

        } catch (OpenemsException e) {

            return null;
        }
    }

    @Override
    public Timedata getTimedata() {
        return this.timedata;
    }

    @Override
    public Channel<Float> getModuleSOC()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                DefaultSunSpecModel.S124.CHA_STATE
        );
    }

    @Override
    public Channel<Float> getModule1DcwChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_1_D_C_W
        );
    }

    @Override
    public Channel<Float> getModule1DcaChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_1_D_C_A
        );
    }

    @Override
    public Channel<Float> getModule1DcvChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_1_D_C_V
        );
    }

    @Override
    public Channel<Float> getModule2DcwChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_2_D_C_W
        );
    }

    @Override
    public Channel<Float> getModule2DcaChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_2_D_C_A
        );
    }

    @Override
    public Channel<Float> getModule2DcvChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_2_D_C_V
        );
    }

    public Channel<Float> getModule3DcwChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_3_D_C_W
        );
    }

    public Channel<Float> getModule4DcwChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_4_D_C_W
        );
    }

    public Channel<Float> getModule3DcaChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_3_D_C_A
        );
    }

    public Channel<Float> getModule4DcaChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_4_D_C_A
        );
    }

    public Channel<Float> getModule3DcVChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_3_D_C_V
        );
    }

    public Channel<Float> getModule4DcVChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_4_D_C_V
        );
    }

    public Channel<Float> getModule3DcWChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_3_D_C_W
        );
    }

    public Channel<Float> getModule4DcWChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_4_D_C_W
        );
    }

    public Channel<Float> getModule3DcWHChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_3_D_C_W_H
        );
    }

    public Channel<Float> getModule4DcWHChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                S160SunSpecModel.S160.MODULE_4_D_C_W_H
        );
    }

    public Channel<Float> getModuleCapacity()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                DefaultSunSpecModel.S120.W_H_RTG
        );
    }

    public Channel<Float> getStorageWChaMaxChannel()
            throws OpenemsException {

        return this.getSunSpecChannelOrError(
                DefaultSunSpecModel.S124.W_CHA_MAX
        );
    }

    @Override
    public boolean isInitialized() {
        return this.isSunSpecInitializationCompleted();
    }

    @Override
    public io.openems.edge.common.channel.IntegerReadChannel getActivePowerChannel() {
        return this.channel(SymmetricBatteryInverter.ChannelId.ACTIVE_POWER);
    }

    @Override
    public io.openems.edge.common.channel.IntegerReadChannel getReactivePowerChannel() {
        return this.channel(SymmetricBatteryInverter.ChannelId.REACTIVE_POWER);
    }

    @Override
    public io.openems.edge.common.channel.IntegerReadChannel getMaxApparentPowerChannel() {
        return this.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER);
    }

    @Override
    public boolean isManaged() {
        return true;
    }

    @Override
    public void _setActivePower(Integer value) {
        this.channel(SymmetricBatteryInverter.ChannelId.ACTIVE_POWER).setNextValue(value);
    }

    @Override
    public void _setActivePower(int value) {
        this.channel(SymmetricBatteryInverter.ChannelId.ACTIVE_POWER).setNextValue(value);
    }

    @Override
    public void _setReactivePower(Integer value) {
        this.channel(SymmetricBatteryInverter.ChannelId.REACTIVE_POWER).setNextValue(value);
    }

    @Override
    public void _setReactivePower(int value) {
        this.channel(SymmetricBatteryInverter.ChannelId.REACTIVE_POWER).setNextValue(value);
    }

    @Override
    public void _setMaxApparentPower(Integer value) {
        this.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER).setNextValue(value);
    }

    @Override
    public void _setMaxApparentPower(int value) {
        this.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER).setNextValue(value);
    }

    @Override
    public io.openems.edge.common.channel.value.Value<Integer> getActivePower() {
        return (io.openems.edge.common.channel.value.Value<Integer>)
                this.channel(SymmetricBatteryInverter.ChannelId.ACTIVE_POWER).value();
    }

    @Override
    public io.openems.edge.common.channel.value.Value<Integer> getReactivePower() {
        return (io.openems.edge.common.channel.value.Value<Integer>)
                this.channel(SymmetricBatteryInverter.ChannelId.REACTIVE_POWER).value();
    }

    @Override
    public io.openems.edge.common.channel.value.Value<Integer> getMaxApparentPower() {
        return (io.openems.edge.common.channel.value.Value<Integer>)
                this.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER).value();
    }

    // -------------------------------------------------------------------------
    // Package-sichtbare Hilfsmethoden fuer ApplyPowerHandler (WMaxLim S123)
    // -------------------------------------------------------------------------

    /** S121.WMax - Nennleistung des Wechselrichters. */
    Channel<Float> getWMaxChannel() throws OpenemsException {
        return this.getSunSpecChannelOrError(DefaultSunSpecModel.S121.W_MAX);
    }

    /** S123.WMaxLimPct schreiben. Wert 0..100 %, wird als FloatWriteChannel geschrieben. */
    @SuppressWarnings("unchecked")
    void writeWMaxLimPct(int value) throws OpenemsNamedException {
        int pct = Math.max(0, Math.min(100, value));

        ((io.openems.edge.common.channel.FloatWriteChannel)
                this.getSunSpecChannelOrError(DefaultSunSpecModel.S123.W_MAX_LIM_PCT))
                .setNextWriteValue((float) pct);
    }

    /** S123.WMaxLim_Ena schreiben. Erwartet enum 0/1. */
    @SuppressWarnings("unchecked")
    void writeWMaxLimEna(int value) throws OpenemsNamedException {
        int ena = value == 0 ? 0 : 1;

        ((io.openems.edge.common.channel.EnumWriteChannel)
                this.getSunSpecChannelOrError(DefaultSunSpecModel.S123.W_MAX_LIM_ENA))
                .setNextWriteValue(ena);
    }
}