package io.openems.edge.evse.chargepoint.abl.simulator;

import java.time.Duration;

import com.google.gson.JsonObject;

import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;

/**
 * REST API for controlling the ABL Modbus simulator.
 *
 * <p>
 * Provides HTTP endpoints for manual testing and control of the simulator.
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>GET /rest/abl/simulator/status - Get current simulator status
 * <li>POST /rest/abl/simulator/ev/connect - Simulate EV connection
 * <li>POST /rest/abl/simulator/ev/disconnect - Simulate EV disconnection
 * <li>POST /rest/abl/simulator/state/{state} - Force state change
 * <li>POST /rest/abl/simulator/error/inject - Inject error
 * <li>POST /rest/abl/simulator/error/clear - Clear error
 * <li>POST /rest/abl/simulator/currents - Set phase currents
 * <li>POST /rest/abl/simulator/start - Start simulator
 * <li>POST /rest/abl/simulator/stop - Stop simulator
 * </ul>
 */
public class AblSimulatorRestApi {

	private AblModbusSimulator simulator;
	private final String defaultIp;
	private final int defaultPort;
	private final int defaultDeviceId;

	/**
	 * Create REST API for simulator control.
	 *
	 * @param defaultIp       default IP address
	 * @param defaultPort     default port
	 * @param defaultDeviceId default device ID
	 */
	public AblSimulatorRestApi(String defaultIp, int defaultPort, int defaultDeviceId) {
		this.defaultIp = defaultIp;
		this.defaultPort = defaultPort;
		this.defaultDeviceId = defaultDeviceId;
	}

	/**
	 * `GET /rest/abl/simulator/status`.
	 *
	 * @return JSON with simulator status
	 */
	public JsonObject getStatus() {
		JsonObject json = new JsonObject();

		try {
			if (this.simulator == null) {
				json.addProperty("running", false);
				json.addProperty("message", "Simulator not started");
			} else {
				json.addProperty("running", true);
				json.addProperty("state", this.simulator.getCurrentState().getName());
				json.addProperty("stateCode",
						String.format("0x%02X", this.simulator.getCurrentState().getValue()));
				json.addProperty("evConnected", this.simulator.isEvConnected());
				json.addProperty("currentSetpoint", this.simulator.getCurrentSetpoint());

				JsonObject currents = new JsonObject();
				currents.addProperty("l1", this.simulator.getPhaseCurrentL1());
				currents.addProperty("l2", this.simulator.getPhaseCurrentL2());
				currents.addProperty("l3", this.simulator.getPhaseCurrentL3());
				json.add("phaseCurrents", currents);

				// Calculate power (P = 230V * I * 3 phases)
				int totalCurrent = this.simulator.getPhaseCurrentL1() + this.simulator.getPhaseCurrentL2()
						+ this.simulator.getPhaseCurrentL3();
				int power = 230 * totalCurrent;
				json.addProperty("activePower", power);
			}

			json.addProperty("success", true);

		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}

		return json;
	}

	/**
	 * `POST /rest/abl/simulator/start`.
	 *
	 * @return JSON with result
	 */
	public JsonObject start() {
		return this.start(this.defaultIp, this.defaultPort, this.defaultDeviceId);
	}

	/**
	 * `POST /rest/abl/simulator/start`.
	 *
	 * @param ip       IP address to bind to
	 * @param port     port number
	 * @param deviceId device ID
	 * @return JSON with result
	 */
	public JsonObject start(String ip, int port, int deviceId) {
		JsonObject json = new JsonObject();

		try {
			if (this.simulator != null) {
				json.addProperty("success", false);
				json.addProperty("error", "Simulator already running");
				return json;
			}

			this.simulator = new AblModbusSimulator(ip, port, deviceId);
			this.simulator.start();

			json.addProperty("success", true);
			json.addProperty("message", "Simulator started on " + ip + ":" + port);
			json.addProperty("deviceId", deviceId);

		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}

		return json;
	}

	/**
	 * `POST /rest/abl/simulator/stop`.
	 *
	 * @return JSON with result
	 */
	public JsonObject stop() {
		JsonObject json = new JsonObject();

		try {
			if (this.simulator == null) {
				json.addProperty("success", false);
				json.addProperty("error", "Simulator not running");
				return json;
			}

			this.simulator.stop();
			this.simulator = null;

			json.addProperty("success", true);
			json.addProperty("message", "Simulator stopped");

		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}

		return json;
	}

	/**
	 * `POST /rest/abl/simulator/ev/connect`.
	 *
	 * @return JSON with result
	 */
	public JsonObject connectEv() {
		JsonObject json = new JsonObject();

		try {
			this.ensureRunning();
			this.simulator.connectEv();

			json.addProperty("success", true);
			json.addProperty("message", "EV connected");
			json.addProperty("newState", this.simulator.getCurrentState().getName());

		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}

		return json;
	}

