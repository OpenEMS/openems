package io.openems.edge.core.appmanager;

import io.openems.edge.common.component.ComponentManager;

public interface ComponentManagerSupplier {

	/**
	 * Gets a {@link ComponentManager}.
	 * 
	 * @return the {@link ComponentManager}
	 */
	ComponentManager getComponentManager();

}
