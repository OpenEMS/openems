package io.openems.edge.timedata.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface TimedataProvider extends OpenemsComponent {

	/**
	 * Gets the {@link Timedata} service.
	 *
	 * @return the service or null if it is not (yet) available.
	 */
	public Timedata getTimedata();

}
