package io.openems.edge.shelly.core;

import java.util.Iterator;

import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.shelly.core", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class ShellyCoreImpl extends AbstractOpenemsComponent implements ShellyCore, OpenemsComponent, EventHandler {

	private Config config = null;
	private ShellyApi api = null;
	private List<ShellyComponent> clients;
	

	public ShellyCoreImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ShellyCore.ChannelId.values() //				
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.api = new ShellyApi(this,config.ip());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:			
			api.iterate();			
			_setCommunicationFailed(api.getCommFailed());
			if(!api.getCommFailed()) {
				updateClients();								
			}
			break;
		}
	}

	@Override
	public String debugLog() {
		//return "L:" + this.getActivePower().asString();
		if(api != null) {
			return "Shelly: "+ api.getType();
		}
		return "Shelly: not valid"; 
		
	}
	
	public ShellyApi getApi() {
		return this.api;
	}
	
	public void registerClient(ShellyComponent client) {
		if(client != null) {
			this.clients.add(client);
		}
	}
		
	protected void updateSymmetricMeter(SymmetricMeter meter, Integer index ) throws OpenemsNamedException  {
		// Symmetric meters are updated with only one of the available meters/emeters
		// Which one is decided by asking the client (wantedIndex())
		// I a symmetric meter wants to show all meters/emeters it should request extended 
		// data and implement that itself.
		JsonObject status = api.getStatus();
		JsonArray meters = null;
		if(index < api.getNumMeters()) {							
			meters = JsonUtils.getAsJsonArray(status, "meters");							
		} else if(index<api.getNumEmeters()) {
			meters = JsonUtils.getAsJsonArray(status, "emeters");
		}
		if(meters != null && index<meters.size()) {
			JsonObject meter1 = JsonUtils.getAsJsonObject(meters.get(index));						
			if(meter1.has("current")) {
				meter._setCurrent(Math.round(JsonUtils.getAsFloat(meter1, "current")));
			}
			if(meter1.has("voltage")) {
				meter._setVoltage(Math.round(JsonUtils.getAsFloat(meter1, "voltage")));
			}
			if(meter1.has("reactive")) {
				meter._setReactivePower(Math.round(JsonUtils.getAsFloat(meter1, "reactive")));
			}
			meter._setActivePower(Math.round(JsonUtils.getAsFloat(meter1, "power")));
		}
	}
	
	protected void updateAsymmetricMeter(AsymmetricMeter meter, Integer index ) throws OpenemsNamedException  {
		// Asymmetric meters have three Legs to update.
		// is the first one is decided by asking the client (wantedIndex()). 
		// It is the assumed that L2 and L2 are the next two indices (if available)
		// If any of these assumptions are not true, the client should request extended 
		// data and implement that itself.

		JsonObject status = api.getStatus();
		JsonArray meters = null;
		if(index < api.getNumMeters()) {							
			meters = JsonUtils.getAsJsonArray(status, "meters");							
		} else if(index<api.getNumEmeters()) {
			meters = JsonUtils.getAsJsonArray(status, "emeters");
		}
		
		if(meters == null) {
			return;
		}
		// Don't like the following part. Can this be done as a loop?
		Integer i = index;		
		if(i<meters.size()) {
			JsonObject meter1 = JsonUtils.getAsJsonObject(meters.get(i));						
			if(meter1.has("current")) {
				meter._setCurrentL1(Math.round(JsonUtils.getAsFloat(meter1, "current")));
			}
			if(meter1.has("voltage")) {
				meter._setVoltageL1(Math.round(JsonUtils.getAsFloat(meter1, "voltage")));
			}
			if(meter1.has("reactive")) {
				meter._setReactivePowerL1(Math.round(JsonUtils.getAsFloat(meter1, "reactive")));
			}
			meter._setActivePowerL1(Math.round(JsonUtils.getAsFloat(meter1, "power")));
		}
		++i;
		if(i<meters.size()) {
			JsonObject meter1 = JsonUtils.getAsJsonObject(meters.get(i));						
			if(meter1.has("current")) {
				meter._setCurrentL2(Math.round(JsonUtils.getAsFloat(meter1, "current")));
			}
			if(meter1.has("voltage")) {
				meter._setVoltageL2(Math.round(JsonUtils.getAsFloat(meter1, "voltage")));
			}
			if(meter1.has("reactive")) {
				meter._setReactivePowerL2(Math.round(JsonUtils.getAsFloat(meter1, "reactive")));
			}
			meter._setActivePowerL2(Math.round(JsonUtils.getAsFloat(meter1, "power")));
		}
		++i;
		if(i<meters.size()) {
			JsonObject meter1 = JsonUtils.getAsJsonObject(meters.get(i));						
			if(meter1.has("current")) {
				meter._setCurrentL3(Math.round(JsonUtils.getAsFloat(meter1, "current")));
			}
			if(meter1.has("voltage")) {
				meter._setVoltageL3(Math.round(JsonUtils.getAsFloat(meter1, "voltage")));
			}
			if(meter1.has("reactive")) {
				meter._setReactivePowerL3(Math.round(JsonUtils.getAsFloat(meter1, "reactive")));
			}
			meter._setActivePowerL3(Math.round(JsonUtils.getAsFloat(meter1, "power")));
		}
		
	}
	
	protected void updateClients() {
		for (Iterator<ShellyComponent> it = clients.iterator(); it.hasNext();) {
			ShellyComponent client = it.next();
			try {				
				if(client.setBaseChannels()) {					
					Integer index = client.wantedIndex();					
					if(client instanceof SymmetricMeter ) {
						SymmetricMeter meter = (SymmetricMeter)client;						
						updateSymmetricMeter(meter,index);
					}
					if(client instanceof AsymmetricMeter ) {
						AsymmetricMeter meter = (AsymmetricMeter)client;						
						updateAsymmetricMeter(meter,index);
					}
				}
				if(client.wantsExtendedData()) {
					client.setExtendedData(api.getStatus());
				}
			} catch (Throwable t) {
				
			} 
		}
	}
	
	protected void updateIos() {
		
	}
}
