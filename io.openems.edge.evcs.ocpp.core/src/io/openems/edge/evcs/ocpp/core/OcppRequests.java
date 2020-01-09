package io.openems.edge.evcs.ocpp.core;

import eu.chargetime.ocpp.model.Request;

public interface OcppRequests {

	/**
	 * Should return an OCPP request that would set the charge power to the defined value.
	 * 
	 * @param value
	 * @return Valid request that can be sent to the EVCS
	 */
	public Request setChargePowerLimit(String chargePower);
	
}
