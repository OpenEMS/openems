package io.openems.edge.controller.heatnetwork.passingstation;

import io.openems.common.exceptions.HeatToLowException;
import io.openems.common.exceptions.NoHeatNeededException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.ValveDefectException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.passingstation.api.ControllerPassingChannel;
import io.openems.edge.heatsystem.components.PassingActivateNature;
import io.openems.edge.heatsystem.components.PassingChannel;
import io.openems.edge.heatsystem.components.Pump;
import io.openems.edge.heatsystem.components.Valve;
import io.openems.edge.thermometer.api.Thermometer;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Passing.Main")
public class ControllerPassingImpl extends AbstractOpenemsComponent implements OpenemsComponent, ControllerPassingChannel, PassingActivateNature, Controller {

    @Reference
    protected ComponentManager cpm;

    private Thermometer primaryForward;
    private Thermometer primaryRewind;
    private Thermometer secondaryForward;
    private Thermometer secondaryRewind;
    private Valve valve;
    private Pump pump;
    private boolean pumpActive = false;

    private boolean isOpen = false;
    private boolean isClosed = true;

    private boolean timeSetHeating = false;


    //for Tpv> minTemp + toleranceTemp
    private static int TOLERANCE_TEMPERATURE = 10;
    private int timeToHeatUp;

    private static int EXTRA_BUFFER_TIME = 2 * 1000;

    //for errorHandling
    private int startingTemperature;
    //T in dC
    private static int ROUND_ABOUT_TEMP = 20;
    private static int WAITING_FOR_TOO_HOT = 30 * 1000;
    //ty
    private long timeStampHeating;

    private long timeStampWarmthPump;


    public ControllerPassingImpl() {

        super(OpenemsComponent.ChannelId.values(),
                ControllerPassingChannel.ChannelId.values(),
                PassingActivateNature.ChannelId.values(),
                Controller.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        //just to make sure; (for the Overseer Controller)
        this.noError().setNextValue(true);
        this.isOpen = false;
        this.isClosed = true;
        //if user doesn't know ; default == 5 min
        if (config.heating_Time() == 0) {
            this.timeToHeatUp = 5 * 1000 * 60;
        }
        this.timeToHeatUp = config.heating_Time() * 1000;

        allocate_Component(config.primary_Forward_Sensor(), "Thermometer", "PF");
        allocate_Component(config.primary_Rewind_Sensor(), "Thermometer", "PR");
        allocate_Component(config.secundary_Forward_Sensor(), "Thermometer", "SF");
        allocate_Component(config.secundary_Rewind_Sensor(), "Thermometer", "SR");
        allocate_Component(config.valve_id(), "Valve", "Valve");
        allocate_Component(config.pump_id(), "Pump", "Pump");

        defaultOptions();
    }

    private void defaultOptions() {
        if (this.primaryRewind.getTemperature().getNextValue().isDefined()) {
            this.startingTemperature = this.primaryRewind.getTemperature().getNextValue().get();
        } else {
            this.startingTemperature = 0;
        }
        valve.changeByPercentage(-100);
        this.pump.controlRelays(false, "");
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        this.getOnOff().setNextValue(false);
        this.valve.forceClose();
        this.pump.changeByPercentage(-100);
    }

    /**
     * Checks all the time if the controller is needed, heat is okay etc.
     * <p>
     * If the MinTemperature is defined (coming from the overseer controller --> Min Temperature needs to be
     * reached by the primaryForward), and there's no Error as well as the OnOff Value was set to true (by the overseer)
     * The Valve is opened. If the Temperature is reached the pump will be activated.
     * If the Time to heat up the system is up Errors will occur (either the Temperature barely changed --> Valve defect
     * or the min temperature can't be reached bc heat is too low.)
     */
    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        if (this.getMinTemperature().value().isDefined()
                && this.getOnOff().value().isDefined()) {
            if (this.noError().value().get()
                    && this.getOnOff().value().get()) {
                try {
                    if (!isOpen) {
                        if (isClosed && valve.readyToChange()) {
                            if (valve.changeByPercentage(100)) {
                                isClosed = false;
                                return;
                            }
                        } else if (!isClosed && valve.readyToChange()) {
                            //controlRelays will be handled by an extra Controller
                            // valve.controlRelays(false, "Open");
                            isOpen = true;
                            timeSetHeating = false;
                        } else {
                            return;
                        }
                    }
                    if (primaryForwardReadyToHeat() && !pumpActive) {

                        timeSetHeating = false;
                        timeStampWarmthPump = System.currentTimeMillis();

                        pump.changeByPercentage(50);
                        pumpActive = true;

                    }
                    if (tooHot()) {
                        pump.changeByPercentage(-100);
                        pumpActive = false;
                        this.noError().setNextValue(false);
                        getErrorCode().setNextValue(2);
                        throw new NoHeatNeededException("Heat is not needed;"
                                + "Shutting down pump and Valves");
                    } else { //Check if there's something wrong with Valve or Heat to low
                        if (primaryForwardReadyToHeat() == false) {
                            if (isOpen && !timeSetHeating) {
                                timeStampHeating = System.currentTimeMillis();
                                timeSetHeating = true;
                                return;
                            }
                            if (shouldBeHeatingByNow()) {

                                this.noError().setNextValue(false);

                                if (Math.abs(primaryRewind.getTemperature().getNextValue().get()
                                        - startingTemperature) <= ROUND_ABOUT_TEMP) {
                                    getErrorCode().setNextValue(0);
                                    throw new ValveDefectException("Temperature barely Changed --> Valve Defect!");

                                } else {
                                    getErrorCode().setNextValue(1);
                                    throw new HeatToLowException("Heat is too low; Min Temperature will not be reached; "
                                            + "Closing Valve");

                                }
                            }
                        }
                    }

                } catch (ValveDefectException | NoHeatNeededException | HeatToLowException e) {
                    this.noError().setNextValue(false);
                    valve.forceClose();
                    //valve.controlRelays(false, "Open");
                    //valve.controlRelays(true, "Closed");
                    throw e;
                }


            } else {

                if (!isClosed) {
                    if (!valve.getIsBusy().getNextValue().get()) {
                        if (valve.changeByPercentage(-100)) {
                            pump.changeByPercentage(-100);
                            pumpActive = false;
                            isOpen = false;
                        }
                    } else if (valve.readyToChange()) {
                        //valve.controlRelays(false, "Closed");
                        isClosed = true;
                        timeSetHeating = false;
                    }

                }
            }

        }
    }

