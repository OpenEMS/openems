package io.openems.edge.io.shelly.shellyplus1pmaddon;


import static io.openems.common.utils.JsonUtils.getAsOptionalFloat;
import static io.openems.common.utils.JsonUtils.getAsOptionalBoolean;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static java.lang.Math.round;

import com.google.gson.JsonObject;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
//import io.openems.edge.io.shelly.shellyplus1pmaddon.AddOnEnums;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.Plus1PMAddOnChannel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class IoShellyPlus1PmAddOnImpl extends AbstractOpenemsComponent implements IoShellyPlus1PmAddOn,
		OpenemsComponent, TimedataProvider, EventHandler {


	private final Logger log = LoggerFactory.getLogger(IoShellyPlus1PmAddOnImpl.class);
	
	private String baseUrl;
	private AddOnEnums.InputType inputType;
	private AddOnEnums.InputIndex inputIndex;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public IoShellyPlus1PmAddOnImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				IoShellyPlus1PmAddOn.ChannelId.values()
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();
		this.inputType = config.type();
		this.inputIndex = config.index();
		if (!this.isEnabled()) {
			return;
		}

		this.httpBridge.subscribeJsonCycle(2, this.baseUrl + "/rpc/Shelly.GetStatus", this::processHttpResult);
//		this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/rpc/Shelly.GetStatus", this::processHttpResult);
		
		
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
		super.deactivate();
	}


	@Override
	public String debugLog() {
		var b = new StringBuilder();

		switch (this.inputType) {
		case ANALOG_INPUT:
			b.append(this.channel(IoShellyPlus1PmAddOn.ChannelId.Add_On_Analog_Input).value().asString());
			break;
		case DIGITAL_INPUT:
			b.append(this.channel(IoShellyPlus1PmAddOn.ChannelId.Add_On_Digital_Input).value().asString());
			break;
		case TEMPERATURE:
			b.append(this.channel(IoShellyPlus1PmAddOn.ChannelId.Add_On_Temperature).value().asString());
			break;
		case TEMPERATURE_AND_HUMIDITY:
			b.append(this.channel(IoShellyPlus1PmAddOn.ChannelId.Add_On_Temperature).value().asString());
			b.append("|").append(this.channel(IoShellyPlus1PmAddOn.ChannelId.Add_On_Humidity).value().asString());
			break;
		case VOLTAGE:
			b.append(this.channel(IoShellyPlus1PmAddOn.ChannelId.Add_On_Voltmeter).value().asString());
			break;
		default:
			break;
		}
		return b.toString();

//		return generateDebugLog(this.digitalOutputChannels, this.getActivePowerChannel(), this.inputChannels());
//		return generateDebugLog(this.digitalOutputChannels, this.getActivePowerChannel());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
	}

	private void processHttpResult(HttpResponse<JsonElement> result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);

		//boolean restartRequired = false;
		

		if (error != null) {
			this.logDebug(this.log, error.getMessage());

		} else {
			try {
				var jsonResponse = getAsJsonObject(result.data());

				getInput(jsonResponse, this.inputType, this.inputIndex);
				
				//var sys = getAsJsonObject(jsonResponse, "sys");
				//restartRequired = getAsBoolean(sys, "restart_required");

			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, e.getMessage());
			}
		}

		//this.channel(IoShellyPlus1PmAddOn.ChannelId.NEEDS_RESTART).setNextValue(restartRequired);
	}


	/**
	 * Extract values from JSON 
	 */

	private void getInput(JsonObject json, AddOnEnums.InputType inputType, AddOnEnums.InputIndex inputIndex) {
	
		
			switch (inputType) {
			case ANALOG_INPUT:
				getInputValue(json, "input:"+ inputIndex.index, (IntegerReadChannel) this.channel(IoShellyPlus1PmAddOn.ChannelId.Add_On_Analog_Input));
				break;
			case DIGITAL_INPUT:
				getInputValue(json, "input:"+ inputIndex.index,  (BooleanReadChannel) this.channel(IoShellyPlus1PmAddOn.ChannelId.Add_On_Digital_Input));
				break;
			case TEMPERATURE:
				getInputValue(json, "temperature:"+ inputIndex.index,  (IntegerReadChannel) this.channel(IoShellyPlus1PmAddOn.ChannelId.Add_On_Temperature));
				break;
			case TEMPERATURE_AND_HUMIDITY:
				getInputValue(json, "temperature:"+ inputIndex.index,  (IntegerReadChannel) this.channel(IoShellyPlus1PmAddOn.ChannelId.Add_On_Temperature));
				getInputValue(json, "humidity:"+ inputIndex.index,  (IntegerReadChannel) this.channel(IoShellyPlus1PmAddOn.ChannelId.Add_On_Humidity));
				break;
			case VOLTAGE:
				getInputValue(json, "voltmeter:"+ inputIndex.index,  (IntegerReadChannel) this.channel(IoShellyPlus1PmAddOn.ChannelId.Add_On_Voltmeter));
				break;
			default:
				break;
		}
	}
	
 	private void getInputValue(JsonObject json, String inputName, IntegerReadChannel ch) {
		try {
			if (json.has(inputName)) {
				final var input = getAsJsonObject(json, inputName);
				
				if (input.has("tC")) {//Temperature
					getAsOptionalFloat(input, "tC").ifPresent(v -> ch.setNextValue(round(v * 10)));
				} else if (input.has("rh")) {//Humidity
					getAsOptionalFloat(input, "rh").ifPresent(v -> ch.setNextValue(round(v)));
				} else if (input.has("voltage")) {
					getAsOptionalFloat(input, "voltage").ifPresent(v -> ch.setNextValue(round(v * 1000)));
				} else if (input.has("percent")) {
					getAsOptionalFloat(input, "percent").ifPresent(v -> ch.setNextValue(round(v)));
				} else {
					ch.setNextValue(null);
				}
			} else {
				ch.setNextValue(null);
			}
			
		} catch (OpenemsNamedException e) {
			this.logDebug(this.log, e.getMessage());
		}
	}
 	
 	private void getInputValue(JsonObject json, String inputName, BooleanReadChannel ch) {
		try {
			if (json.has(inputName)) {
				final var input = getAsJsonObject(json, inputName);
				
				if (input.has("state"))  {
					getAsOptionalBoolean(input, "state").ifPresent(v -> ch.setNextValue(v));
				} else {
					ch.setNextValue(null);
				}
			} else {
				ch.setNextValue(null);
			}
			
		} catch (OpenemsNamedException e) {
			this.logDebug(this.log, e.getMessage());
		}
	}



	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}