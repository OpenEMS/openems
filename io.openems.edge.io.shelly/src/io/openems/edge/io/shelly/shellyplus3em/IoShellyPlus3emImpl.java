package io.openems.edge.io.shelly.shellyplus3em;

import java.util.Objects;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(
        name = "IO.Shelly.Plus3EM",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@EventTopics({
        EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE,
        EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE
})
public class IoShellyPlus3emImpl extends AbstractOpenemsComponent implements IoShellyPlus3em, DigitalOutput,
        SinglePhaseMeter, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	
    private final Logger log = LoggerFactory.getLogger(IoShellyPlus3emImpl.class);
    private final BooleanWriteChannel[] digitalOutputChannels;

    private MeterType meterType = null;
    private SinglePhase phase = null;
    private String baseUrl;

	private volatile Timedata timedata;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private BridgeHttpFactory httpBridgeFactory;
    private BridgeHttp httpBridge;

    public IoShellyPlus3emImpl() {
        super(
                OpenemsComponent.ChannelId.values(),
                ElectricityMeter.ChannelId.values(),
                DigitalOutput.ChannelId.values(),
                IoShellyPlus3em.ChannelId.values()
        );
        this.digitalOutputChannels = new BooleanWriteChannel[]{
                this.channel(IoShellyPlus3em.ChannelId.RELAY)
        };

        ElectricityMeter.calculateSumActivePowerFromPhases(this);
    }

    @Activate
    protected void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.meterType = config.type();
        this.baseUrl = "http://" + config.ip();
        this.httpBridge = this.httpBridgeFactory.get();

        if (this.isEnabled()) {
            this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/status", this::processHttpResult);
        }
    }

    @Deactivate
    protected void deactivate() {
        if (this.httpBridge != null) {
            this.httpBridgeFactory.unget(this.httpBridge);
            this.httpBridge = null;
        }
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
        b.append(valueOpt.isPresent() ? (valueOpt.get() ? "ON" : "OFF") : "Unknown");
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
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
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
			JsonObject jsonResponse = JsonUtils.getAsJsonObject(result);

			JsonArray relays = JsonUtils.getAsJsonArray(jsonResponse, "relays");
			if (relays != null && !relays.isEmpty()) {
				JsonObject relay = JsonUtils.getAsJsonObject(relays.get(0));
				boolean isOn = JsonUtils.getAsBoolean(relay, "ison");
				this.getRelayChannel().setNextWriteValue(isOn);
				this._setRelay(isOn);
			}

			JsonObject updateObject = JsonUtils.getAsJsonObject(jsonResponse, "update");
			if (updateObject != null) {
				boolean hasUpdate = JsonUtils.getAsBoolean(updateObject, "has_update");
				this.channel(IoShellyPlus3em.ChannelId.HAS_UPDATE).setNextValue(hasUpdate);
			}

			JsonArray emeters = JsonUtils.getAsJsonArray(jsonResponse, "emeters");
			for (int i = 0; i < emeters.size(); i++) {
				JsonObject emeter = JsonUtils.getAsJsonObject(emeters.get(i));
				float power = JsonUtils.getAsFloat(emeter, "power");
				float voltage = JsonUtils.getAsFloat(emeter, "voltage");
				float current = JsonUtils.getAsFloat(emeter, "current");

				this.setValuesForPhase(i + 1, voltage, current, power);
			}

		} catch (OpenemsNamedException e) {
			this._setRelay(null);
			this._setActivePower(null);
			this._setActiveProductionEnergy(null);
			this.logDebug(this.log, e.getMessage());
		}
	}

	private void setValuesForPhase(int phase, Float voltage, Float current, Float power) {
		int scaledVoltage = Math.round(voltage * 1000);
		int scaledCurrent = Math.round(current * 1000);
		int scaledPower = Math.round(power);

		switch (phase) {
		case 1:
			this._setActivePowerL1(scaledPower);
			this._setVoltageL1(scaledVoltage);
			this._setCurrentL1(scaledCurrent);
			break;
		case 2:
			this._setActivePowerL2(scaledPower);
			this._setVoltageL2(scaledVoltage);
			this._setCurrentL2(scaledCurrent);
			break;
		case 3:
			this._setActivePowerL3(scaledPower);
			this._setVoltageL3(scaledVoltage);
			this._setCurrentL3(scaledCurrent);
			break;
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
            return;
        }
        if (Objects.equals(readValue, writeValue.get())) {
            return;
        }
        final String url = this.baseUrl + "/relay/" + index + "?turn=" + (writeValue.get() ? "on" : "off");
        this.httpBridge.get(url).whenComplete((t, e) -> {
            this._setSlaveCommunicationFailed(e != null);
            if (e == null) {
                this.logInfo(this.log, "Executed write successfully for URL: " + url);
            } else {
                this.logError(this.log, "Failed to execute write for URL: " + url + "; Error: " + e.getMessage());
            }
        });
    }

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower >= 0) {
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(-activePower);
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