	/**
	 * `POST /rest/abl/simulator/ev/disconnect`.
	 *
	 * @return JSON with result
	 */
	public JsonObject disconnectEv() {
		JsonObject json = new JsonObject();

		try {
			this.ensureRunning();
			this.simulator.disconnectEv();

			json.addProperty("success", true);
			json.addProperty("message", "EV disconnected");
			json.addProperty("newState", this.simulator.getCurrentState().getName());

		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}

		return json;
	}

	/**
	 * `POST /rest/abl/simulator/state/{state}`.
	 *
	 * @param stateCode state code in hex (e.g., "A1", "C2")
	 * @return JSON with result
	 */
	public JsonObject setState(String stateCode) {
		JsonObject json = new JsonObject();

		try {
			this.ensureRunning();

			int code = Integer.parseInt(stateCode, 16);
			ChargingState state = ChargingState.fromValue(code);

			if (state == ChargingState.UNDEFINED) {
				throw new IllegalArgumentException("Invalid state code: " + stateCode);
			}

			this.simulator.forceState(state);

			json.addProperty("success", true);
			json.addProperty("message", "State changed to " + state.getName());
			json.addProperty("newState", state.getName());

		} catch (NumberFormatException e) {
			json.addProperty("success", false);
			json.addProperty("error", "Invalid hex code: " + stateCode);
		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}

		return json;
	}

	/**
	 * `POST /rest/abl/simulator/error/inject`.
	 *
	 * @param errorCode        error code in hex (F1-F11)
	 * @param autoRecoverAfter auto-recovery duration in seconds (0 = no recovery)
	 * @return JSON with result
	 */
	public JsonObject injectError(String errorCode, int autoRecoverAfter) {
		JsonObject json = new JsonObject();

		try {
			this.ensureRunning();

			int code = Integer.parseInt(errorCode, 16);
			ChargingState errorState = ChargingState.fromValue(code);

			if (errorState.status != io.openems.edge.evse.chargepoint.abl.enums.Status.ERROR) {
				throw new IllegalArgumentException(errorCode + " is not an error state");
			}

			Duration recovery = autoRecoverAfter > 0 ? Duration.ofSeconds(autoRecoverAfter) : null;
			this.simulator.injectError(errorState, recovery);

			json.addProperty("success", true);
			json.addProperty("message", "Error injected: " + errorState.getName());
			json.addProperty("newState", errorState.getName());
			if (autoRecoverAfter > 0) {
				json.addProperty("autoRecoveryAfter", autoRecoverAfter + " seconds");
			}

		} catch (NumberFormatException e) {
			json.addProperty("success", false);
			json.addProperty("error", "Invalid hex code: " + errorCode);
		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}

		return json;
	}

	/**
	 * `POST /rest/abl/simulator/error/clear`.
	 *
	 * @return JSON with result
	 */
	public JsonObject clearError() {
		JsonObject json = new JsonObject();

		try {
			this.ensureRunning();
			this.simulator.clearError();

			json.addProperty("success", true);
			json.addProperty("message", "Error cleared");
			json.addProperty("newState", this.simulator.getCurrentState().getName());

		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}

		return json;
	}

	/**
	 * `POST /rest/abl/simulator/currents`.
	 *
	 * @param l1 phase 1 current in Ampere
	 * @param l2 phase 2 current in Ampere
	 * @param l3 phase 3 current in Ampere
	 * @return JSON with result
	 */
	public JsonObject setPhaseCurrents(int l1, int l2, int l3) {
		JsonObject json = new JsonObject();

		try {
			this.ensureRunning();

			if (l1 < 0 || l1 > 80 || l2 < 0 || l2 > 80 || l3 < 0 || l3 > 80) {
				throw new IllegalArgumentException("Phase currents must be between 0 and 80 A");
			}

			this.simulator.setPhaseCurrents(l1, l2, l3);

			json.addProperty("success", true);
			json.addProperty("message", String.format("Phase currents set to L1=%dA, L2=%dA, L3=%dA", l1, l2, l3));

		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}

		return json;
	}

	/**
	 * `GET /rest/abl/simulator/states`.
	 *
	 * @return JSON with available states
	 */
	public JsonObject getAvailableStates() {
		JsonObject json = new JsonObject();
		JsonObject states = new JsonObject();

		for (ChargingState state : ChargingState.values()) {
			if (state != ChargingState.UNDEFINED) {
				states.addProperty(String.format("0x%02X", state.getValue()), state.getName());
			}
		}

		json.add("states", states);
		json.addProperty("success", true);

		return json;
	}

	/**
	 * Ensure simulator is running.
	 *
	 * @throws IllegalStateException if simulator is not running
	 */
	private void ensureRunning() {
		if (this.simulator == null) {
			throw new IllegalStateException("Simulator is not running. Start it first.");
		}
	}

	/**
	 * Get the simulator instance (for testing).
	 *
	 * @return simulator instance
	 */
	public AblModbusSimulator getSimulator() {
		return this.simulator;
	}
}
