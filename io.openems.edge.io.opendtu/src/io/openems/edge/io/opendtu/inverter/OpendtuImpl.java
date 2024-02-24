package io.openems.edge.io.opendtu.inverter;

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

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.opendtu.common.OpendtuApi;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class OpendtuImpl extends AbstractOpenemsComponent
		implements Opendtu, SinglePhaseMeter, ElectricityMeter, OpenemsComponent, EventHandler, TimedataProvider {

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;
	
	private final CalculateEnergyFromPower calculateActualEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	
	private final Logger log = LoggerFactory.getLogger(OpendtuImpl.class);

	private OpendtuApi opendtuApi = null;
	private MeterType meterType = null;
	private SinglePhase phase = null;
	private String serialNumber;
    Integer limitValue = null;
    private boolean isInitialPowerLimitSet = false;
    

	public OpendtuImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Opendtu.ChannelId.values() //
		);

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
	    super.activate(context, config.id(), config.alias(), config.enabled());
	    this.opendtuApi = new OpendtuApi(config.ip(), config.username(), config.password());
	    this.meterType = config.type();
	    this.phase = config.phase();
	    this.serialNumber = config.serialNumber();
	    
	    // Set the initial power limit from the configuration, only if not set before
	    if (!this.isInitialPowerLimitSet) {
	        Integer initialPowerLimit = config.initialPowerLimit();
	        if (initialPowerLimit != null) {
	            this.setInitialPowerLimit(initialPowerLimit, initialPowerLimit);
	            this.isInitialPowerLimitSet = true;
	        }
	    }
	}
	@Override
	@Deactivate
	protected void deactivate() {
	    this.isInitialPowerLimitSet = false;
		super.deactivate();
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder();
		b.append(this.getActivePowerChannel().value().asString());
		return b.toString();
	}

	@Override
	public void handleEvent(Event event) {
		this.calculateEnergy();
	    if (!this.isEnabled()) {
	        return;
	    }

	    switch (event.getTopic()) {
	    case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
	        this.eventBeforeProcessImage();
	        this.updateLimitStatusChannel();
	        break;
	    }
	}


	private void setInitialPowerLimit(Integer limitValue, Integer lastSetPowerLimit) {

	    try {
	        JsonObject response = this.opendtuApi.setPowerLimit(this.serialNumber, 1, limitValue);
	        String messageType = JsonUtils.getAsString(response, "type");
	        
	        this.logInfo(log, "Called PowerLimit");

	        if (!"success".equals(messageType)) {
	            this.logWarn(log, "Failed to set power limit on activation. Response: " + response.toString());
	        } else {
	            lastSetPowerLimit = limitValue; // Update the last set value only on successful set
	        }
	    } catch (OpenemsNamedException e) {
	        this.logError(log, "Error setting power limit on activation: " + e.getMessage());
	    }
	}

	/**
	 * Execute on Cycle Event "Before Process Image".
	 */
	private void eventBeforeProcessImage() {
	    try {
	        var json = this.opendtuApi.getStatusForInverter(serialNumber);

	        var inverters = JsonUtils.getAsJsonArray(json, "inverters");
	        for (var inverterElem : inverters) {
	            var inverter = inverterElem.getAsJsonObject();
	            String serial = JsonUtils.getAsString(inverter, "serial");

	            if (inverter.has("AC")) {
	                var acData = inverter.getAsJsonObject("AC");
	                if (acData.has("0")) {
	                    var ac0Data = acData.getAsJsonObject("0");

	                    float power = extractValueAsInteger(ac0Data, "Power");
	                    float voltage = extractValueAsInteger(ac0Data, "Voltage");
	                    float current = extractValueAsInteger(ac0Data, "Current");

	                    setValuesForInverter(serial, voltage, current, power);
	                    this.channel(Opendtu.ChannelId.WARN_INVERTER_NOT_REACHABLE).setNextValue(false);
	                    this._setSlaveCommunicationFailed(false);
	                }
	            }
	        }
	    } catch (OpenemsNamedException e) {
	        this.logError(this.log, "Unable to read from openDTU API: " + e.getMessage());
	        this.channel(Opendtu.ChannelId.WARN_INVERTER_NOT_REACHABLE).setNextValue(true);
	        this._setSlaveCommunicationFailed(true);
	    }
	}
	
	private float extractValueAsInteger(JsonObject jsonObject, String key) {
	    if (jsonObject.has(key) && jsonObject.get(key).isJsonObject()) {
	        var data = jsonObject.getAsJsonObject(key);
	        if (data.has("v") && data.get("v").isJsonPrimitive()) {
	            return data.get("v").getAsFloat();
	        }
	    }
	    return 0; // or handle this case as needed
	}

	private void setValuesForInverter(String serial, float voltage, float current, float power) {
	    // Check if the serial number matches the configured serial number
	    if (this.serialNumber.equals(serial)) {
	    	
			int scaledVoltage = Math.round(voltage * 1000);
			int scaledCurrent = Math.round(current * 1000);
			int scaledPower = Math.round(power);
			
	    	// Check the configured phase and set values accordingly
	        if (this.phase != null) {
	            switch (this.phase) {
	            case L1:
	                this._setActivePowerL1(scaledPower);
	                this._setVoltageL1(scaledVoltage);
	                this._setCurrentL1(scaledCurrent);
	                
	                this._setVoltageL2(0);
	                this._setCurrentL2(0);
	                
	                this._setVoltageL3(0);
	                this._setCurrentL3(0);
	                break;
	            case L2:
	                this._setActivePowerL2(scaledPower);
	                this._setVoltageL2(scaledVoltage);
	                this._setCurrentL2(scaledCurrent);
	                
	                this._setVoltageL1(0);
	                this._setCurrentL1(0);
	                
	                this._setVoltageL3(0);
	                this._setCurrentL3(0);
	                break;
	            case L3:
	                this._setActivePowerL3(scaledPower);
	                this._setVoltageL3(scaledVoltage);
	                this._setCurrentL3(scaledCurrent);
	                
	                this._setVoltageL1(0);
	                this._setCurrentL1(0);
	                
	                this._setVoltageL2(0);
	                this._setCurrentL2(0);
	                break;
	        }
	        }
	        // Set frequency (common for all phases)
            this._setActivePower(scaledPower);
	    }
	}

