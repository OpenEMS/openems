package io.openems.edge.predictor.persistant.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map.Entry;
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
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.predictor.api.HourlyPrediction;
import io.openems.edge.predictor.api.ProductionHourlyPredictor;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Predictor.PersitantModel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE//
)
public class ProductionPersistantModelPredictor extends AbstractPersistentModelPredictor
		implements ProductionHourlyPredictor, OpenemsComponent, EventHandler {

	// private final Logger log = LoggerFactory.getLogger(PersistantModel.class);

	// private Config config;

	@Reference
	protected ComponentManager componentManager;

	private DataCollectorWorker worker = new DataCollectorWorker(this);
	public TreeMap<LocalDateTime, Long> hourlyEnergyData = new TreeMap<LocalDateTime, Long>();

	public ProductionPersistantModelPredictor() {
		super("_sum/ProductionActivePower");
	}

	LocalDateTime start = LocalDateTime.now();
	LocalDateTime end = LocalDateTime.now().plusMinutes(24);

	@Override
	public HourlyPrediction get24hPrediction() {		
		
		hourlyEnergyData = worker.hourlyEnergyData;

		ArrayList<Long> valueList = new ArrayList<Long>();
		
		if (!hourlyEnergyData.isEmpty()) {			
			for (Entry<LocalDateTime, Long> entry : hourlyEnergyData.entrySet()) {
				valueList.add(entry.getValue());
			}
		}
		Integer[] val = valueList.toArray(new Integer[valueList.size()]);
		HourlyPrediction hourlyPrediction = new HourlyPrediction(val, hourlyEnergyData.firstKey(), "1 hr");
		return hourlyPrediction;
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.alias(), config.id(), config.enabled());
		this.worker.activate(this.id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			worker.triggerNextRun();

		}
	}

	@Override
	public String debugLog() {
		return "Predicted --->: " + worker.hourlyEnergyData.toString();
	}

}
