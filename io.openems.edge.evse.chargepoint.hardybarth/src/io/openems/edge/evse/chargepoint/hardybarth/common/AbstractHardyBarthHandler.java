package io.openems.edge.evse.chargepoint.hardybarth.common;

import static io.openems.common.bridge.http.api.HttpMethod.GET;
import static io.openems.common.bridge.http.api.HttpMethod.PUT;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.meter.api.PhaseRotation.setPhaseRotatedActivePowerChannels;
import static io.openems.edge.meter.api.PhaseRotation.setPhaseRotatedCurrentChannels;
import static io.openems.edge.meter.api.PhaseRotation.setPhaseRotatedVoltageChannels;
import static java.lang.Math.round;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.DefaultDelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProvider.Delay;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.BooleanConsumer;
import io.openems.common.types.HttpStatus;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.FunctionUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evse.chargepoint.hardybarth.common.HardyBarth.PathProvider;
import io.openems.edge.meter.api.PhaseRotation;

public abstract class AbstractHardyBarthHandler<T extends HardyBarth> {

	private static final float SCALE_FACTOR_MINUS_1 = 0.1F;

	// Default Heartbeat timeout is 30 seconds
	public static final int HEART_BEAT_TIME = 15;

	private final Logger log = LoggerFactory.getLogger(AbstractHardyBarthHandler.class);

	// TODO protected
	protected final T parent;
	protected final LogVerbosity logVerbosity;

	private final String ip;
	private final String apikey;
	private final BiConsumer<Logger, String> logInfoCallback;
	private final BridgeHttpFactory httpBridgeFactory;
	private final HttpBridgeCycleService cycleService;
	private final HttpBridgeTimeService timeService;
	private BridgeHttp httpBridge;

	protected AbstractHardyBarthHandler(T parent, String ip, String apikey, PhaseRotation phaseRotation,
			LogVerbosity logVerbosity, BiConsumer<Logger, String> logInfo, BridgeHttpFactory httpBridgeFactory,
			HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition, BooleanConsumer communicationFailed) {
		this.parent = parent;
		this.ip = ip;
		this.apikey = apikey;
		this.logVerbosity = logVerbosity;
		this.logInfoCallback = logInfo;
		this.httpBridgeFactory = httpBridgeFactory;
		this.httpBridge = httpBridgeFactory.get();

		this.cycleService = this.httpBridge.createService(httpBridgeCycleServiceDefinition);
		this.cycleService.subscribeCycle(1, //
				this.createEndpoint(GET, "/api", null), //
				t -> {
					this.handleGetApiCallResponse(t, phaseRotation);
					communicationFailed.accept(false);
				}, //
				t -> communicationFailed.accept(true));
		this.timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
		var delay = Delay.of(Duration.ofSeconds(HEART_BEAT_TIME));
		var heartBeat = parent.isReadOnly() ? "off" : "on";
		this.timeService.subscribeTime(new DefaultDelayTimeProvider(() -> Delay.immediate(), //
				t -> delay, error -> delay),
				this.createEndpoint(PUT, "/api/secc", buildJsonObject() //
						.addProperty("salia/heartbeat", heartBeat) //
						.build()),
				t -> {
					switch (this.logVerbosity) {
					case NONE, DEBUG_LOG -> doNothing();
					case WRITES, READS -> this.logInfo("Set heartbeat=" + heartBeat);
					}
				}, //
				t -> {
					doNothing();
				});
	}

	/**
	 * Called on deactivate.
	 */
	public void deactivate() {
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
	}

