package io.openems.edge.predictor.persistant.model;

import java.time.LocalDateTime;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.sum.Sum;

public class DataCollectorWorker extends AbstractCycleWorker {

	private final Logger log = LoggerFactory.getLogger(DataCollectorWorker.class);

	private ProductionPersistantModelPredictor parent;

	public DataCollectorWorker(ProductionPersistantModelPredictor parent) {
		this.parent = parent;
	}

	private long currentProduction;
	LocalDateTime currentHour = LocalDateTime.now();
	private boolean executed = false;
	public TreeMap<LocalDateTime, Long> hourlyEnergyData = new TreeMap<LocalDateTime, Long>();

	public void calculateConsumption(LocalDateTime start, LocalDateTime end) throws OpenemsNamedException {

		LocalDateTime now = LocalDateTime.now();
		ChannelAddress channelAddress = ChannelAddress.fromString(parent.channelAddress);

		Long productionEnergy = parseLong(
				parent.componentManager.getChannel(channelAddress).value().asStringWithoutUnit());

		if (!executed) {
			this.currentProduction = productionEnergy;
			this.currentHour = now.withNano(0).withSecond(0);
			this.executed = true;
		}
		// Detects the switching of hour
		if (now.getMinute() == currentHour.plusMinutes(1).getMinute()) {
			log.info(" Switching of the hour detected and updating at: " + " [ " + now + " ] ");
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

	@Override
	protected void forever() throws Throwable {
		calculateConsumption(parent.start, parent.end);
	}

	@Override
	public void triggerNextRun() {
		super.triggerNextRun();
	}

}
