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

public class GoRunningHandler extends StateHandler<State, Context> {

	private static final int TIMEOUT_SECONDS = 120; // [s]
	private static final String URI_STATE = "bcsPowerState";
	private static final String URI_RELEASE = "releaseStatus";

	private final Logger log = LoggerFactory.getLogger(GoRunningHandler.class);
	private final Timeout timeout = Timeout.ofSeconds(TIMEOUT_SECONDS);
	private final Map<String, CycleEndpoint> activeEndpoints = new HashMap<>();

	protected GoRunningState goRunningState;

	private boolean resultState = false;
	private boolean resultRelease = false;

	protected static record GoRunningState(GoRunningSubState subState) {
	}

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeout.start(context.clock);
		this.goRunningState = new GoRunningState(GoRunningSubState.CHECK_BCS_POWER_STATE);

		this.subscribeIfNotActive(context, URI_STATE, value -> this.resultState = value);
		this.subscribeIfNotActive(context, URI_RELEASE, value -> this.resultRelease = value);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();
		final var nextSubState = this.getNextSubState(context);

		battery._setGoRunningStateMachine(nextSubState);

		if (nextSubState != this.goRunningState.subState) {
			this.goRunningState = new GoRunningState(nextSubState);
		} else if (this.timeout.elapsed(context.clock)) {
			throw new OpenemsException(
					"Timeout [" + TIMEOUT_SECONDS + "s] in GoRunning-" + this.goRunningState.subState);
		}

		return switch (nextSubState) {
		case ERROR -> State.ERROR;
		case FINISHED -> State.RUNNING;
		default -> State.GO_RUNNING;
		};
	}

	private GoRunningSubState getNextSubState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();

		return switch (this.goRunningState.subState) {
		case CHECK_BCS_POWER_STATE -> this.resultState//
				? GoRunningSubState.CHECK_BCS_INVERTER_RELEASE //
				: GoRunningSubState.ACTIVATE_BCS_POWER_STATE;

		case ACTIVATE_BCS_POWER_STATE -> {
			context.setComponent(context, URI_STATE, "1");
			yield this.resultState //
					? GoRunningSubState.CHECK_BCS_INVERTER_RELEASE //
					: GoRunningSubState.ACTIVATE_BCS_POWER_STATE;
		}

		case CHECK_BCS_INVERTER_RELEASE -> this.resultRelease//
				? GoRunningSubState.START_BATTERY //
				: GoRunningSubState.ACTIVATE_INVERTER_RELEASE;

		case ACTIVATE_INVERTER_RELEASE -> {
			context.setComponent(context, URI_RELEASE, "1");
			yield this.resultRelease //
					? GoRunningSubState.START_BATTERY //
					: GoRunningSubState.ACTIVATE_INVERTER_RELEASE;
		}

		case START_BATTERY -> {
			battery.startBattery();
			yield battery.isRunning() //
					? GoRunningSubState.FINISHED //
					: GoRunningSubState.START_BATTERY;
		}

		case ERROR, UNDEFINED -> GoRunningSubState.ERROR;
		case FINISHED -> GoRunningSubState.FINISHED;
		};
	}

	private void subscribeIfNotActive(Context context, String uri, Consumer<Boolean> consumer) {
		if (this.activeEndpoints.containsKey(uri)) {
			return;
		}

		final var endpoint = context.getEndpoint(uri, null);

		var cycle = context.cycleService.subscribeCycle(//
				1, //
				endpoint, //
				success -> this.handleResponse(success, endpoint, consumer), //
				error -> context.logWarn(this.log, //
						"Failed to retrieve component value from URI [" + endpoint.url() + "]: " + error.getMessage()));

		this.activeEndpoints.put(uri, cycle);
	}

	private void handleResponse(HttpResponse<String> success, Endpoint endpoint, Consumer<Boolean> consumer)
			throws OpenemsNamedException {

		try {
			var responseData = success.data();
			if (responseData == null || responseData.isEmpty()) {
				this.log.warn("Enpoint [{}] returned null or empty response", endpoint.url());
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
		return State.GO_RUNNING.asCamelCase() + "-" + EnumUtils.nameAsCamelCase(this.goRunningState.subState);
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		for (var entry : this.activeEndpoints.values()) {
			context.cycleService.removeCycleEndpoint(entry);
		}
		this.activeEndpoints.clear();
	}
}
