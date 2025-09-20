package io.openems.edge.common.component;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface ComponentManagerProvider extends OpenemsComponent {

	/**
	 * Gets the {@link ComponentManager} service.
	 *
	 * @return the service or null if it is not (yet) available.
	 */
	public ComponentManager getComponentManager();

}
