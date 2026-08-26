package io.openems.edge.goodwe.update;

import io.openems.edge.common.update.Updateable;
import io.openems.edge.goodwe.common.enums.GoodWeType;

public interface GoodWeBatteryInverterUpdateParams {

	/**
	 * Gets the update meta information.
	 * 
	 * @return the meta information
	 */
	Updateable.UpdateableMetaInfo getMetaInfo();

	/**
	 * Gets the dsp download location.
	 * 
	 * @param updateParams the parameters to build the url
	 * @return the url to the dsp file
	 */
	String getDspDownloadLocation(GoodWeUpdateParams updateParams);

	/**
	 * Gets the arm download location.
	 * 
	 * @param updateParams the parameters to build the url
	 * @return the url to the arm file
	 */
	String getArmDownloadLocation(GoodWeUpdateParams updateParams);

	/**
	 * Gets the update params to a specific goodwe type.
	 * 
	 * @param goodWeType the type of the goodwe inverter
	 * @return the update params; null if not available for the provided
	 *         {@link GoodWeType}
	 */
	GoodWeUpdateParams getParams(GoodWeType goodWeType);

}
