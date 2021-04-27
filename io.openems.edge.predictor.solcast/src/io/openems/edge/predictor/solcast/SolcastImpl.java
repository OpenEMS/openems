package io.openems.edge.predictor.solcast;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.oneday.AbstractPredictor24Hours;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.SolcastModel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class SolcastImpl extends AbstractPredictor24Hours implements Predictor24Hours, OpenemsComponent, EventHandler {

	private SolcastAPI solarforcastAPI = null; 
	private boolean executed;
	private final Logger log = LoggerFactory.getLogger(SolcastImpl.class);
	LocalDateTime prevHour = LocalDateTime.now();
	private TreeMap<LocalDateTime, Integer> hourlySolarData_00 = new TreeMap<LocalDateTime, Integer>();
	private TreeMap<LocalDateTime, Integer> hourlySolarData_10 = new TreeMap<LocalDateTime, Integer>();
	private TreeMap<LocalDateTime, Integer> hourlySolarData_90 = new TreeMap<LocalDateTime, Integer>();
		
	@Reference
	private ComponentManager componentManager;

	public SolcastImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				Solcast.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.channelAddresses());
		this.solarforcastAPI = new SolcastAPI("https://api.solcast.com.au/rooftop_sites/" + config.resource_id() + "/forecasts?format=json&api_key=" + config.key(), 
				config.starttime(), config.endtime(), config.limitedAPI(), config.debug(), config.debug_file());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	
	@Override
	protected Prediction24Hours createNewPrediction(ChannelAddress channelAddress) {
		
		Integer[] result = new Integer[96];
		
		if (channelAddress.getChannelId().equals("Predict")) {
			int i = Math.max(0, 48 - this.hourlySolarData_00.size());
			for (Entry<LocalDateTime, Integer> entry : this.hourlySolarData_00.entrySet()) {
				result[i++] = entry.getValue();
				result[i++] = entry.getValue();
			}
		}
		else if (channelAddress.getChannelId().equals("Predict10")) {
			int i = Math.max(0, 48 - this.hourlySolarData_10.size());
			for (Entry<LocalDateTime, Integer> entry : this.hourlySolarData_10.entrySet()) {
				result[i++] = entry.getValue();
				result[i++] = entry.getValue();
			}		
		}
		else if (channelAddress.getChannelId().equals("Predict90")) {
			int i = Math.max(0, 48 - this.hourlySolarData_90.size());
			for (Entry<LocalDateTime, Integer> entry : this.hourlySolarData_90.entrySet()) {
				result[i++] = entry.getValue();
				result[i++] = entry.getValue();
			}		
		}
		else {
			return null;
		}
		return new Prediction24Hours(result);
	}
	
	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			try {
				this.calculatePrediction();
				this.channel(Solcast.ChannelId.UNABLE_TO_PREDICT).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, e.getMessage());
				this.channel(Solcast.ChannelId.UNABLE_TO_PREDICT).setNextValue(true);
			}
		}
	}
	
	/*
	 * This method gets the value from the Channel every one hour and updates the TreeMap.
	 */
	private void calculatePrediction() throws OpenemsNamedException {
		LocalDateTime currentHour = LocalDateTime.now(this.componentManager.getClock()).withNano(0).withMinute(0).withSecond(0);
		JsonArray js = null;

		if (!executed) {
			// First time execution - Map is still empty	
			js = this.solarforcastAPI.getSolarForecast(24);			
			this.prevHour = currentHour;
			this.executed = true;
			
		} else if (currentHour.isAfter(this.prevHour)) {
			// hour changed -> get new forecast
			js = this.solarforcastAPI.getSolarForecast(24);

			this.prevHour = currentHour;
		} else {
			// hour did not change -> return
			return;
		}
		
		if (js != null) {
			hourlySolarData_00.clear();
			hourlySolarData_10.clear();
			hourlySolarData_90.clear();
			for (Integer i = 0; i < js.size(); i++) {			
				JsonElement time = js.get(i).getAsJsonObject().get("time");
				LocalDateTime t = OffsetDateTime.parse(time.getAsString()).toLocalDateTime();
				JsonElement solar = js.get(i).getAsJsonObject().get("pv_estimate");
				JsonElement solar10 = js.get(i).getAsJsonObject().get("pv_estimate10");
				JsonElement solar90 = js.get(i).getAsJsonObject().get("pv_estimate90");
				hourlySolarData_00.put(t, solar.getAsInt());
				hourlySolarData_10.put(t, solar10.getAsInt());
				hourlySolarData_90.put(t, solar90.getAsInt());	
			}	
			this.channel(Solcast.ChannelId.PREDICT_ENABLED).setNextValue(true);
			
		}
		else {			
			// remove first 2 elements and add two new elements with null
			hourlySolarData_00.pollFirstEntry();
			hourlySolarData_00.pollFirstEntry();
			hourlySolarData_10.pollFirstEntry();
			hourlySolarData_10.pollFirstEntry();
			hourlySolarData_90.pollFirstEntry();
			hourlySolarData_90.pollFirstEntry();
			
			LocalDateTime DateEnd;
			if (hourlySolarData_00.isEmpty()) {
				DateEnd = currentHour;
			}
			else {
				DateEnd = hourlySolarData_00.lastKey().plusHours(1);
			}
			hourlySolarData_00.put(DateEnd, null);
			hourlySolarData_00.put(DateEnd, null);
			hourlySolarData_10.put(DateEnd, null);
			hourlySolarData_10.put(DateEnd, null);
			hourlySolarData_90.put(DateEnd, null);	
			hourlySolarData_90.put(DateEnd, null);	
			this.channel(Solcast.ChannelId.PREDICT_ENABLED).setNextValue(false);			
			
		}
		this.channel(Solcast.ChannelId.PREDICT).setNextValue(hourlySolarData_00.firstEntry().getValue());
		this.channel(Solcast.ChannelId.PREDICT10).setNextValue(hourlySolarData_10.firstEntry().getValue());
		this.channel(Solcast.ChannelId.PREDICT90).setNextValue(hourlySolarData_90.firstEntry().getValue());

	}

	@Override
	public String debugLog() {
		return "Prediction: " + this.channel(Solcast.ChannelId.PREDICT).value().toString();
	}
}
