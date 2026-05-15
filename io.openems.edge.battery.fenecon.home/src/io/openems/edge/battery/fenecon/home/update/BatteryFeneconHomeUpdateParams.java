package io.openems.edge.battery.fenecon.home.update;

import com.google.common.hash.HashCode;

import io.openems.edge.battery.fenecon.home.BatteryFeneconHomeHardwareType;
import io.openems.edge.battery.fenecon.home.TwoPartVersion;
import io.openems.edge.common.update.Updateable;

public interface BatteryFeneconHomeUpdateParams {

	/**
	 * Gets the update meta information.
	 *
	 * @return the meta information
	 */
	Updateable.UpdateableMetaInfo getMetaInfo();

	/**
	 * Gets the arm download location.
	 *
	 * @param updateParams the parameters to build the url
	 * @return the url to the arm file
	 */
	String getArmDownloadLocation(UpdateParams updateParams);

	/**
	 * Gets the update params to a specific fenecon home battery type.
	 *
	 * @param hardwareType the type of the fenecon home battery
	 * @return the update params; null if not available for the provided
	 *         {@link BatteryFeneconHomeHardwareType}
	 */
	UpdateParams getParams(BatteryFeneconHomeHardwareType hardwareType);

	record UpdateParams(String storageName, TwoPartVersion version, HashCode sha256) {

	}
}
