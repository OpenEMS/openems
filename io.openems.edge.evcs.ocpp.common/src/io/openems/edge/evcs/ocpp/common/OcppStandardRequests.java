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

	/**
	 * Should return an OCPP request that would set the display text to the given
	 * text.
	 * 
	 * <p>
	 * Attention: The given text could be to long or include characters that are not
	 * supported by each EVCS. Return null if this feature is not supported by the
	 * charger
	 * 
	 * @param text Text to be displayed
	 * @return Valid request that can be sent to the EVCS.
	 */
	Request setDisplayText(String text);

}
