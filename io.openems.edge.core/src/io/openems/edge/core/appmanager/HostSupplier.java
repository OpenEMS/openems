package io.openems.edge.core.appmanager;

import io.openems.edge.common.host.Host;

public interface HostSupplier {

	/**
	 * Gets a {@link Host}.
	 * 
	 * @return the {@link Host}
	 */
	Host getHost();

}
