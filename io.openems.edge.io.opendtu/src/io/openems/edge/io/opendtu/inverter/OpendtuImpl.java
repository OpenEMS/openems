package io.openems.edge.io.opendtu.inverter;

import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static java.lang.Math.round;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

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
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
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
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class OpendtuImpl extends AbstractOpenemsComponent
		implements Opendtu, SinglePhaseMeter, ElectricityMeter, OpenemsComponent, EventHandler, TimedataProvider {

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.OPTIONAL //
	)

	private volatile BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;
	private String baseUrl;

	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateActualEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	private final Logger log = LoggerFactory.getLogger(OpendtuImpl.class);

	private MeterType meterType = null;
	private SinglePhase phase = null;
	private String serialNumber;
	private Boolean isInitialPowerLimitSet = false;

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

		this.meterType = config.type();
		this.phase = config.phase();
		this.serialNumber = config.serialNumber();
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();

		String auth = config.username() + ":" + config.password();
		String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

		String inverterStatus = "/api/livedata/status?inv=" + config.serialNumber();
		if (this.isEnabled()) {
			this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + inverterStatus, this::processHttpResult);
		}

		if (!this.isInitialPowerLimitSet) {

			Map<String, String> properties = Map.of("Authorization", "Basic " + encodedAuth, "Content-Type",
					"application/x-www-form-urlencoded");
			String payloadContent = String.format("{\"serial\":\"%s\", \"limit_type\":1, \"limit_value\":%d}",
					this.serialNumber, config.initialPowerLimit());
			String formattedPayload = "data=" + URLEncoder.encode(payloadContent, StandardCharsets.UTF_8);

			BridgeHttp.Endpoint endpoint = new BridgeHttp.Endpoint(this.baseUrl + "/api/limit/config", HttpMethod.POST,
					BridgeHttp.DEFAULT_CONNECT_TIMEOUT, BridgeHttp.DEFAULT_READ_TIMEOUT, formattedPayload, properties);

			this.httpBridge.request(endpoint).thenAccept(response -> {
				this.channel(Opendtu.ChannelId.POWER_LIMIT).setNextValue(config.initialPowerLimit());
				this.channel(Opendtu.ChannelId.POWER_LIMIT_FAULT).setNextValue(false);
			}).exceptionally(ex -> {
				this.channel(Opendtu.ChannelId.POWER_LIMIT_FAULT).setNextValue(true);
				return null;
			});

			this.isInitialPowerLimitSet = true;
		}
	}

	private void processHttpResult(JsonElement result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);

		Integer power = null;
		Integer reactivepower = null;
		Integer voltage = null;
		Integer current = null;
		Integer frequency = null;

		if (error != null) {
			this.logDebug(this.log, error.getMessage());
		} else {
			try {
				var response = getAsJsonObject(result);
				var invertersArray = getAsJsonArray(response, "inverters");
				var firstInverter = invertersArray.get(0).getAsJsonObject();
				var acData = getAsJsonObject(firstInverter, "AC");
				var ac0Data = getAsJsonObject(acData, "0");

				var powerObj = getAsJsonObject(ac0Data, "Power");
				power = round(getAsFloat(powerObj, "v"));

				var reactivePowerObj = getAsJsonObject(ac0Data, "ReactivePower");
				reactivepower = round(getAsFloat(reactivePowerObj, "v") * 1000);

				var voltageObj = getAsJsonObject(ac0Data, "Voltage");
				voltage = round(getAsFloat(voltageObj, "v") * 1000);

				var currentObj = getAsJsonObject(ac0Data, "Current");
				current = round(getAsFloat(currentObj, "v") * 1000);

				var frequencyObj = getAsJsonObject(ac0Data, "Frequency");
				frequency = getAsInt(frequencyObj, "v") * 1000;

			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, e.getMessage());
			}
		}

		switch (this.phase) {
		case L1:
			this._setVoltageL1(voltage);
			this._setCurrentL1(current);
			break;
		case L2:
			this._setVoltageL2(voltage);
			this._setCurrentL2(current);
			break;
		case L3:
			this._setVoltageL3(voltage);
			this._setCurrentL3(current);
			break;
		}

		this._setReactivePower(reactivepower);
		this._setCurrent(current);
		this._setVoltage(voltage);
		this._setFrequency(frequency);
		this._setActivePower(power);

	}

	@Deactivate
	protected void deactivate() {
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
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
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.updateLimitStatusChannel();
			this.calculateEnergy();
			break;
		}
	}

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

	private void updateLimitStatusChannel() {
		String url = this.baseUrl + "/api/limit/status";
		this.httpBridge.getJson(url).thenAccept(responseJson -> {
			try {
				var response = getAsJsonObject(responseJson);
				var inverterData = response.getAsJsonObject(this.serialNumber);

				String limitSetStatus = inverterData.get("limit_set_status").getAsString();
				this.channel(Opendtu.ChannelId.LIMIT_STATUS).setNextValue(limitSetStatus);

			} catch (OpenemsNamedException e) {
				this.logError(this.log, "Error processing limit status response: " + e.getMessage());
			}
		}).exceptionally(ex -> {
			this.logError(this.log, "Error updating limit status channel: " + ex.getMessage());
			return null;
		});
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