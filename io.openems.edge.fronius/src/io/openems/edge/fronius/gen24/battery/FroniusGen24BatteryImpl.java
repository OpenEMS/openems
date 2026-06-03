package io.openems.edge.fronius.gen24.battery;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.fronius.gen24.batteryinverter.BatteryInverterFroniusGen24;

@Designate(ocd = Config.class, factory = true)
@Component(
        name = "Ess.Fronius.Gen24.Battery",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@GenerateTargetsFromReferences("Modbus")
public class FroniusGen24BatteryImpl
        extends AbstractOpenemsModbusComponent
        implements Battery,
        FroniusGen24,
        ModbusComponent,
        OpenemsComponent {

    private static final long UPDATE_INTERVAL_SECONDS = 1;

    private final Logger log =
            LoggerFactory.getLogger(FroniusGen24BatteryImpl.class);

    private Config config;

    private ScheduledExecutorService valueUpdater;
    private ScheduledFuture<?> valueUpdaterFuture;

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

    @Reference(
            policy = STATIC,
            policyOption = GREEDY,
            cardinality = MANDATORY
    )
    private BatteryInverterFroniusGen24 inverter;

    public FroniusGen24BatteryImpl() {
        super(
                OpenemsComponent.ChannelId.values(),
                ModbusComponent.ChannelId.values(),
                Battery.ChannelId.values(),
                StartStoppable.ChannelId.values(),
                FroniusGen24.ChannelId.values()
        );
    }

    @Activate
    private void activate(
            ComponentContext context,
            Config config
    ) throws OpenemsException {

        this.config = config;

        super.activate(
                context,
                config.id(),
                config.alias(),
                config.enabled(),
                config.modbusUnitId()
        );

        this.setConfiguredVoltageLimits();
        this.startValueUpdater(config);
    }

    @Deactivate
    @Override
    protected void deactivate() {

        if (this.valueUpdaterFuture != null) {
            this.valueUpdaterFuture.cancel(false);
            this.valueUpdaterFuture = null;
        }

        if (this.valueUpdater != null) {
            this.valueUpdater.shutdownNow();
            this.valueUpdater = null;
        }

        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {

        return new ModbusProtocol(
                this,

                new FC16WriteRegistersTask(
                        40348,
                        m(
                                FroniusGen24.ChannelId.SET_STORAGE_CONTROL_MODE,
                                new UnsignedWordElement(40348)
                        )
                ),

                new FC16WriteRegistersTask(
                        40355,
                        m(
                                FroniusGen24.ChannelId.SET_OUT_W_RTE,
                                new SignedWordElement(40355)
                        ),

                        m(
                                FroniusGen24.ChannelId.SET_IN_W_RTE,
                                new SignedWordElement(40356)
                        )
                )
        );
    }

    private void startValueUpdater(Config config) {

        this.valueUpdater = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(
                    r,
                    "openems-fronius-gen24-battery-values-" + config.id()
            );
            thread.setDaemon(true);
            return thread;
        });

        this.valueUpdaterFuture = this.valueUpdater.scheduleWithFixedDelay(
                this::updateFromInverterSafely,
                0,
                UPDATE_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void updateFromInverterSafely() {

        try {
            this.updateAllValues();

        } catch (Exception e) {
            this.log.warn(
                    "Failed to update Fronius Gen24 battery values",
                    e
            );
        }
    }

    private void updateAllValues() {

        this.updateSoc();
        this.updateCurrent();
        this.updateVoltage();
        this.updateChargePower();
        this.updateDischargePower();
        this.updateCapacity();
        this.updatePowerLimits();
    }

    private void updateSoc() {

        this.channel(Battery.ChannelId.SOC)
                .setNextValue(
                        this.toInteger(
                                this.readFloat(this.inverter::getModuleSOC)
                        )
                );
    }

    private void updateCurrent() {

        Float module3 = this.readFloat(
                this.inverter::getModule3DcaChannel
        );

        Float module4 = this.readFloat(
                this.inverter::getModule4DcaChannel
        );

        this.channel(Battery.ChannelId.CURRENT)
                .setNextValue(
                        this.maxAsInteger(module3, module4)
                );
    }

    private void updateVoltage() {

        Float module3 = this.readFloat(
                this.inverter::getModule3DcVChannel
        );

        Float module4 = this.readFloat(
                this.inverter::getModule4DcVChannel
        );

        this.channel(Battery.ChannelId.VOLTAGE)
                .setNextValue(
                        this.maxAsInteger(module3, module4)
                );
    }

    private void updateChargePower() {

        this.channel(FroniusGen24.ChannelId.CUR_BAT_CHA)
                .setNextValue(
                        this.toInteger(
                                this.readFloat(
                                        this.inverter::getModule3DcWChannel
                                )
                        )
                );
    }

    private void updateDischargePower() {

        this.channel(FroniusGen24.ChannelId.CUR_BAT_DSCH)
                .setNextValue(
                        this.toInteger(
                                this.readFloat(
                                        this.inverter::getModule4DcWChannel
                                )
                        )
                );
    }

    private void updateCapacity() {

        this.channel(Battery.ChannelId.CAPACITY)
                .setNextValue(
                        this.toInteger(
                                this.readFloat(
                                        this.inverter::getModuleCapacity
                                )
                        )
                );
    }

    private void updatePowerLimits() {

        Float wChaMax = this.readFloat(
                this.inverter::getStorageWChaMaxChannel
        );

        Integer voltage = this.readBatteryVoltage();

        Integer currentLimit = this.calculateCurrentLimit(
                wChaMax,
                voltage
        );

        this._setChargeMaxCurrent(currentLimit);
        this._setDischargeMaxCurrent(currentLimit);
    }

    private void setConfiguredVoltageLimits() {

        int chargeMaxVoltage = Math.max(
                1,
                this.config.chargeMaxVoltage()
        );

        int dischargeMinVoltage = Math.max(
                1,
                this.config.dischargeMinVoltage()
        );

        this._setChargeMaxVoltage(chargeMaxVoltage);
        this._setDischargeMinVoltage(dischargeMinVoltage);
    }

    private Integer readBatteryVoltage() {

        Value<Integer> nextValue =
                this.getVoltageChannel().getNextValue();

        if (nextValue.isDefined()) {
            return nextValue.get();
        }

        Value<Integer> value =
                this.getVoltageChannel().value();

        if (value.isDefined()) {
            return value.get();
        }

        return null;
    }

    private Float readFloat(FloatChannelSupplier channelSupplier) {

        try {

            Channel<Float> channel = channelSupplier.get();

            Value<Float> nextValue = channel.getNextValue();

            if (nextValue.isDefined()) {
                return nextValue.get();
            }

            Value<Float> value = channel.value();

            if (value.isDefined()) {
                return value.get();
            }

            return null;

        } catch (OpenemsException e) {
            return null;
        }
    }

    private Integer toInteger(Float value) {

        if (value == null) {
            return null;
        }

        return value.intValue();
    }

    private Integer maxAsInteger(
            Float first,
            Float second
    ) {

        Integer firstValue = this.toInteger(first);
        Integer secondValue = this.toInteger(second);

        if (firstValue == null) {
            return secondValue;
        }

        if (secondValue == null) {
            return firstValue;
        }

        return Math.max(firstValue, secondValue);
    }

    private Integer calculateCurrentLimit(
            Float wChaMax,
            Integer voltage
    ) {
        if (voltage == null || voltage <= 0) {
            return null;
        }
        if (wChaMax == null) {
            return null;
        }
        return Math.max(0, (int) Math.floor(Math.abs(wChaMax) / voltage));
    }

    private int getNumberOfModules() {

        return Math.max(
                1,
                this.config.numberOfModules()
        );
    }

    private float getChargeMaxVoltagePerModule() {

        return (float) this.config.chargeMaxVoltage()
                / this.getNumberOfModules();
    }

    private float getDischargeMinVoltagePerModule() {

        return (float) this.config.dischargeMinVoltage()
                / this.getNumberOfModules();
    }

    @Override
    public void setStartStop(StartStop value)
            throws OpenemsNamedException {

        this._setStartStop(value);
    }

    @Override
    public String debugLog() {

        return "Soc:" + this.getSoc().asString()
                + "|V:" + this.getVoltage().asString()
                + "|ChaMaxV:" + this.getChargeMaxVoltage().asString()
                + "|DschMinV:" + this.getDischargeMinVoltage().asString()
                + "|ChaMaxI:" + this.getChargeMaxCurrent().asString()
                + "|DschMaxI:" + this.getDischargeMaxCurrent().asString()
                + "|Modules:" + this.getNumberOfModules()
                + "|ChaMaxV/Mod:" + this.getChargeMaxVoltagePerModule()
                + "|DschMinV/Mod:" + this.getDischargeMinVoltagePerModule();
    }

    @FunctionalInterface
    private interface FloatChannelSupplier {

        Channel<Float> get() throws OpenemsException;
    }
}
