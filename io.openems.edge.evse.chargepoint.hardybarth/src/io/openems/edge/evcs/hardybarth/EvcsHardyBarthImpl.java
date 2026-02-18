package io.openems.edge.evcs.hardybarth;

import static io.openems.common.bridge.http.api.BridgeHttp.DEFAULT_CONNECT_TIMEOUT;
import static io.openems.common.bridge.http.api.BridgeHttp.DEFAULT_READ_TIMEOUT;
import static io.openems.common.bridge.http.api.HttpMethod.GET;
import static io.openems.common.bridge.http.api.HttpMethod.PUT;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static io.openems.edge.evcs.api.ChargingType.AC;
import static io.openems.edge.evcs.api.Phases.THREE_PHASE;
import static java.lang.Math.round;
import static java.util.Collections.emptyMap;

import java.time.Duration;
import java.util.Optional;
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

import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.time.DefaultDelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProvider.Delay;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.HttpStatus;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.DeprecatedEvcs;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.HardyBarth", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_EXECUTE_WRITE, //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class EvcsHardyBarthImpl extends AbstractManagedEvcsComponent
		implements OpenemsComponent, EventHandler, EvcsHardyBarth, Evcs, ManagedEvcs, DeprecatedEvcs, ElectricityMeter {

	// Default Heartbeat timeout is 30 seconds
	private static final int HEART_BEAT_TIME = 15;

	private final Logger log = LoggerFactory.getLogger(EvcsHardyBarthImpl.class);

	private HardyBarthReadUtils readUtils;

	/**
	 * Master EVCS is responsible for RFID authentication (Not implemented for now).
	 */
	protected boolean masterEvcs = true;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;
	private BridgeHttp httpBridge;
	private HttpBridgeCycleService cycleService;
	private HttpBridgeTimeService timeService;

	private Config config;

	@Reference
	private EvcsPower evcsPower;

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
		this.config = config;
		this.readUtils = new HardyBarthReadUtils(this, config.logVerbosity(), this::logInfo);
		super.activate(context, config.id(), config.alias(), config.enabled());
		// TODO stop here if not enabled
		this._setChargingType(AC);
		this._setFixedMinimumHardwarePower(config.minHwCurrent() / 1000 * 3 * 230);
		this._setFixedMaximumHardwarePower(config.maxHwCurrent() / 1000 * 3 * 230);
		this._setPowerPrecision(230);
		this._setPhases(THREE_PHASE);

		this.httpBridge = this.httpBridgeFactory.get();
		this.cycleService = this.httpBridge.createService(this.httpBridgeCycleServiceDefinition);
		this.cycleService.subscribeCycle(1, //
				this.createEndpoint(GET, "/api", null), //
				t -> {
					this.readUtils.handleGetApiCallResponse(t, config.phaseRotation());
					this._setChargingstationCommunicationFailed(false);
				}, //
				t -> this._setChargingstationCommunicationFailed(true));
		this.timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
		var delay = Delay.of(Duration.ofSeconds(HEART_BEAT_TIME));
		var heartBeat = this.config.readOnly() ? "off" : "on";
		this.timeService.subscribeTime(new DefaultDelayTimeProvider(() -> Delay.immediate(), //
				t -> delay, error -> delay),
				this.createEndpoint(PUT, "/api/secc", buildJsonObject() //
						.addProperty("salia/heartbeat", heartBeat) //
						.build()),
				t -> {
					switch (this.config.logVerbosity()) {
					case NONE, DEBUG_LOG -> doNothing();
					case WRITES, READS -> this.logInfo(this.log, "Set heartbeat=" + heartBeat);
					}
				}, //
				t -> {
					doNothing();
				});
	}

	/**
	 * Set manual mode.
	 * 
	 * <p>
	 * Sets the chargemode to manual if not set.
	 */
	private void setManualMode() {
		if (this.isReadOnly()) {
			return;
		}
		var chargeMode = "manual";
		StringReadChannel channelChargeMode = this.channel(EvcsHardyBarth.ChannelId.RAW_SALIA_CHARGE_MODE);
		Optional<String> valueOpt = channelChargeMode.value().asOptional();
		if (valueOpt.map(t -> t.equals(chargeMode)).orElse(false)) {
			return;
		}
		this.httpBridge //
				.requestJson(this.createEndpoint(PUT, "/api/secc", buildJsonObject() //
						.addProperty("salia/chargemode", chargeMode) //
						.build())) //
				.thenAccept(t -> {
					switch (this.config.logVerbosity()) {

					case NONE, DEBUG_LOG -> doNothing();
					case WRITES, READS -> this.logInfo(this.log, "Set chargemode=" + chargeMode);
					}
				});
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	protected WriteHandler createWriteHandler() {
		return new HardyBarthWriteHandler(this);
	}

	@Override
	public String debugLog() {
		return this.readUtils.debugLog();
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public boolean applyChargePowerLimit(int power) {
		// Convert it to ampere and apply hard limits
		int phases = this.getPhasesAsInt();
		final var current = round(power / (float) phases / 230.F);

		return this.setTarget(current);
	}

	@Override
	public boolean pauseChargeProcess() {
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
		switch (this.config.logVerbosity()) {
		case NONE, DEBUG_LOG -> doNothing();
		case WRITES, READS -> this.logInfo(this.log, "Set Target=" + current);
		}

		final var ep = this.createEndpoint(PUT, "/api/secc", buildJsonObject() //
				.addProperty("grid_current_limit", current) //
				.build());

		final var result = this.httpBridge.requestJson(ep);
		try {
			return result.get().status().equals(HttpStatus.OK);

		} catch (InterruptedException | ExecutionException e) {
			this.log.error("Unable to set EVCS Target");
			return false;
		}
	}

	private Endpoint createEndpoint(HttpMethod httpMethod, String url, JsonObject body) {
		return new Endpoint(//
				new StringBuilder("http://").append(this.config.ip()).append(url).toString(), //
				httpMethod, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, //
				body == null ? null : body.toString(), //
				emptyMap());
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

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return switch (this.config.logVerbosity()) {
		case NONE -> false;
		case DEBUG_LOG, READS, WRITES -> true;
		};
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
		-> {
			this.setManualMode();
		}
		}
	}

	@Override
	public int getWriteInterval() {
		return HEART_BEAT_TIME;
	}
}
