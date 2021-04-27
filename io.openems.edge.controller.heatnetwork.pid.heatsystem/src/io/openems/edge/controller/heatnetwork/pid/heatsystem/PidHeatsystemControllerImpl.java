package io.openems.edge.controller.heatnetwork.pid.heatsystem;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.pid.heatsystem.api.PidHeatsystemController;
import io.openems.edge.heatsystem.components.PassingActivateNature;
import io.openems.edge.heatsystem.components.PassingForPid;
import io.openems.edge.heatsystem.components.Pump;
import io.openems.edge.thermometer.api.Thermometer;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Passing.Pid")
public class PidHeatsystemControllerImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, PidHeatsystemController {

    private final Logger log = LoggerFactory.getLogger(PidHeatsystemControllerImpl.class);

    @Reference
    ComponentManager cpm;

    private PassingForPid passingForPid;
    private Thermometer thermometer;
    private PassingActivateNature passing;
    private boolean isPump;
    private PidFilter pidFilter;
    private int offPercentage = 0;
    private int counter = 0;
    private int waitticks = 10;
    private double intervalTime = 2000;
    private double timestamp = 0;
    private Config config;

    public PidHeatsystemControllerImpl() {
        super(OpenemsComponent.ChannelId.values(), Controller.ChannelId.values(), PidHeatsystemController.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        AtomicBoolean instanceFound = new AtomicBoolean(false);

        cpm.getAllComponents().stream().filter(component -> component.id().equals(config.id())).findFirst().ifPresent(consumer -> {
            instanceFound.set(true);
        });
        if (instanceFound.get() == true) {
            return;
        }
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled());
        allocateComponents();

        this.pidFilter = new PidFilter(config.proportionalGain(), config.integralGain(), config.derivativeGain());
        pidFilter.setLimits(-200, 200);

        this.setMinTemperature().setNextValue(config.setPoint_Temperature());

        this.offPercentage = config.offPercentage();
    }

    private void allocateComponents() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        allocateComponent(config.temperatureSensorId());
        allocateComponent(config.allocatedPassingDevice());

        if (config.useDependency()) {
            allocateComponent(config.passingControllerId());
        }
    }

    /**
     * <p>Allocate the Component.</p>
     *
     * @param Device String from Config; needs to be an instance of PassingForPid/Thermometer/ControllerPassingChannel.
     *               <p>
     *               Allocate the Component --> Access to Channels
     *               </p>
     * @throws OpenemsError.OpenemsNamedException when cpm can't access / somethings wrong with cpm.
     * @throws ConfigurationException             when cpm tries to access device but it's not correct instance.
     */
    private void allocateComponent(String Device) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (cpm.getComponent(Device) instanceof PassingForPid) {
            if (cpm.getComponent(Device) instanceof Pump) {
                this.isPump = true;
            }
            this.passingForPid = cpm.getComponent(Device);
        } else if (cpm.getComponent(Device) instanceof Thermometer) {
            this.thermometer = cpm.getComponent(Device);
        } else if (cpm.getComponent(Device) instanceof PassingActivateNature) {
            this.passing = cpm.getComponent(Device);
        } else {
            throw new ConfigurationException("The configured Component is neither Valve, Pump, PassingController nor TemperatureSensor! Please Check "
                    + Device, "Configured Component is incorrect!");
        }

    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    /**
     * Controls a pump or valve via PID.
     *
     * <p>
     * Only activates/runs if PassingController is active!
     * if the Temperature from the temperatureSensor ist defined and the controller is ready to calc,
     * PID value is calculated by setPointTemperature; usually from config. And the measured Temperature.
     * This output is saved and the currentPowerLevel of the passingForPid substracts it's value from the output.
     * if the passingForPid is a pump; the value will be inverted --> slower pump --> water will be heated up faster
     * And vice versa.
     * the output is divided by 10 bc the return value is in %*10 (or at least thats what i think due to testing the
     * return values are sometimes 200 etc).
     *
     * </p>
     */
    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        if (componentIsMissing()) {
            log.warn("A Component is Missing in the Controller : " + super.id());
            return;
        }
        counter++;
        if (counter % waitticks != 0) {
            return;
        }
        if (counter % waitticks == 0) {
            counter = waitticks;
        }
        boolean activeDependency = true;
        if (this.passing != null) {
            activeDependency = this.passing.getOnOff().value().isDefined() && this.passing.getOnOff().value().get();
        }
        boolean ownActivation = true;
        if (this.turnOn().value().isDefined()) {
            ownActivation = this.turnOn().value().get();
        }

        if (ownActivation) {
            if (activeDependency) {
                if (this.thermometer.getTemperatureChannel().getNextValue().isDefined()) {
                    this.timestamp = System.currentTimeMillis();
                    double output = pidFilter.applyPidFilter(this.thermometer.getTemperatureChannel().getNextValue().get(), this.setMinTemperature().value().get());
                    // is percentage value fix if so substract from current powerlevel?
                    output -= this.passingForPid.getPowerLevel().getNextValue().get();

                    if (this.isPump) {
                        output *= -1;
                    }
                    if (this.passingForPid.readyToChange()) {
                        this.passingForPid.changeByPercentage(output / 10);
                    }

                }
            } else if (this.passingForPid.readyToChange()) {
                if (this.passingForPid.getPowerLevel().value().isDefined()) {
                    int percentToChange = offPercentage - this.passingForPid.getPowerLevel().value().get().intValue();
                    this.passingForPid.changeByPercentage(percentToChange);
                } else {
                    this.passingForPid.changeByPercentage(offPercentage);
                }
            }
        } else {
            if (this.passingForPid.readyToChange()) {
                if (this.passingForPid.getPowerLevel().value().isDefined()) {
                    int percentToChange = offPercentage - this.passingForPid.getPowerLevel().value().get().intValue();
                    this.passingForPid.changeByPercentage(percentToChange);
                } else {
                    this.passingForPid.changeByPercentage(offPercentage);
                }
            }
        }
    }

    private boolean componentIsMissing() {
        try {
            if (this.passingForPid.isEnabled() == false) {
                this.passingForPid = cpm.getComponent(config.allocatedPassingDevice());
            }
            if (config.useDependency() == true && this.passing.isEnabled() == false) {
                this.passing = cpm.getComponent(config.passingControllerId());
            }
            if (this.thermometer.isEnabled() == false) {
                this.thermometer = cpm.getComponent(config.temperatureSensorId());
            }
            return false;
        } catch (OpenemsError.OpenemsNamedException e) {
            return true;
        }
    }
}
