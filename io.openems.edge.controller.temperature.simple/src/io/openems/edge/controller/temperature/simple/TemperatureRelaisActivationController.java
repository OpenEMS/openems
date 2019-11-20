package io.openems.edge.controller.temperature.simple;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.raspberrypi.sensors.temperaturesensor.TemperatureSensor;
import io.openems.edge.relais.RelaisActuator;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "SimpleTemperatureController")
public class TemperatureRelaisActivationController extends AbstractOpenemsComponent implements OpenemsComponent, Controller {

	@Reference
	ComponentManager cpm;

	private TemperatureSensor temperatureSensor;
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
			RelaisActuator tempR = cpm.getComponent(config.relaisId());
			if (tempR.getRelaisId().equals(config.relaisId())) {
				this.relaisActuator = tempR;
			}
		}
		if (cpm.getComponent(config.temperatureId()) instanceof TemperatureSensor) {
			TemperatureSensor tempT = cpm.getComponent(config.temperatureId());
			if (tempT.getTemperatureSensorId().equals(config.temperatureId())) {
				this.temperatureSensor = tempT;
			}
		}
	}

	@Deactivate
	public void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsError.OpenemsNamedException {
//		int temp = temperatureSensor.getTemperatureOfSensor().getNextValue().get();
		String temperature = temperatureSensor.getTemperatureOfSensor().value().toString().replaceAll("[a-zA-Z _]", "").trim();
		if (Integer.parseInt(temperature) + toleranceTemperature < minTemp) {
			//increase Temperature
			if (relaisActuator.isCloser()) {
				//for warming purposes a closer relais has to be closed --> closed circuit default
				relaisActuator.getRelaisChannelValue().setNextWriteValue(true);
			} else {
				//same as above; relais is a opener --> so it has to be deactivated for closed circuit
				relaisActuator.getRelaisChannelValue().setNextWriteValue(false);
			}
		} else if (Integer.parseInt(temperature) - toleranceTemperature > maxTemp) {
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
