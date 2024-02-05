package io.openems.edge.io.shelly.shellyplus1pm;

import java.util.Objects;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.meter.api.SinglePhaseMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.Plus1PM", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class IoShellyPlus1PMImpl extends AbstractOpenemsComponent
		implements IoShellyPlus1PM, DigitalOutput, SinglePhaseMeter, ElectricityMeter, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(IoShellyPlus1PMImpl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;

	private MeterType meterType = null;
	private SinglePhase phase = null;
	private String baseUrl;

	@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
	private BridgeHttp httpBridge;

	public IoShellyPlus1PMImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoShellyPlus1PM.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(IoShellyPlus1PM.ChannelId.RELAY) //
		};

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = config.type();
		this.phase = config.phase();
		this.baseUrl = "http://" + config.ip();

		if (!this.isEnabled()) {
			return;
		}

		this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/rpc/Shelly.GetStatus", this::processHttpResult);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}

	@Override
	public String debugLog() {
	    var b = new StringBuilder();
	    var valueOpt = this.getRelayChannel().value().asOptional();
	    if (valueOpt.isPresent()) {
	        b.append(valueOpt.get() ? "ON" : "OFF");
	    } else {
	        b.append("Unknown");
	    }
	    b.append("|");
	    b.append(this.getActivePowerChannel().value().asString());
	    
	    return b.toString();
	}


	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> {
			this.executeWrite(this.getRelayChannel(), 0);
		}
		}
	}

	private void processHttpResult(JsonElement result, Throwable error) {
	    this._setSlaveCommunicationFailed(result == null);
	    if (error != null) {
	        this._setRelay(null);
	        this._setActivePower(null);
	        this._setActiveProductionEnergy(null);
	        this.logDebug(this.log, error.getMessage());
	        return;
	    }

	    try {
	        JsonObject jsonResponse = result.getAsJsonObject();
	        // Extracting data from "switch:0"
	        JsonObject switch0 = jsonResponse.getAsJsonObject("switch:0");
	        if (switch0 != null) {
	            boolean relayIson = switch0.get("output").getAsBoolean();
	            float power = switch0.get("apower").getAsFloat();
	            int voltage = (int) switch0.get("voltage").getAsFloat();
	            int current = (int) switch0.get("current").getAsFloat();
	            
	            
	            JsonObject aenergy = switch0.getAsJsonObject("aenergy");
	            long energy = aenergy != null ? aenergy.get("total").getAsLong() : 0;
	            
	            this._setRelay(relayIson);
	            this._setActivePower(Math.round(power));
	            this._setActiveProductionEnergy(energy / 60);
	            
	            int millivolt = (voltage * 1000);
	            int milliamp = (current * 1000);


	            if (this.phase != null) {
	                switch (this.phase) {
	                    case L1:
	        	            this._setVoltageL1(millivolt);
	        	            this._setCurrentL1(milliamp);
	                        break;
	                    case L2:
	        	            this._setVoltageL2(millivolt);
	        	            this._setCurrentL2(milliamp); 
	        	            break;
	                    case L3:
	        	            this._setVoltageL3(millivolt);
	        	            this._setCurrentL3(milliamp);
	        	            break;
	                }
	            }
	        }
	    } catch (Exception e) {
	        this._setRelay(null);
	        this._setActivePower(null);
	        this._setActiveProductionEnergy(null);
	        this.logDebug(this.log, e.getMessage());
	    }
	}


	/**
	 * Execute on Cycle Event "Execute Write".
	 * 
	 * @param channel write channel
	 * @param index   index
	 */
	private void executeWrite(BooleanWriteChannel channel, int index) {
		var readValue = channel.value().get();
		var writeValue = channel.getNextWriteValueAndReset();
		if (writeValue.isEmpty()) {
			// no write value
			return;
		}
		if (Objects.equals(readValue, writeValue.get())) {
			// read value = write value
			return;
		}
		final var url = this.baseUrl + "/relay/" + index + "?turn=" + (writeValue.get() ? "on" : "off");
		this.httpBridge.request(url).whenComplete((t, e) -> {
			this._setSlaveCommunicationFailed(e != null);
		});
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	public SinglePhase getPhase() {
		return this.phase;
	}

}
