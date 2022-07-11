package io.openems.edge.core.appmanager.dependency;

import java.util.UUID;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppManager;
import io.openems.edge.core.appmanager.AppManagerImpl;

public class DependencyUtil {

	/**
	 * Temporary field to avoid endless loop.
	 */
	private static boolean isCurrentlyRunning = false;

	/**
	 * Gets the instanceId of the first found app that has the given componentId in
	 * its {@link AppConfiguration}.
	 *
	 * <p>
	 * NOTE: when calling this inside an app configuration it can lead to an endless
	 * loop
	 *
	 * @param componentManager    a componentManager to get the appManager
	 * @param componentId         the component id that the app should have
	 * @param currentlyCallingApp the app that is currently calling this methode
	 * @return the found instanceId or null if no app has this component
	 */
	public static final UUID getInstanceIdOfAppWhichHasComponent(ComponentManager componentManager, String componentId,
			String currentlyCallingApp) {
		if (isCurrentlyRunning) {
			return null;
		}
		isCurrentlyRunning = true;
		var appManagerImpl = DependencyUtil.getAppManagerImpl(componentManager);
		if (appManagerImpl == null) {
			isCurrentlyRunning = false;
			return null;
		}
		for (var entry : appManagerImpl.appConfigs()) {
			if (entry.getValue().components.stream().anyMatch(c -> c.getId().equals(componentId))) {
				isCurrentlyRunning = false;
				return entry.getKey().instanceId;
			}
		}
		isCurrentlyRunning = false;
		return null;
	}

	private static final AppManagerImpl getAppManagerImpl(ComponentManager componentManager) {
		var appManager = componentManager.getEnabledComponentsOfType(AppManager.class);
		if (appManager.size() != 1 || !(appManager.get(0) instanceof AppManagerImpl)) {
			return null;
		}
		return (AppManagerImpl) appManager.get(0);
	}

}
