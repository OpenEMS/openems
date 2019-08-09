package io.openems.edge.predictor.persistant.consumption;

import java.time.LocalDateTime;
import java.util.TreeMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.predictor.api.ConsumptionHourlyPredictor;
import io.openems.edge.predictor.persistant.model.AbstractPersistentModelPredictor;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Predictor.Consumption.PersistantModel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

public class ConsumptionPersistantModelPredictor extends AbstractPersistentModelPredictor
		implements ConsumptionHourlyPredictor, OpenemsComponent, EventHandler {

	@Reference
	protected ComponentManager componentManager;

	public TreeMap<LocalDateTime, Long> hourlyEnergyData = new TreeMap<LocalDateTime, Long>();

	public ConsumptionPersistantModelPredictor() {
		super("_sum/ConsumptionActiveEnergy");
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.alias(), config.id(), config.enabled());

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

	}

	@Override
	public String debugLog() {
		return "ConsumptionPredicted : " + super.hourlyEnergyData.toString();
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			try {
				// calculateConsumption();
				calculateEnergyValue();
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}

		}
	}

}
