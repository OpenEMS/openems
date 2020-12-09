package io.openems.edge.predictor.persistencemodel;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.predictor.api.hourly.HourlyPrediction;
import io.openems.edge.predictor.api.hourly.HourlyPredictor;

public abstract class AbstractPersistenceModelPredictor extends AbstractOpenemsComponent implements HourlyPredictor {

	private final Logger log = LoggerFactory.getLogger(AbstractPersistenceModelPredictor.class);

	private final ChannelAddress channelAddress;
	private boolean executed;
	private long currentEnergy;
	ZonedDateTime prevHour = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC);

	private final TreeMap<ZonedDateTime, Integer> hourlyEnergyData = new TreeMap<ZonedDateTime, Integer>();

	protected AbstractPersistenceModelPredictor(String componentId,
			io.openems.edge.common.channel.ChannelId channelId) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				PredictorChannelId.values() //
		);
		this.channelAddress = new ChannelAddress(componentId, channelId.id());
	}

	protected abstract ComponentManager getComponentManager();

	/**
	 * Collects the persistence model data on every cycle.
	 * 
	 * @param event the Event provided by {@link EventHandler}.
	 */
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			try {
				this.calculateEnergyValue();

				this.channel(PredictorChannelId.UNABLE_TO_PREDICT).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, e.getMessage());
				this.channel(PredictorChannelId.UNABLE_TO_PREDICT).setNextValue(true);
			}

		}
	}

	/*
	 * This method gets the value from the Channel every one hour and updates the
	 * TreeMap.
	 */
	private void calculateEnergyValue() throws OpenemsNamedException {
		LongReadChannel channel = this.getComponentManager().getChannel(this.channelAddress);
		Optional<Long> energyOpt = channel.value().asOptional();

		// Stop early if there is no energy available (yet)
		if (!energyOpt.isPresent()) {
			return;
		}
		long energy = energyOpt.get();

		ZonedDateTime currentHour = ZonedDateTime.now(this.getComponentManager().getClock()).withNano(0).withMinute(0)
				.withSecond(0).withZoneSameInstant(ZoneOffset.UTC);

		if (!executed) {
			// First time execution - Map is still empty
			this.currentEnergy = energy;
			this.prevHour = currentHour;
			this.executed = true;
		} else if (currentHour.isAfter(this.prevHour)) {
			// hour changed -> calculate delta and record value
			int delta = (int) (energy - this.currentEnergy);
			this.hourlyEnergyData.put(this.prevHour, delta);
			this.prevHour = currentHour;
			this.currentEnergy = energy;
		} else {
			// hour did not change -> return
			return;
		}

		// We added an entry to the map. Implement circular buffer.
		if (this.hourlyEnergyData.size() > 24) {
			this.hourlyEnergyData.remove(this.hourlyEnergyData.firstKey());
		}
	}

	@Override
	public HourlyPrediction get24hPrediction() {
		Integer[] values = new Integer[24];
		int i = Math.max(0, 24 - this.hourlyEnergyData.size());

		for (Entry<ZonedDateTime, Integer> entry : this.hourlyEnergyData.entrySet()) {
			values[i++] = entry.getValue();
		}
		ZonedDateTime currentHour = ZonedDateTime.now(this.getComponentManager().getClock()).withNano(0).withMinute(0)
				.withSecond(0).withZoneSameInstant(ZoneOffset.UTC);

		HourlyPrediction hourlyPrediction = new HourlyPrediction(values, currentHour);
		return hourlyPrediction;
	}

}
