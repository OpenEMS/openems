package io.openems.edge.evcs.hardybarth;

import static io.openems.edge.bridge.http.api.BridgeHttp.DEFAULT_CONNECT_TIMEOUT;
import static io.openems.edge.bridge.http.api.BridgeHttp.DEFAULT_READ_TIMEOUT;
import static io.openems.edge.bridge.http.api.HttpMethod.GET;
import static io.openems.edge.bridge.http.api.HttpMethod.PUT;
import static io.openems.edge.evcs.api.ChargingType.AC;
import static io.openems.edge.evcs.api.Phases.THREE_PHASE;
import static java.lang.Math.round;
import static java.util.Collections.emptyMap;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.HttpStatus;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.DeprecatedEvcs;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.HardyBarth", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class EvcsHardyBarthImpl extends AbstractManagedEvcsComponent
		implements OpenemsComponent, EventHandler, EvcsHardyBarth, Evcs, ManagedEvcs, DeprecatedEvcs, ElectricityMeter {

	private final Logger log = LoggerFactory.getLogger(EvcsHardyBarthImpl.class);

	protected final HardyBarthReadUtils readUtils = new HardyBarthReadUtils(this);

	/**
	 * Master EVCS is responsible for RFID authentication (Not implemented for now).
	 */
	protected boolean masterEvcs = true;

	private BridgeHttp httpBridge;
	private Config config;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	public EvcsHardyBarthImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				EvcsHardyBarth.ChannelId.values(), //
				DeprecatedEvcs.ChannelId.values() //
		);
		DeprecatedEvcs.copyToDeprecatedEvcsChannels(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		// TODO stop here if not enabled
		this.config = config;
		this._setChargingType(AC);
		this._setFixedMinimumHardwarePower(config.minHwCurrent() / 1000 * 3 * 230);
		this._setFixedMaximumHardwarePower(config.maxHwCurrent() / 1000 * 3 * 230);
		this._setPowerPrecision(230);
		this._setPhases(THREE_PHASE);

		this.httpBridge = this.httpBridgeFactory.get();
		// formerly .setHeartbeat
		// The internal heartbeat is currently too fast - it is not enough to write
		// every second by default. We have to disable it to run the evcs
		// properly.
		// TODO: The manufacturer must be asked if it is possible to read the heartbeat
		// status so that we can check if the heartbeat is really disabled and if the
		// heartbeat time can be increased to be able to use this feature.
		this.httpBridge.subscribeCycle(1, //
				this.createEndpoint(PUT, "/api/secc", "{\"salia/heartbeat\":\"off\"}"), //
				t -> this._setChargingstationCommunicationFailed(false),
				t -> this._setChargingstationCommunicationFailed(true));
		this.httpBridge.subscribeCycle(1, //
				this.createEndpoint(GET, "/api", null), //
				t -> {
					this.readUtils.handleGetApiCallResponse(t, config.phaseRotation());
					this._setChargingstationCommunicationFailed(false);
				}, //
				t -> this._setChargingstationCommunicationFailed(true));
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.MANAGED_CONSUMPTION_METERED;
	}

	private Endpoint getTargetEndpoint(int target) {
		return this.createEndpoint(PUT, "/api/secc", "{\"salia/pausecharging\":\"" + target + "\"}");
	}

	private Endpoint createEndpoint(HttpMethod httpMethod, String url, String body) {
		return createEndpoint(this.config.ip(), httpMethod, url, body);
	}

	protected static Endpoint createEndpoint(String ip, HttpMethod httpMethod, String url, String body) {
		return new Endpoint(//
				new StringBuilder("http://").append(ip).append(url).toString(), //
				httpMethod, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, //
				body, //
				emptyMap());
	}

	@Override
	protected WriteHandler createWriteHandler() {
		return new HardyBarthWriteHandler(this);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		super.handleEvent(event);
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
			-> this.setManualMode();
		}
	}

	/**
	 * Set manual mode.
	 * 
	 * <p>
	 * Sets the chargemode to manual if not set.
	 */
	private void setManualMode() {
		StringReadChannel channelChargeMode = this.channel(EvcsHardyBarth.ChannelId.RAW_SALIA_CHARGE_MODE);
		Optional<String> valueOpt = channelChargeMode.value().asOptional();
		if (valueOpt.map(t -> !t.equals("manual")).orElse(true)) {
			return;
		}
		this.debugLog("Setting HardyBarth to manual chargemode");
		this.httpBridge //
				.requestJson(this.createEndpoint(PUT, "/api/secc", "{\"salia/chargemode\":\"manual\"}")) //
				.thenAccept(t -> this.debugLog(t.toString()));

	}

	/**
	 * Enable external meter.
	 * 
	 * <p>
	 * Enables the external meter if not set.
	 */
	// TODO: Set the external meter to true because it's disabled per default.
	// Not usable for now, because we haven't an update process defined and
	// this REST Entry is only available with a beta firmware
	// (http://salia.echarge.de/firmware/firmware_1.37.8_beta.image) or the next
	// higher stable version. Be aware that the REST call and the update should not
	// be called every cycle
	/*
	 * private void enableExternalMeter() {
	 * 
	 * BooleanReadChannel channelChargeMode =
	 * this.parent.channel(HardyBarth.ChannelId.RAW_SALIA_CHANGE_METER);
	 * Optional<Boolean> valueOpt = channelChargeMode.value().asOptional(); if
	 * (valueOpt.isPresent()) { if (!valueOpt.get().equals(true)) { // Enable
	 * external meter try {
	 * this.parent.debugLog("Enable external meter of HardyBarth " +
	 * this.parent.id()); JsonElement result =
	 * this.parent.api.sendPutRequest("/api/secc", "salia/changemeter",
	 * "enable | /dev/ttymxc0 | klefr | 9600 | none | 1");
	 * this.parent.debugLog(result.toString());
	 * 
	 * if (result.toString().equals("{\"result\":\"ok\"}")) { // Reboot the charger
	 * this.parent.debugLog("Reboot of HardyBarth " + this.parent.id()); JsonElement
	 * resultReboot = this.parent.api.sendPutRequest("/api/secc",
	 * "salia/servicereboot", "1"); this.parent.debugLog(resultReboot.toString()); }
	 * } catch (OpenemsNamedException e) { e.printStackTrace(); } } } }
	 */

	/**
	 * Debug Log.
	 *
	 * <p>
	 * Logging only if the debug mode is enabled
	 *
	 * @param message text that should be logged
	 */
	public void debugLog(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsNamedException {
		// TODO: Use power precision to set valid power if it is used in UI part too
		// e.g. int precision = TypeUtils.getAsType(OpenemsType.INTEGER,
		// this.getPowerPrecision().orElse(230d));
		// power = IntUtils.roundToPrecision(power, Round.TOWARDS_ZERO, precision);

		// Convert it to ampere and apply hard limits
		int phases = this.getPhasesAsInt();
		final var current = round(power / (float) phases / 230.F);

		return this.setTarget(current);
	}

	@Override
	public boolean pauseChargeProcess() throws OpenemsNamedException {
		return this.setTarget(0);
	}

	/**
	 * Set current target to the charger.
	 * 
	 * @param current current target in A
	 * @return boolean if the target was set
	 * @throws OpenemsNamedException on error
	 */
	private boolean setTarget(int current) {
		CompletableFuture<HttpResponse<JsonElement>> resultPause = null;
		if (current > 0) {
			// Send stop pause request
			resultPause = this.httpBridge.requestJson(this.getTargetEndpoint(0));
		} else {
			resultPause = this.httpBridge.requestJson(this.getTargetEndpoint(1));
			this.debugLog("Setting HardyBarth " + this.alias() + " to pause");
		}

		// Send charge power limit
		final var resultLimit = this.httpBridge.requestJson(
				this.createEndpoint(PUT, "/api/secc", "{\"" + "grid_current_limit" + "\":\"" + current + "\"}"));
		try {
			return resultLimit.get().status().equals(HttpStatus.OK) && resultPause.get().status().equals(HttpStatus.OK);
		} catch (InterruptedException | ExecutionException e) {
			this.log.error("Unable to set EVCS Target");
			return false;
		}
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 30;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return round(this.config.minHwCurrent() / 1000f) * DEFAULT_VOLTAGE * THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return round(this.config.maxHwCurrent() / 1000f) * DEFAULT_VOLTAGE * THREE_PHASE.getValue();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}
}
