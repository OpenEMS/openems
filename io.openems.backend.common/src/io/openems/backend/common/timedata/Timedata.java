package io.openems.backend.common.timedata;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface Timedata extends BackendTimedata {

	/**
	 * Returns a unique ID for this OpenEMS component.
	 *
	 * @return the unique ID
	 */
	public String id();

}
