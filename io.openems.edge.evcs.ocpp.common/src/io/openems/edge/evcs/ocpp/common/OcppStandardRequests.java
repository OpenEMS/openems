package io.openems.edge.evcs.ocpp.common;

import eu.chargetime.ocpp.model.Request;

public interface OcppStandardRequests {

	/**
	 * Should return an OCPP request that would set the charge power to the defined
	 * value.
	 *
	 * <p>
	 * Attention: The given power is given in watt. EVCS with the charging type AC
	 * mostly send their limit values as amps.
	 *
	 * @param chargePower power that should be charged in watt.
	 * @return Valid request that can be sent to the EVCS.
	 */
	Request setChargePowerLimit(int chargePower);

}
