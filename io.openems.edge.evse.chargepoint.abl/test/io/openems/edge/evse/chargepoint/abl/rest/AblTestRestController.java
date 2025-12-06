package io.openems.edge.evse.chargepoint.abl.rest;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.abl.DummyAblChargePoint;
import io.openems.edge.evse.chargepoint.abl.EvseChargePointAbl;
import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;

/**
 * REST Controller for manual testing of ABL Charge Point (dummy component).
 *
 * <p>
 * Provides HTTP endpoints to control and monitor a DummyAblChargePoint instance
 * for manual testing via browser or REST client.
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>GET /rest/abl/test/status - Get current status
 * <li>POST /rest/abl/test/ev/connect - Simulate EV connection
 * <li>POST /rest/abl/test/ev/disconnect - Simulate EV disconnection
 * <li>POST /rest/abl/test/current/{milliampere} - Set charging current
 * <li>POST /rest/abl/test/state/{state} - Force state change
 * <li>POST /rest/abl/test/error/{errorCode} - Inject error
 * <li>POST /rest/abl/test/currents - Set phase currents manually
 * <li>POST /rest/abl/test/reset - Reset to initial state
 * </ul>
 */
@Component(//
		name = "Test.Rest.Abl.ChargePoint", //
		immediate = true //
)
public class AblTestRestController {

	private DummyAblChargePoint dummyChargePoint;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile ComponentManager componentManager;

	/**
	 * create and register the dummy charge point for testing.
	 */
	@Activate
	public void activate() {
		// Create a dummy charge point for testing
		this.dummyChargePoint = new DummyAblChargePoint("ablTest0");
	}

