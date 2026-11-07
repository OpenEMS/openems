package io.openems.edge.goodwe.stsbox.update;

import io.openems.edge.common.update.Updateable;
import io.openems.edge.goodwe.common.enums.GoodWeType;

public interface GoodWeStsBoxUpdateParams {

	/**
	 * Gets the update meta information.
	 * 
	 * @return the meta information
	 */
	Updateable.UpdateableMetaInfo getMetaInfo();

	/**
	 * Gets the sts download location.
	 * 
	 * @param updateParams the parameters to build the url
	 * @return the url to the sts file
	 */
	String getDownloadLocation(GoodWeStsBoxParams updateParams);

	/**
	 * Gets the update params to a specific goodwe type.
	 * 
	 * @param goodWeType the {@link GoodWeType} of the inverter where this sts box
	 *                   is connected to
	 * @return the update params; null if not available for the provided
	 *         {@link GoodWeType}
	 */
	GoodWeStsBoxParams getParams(GoodWeType goodWeType);

}
