package io.openems.edge.predictor.persistant.model;

import java.time.LocalDateTime;
import java.util.TreeMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.predictor.api.HourlyPredictor;

public abstract class AbstractPersistentModelPredictor extends AbstractOpenemsComponent implements HourlyPredictor {

	String channelAddress;
	public TreeMap<LocalDateTime, Long> hourlyEnergyData = new TreeMap<LocalDateTime, Long>();
	private long currentEnergy;
	LocalDateTime currentHour = LocalDateTime.now();
	private boolean executed = false;

	protected AbstractPersistentModelPredictor(String channelAddress) {
		super(//
				OpenemsComponent.ChannelId.values()//
		);//
		this.channelAddress = channelAddress;//

	}

	protected abstract ComponentManager getComponentManager();

	/*
	 * This method get the value from the ChannelAddress every one hour, and updates
	 * the the TreeMap
	 * 
	 */

	public void calculateEnergyValue() throws OpenemsNamedException {

		LocalDateTime now = LocalDateTime.now();
		ChannelAddress channelAddress = ChannelAddress.fromString(this.channelAddress);

		Long energy = parseLong(getComponentManager().getChannel(channelAddress).value().asStringWithoutUnit());

		if (!executed) {
			this.currentEnergy = energy;
			this.currentHour = now;
			this.executed = true;
		}

		// Detects the switching of hour
		if (now.getHour() == currentHour.plusHours(1).getHour()) {

			long totalEnergy;
			totalEnergy = energy - currentEnergy;

			this.hourlyEnergyData.put(currentHour.withNano(0).withMinute(0).withSecond(0), totalEnergy);
			this.currentEnergy = energy;
			this.currentHour = now;
		}

		if (this.hourlyEnergyData.size() > 24) {
			this.hourlyEnergyData.remove(this.hourlyEnergyData.firstKey());
		}
	}

	public void calculateEnegryValue24min() throws OpenemsNamedException {

		LocalDateTime now = LocalDateTime.now();
		ChannelAddress channelAddress = ChannelAddress.fromString(this.channelAddress);

		Long energy = parseLong(getComponentManager().getChannel(channelAddress).value().asStringWithoutUnit());

		if (!executed) {
			this.currentEnergy = energy;
			this.currentHour = now.withNano(0).withSecond(0);
			this.executed = true;
		}
		// Detects the switching of hour
		if (now.getMinute() == currentHour.plusMinutes(1).getMinute()) {
			long totalEnergy;
			totalEnergy = energy - currentEnergy;

			this.hourlyEnergyData.put(currentHour.withNano(0).withSecond(0), totalEnergy);
			this.currentEnergy = energy;
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
