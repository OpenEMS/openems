package io.openems.edge.controller.heatnetwork.passingstation.overseer;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.passingstation.api.ControllerPassingChannel;
import io.openems.edge.thermometer.api.Thermometer;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Passing.Overseer")
public class ControllerOverseerImpl extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

    protected ControllerPassingChannel passing;
    protected List<Thermometer> temperatureSensor = new ArrayList<>();
    private int tolerance;
    private long coolDownTime;
    private boolean coolDownTimeSet;

    public ControllerOverseerImpl() {
        super(OpenemsComponent.ChannelId.values(), Controller.ChannelId.values());
    }

    @Reference
    ComponentManager cpm;

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());

        allocateComponents(config.allocated_Passing_Controller(), config.allocated_Temperature_Sensor());

        passing.getMinTemperature().setNextWriteValue(config.min_Temperature());
        this.tolerance = config.tolerated_Temperature_Range();
    }

    @Deactivate
    public void deactivate() {

        super.deactivate();
        try {
            this.passing.getOnOff().setNextWriteValue(false);
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }

    private void allocateComponents(String controller, String[] temperatureSensor) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (cpm.getComponent(controller) instanceof ControllerPassingChannel) {
            passing = cpm.getComponent(controller);

        } else {
            throw new ConfigurationException(controller,
                    "Allocated Passing Controller not a Passing Controller; Check if Name is correct and try again");
        }
        ConfigurationException[] exConfig = {null};
        OpenemsError.OpenemsNamedException[] exNamed = {null};
        Arrays.stream(temperatureSensor).forEach(thermometer -> {
            try {

                if (cpm.getComponent(thermometer) instanceof Thermometer) {
                    this.temperatureSensor.add(cpm.getComponent(thermometer));
                } else {
                    throw new ConfigurationException(thermometer,
                            "Allocated Temperature Sensor is not Correct; Check Name and try again.");
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                exNamed[0] = e;
            } catch (ConfigurationException e) {
                exConfig[0] = e;
            }

        });
        if (exConfig[0] != null) {
            throw exConfig[0];
        }
        if (exNamed[0] != null) {
            throw exNamed[0];
        }
    }


    /**
     * Activates and Deactivates the PassingController, depending if the Temperature setPoint is reached or not.
     */

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        boolean heatingReached = this.heatingReached();
        boolean passingStationNoError = this.passing.noError().getNextValue().get();

        if (passing == null) {
            throw new RuntimeException("The Allocated Passing Controller is not active, please Check.");
        } else if (heatingReached == false && passingStationNoError == true) {

            this.passing.getOnOff().setNextWriteValue(true);
        } else if (heatingReached == true && passingStationNoError == true) {

            this.passing.getOnOff().setNextWriteValue(false);

        } else {
            passing.getOnOff().setNextWriteValue(false);

            if (coolDownTimeSet == false && this.passing.getErrorCode().getNextValue().get() == 2) {
                this.coolDownTime = System.currentTimeMillis();
                coolDownTimeSet = true;
            }
            //After Cooldown set Value to true; Only happens if ErrorCode was 2.
            if (coolDownTimeSet == true) {
                if (System.currentTimeMillis() - coolDownTime > 30 * 1000) {
                    passing.noError().setNextValue(true);
                    passing.getOnOff().setNextWriteValue(true);
                    this.coolDownTimeSet = false;
                    return;
                }
            }
            throw new OpenemsException("The Passing Controller got an Error! With ErrorCode: "
                    + this.passing.getErrorCode().getNextValue().get());
        }
    }

    /**
     * Checks if the MinTemperature is reached. (comparing with own TemperatureSensor)
     *
     * @return a boolean depending if heat is reached or not.
     */
    private boolean heatingReached() {
        if (passing.getMinTemperature().value().isDefined()) {
            return this.temperatureSensor.stream().noneMatch(
                    thermometer -> thermometer.getTemperature().getNextValue().get() <= passing.getMinTemperature().value().get());

        }
        return true;
    }
}
