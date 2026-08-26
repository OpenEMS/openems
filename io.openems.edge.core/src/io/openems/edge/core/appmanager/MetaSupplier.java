package io.openems.edge.core.appmanager;

import io.openems.edge.common.meta.Meta;

public interface MetaSupplier {

	/**
	 * Gets a {@link Meta}.
	 * 
	 * @return the {@link Meta}
	 */
	Meta getMeta();

}
