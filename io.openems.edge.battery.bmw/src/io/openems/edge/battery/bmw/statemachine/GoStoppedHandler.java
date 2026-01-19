package io.openems.edge.battery.bmw.statemachine;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Timeout;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.battery.bmw.statemachine.StateMachine.State;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService.CycleEndpoint;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(GoStoppedHandler.class);
	private static final int TIMEOUT_SECONDS = 120;
	private static final String URI_STATE = "bcsPowerState";
	private static final String URI_RELEASE = "releaseStatus";

	private final Timeout timeout = Timeout.ofSeconds(TIMEOUT_SECONDS);

	private boolean resultState = false;
	private boolean resultRelease = false;

	private final Map<String, CycleEndpoint> activeEndpoints = new HashMap<>();

	protected static record GoStoppingState(GoStoppingSubState subState) {
	}

	protected GoStoppingState goStoppingState;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeout.start(context.clock);
		this.goStoppingState = new GoStoppingState(GoStoppingSubState.STOP_BATTERY);

		this.subscribeIfNotActive(context, URI_STATE, value -> this.resultState = value);
		this.subscribeIfNotActive(context, URI_RELEASE, value -> this.resultRelease = value);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();
		var nextSubState = this.getNextSubState(context);

		battery._setGoStoppingStateMachine(nextSubState);

		if (nextSubState != this.goStoppingState.subState) {
			this.goStoppingState = new GoStoppingState(nextSubState);
		} else if (this.timeout.elapsed(context.clock)) {
			throw new OpenemsException(
					"Timeout [" + TIMEOUT_SECONDS + "s] in GoStopping-" + this.goStoppingState.subState);
		}

		return switch (nextSubState) {
		case ERROR -> State.ERROR;
		case FINISHED -> State.STOPPED;
		default -> State.GO_STOPPED;
		};
	}

	private GoStoppingSubState getNextSubState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();

		return switch (this.goStoppingState.subState) {
		case STOP_BATTERY -> {
			battery.stopBattery();
			yield !battery.isRunning() //
					? GoStoppingSubState.DEACTIVATE_INVERTER_RELEASE //
					: GoStoppingSubState.STOP_BATTERY;
		}
		case DEACTIVATE_INVERTER_RELEASE -> {
			context.setComponent(context, URI_RELEASE, "0"); // Send "0" to deactivate
			yield this.resultRelease //
					? GoStoppingSubState.DEACTIVATE_INVERTER_RELEASE //
					: GoStoppingSubState.CHECK_INVERTER_RELEASE_OFF;
		}
		case CHECK_INVERTER_RELEASE_OFF -> {
			yield !this.resultRelease //
					? GoStoppingSubState.DEACTIVATE_BCS_POWER_STATE //
					: GoStoppingSubState.DEACTIVATE_INVERTER_RELEASE;
		}
		case DEACTIVATE_BCS_POWER_STATE -> {
			context.setComponent(context, URI_STATE, "0"); // Send "0" to deactivate
			yield this.resultState //
					? GoStoppingSubState.DEACTIVATE_BCS_POWER_STATE //
					: GoStoppingSubState.CHECK_BCS_POWER_STATE_OFF;
		}
		case CHECK_BCS_POWER_STATE_OFF -> {
			yield !this.resultState //
					? GoStoppingSubState.FINISHED //
					: GoStoppingSubState.DEACTIVATE_BCS_POWER_STATE;
		}
		case ERROR, UNDEFINED -> GoStoppingSubState.ERROR;
		case FINISHED -> GoStoppingSubState.FINISHED;
		};
	}

	private void subscribeIfNotActive(Context context, String uri, java.util.function.Consumer<Boolean> consumer) {
		if (this.activeEndpoints.containsKey(uri)) {
			return;
		}

		final var endpoint = context.getEndpoint(uri, null);

		var cycle = context.cycleService.subscribeCycle(//
				1, //
				endpoint, //
				success -> this.handleResponse(success, endpoint, consumer), //
				error -> context.logWarn(this.log,
						"Failed to retrieve component value from URI [" + endpoint.url() + "]: " + error.getMessage()));

		this.activeEndpoints.put(uri, cycle);
	}

	private void handleResponse(HttpResponse<String> success, Endpoint endpoint, Consumer<Boolean> consumer)
			throws OpenemsNamedException {
		try {
			var responseData = success.data();
			if (responseData == null || responseData.isEmpty()) {
				this.log.warn("Endpoint [{}] returned null or empty response", endpoint.url());
				consumer.accept(false);
				return;
			}

			var json = JsonUtils.parse(responseData).getAsJsonObject();
			if (json == null || !json.has("data") || json.get("data").isJsonNull()) {
				this.log.warn("Endpoint [{}] returned invalid JSON or missing 'data' field: {}", endpoint.url(),
						responseData);
				consumer.accept(false);
				return;
			}

			int value = json.get("data").getAsInt();
			consumer.accept(value == 1);

		} catch (Exception e) {
			this.log.warn("Failed to parse response from [{}]: {}", endpoint.url(), e.getMessage());
			consumer.accept(false);
		}
	}

	@Override
	protected String debugLog() {
		return State.GO_STOPPED.asCamelCase() + "-" + EnumUtils.nameAsCamelCase(this.goStoppingState.subState);
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		for (var entry : this.activeEndpoints.values()) {
			context.cycleService.removeCycleEndpoint(entry);
		}
		this.activeEndpoints.clear();
	}
}