/**	
	
	// Helper method to calculate power
	private Integer calculatePower(Integer current, Integer voltage) {
	    if (current == null || voltage == null) {
	        return 0;
	    }
	    this.logInfo(log, "Current: " + current + " Voltage: " + voltage);
	    // Power calculation in Watts (W), given current in Amperes (A) and voltage in Volts (V)
	    return (current * voltage);
	}
*/

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		var actualPower = this.getActivePower().get();
		if (actualPower == null) {
			// Not available
			this.calculateActualEnergy.update(null);
		} else if (actualPower > 0) {
			this.calculateActualEnergy.update(actualPower);
		} else {
			this.calculateActualEnergy.update(0);
		}
	}
	
	/**
	 * Updates the LimitStatus channel using the value from the API.
	 */
	private void updateLimitStatusChannel() {
	    try {
	        JsonObject limitStatusResponse = this.opendtuApi.getLimitStatus();

	        // Extract the limit_set_status for the inverter with the given serial number
	        if (limitStatusResponse.has(this.serialNumber)) {
	            JsonObject inverterData = limitStatusResponse.getAsJsonObject(this.serialNumber);
	            String limitSetStatus = JsonUtils.getAsString(inverterData, "limit_set_status");

	            // Update the LimitStatus channel
	            this.channel(Opendtu.ChannelId.LIMIT_STATUS).setNextValue(limitSetStatus);
	        } else {
	            this.logWarn(log, "Serial number [" + this.serialNumber + "] not found in limit status response.");
	        }
	    } catch (OpenemsNamedException e) {
	        this.logError(this.log, "Error updating limit status channel: " + e.getMessage());
	    }
	}
	

	@Override
	public Timedata getTimedata() {
		return this.timedata;
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