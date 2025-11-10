package io.openems.edge.battery.bmw.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Timeout;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.battery.bmw.statemachine.StateMachine.State;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService.CycleEndpoint;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private static final int TIMEOUT = 120; // [s]
	private static final String URI_STATE = "bcsPowerState";
	private static final String URI_RELEASE = "releaseStatus";
	private final Logger log = LoggerFactory.getLogger(GoRunningHandler.class);

	private boolean resultState = false;
	private boolean resultRelease = false;

	private CycleEndpoint cycleStateEndpoint;
	private CycleEndpoint cycleReleaseEndpoint;

	protected static record GoRunningState(GoRunningSubState subState) {
	}

	protected GoRunningState goRunningState;
	private final Timeout timeout = Timeout.ofSeconds(TIMEOUT);

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeout.start(context.clock);
		this.goRunningState = new GoRunningState(GoRunningSubState.CHECK_BCS_POWER_STATE);
		this.cycleStateEndpoint = this.subscribeHttpEndpoints(context, URI_STATE);
		this.cycleReleaseEndpoint = this.subscribeHttpEndpoints(context, URI_RELEASE);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();
		var nextSubState = this.getNextSubState(context);
		battery._setGoRunningStateMachine(nextSubState);

		if (nextSubState != this.goRunningState.subState) {
			// Record State changes
			this.goRunningState = new GoRunningState(nextSubState);
		} else if (this.timeout.elapsed(context.clock)) {
			// Handle GoRunningHandler State-timeout
			throw new OpenemsException("Timeout [" + TIMEOUT + "s] in GoRunning-" + this.goRunningState.subState);
		}

		if (nextSubState == GoRunningSubState.ERROR) {
			return State.ERROR;
		}

		if (nextSubState == GoRunningSubState.FINISHED) {
			return State.RUNNING;
		}

		return State.GO_RUNNING;
	}

	private GoRunningSubState getNextSubState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();
		return switch (this.goRunningState.subState) {
		case CHECK_BCS_POWER_STATE -> {
			yield this.resultState//
					? GoRunningSubState.CHECK_BCS_INVERTER_RELEASE //
					: GoRunningSubState.ACTIVATE_BCS_POWER_STATE;
		}
		case ACTIVATE_BCS_POWER_STATE -> {
			context.setComponent(context, URI_STATE);
			if (!this.resultState) {
				yield GoRunningSubState.ACTIVATE_BCS_POWER_STATE;
			}
			yield GoRunningSubState.CHECK_BCS_INVERTER_RELEASE;
		}
		case CHECK_BCS_INVERTER_RELEASE -> {
			yield this.resultRelease//
					? GoRunningSubState.START_BATTERY //
					: GoRunningSubState.ACTIVATE_INVERTER_RELEASE;
		}
		case ACTIVATE_INVERTER_RELEASE -> {
			context.setComponent(context, URI_RELEASE);
			if (!this.resultRelease) {
				yield GoRunningSubState.ACTIVATE_INVERTER_RELEASE;
			}
			yield GoRunningSubState.START_BATTERY;
		}
		case START_BATTERY -> {
			battery.startBattery();
			if (battery.isRunning()) {
				yield GoRunningSubState.FINISHED;
			}
			yield GoRunningSubState.START_BATTERY;
		}
		case ERROR, UNDEFINED -> GoRunningSubState.ERROR;
		case FINISHED -> GoRunningSubState.FINISHED;
		};
	}

	private CycleEndpoint subscribeHttpEndpoints(Context context, String uri) {
		final var endPoint = context.getEndpoint(uri, null);
		return context.cycleService.subscribeCycle(//
				1, //
				endPoint, //
				success -> this.getAndSetResult(success, endPoint), //
				error -> context.logWarn(this.log, "Failed to retrieve component value from URI [ " + endPoint.url()
						+ " ] : " + error.getMessage()));
	}

	/**
	 * Get the result and set the condition for power state and inverter release
	 * state.
	 * 
	 * @param success  the Success
	 * @param endPoint the Endpoint
	 * @throws OpenemsNamedException on error
	 */
	private void getAndSetResult(HttpResponse<String> success, Endpoint endPoint) throws OpenemsNamedException {
		var dataObject = JsonUtils.parse(success.data()).getAsJsonObject().get("data");
		var value = 0;
		if (dataObject != null && !dataObject.isJsonNull()) {
			value = dataObject.getAsInt();
		}
		if (this.containsUrl(endPoint.url(), URI_STATE)) {
			this.resultState = value == 1;
		}
		if (this.containsUrl(endPoint.url(), URI_RELEASE)) {
			this.resultRelease = value == 1;
		}
	}

	/**
	 * Check for url.
	 * 
	 * @param url the url string
	 * @param uri the uri to compare and check
	 * @return boolean if the String available
	 */
	public boolean containsUrl(String url, String uri) {
		if (url == null || url.isEmpty()) {
			return false;
		}
		return url.contains(uri);
	}

	@Override
	protected String debugLog() {
		return State.GO_RUNNING.asCamelCase() + "-" + EnumUtils.nameAsCamelCase(this.goRunningState.subState);
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		context.cycleService.removeCycleEndpoint(this.cycleStateEndpoint);
		context.cycleService.removeCycleEndpoint(this.cycleReleaseEndpoint);
	}
}