	/**
	 * Called on handleEvent.
	 * 
	 * @param event the {@link Event} with TOPIC_CYCLE_AFTER_PROCESS_IMAGE
	 */
	public void handleAfterProcessImageEvent(Event event) {
		final var hb = this.parent;
		if (hb.isReadOnly()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
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
		final var hb = this.parent;
		if (hb.isReadOnly()) {
			return;
		}
		var chargeMode = "manual";
		StringReadChannel channelChargeMode = hb.channel(HardyBarth.ChannelId.RAW_SALIA_CHARGE_MODE);
		Optional<String> valueOpt = channelChargeMode.value().asOptional();
		if (valueOpt.map(t -> t.equals(chargeMode)).orElse(false)) {
			return;
		}
		this.httpBridge //
				.requestJson(this.createEndpoint(PUT, "/api/secc", buildJsonObject() //
						.addProperty("salia/chargemode", chargeMode) //
						.build())) //
				.thenAccept(t -> {
					switch (this.logVerbosity) {
					case NONE, DEBUG_LOG -> FunctionUtils.doNothing();
					case WRITES, READS -> this.logInfo("Set chargemode=" + chargeMode);
					}
				});
	}

	/**
	 * Set current target to the charger.
	 * 
	 * @param current current target in A
	 * @return boolean if the target was set
	 * @throws OpenemsNamedException on error
	 */
	public boolean setTarget(int current) {
		switch (this.logVerbosity) {
		case NONE, DEBUG_LOG -> doNothing();
		case WRITES, READS -> this.logInfo("Set Target=" + current);
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
		var endpoint = BridgeHttp.create(//
				new StringBuilder("http://").append(this.ip).append(url).toString()) //
				.setBody(body == null //
						? null //
						: body.toString()) //
				.setMethod(httpMethod);
		if (this.apikey != null) {
			endpoint.setHeader("apikey", this.apikey);
		}
		return endpoint.build();
	}

	/**
	 * Handles a Response from a http call for the endpoint /api GET.
	 * 
	 * @param response      the {@link HttpResponse} to be handled
	 * @param phaseRotation the configured {@link PhaseRotation}
	 * @throws OpenemsNamedException when json can not be parsed
	 */
	public void handleGetApiCallResponse(HttpResponse<String> response, PhaseRotation phaseRotation)
			throws OpenemsNamedException {
		final var json = parseToJsonObject(response.data());
		switch (this.logVerbosity) {
		case NONE, DEBUG_LOG -> FunctionUtils.doNothing();
		case READS, WRITES -> this.logInfo("RESPONSE " + json);
		}
		this.parent.channels() //
				.stream() //
				.map(Channel::channelId) //
				.filter(PathProvider.class::isInstance) //
				.map(PathProvider.class::cast) //
				.forEach(c -> {
					final var channelId = (io.openems.edge.common.channel.ChannelId) c;
					final var path = c.getPath();
					final var value = getValueFromJson(channelId.doc().getType(), json, path.converter(),
							path.jsonPaths());
					setValue(this.parent, channelId, value);
				});
		this.updateChannels(json, phaseRotation);
	}

	/**
	 * Called by {@link OpenemsComponent#debugLog()}.
	 * 
	 * @return the debugLog string
	 */
	public String debugLog() {
		final var hb = this.parent;
		var b = new StringBuilder() //
				.append("L:").append(hb.getActivePower().asString());
		return b.toString();
	}

	// TODO Protected
	protected void logInfo(String message) {
		this.logInfoCallback.accept(this.log, message);
	}

	protected void updateChannels(JsonElement json, PhaseRotation phaseRotation) {
		final var hb = this.parent;

		// Energy
		hb._setActiveProductionEnergy(getValueFromJson(LONG, json, //
				value -> TypeUtils.<Long>getAsType(LONG, value), //
				"secc", "port0", "metering", "energy", "active_import", "actual"));

		// Current
		final var currentL1 = getAsInteger(json, 1, "secc", "port0", "metering", "current", "ac", "l1", "actual");
		final var currentL2 = getAsInteger(json, 1, "secc", "port0", "metering", "current", "ac", "l2", "actual");
		final var currentL3 = getAsInteger(json, 1, "secc", "port0", "metering", "current", "ac", "l3", "actual");

		// Checks if value are null and if they are checks if its the third error or
		// more
		// and otherwise returns, so that no new values are written this cycle
		// TODO: find a better long term solution
		if (currentL1 == null || currentL2 == null || currentL3 == null) {
			this.logInfo("Invalid current values detected");
			if (this.handleUndefinedError()) {
				return;
			}
		}

		// Power
		final var activePowerL1 = getAsInteger(json, SCALE_FACTOR_MINUS_1, //
				"secc", "port0", "metering", "power", "active", "ac", "l1", "actual");
		final var activePowerL2 = getAsInteger(json, SCALE_FACTOR_MINUS_1, //
				"secc", "port0", "metering", "power", "active", "ac", "l2", "actual");
		final var activePowerL3 = getAsInteger(json, SCALE_FACTOR_MINUS_1, //
				"secc", "port0", "metering", "power", "active", "ac", "l3", "actual");

		// Voltage
		final var voltageL1 = activePowerL1 == null ? null : round(activePowerL1 * 1_000_000F / currentL1);
		final var voltageL2 = activePowerL2 == null ? null : round(activePowerL2 * 1_000_000F / currentL2);
		final var voltageL3 = activePowerL3 == null ? null : round(activePowerL3 * 1_000_000F / currentL3);

		if (activePowerL1 == null || activePowerL2 == null || activePowerL3 == null) {
			this.logInfo("Active power values are null");
			if (this.handleUndefinedError()) {
				return;
			}
		}

		if (voltageL1 == null || voltageL2 == null || voltageL3 == null) {
			this.logInfo("Voltage values are null");
			if (this.handleUndefinedError()) {
				return;
			}
		}

		setPhaseRotatedVoltageChannels(hb, voltageL1, voltageL2, voltageL3);
		setPhaseRotatedCurrentChannels(hb, currentL1, currentL2, currentL3);
		setPhaseRotatedActivePowerChannels(hb, activePowerL1, activePowerL2, activePowerL3);

		// ACTIVE_POWER
		final var activePower = Optional.ofNullable(getAsInteger(json, SCALE_FACTOR_MINUS_1, //
				"secc", "port0", "metering", "power", "active_total", "actual")) //
				.map(p -> p < 100 ? 0 : p) // Ignore the consumption of the charger itself
				.orElse(null);

		if (activePower == null) {
			if (this.handleUndefinedError()) {
				return;
			}
		}

		hb._setActivePower(activePower);
		this.undefinedErrorCounter = 0;

		this.setChannels(hb, json, null /* TODO PhaseRotation */, currentL1, currentL2, currentL3, activePower);
	}

	protected abstract void setChannels(T hb, JsonElement json, PhaseRotation phaseRotation, Integer currentL1,
			Integer currentL2, Integer currentL3, Integer activePower);

	private int undefinedErrorCounter = 0;

	private boolean handleUndefinedError() {
		this.undefinedErrorCounter++;
		return this.undefinedErrorCounter <= 3;
	}

	/**
	 * Get the last JSON element and it's value, by running through the given
	 * jsonPath.
	 *
	 * @param openemsType the {@link OpenemsType}s
	 * @param json        Raw JsonElement.
	 * @param converter   Converter, to convert the raw JSON value into a proper
	 *                    Channel.
	 * @param jsonPaths   Whole JSON path, where the JsonElement for the given
	 *                    channel is located.
	 * @param <T>         return type
	 * @return Value of the last JsonElement by running through the specified JSON
	 *         path.
	 */
	protected static <T> T getValueFromJson(OpenemsType openemsType, JsonElement json, Function<Object, T> converter,
			String... jsonPaths) {

		var currentJsonElement = json;
		// Go through the whole jsonPath of the current channelId
		for (var i = 0; i < jsonPaths.length; i++) {
			var currentPathMember = jsonPaths[i];
			try {
				if (i == jsonPaths.length - 1) {
					// Last path element
					var value = getJsonElementValue(currentJsonElement, openemsType, jsonPaths[i]);

					// Return the converted value
					return converter.apply(value);
				}
				// Not last path element
				currentJsonElement = JsonUtils.getAsJsonObject(currentJsonElement, currentPathMember);
			} catch (OpenemsNamedException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Get Value of the given JsonElement in the required type.
	 *
	 * @param jsonElement Element as JSON.
	 * @param openemsType Required type.
	 * @param memberName  Member name of the JSON Element.
	 * @return Value in the required type.
	 * @throws OpenemsNamedException Failed to get the value.
	 */
	private static Object getJsonElementValue(JsonElement jsonElement, OpenemsType openemsType, String memberName)
			throws OpenemsNamedException {
		return switch (openemsType) {
		case BOOLEAN -> JsonUtils.getAsInt(jsonElement, memberName) == 1;
		case DOUBLE -> JsonUtils.getAsDouble(jsonElement, memberName);
		case FLOAT -> JsonUtils.getAsFloat(jsonElement, memberName);
		case INTEGER -> JsonUtils.getAsInt(jsonElement, memberName);
		case LONG -> JsonUtils.getAsLong(jsonElement, memberName);
		case SHORT -> JsonUtils.getAsShort(jsonElement, memberName);
		case STRING -> JsonUtils.getAsString(jsonElement, memberName);
		};
	}

	protected static Integer getAsInteger(JsonElement json, float scaleFactor, String... jsonPaths) {
		return getValueFromJson(INTEGER, json, //
				value -> value == null ? null //
						: round(TypeUtils.<Integer>getAsType(INTEGER, value) * scaleFactor), //
				jsonPaths);
	}
}
