package io.openems.edge.io.opendtu.inverter;

import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static java.lang.Math.round;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)

@Component(//
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})

public class OpendtuImpl extends AbstractOpenemsComponent implements Opendtu, ElectricityMeter, OpenemsComponent,
		EventHandler, TimedataProvider, ManagedSymmetricPvInverter {

	private final Logger log = LoggerFactory.getLogger(OpendtuImpl.class);

	@Reference()
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.OPTIONAL //
	)

	private volatile Timedata timedata;

	private final CalculateEnergyFromPower calculateActualEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateActualEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1);
	private final CalculateEnergyFromPower calculateActualEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2);
	private final CalculateEnergyFromPower calculateActualEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3);

	private String baseUrl;
	private String encodedAuth;
	private Config config;

	private Boolean isInitialPowerLimitSet = false;
	private MeterType meterType = null;
	private SinglePhase phase = null;
	private boolean setLimitsAllInverters = true;

	private List<InverterData> validInverters = new ArrayList<>();
	private Map<String, InverterData> inverterDataMap = new ConcurrentHashMap<>();

	public OpendtuImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				Opendtu.ChannelId.values() //
		);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		String auth = config.username() + ":" + config.password();
		this.encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
		this.validInverters = InverterData.collectInverterData(config);
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();
		this.meterType = config.type();

		for (InverterData inverter : this.validInverters) {
			this.inverterDataMap.put(inverter.getSerialNumber(), inverter);
			String inverterStatusUrl = "/api/livedata/status?inv=" + inverter.getSerialNumber();
			if (this.isEnabled()) {
				this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + inverterStatusUrl, this::processHttpResult);
			}
		}

		String limitStatusApiUrl = "/api/limit/status";
		this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + limitStatusApiUrl, this::processLimitStatusUpdate);

		if (!this.isInitialPowerLimitSet) {
			if (config.absoluteLimit() != -1 || config.relativeLimit() != -1) {
				this.validInverters.forEach(inverter -> this.determineAndSetLimit(config, inverter));
			} else {
				this.logDebug(this.log, "Skipping power limit initialization: both limits are unset (-1).");
			}
			this.isInitialPowerLimitSet = true;
		}

	}

	private void determineAndSetLimit(Config config, InverterData inverter) {
		Integer limitValue;
		Integer limitType;

		Map<String, String> properties = Map.of(//
				"Authorization", "Basic " + this.encodedAuth, //
				"Content-Type", "application/x-www-form-urlencoded" //
		);

		if (config.absoluteLimit() == -1 && config.relativeLimit() == -1) {
			return;
		}
		if (config.absoluteLimit() != -1) {
			limitValue = config.absoluteLimit();
			limitType = 0; // Absolute limit type.
		} else {
			limitValue = (config.relativeLimit() != -1) ? config.relativeLimit() : 100;
			limitType = 1; // Relative limit type, either specified or default to 100%.
		}

		String payloadContent = String.format("{\"serial\":\"%s\", \"limit_type\":%d, \"limit_value\":%d}",
				inverter.getSerialNumber(), limitType, limitValue);

		String formattedPayload = "data=" + URLEncoder.encode(payloadContent, StandardCharsets.UTF_8);

		BridgeHttp.Endpoint endpoint = new BridgeHttp.Endpoint(this.baseUrl + "/api/limit/config", HttpMethod.POST,
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, BridgeHttp.DEFAULT_READ_TIMEOUT, formattedPayload, properties);

		this.httpBridge.request(endpoint)
				.thenAccept(response -> this.handlePowerLimitResponse(inverter, limitType, limitValue))
				.exceptionally(ex -> this.handlePowerLimitError(inverter, ex));
	}

	private void processLimitStatusUpdate(JsonElement responseJson, Throwable error) {
		this._setSlaveCommunicationFailed(responseJson == null);

		JsonObject inverterLimitInfo = null;
		String inverterSerialNumber = null;
		Integer limitRelative = null;
		Integer limitAbsolute = null;
		String limitAdjustmentStatus = null;
		InverterData inverterData = null;

		if (error != null) {
			this.logDebug(this.log, error.getMessage());
			return;
		}

		try {
			var response = getAsJsonObject(responseJson);

			for (Map.Entry<String, JsonElement> entry : response.entrySet()) {
				inverterSerialNumber = entry.getKey();
				inverterLimitInfo = getAsJsonObject(entry.getValue());

				limitRelative = round(getAsFloat(inverterLimitInfo, "limit_relative"));
				limitAbsolute = round(getAsFloat(inverterLimitInfo, "max_power"));
				limitAdjustmentStatus = getAsString(inverterLimitInfo, "limit_set_status");

				inverterData = this.inverterDataMap.get(inverterSerialNumber);
				if (inverterData != null) {
					inverterData.setLimitAbsolute(limitAbsolute);
					inverterData.setLimitRelative(limitRelative);
					inverterData.setLimitStatus(limitAdjustmentStatus);
					this.logDebug(this.log,
							"Limit Status: " + limitAdjustmentStatus + " for Inverter: " + inverterSerialNumber);
				} else {
					this.logError(this.log, "Inverter data not found for serial number: " + inverterSerialNumber);
				}
			}
			if (this.setLimitsAllInverters) {
				this.channel(Opendtu.ChannelId.ABSOLUTE_LIMIT).setNextValue(InverterData.getTotalLimitAbsolute());
			}
		} catch (OpenemsNamedException e) {
			this.logDebug(this.log, e.getMessage());
			this._setSlaveCommunicationFailed(true);
			this.setLimitsAllInverters = false;
		}
	}

	private void handlePowerLimitResponse(InverterData inverter, int limitType, int limitValue) {
		this.logDebug(this.log, "Power limit successfully set for inverter [" + inverter.getSerialNumber()
				+ "]. LimitType: " + limitType + ", LimitValue: " + limitValue);
		inverter.setLimitType(limitType);

		if (limitType == 0) { // Absolute limit type
			inverter.setLimitAbsolute(limitValue);
			inverter.setLimitRelative(0);
		} else { // Relative limit type
			inverter.setLimitRelative(limitValue);
			inverter.setLimitAbsolute(0);
		}
	}

	private Void handlePowerLimitError(InverterData inverter, Throwable ex) {
		this.logDebug(this.log,
				"Error setting power limit for inverter [" + inverter.getSerialNumber() + "] " + ex.getMessage());
		this.channel(Opendtu.ChannelId.POWER_LIMIT_FAULT).setNextValue(true);
		return null;
	}

	private void processHttpResult(JsonElement result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);

		Integer power = null;
		Integer reactivepower = null;
		Integer voltage = null;
		Integer current = null;
		Integer frequency = null;
		Integer totalPower = null;
		String serialNumber = null;
		Integer powerLimitPerPhaseAbsolute = null;
		Integer powerLimitPerPhaseRelative = null;
		Integer limitHardware = null;
		InverterData inverterData = null;

		if (error != null) {
			this.logDebug(this.log, error.getMessage());
		} else {
			try {
				var response = getAsJsonObject(result);

				var totalObject = getAsJsonObject(response, "total");
				var totalPowerObject = getAsJsonObject(totalObject, "Power");
				totalPower = round(getAsFloat(totalPowerObject, "v"));

				var invertersArray = getAsJsonArray(response, "inverters");
				var inverterResponse = getAsJsonObject(invertersArray.get(0));
				serialNumber = getAsString(inverterResponse, "serial");

				powerLimitPerPhaseAbsolute = round(getAsFloat(inverterResponse, "limit_absolute"));
				powerLimitPerPhaseRelative = round(getAsFloat(inverterResponse, "limit_relative"));

				limitHardware = round(powerLimitPerPhaseAbsolute * (100 / powerLimitPerPhaseRelative)); // Calculate

				var acData = getAsJsonObject(inverterResponse, "AC");
				var ac0Data = getAsJsonObject(acData, "0");

				var powerObj = getAsJsonObject(ac0Data, "Power");
				power = round(getAsFloat(powerObj, "v"));

				var reactivePowerObj = getAsJsonObject(ac0Data, "ReactivePower");
				reactivepower = round(getAsFloat(reactivePowerObj, "v"));

				var voltageObj = getAsJsonObject(ac0Data, "Voltage");
				voltage = round(getAsFloat(voltageObj, "v") * 1000);

				var currentObj = getAsJsonObject(ac0Data, "Current");
				current = round(getAsFloat(currentObj, "v") * 1000);

				var frequencyObj = getAsJsonObject(ac0Data, "Frequency");
				frequency = round(getAsInt(frequencyObj, "v") * 1000);

			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, e.getMessage());
				this._setSlaveCommunicationFailed(true);
			}
		}

		inverterData = this.inverterDataMap.get(serialNumber);
		inverterData.setPower(power);
		inverterData.setCurrent(current);
		inverterData.setVoltage(voltage);
		inverterData.setFrequency(frequency);
		inverterData.setLimitAbsolute(powerLimitPerPhaseAbsolute);
		inverterData.setLimitRelative(powerLimitPerPhaseRelative);
		inverterData.setLimitHardware(limitHardware);

		String phase = inverterData.getPhase();

		switch (phase) {
		case "L1":
			this._setActivePowerL1(power);
			this._setVoltageL1(voltage);
			this._setCurrentL1(current);
			this._setReactivePowerL1(reactivepower);
			break;
		case "L2":
			this._setActivePowerL2(power);
			this._setVoltageL2(voltage);
			this._setCurrentL2(current);
			this._setReactivePowerL2(reactivepower);
			break;
		case "L3":
			this._setActivePowerL3(power);
			this._setVoltageL3(voltage);
			this._setCurrentL3(current);
			this._setReactivePowerL3(reactivepower);
			break;
		}

		this._setFrequency(frequency);
		this._setActivePower(totalPower);
		this._setSlaveCommunicationFailed(false);
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
		this.calculateEnergy();
		if (!this.isEnabled()) {
			return;
		}
	}

	public void setActivePowerLimit(int powerLimit) throws OpenemsNamedException {
		boolean skipProcessing = false;
		for (InverterData inverterData : this.inverterDataMap.values()) {
			if ("Pending".equals(inverterData.getLimitStatus())) {
				this.logDebug(this.log,
						"At least one inverter is still in 'Pending' state. Skipping setting power limits.");
				skipProcessing = true;
				break;
			}
		}
		if (skipProcessing || (this.config.absoluteLimit() == -1 && this.config.relativeLimit() == -1)) {
			return;
		}

		long now = System.currentTimeMillis();

		this.inverterDataMap.forEach((serialNumber, inverterData) -> {
			if (this.shouldUpdateInverter(powerLimit, inverterData, now)) {
				this.updateInverterLimit(serialNumber, inverterData, inverterData.getLimitAbsoluteWanted(), now);
			}
		});
	}

	private boolean shouldUpdateInverter(int powerLimit, InverterData inverterData, long now) {
		final Long elapsedTimeSinceLastUpdate = now - inverterData.getLastUpdate();
		final Long requiredDelay = TimeUnit.SECONDS.toMillis(this.config.delay());
		this.calculateNewPowerLimit(powerLimit, inverterData);
		Integer newIndividualLimit = inverterData.getLimitAbsoluteWanted();

		if (newIndividualLimit == 0) {
			return false;
		}
		if (Math.abs(newIndividualLimit - inverterData.getLimitAbsolute()) < this.config.threshold()) {
			this.logDebug(this.log, "setActivePowerLimit -> Difference beyond threshold(" + this.config.threshold()
					+ "W) too low. [" + inverterData.getSerialNumber() + "] Wanted Power:" + newIndividualLimit);
			return false;
		}
		return elapsedTimeSinceLastUpdate >= requiredDelay;
	}

	private boolean calculateNewPowerLimit(int powerLimit, InverterData inverterData) {
		var totalPower = InverterData.getTotalPower();
		var totalLimitHardware = InverterData.getTotalLimitHardware();
		var powerToDistribute = powerLimit - totalPower;
		var maxLimit = inverterData.getLimitHardware();
		final Integer min_limit = 50;
		var newLimit = 0;

		// Calculation based on conditions
		if (powerLimit < totalLimitHardware && totalPower > 0) {
			var productionShare = (double) inverterData.getPower() / totalPower;
			var additionalLimit = (int) round(powerToDistribute * productionShare);
			newLimit = min(maxLimit, max(min_limit, inverterData.getPower() + additionalLimit));
		} else {
			newLimit = maxLimit;
		}

		// Decision based on calculated value
		if (newLimit != 0) {
			inverterData.setLimitAbsoluteWanted(newLimit);
			return true; // True when a new limit is set
		}
		return false; // False when no new limit is set or new limit is 0
	}

	private void updateInverterLimit(String serialNumber, InverterData inverterData, int newPowerLimit, long now) {
		this.logDebug(this.log,
				"setActivePowerLimit -> Trying to set limit for [" + serialNumber + "] :" + newPowerLimit);

		String payloadContent = String.format("{\"serial\":\"%s\", \"limit_type\":0, \"limit_value\":%d}", serialNumber,
				newPowerLimit);
		String formattedPayload = "data=" + URLEncoder.encode(payloadContent, StandardCharsets.UTF_8);

		Map<String, String> properties = Map.of("Authorization", "Basic " + this.encodedAuth, "Content-Type",
				"application/x-www-form-urlencoded");
		BridgeHttp.Endpoint endpoint = new BridgeHttp.Endpoint(this.baseUrl + "/api/limit/config", HttpMethod.POST,
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, BridgeHttp.DEFAULT_READ_TIMEOUT, formattedPayload, properties);

		this.httpBridge.request(endpoint).thenAccept(response -> {
			this.logDebug(this.log, "Limit " + newPowerLimit + " successfully set for inverter [" + serialNumber + "]");
			inverterData.setLastUpdate(now);
			this.setLimitsAllInverters = true;
		}).exceptionally(ex -> {
			this.log.error("Error setting limit for inverter [{}]: {}", serialNumber, ex.getMessage());
			this.channel(Opendtu.ChannelId.POWER_LIMIT_FAULT).setNextValue(true);
			this.setLimitsAllInverters = false;
			return null;
		});

	}

	private void calculateEnergy() {
		var actualPower = this.getActivePower().get();
		if (actualPower == null) {
			this.calculateActualEnergy.update(null);
		} else if (actualPower > 0) {
			this.calculateActualEnergy.update(actualPower);
		} else {
			this.calculateActualEnergy.update(0);
		}

		var actualPowerL1 = this.getActivePowerL1().get();
		if (actualPowerL1 == null) {
			this.calculateActualEnergyL1.update(null);
		} else if (actualPowerL1 > 0) {
			this.calculateActualEnergyL1.update(actualPowerL1);
		} else {
			this.calculateActualEnergyL1.update(0);
		}

		var actualPowerL2 = this.getActivePowerL2().get();
		if (actualPowerL2 == null) {
			this.calculateActualEnergyL2.update(null);
		} else if (actualPowerL2 > 0) {
			this.calculateActualEnergyL2.update(actualPowerL2);
		} else {
			this.calculateActualEnergyL2.update(0);
		}

		var actualPowerL3 = this.getActivePowerL3().get();
		if (actualPowerL3 == null) {
			this.calculateActualEnergyL3.update(null);
		} else if (actualPowerL3 > 0) {
			this.calculateActualEnergyL3.update(actualPowerL3);
		} else {
			this.calculateActualEnergyL3.update(0);
		}

	}

	/**
	 * Constructs and returns a ModbusSlaveTable containing the combined Modbus
	 * slave nature tables of multiple components.
	 * 
	 * @param accessMode The AccessMode specifying the type of access allowed
	 * @return A new ModbusSlaveTable instance that combines the Modbus nature
	 *         tables
	 */
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(Opendtu.class, accessMode, 100) //
						.build());
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

	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

}