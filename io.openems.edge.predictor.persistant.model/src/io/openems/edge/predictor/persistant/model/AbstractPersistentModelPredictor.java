package io.openems.edge.predictor.persistant.model;

import java.time.LocalDateTime;
import java.util.TreeMap;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.predictor.api.HourlyPrediction;
import io.openems.edge.predictor.api.HourlyPredictor;


public abstract class AbstractPersistentModelPredictor extends AbstractOpenemsComponent implements HourlyPredictor, EventHandler {

	public TreeMap<LocalDateTime, Integer> hourlyConsumption = new TreeMap<>();

	String channelAddress;
	public TreeMap<LocalDateTime, Long> hourlyEnergyData = new TreeMap<LocalDateTime, Long>();
	private long currentProduction;
	LocalDateTime currentHour = LocalDateTime.now();
	private boolean executed = false;


	protected AbstractPersistentModelPredictor(String channelAddress) {
		super(//
				OpenemsComponent.ChannelId.values()//
		);
		this.channelAddress = channelAddress;

	}

	@Override
	public HourlyPrediction get24hPrediction() {
		return null;
	}

	protected Long getChannelValue(ChannelAddress channelAddress) {
		return null;
	}
	
	protected abstract ComponentManager getComponentManager();
	
	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			try {
				calculateConsumption();
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}

		}
	}



	public void calculateConsumption() throws OpenemsNamedException {

		LocalDateTime now = LocalDateTime.now();
		ChannelAddress channelAddress = ChannelAddress.fromString(this.channelAddress);

		Long productionEnergy = parseLong(getComponentManager().getChannel(channelAddress).value().asStringWithoutUnit());

		if (!executed) {
			this.currentProduction = productionEnergy;
			this.currentHour = now.withNano(0).withSecond(0);
			this.executed = true;
		}
		// Detects the switching of hour
		if (now.getMinute() == currentHour.plusMinutes(1).getMinute()) {
			long totalConsumption;
			totalConsumption = productionEnergy - currentProduction;

			this.hourlyEnergyData.put(currentHour.withNano(0).withSecond(0), totalConsumption);
			this.currentProduction = productionEnergy;
			this.currentHour = now;
		}

		if (this.hourlyEnergyData.size() > 24) {
			this.hourlyEnergyData.remove(this.hourlyEnergyData.firstKey());
		}

	}

	private Long parseLong(String asStringWithoutUnit) {
		return Long.parseLong(asStringWithoutUnit);
	}


}
