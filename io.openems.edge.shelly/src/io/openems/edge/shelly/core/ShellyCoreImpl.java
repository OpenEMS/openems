package io.openems.edge.shelly.core;

import java.util.ArrayList;
import java.util.Iterator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
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
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
		} //
)
public class ShellyCoreImpl extends AbstractOpenemsComponent implements ShellyCore, OpenemsComponent, EventHandler {

	private ShellyApi api = null;
	private List<ShellyComponent> clients;
	private final Logger log = LoggerFactory.getLogger(ShellyCoreImpl.class);
	

	public ShellyCoreImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ShellyCore.ChannelId.values() //				
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.api = new ShellyApi(this,config.ip());
		clients = new ArrayList<ShellyComponent>();
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
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.eventExecuteWrite();
			break;
		}
	}
	
	public ShellyApi getApi() {
		return this.api;
	}
	
	public void registerClient(ShellyComponent client) {
		if(client != null && !this.clients.contains(client)) {
			this.clients.add(client);
			this.logInfo(log, "New client registered");
		}
	}
		
	public void unregisterClient(ShellyComponent client) {
		this.logInfo(log, "Unregister client");
		this.clients.remove(client);
	}
	
	public void setRelay(Integer index, Boolean value) throws OpenemsNamedException{
		this.api.setRelayTurn(index, value);
	}
	
	protected void updateSymmetricMeter(SymmetricMeter meter, Integer index ) throws OpenemsNamedException  {
		// Symmetric meters are updated with only one of the available meters/emeters
		// Which one is decided by asking the client (wantedIndex())
		// I a symmetric meter wants to show all meters/emeters it should request extended 
		// data and implement that itself.
		JsonObject status = api.getStatus();
		JsonArray meters = null;
		MeterType type= meter.getMeterType();
		
		if(index < api.getNumMeters()) {							
			meters = JsonUtils.getAsJsonArray(status, "meters");							
		} else if(index<api.getNumEmeters()) {
			meters = JsonUtils.getAsJsonArray(status, "emeters");
		}
		if(meters != null && index<meters.size()) {
			JsonObject meter1 = JsonUtils.getAsJsonObject(meters.get(index));						
			
			if(meter1.has("voltage")) {
				meter._setVoltage(Math.round(JsonUtils.getAsFloat(meter1, "voltage")*1000.0f));
			}
			
			int factor = 1;
			switch(type) {
			case GRID:
			case PRODUCTION:			
			case PRODUCTION_AND_CONSUMPTION:
				factor = 1;
				break;
			case CONSUMPTION_METERED:			
			case CONSUMPTION_NOT_METERED:
				factor = -1;
				break;
			}
			
			if(meter1.has("current")) {
				meter._setCurrent(Math.round(JsonUtils.getAsFloat(meter1, "current")*factor*1000.0f));
			}
			
			if(meter1.has("reactive")) {
				meter._setReactivePower(TypeUtils.multiply(Math.round(JsonUtils.getAsFloat(meter1, "reactive")),factor));
			}
						
			if(meter1.has("total")) {
				if(factor < 0) {
					meter._setActiveConsumptionEnergy(TypeUtils.multiply(Math.round(JsonUtils.getAsFloat(meter1, "total")),factor));
				} else {
					meter._setActiveProductionEnergy(TypeUtils.multiply(Math.round(JsonUtils.getAsFloat(meter1, "total")),factor));
				}
			}
			
			meter._setActivePower(TypeUtils.multiply(Math.round(JsonUtils.getAsFloat(meter1, "power")),factor));
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
	
	protected void updateOutput(DigitalOutput out, Integer index ) throws OpenemsNamedException {

		// This will set the digitalOutput[0] to the value of the relay represented by this class.

		BooleanWriteChannel[] outputChannels = out.digitalOutputChannels();			 			
		if(outputChannels[0] != null) {
			JsonObject status = this.api.getStatus();
			JsonArray relays = null;
			if(index < this.api.getNumRelays()) {							
				relays = JsonUtils.getAsJsonArray(status, "relays");
				JsonObject relay = JsonUtils.getAsJsonObject(relays.get(index));						
				outputChannels[0].setNextValue(JsonUtils.getAsBoolean(relay, "ison"));
			}				
		}	
	}
	
	protected void writeOutput(DigitalOutput out, Integer index) throws OpenemsNamedException {
		BooleanWriteChannel[] outputChannels = out.digitalOutputChannels();		
		Boolean readValue = outputChannels[0].value().get();
		Optional<Boolean> writeValue = outputChannels[0].getNextWriteValueAndReset();
		if (!writeValue.isPresent()) {
			// no write value
			return;
		}
		if (Objects.equals(readValue, writeValue.get())) {
			// read value == write value
			return;
		}
		this.setRelay(index, writeValue.get());
	}

	protected void eventExecuteWrite() {
		for (Iterator<ShellyComponent> it = clients.iterator(); it.hasNext();) {
			ShellyComponent client = it.next();
			try {				
				if(client.setBaseChannels()) {					
					Integer index = client.wantedIndex();										
					if(client instanceof DigitalOutput ) {
						DigitalOutput out = (DigitalOutput)client;						
						writeOutput(out,index);
					}
				}				
			} catch (OpenemsNamedException e) {
				
			} 
		}
	}
	
	protected void updateClients() {
		for (Iterator<ShellyComponent> it = clients.iterator(); it.hasNext();) {
			ShellyComponent client = it.next();
			try {	
				client._setSlaveCommunicationFailed(api.getCommFailed());
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
					if(client instanceof DigitalOutput ) {
						DigitalOutput out = (DigitalOutput)client;						
						updateOutput(out,index);
					}
				}
				if(client.wantsExtendedData()) {
					client.setExtendedData(api.getStatus());
				}
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "Update clients failed with "+e.getMessage());
			} 
		}
	}
}