    private boolean shouldBeHeatingByNow() {
        return System.currentTimeMillis() - timeStampHeating > timeToHeatUp + EXTRA_BUFFER_TIME;
    }

    /**
     * Checks if the minTemperature is reached, Buffer Temperature is important bc of heatloss.
     *
     * @return the temperature is reached or not.
     */
    private boolean primaryForwardReadyToHeat() {
        return primaryForward.getTemperature().getNextValue().get()
                >= this.getMinTemperature().getNextValue().get() + TOLERANCE_TEMPERATURE;
    }


    /**
     * This function allocates all Components the Controller needs.
     *
     * @param id        The Unique id of the device.
     * @param type      is the Device a Thermometer, Pump or Valve
     * @param exactType only important for the TemperatureSensor, to validate it's task.
     * @throws OpenemsError.OpenemsNamedException coming from the componentManager. if the getComponent method
     *                                            throws an error.
     * @throws ConfigurationException             if the configured component is not the correct instanceof.
     */
    private void allocate_Component(String id, String type, String exactType) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        switch (type) {
            case "Thermometer":
                if (cpm.getComponent(id) instanceof Thermometer) {
                    Thermometer th = cpm.getComponent(id);
                    switch (exactType) {
                        case "PF":
                            this.primaryForward = th;
                            break;
                        case "PR":
                            this.primaryRewind = th;
                            break;
                        case "SF":
                            this.secondaryForward = th;
                            break;
                        case "SR":
                            this.secondaryRewind = th;
                            break;
                    }
                } else {
                    throw new ConfigurationException(id, "The temperature-sensor " + id + " Is not a (configured) temperature sensor.");
                }

                break;
            case "Pump":
                if (cpm.getComponent(id) instanceof PassingChannel) {
                    this.pump = cpm.getComponent(id);
                } else {
                    throw new ConfigurationException(id, "The Pump " + id + " Is not a (configured) Pump.");
                }
                break;
            case "Valve":
                if (cpm.getComponent(id) instanceof PassingChannel) {
                    this.valve = cpm.getComponent(id);
                } else {
                    throw new ConfigurationException(id, "The Valve " + id + " Is not a (configured) Valve.");
                }
                break;
        }
    }

    /**
     * Checks if it's getting too hot (should never occur bc of the overseer controller but you never know).
     * Can only occur if the minTemperature is reached by the secondary forward.
     *
     * @return true if the secondary rewind is as hot as the secondary forward --> no heat loss --> no heat needed.
     */
    private boolean tooHot() {
        if (System.currentTimeMillis() - this.timeStampWarmthPump > WAITING_FOR_TOO_HOT) {
            if (this.secondaryForward.getTemperature().value().get() >= this.getMinTemperature().value().get()) {
                return this.secondaryRewind.getTemperature().value().get() + TOLERANCE_TEMPERATURE
                        > this.secondaryForward.getTemperature().value().get();
            }
            return false;
        }
        return false;
    }


}