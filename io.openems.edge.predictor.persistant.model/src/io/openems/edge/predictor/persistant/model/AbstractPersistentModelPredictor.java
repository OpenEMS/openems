package io.openems.edge.predictor.persistant.model;

import java.time.LocalDateTime;
import java.util.TreeMap;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.predictor.api.HourlyPrediction;
import io.openems.edge.predictor.api.HourlyPredictor;

public abstract class AbstractPersistentModelPredictor extends AbstractOpenemsComponent implements HourlyPredictor {

	public TreeMap<LocalDateTime, Integer> hourlyConsumption = new TreeMap<>();
	
	String channelAddress; 

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
	


	protected Long getChannelValue( ChannelAddress channelAddress) {		
		return null;
	}


}
