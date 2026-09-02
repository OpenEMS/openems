package io.openems.edge.evcs.hardybarth.ecb1;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Common parent interface for components driven by the Hardy Barth ECB1 REST
 * API. Implemented by both the legacy EVCS and the new EVSE component.
 */
public interface Ecb1Parent extends OpenemsComponent, ElectricityMeter {

	/**
	 * Called once per cycle when a chargecontrol response is successfully parsed.
	 *
	 * @param state     IEC 61851 state string (A/B/C/D/E/F), may be null
	 * @param stateId   numeric state ID, may be null
	 * @param connected true when a vehicle is plugged in, may be null
	 */
	void onChargeControlStatus(String state, Integer stateId, Boolean connected);

	/**
	 * Called when the ECB1 communication status changes.
	 *
	 * @param failed true if communication is currently failing
	 */
	void onCommunicationFailed(boolean failed);
}
