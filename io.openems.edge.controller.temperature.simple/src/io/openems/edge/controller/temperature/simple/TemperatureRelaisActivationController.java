package io.openems.edge.controller.temperature.simple;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.relais.api.RelaisActuator;
import io.openems.edge.thermometer.api.Thermometer;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "SimpleTemperatureController")
public class TemperatureRelaisActivationController extends AbstractOpenemsComponent implements OpenemsComponent, Controller {

    @Reference
    ComponentManager cpm;

    private Thermometer temperatureSensor;
    private RelaisActuator relaisActuator;
    private float toleranceTemperature;
    private float maxTemp;
    private float minTemp;


    public TemperatureRelaisActivationController() {
        super(OpenemsComponent.ChannelId.values(), Controller.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());

        this.toleranceTemperature = config.toleranceTemperature();
        this.maxTemp = config.TemperatureMax();
        this.minTemp = config.TemperatureMin();

        if (cpm.getComponent(config.relaisId()) instanceof RelaisActuator) {
            this.relaisActuator = cpm.getComponent(config.relaisId());
        }

        if (cpm.getComponent(config.temperatureId()) instanceof Thermometer) {
            temperatureSensor = cpm.getComponent(config.temperatureId());
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {

        // String temperature = temperatureSensor.getTemperatureOfSensor().value().toString().replaceAll("[a-zA-Z _]", "").trim();
        int temperature = temperatureSensor.getTemperature().value().get();
        if (temperature + toleranceTemperature < minTemp) {
            //increase Temperature
            if (relaisActuator.isCloser()) {
                //for warming purposes a closer relais has to be closed --> closed circuit default
                relaisActuator.getRelaisChannelValue().setNextWriteValue(true);
            } else {
                //same as above; relais is a opener --> so it has to be deactivated for closed circuit
                relaisActuator.getRelaisChannelValue().setNextWriteValue(false);
            }
        } else if (temperature - toleranceTemperature > maxTemp) {
            if (relaisActuator.isCloser()) {
                //logic is Vice Versa here: | | <-- inactive |_| <---active
                relaisActuator.getRelaisChannelValue().setNextWriteValue(false);
            } else {
                //inactive --> |_|   active --> | |
                relaisActuator.getRelaisChannelValue().setNextWriteValue(true);
            }
        }
    }
}