	/**
	 * `GET /rest/abl/test/status`.
	 *
	 * <p>
	 * Returns current status of the dummy charge point.
	 *
	 * @return JSON with status information
	 */
	public JsonObject getStatus() {
		JsonObject json = new JsonObject();

		try {
			json.addProperty("state", this.dummyChargePoint.getCurrentState().getName());
			json.addProperty("stateCode", String.format("0x%02X", this.dummyChargePoint.getCurrentState().getValue()));
			json.addProperty("evConnected", this.dummyChargePoint.isEvConnected());
			json.addProperty("currentSetpoint", this.dummyChargePoint.getCurrentSetpointMa());

			// Get phase currents from channels
			Integer l1 = (Integer) this.dummyChargePoint.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L1).value()
					.orElse(null);
			l1 = (l1 != null) ? l1 : 0;
			Integer l2 = (Integer) this.dummyChargePoint.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L2).value()
					.orElse(null);
			l2 = (l2 != null) ? l2 : 0;
			Integer l3 = (Integer) this.dummyChargePoint.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L3).value()
					.orElse(null);
			l3 = (l3 != null) ? l3 : 0;

			JsonObject currents = new JsonObject();
			currents.addProperty("l1", l1);
			currents.addProperty("l2", l2);
			currents.addProperty("l3", l3);
			json.add("phaseCurrents", currents);

			// Get power
			Integer power = (Integer) this.dummyChargePoint.channel(io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER)
					.value().orElse(null);
			power = (power != null) ? power : 0;
			json.addProperty("activePower", power);

			// Get ready for charging
			Boolean ready = (Boolean) this.dummyChargePoint
					.channel(io.openems.edge.evse.api.chargepoint.EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING)
					.value().orElse(null);
			ready = (ready != null) ? ready : false;
			json.addProperty("isReadyForCharging", ready);

			json.addProperty("success", true);

		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}

		return json;
	}

	/**
	 * `POST /rest/abl/test/ev/connect`.
	 *
	 * <p>
	 * Simulates EV connection (plug-in).
	 *
	 * @return JSON with result
	 */
	public JsonObject connectEv() {
		JsonObject json = new JsonObject();
		try {
			this.dummyChargePoint.connectEv();
			json.addProperty("success", true);
			json.addProperty("message", "EV connected");
			json.addProperty("newState", this.dummyChargePoint.getCurrentState().getName());
		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}
		return json;
	}

	/**
	 * `POST /rest/abl/test/ev/disconnect`.
	 *
	 * <p>
	 * Simulates EV disconnection (unplug).
	 *
	 * @return JSON with result
	 */
	public JsonObject disconnectEv() {
		JsonObject json = new JsonObject();
		try {
			this.dummyChargePoint.disconnectEv();
			json.addProperty("success", true);
			json.addProperty("message", "EV disconnected");
			json.addProperty("newState", this.dummyChargePoint.getCurrentState().getName());
		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}
		return json;
	}

	/**
	 * `POST /rest/abl/test/current/{milliampere}`.
	 *
	 * <p>
	 * Sets the charging current setpoint.
	 *
	 * @param milliampere desired current in mA (0-32000)
	 * @return JSON with result
	 */
	public JsonObject setCurrent(int milliampere) {
		JsonObject json = new JsonObject();
		try {
			if (milliampere < 0 || milliampere > 32000) {
				throw new OpenemsException("Current must be between 0 and 32000 mA");
			}

			ChargePointAbilities abilities = this.dummyChargePoint.getChargePointAbilities();
			if (abilities == null) {
				throw new OpenemsException("Charge point is in read-only mode");
			}

			ChargePointActions actions = ChargePointActions.from(abilities) //
					.setApplySetPoint(new ApplySetPoint.Action.MilliAmpere(milliampere)) //
					.build();

			this.dummyChargePoint.apply(actions);

			json.addProperty("success", true);
			json.addProperty("message", "Current set to " + milliampere + " mA");
			json.addProperty("newState", this.dummyChargePoint.getCurrentState().getName());
		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}
		return json;
	}

	/**
	 * `POST /rest/abl/test/state/{state}`.
	 *
	 * <p>
	 * Forces a state change (for testing).
	 *
	 * @param stateCode state code in hex (e.g., "A1", "C2", "F9")
	 * @return JSON with result
	 */
	public JsonObject setState(String stateCode) {
		JsonObject json = new JsonObject();
		try {
			int code = Integer.parseInt(stateCode, 16);
			ChargingState state = ChargingState.fromValue(code);

			if (state == ChargingState.UNDEFINED) {
				throw new OpenemsException("Invalid state code: " + stateCode);
			}

			this.dummyChargePoint.setState(state);

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
	 * `POST /rest/abl/test/error/{errorCode}`.
	 *
	 * <p>
	 * Injects an error state.
	 *
	 * @param errorCode error code (F1-F11, in hex without 0x prefix)
	 * @return JSON with result
	 */
	public JsonObject injectError(String errorCode) {
		JsonObject json = new JsonObject();
		try {
			int code = Integer.parseInt(errorCode, 16);
			ChargingState errorState = ChargingState.fromValue(code);

			if (errorState.status != io.openems.edge.evse.chargepoint.abl.enums.Status.ERROR) {
				throw new OpenemsException(errorCode + " is not an error state");
			}

			this.dummyChargePoint.injectError(errorState);

			json.addProperty("success", true);
			json.addProperty("message", "Error injected: " + errorState.getName());
			json.addProperty("newState", errorState.getName());
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
	 * `POST /rest/abl/test/currents`.
	 *
	 * <p>
	 * Sets phase currents manually.
	 *
	 * @param l1 phase 1 current in Ampere
	 * @param l2 phase 2 current in Ampere
	 * @param l3 phase 3 current in Ampere
	 * @return JSON with result
	 */
	public JsonObject setPhaseCurrents(int l1, int l2, int l3) {
		JsonObject json = new JsonObject();
		try {
			if (l1 < 0 || l1 > 80 || l2 < 0 || l2 > 80 || l3 < 0 || l3 > 80) {
				throw new OpenemsException("Phase currents must be between 0 and 80 A");
			}

			this.dummyChargePoint.setPhaseCurrents(l1, l2, l3);

			json.addProperty("success", true);
			json.addProperty("message", String.format("Phase currents set to L1=%dA, L2=%dA, L3=%dA", l1, l2, l3));
		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}
		return json;
	}

	/**
	 * `POST /rest/abl/test/reset`.
	 *
	 * <p>
	 * Resets the dummy charge point to initial state.
	 *
	 * @return JSON with result
	 */
	public JsonObject reset() {
		JsonObject json = new JsonObject();
		try {
			this.dummyChargePoint.disconnectEv();
			this.dummyChargePoint.setState(ChargingState.A1);
			this.dummyChargePoint.setPhaseCurrents(0, 0, 0);

			json.addProperty("success", true);
			json.addProperty("message", "Dummy charge point reset to initial state");
		} catch (Exception e) {
			json.addProperty("success", false);
			json.addProperty("error", e.getMessage());
		}
		return json;
	}

	/**
	 * Get available states for UI dropdown.
	 *
	 * @return JSON with all available states
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
	 * Get the dummy charge point instance (for testing).
	 *
	 * @return the dummy charge point
	 */
	public DummyAblChargePoint getDummyChargePoint() {
		return this.dummyChargePoint;
	}
}
