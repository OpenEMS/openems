package io.openems.edge.core.appmanager.dependency;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppManager;
import io.openems.edge.core.appmanager.AppManagerImpl;

public class DependencyUtil {

	/**
	 * Temporary field to avoid endless loop.
	 */
	private static final Set<String> CURRENTLY_CALLING_APP_IDS = new HashSet<String>();

	/**
	 * Gets the instanceId of the first found app that has the given componentId in
	 * its {@link AppConfiguration}.
	 * 
	 * <p>
	 * WARN: when calling this inside an app configuration it can lead to an endless
	 * loop
	 * 
	 * @param componentManager a componentManager to get the appManager
	 * @param componentId      the component id that the app should have
	 * @return the found instanceId or null if no app has this component
	 */
	public static final UUID getInstanceIdOfAppWhichHasComponent(ComponentManager componentManager, String componentId,
			String currentlyCallingApp) {
		var appManagerImpl = DependencyUtil.getAppManagerImpl(componentManager);
		if (appManagerImpl == null) {
			return null;
		}
		CURRENTLY_CALLING_APP_IDS.add(currentlyCallingApp);
		for (var entry : appManagerImpl.appConfigs(appManagerImpl.getInstantiatedApps(),
				i -> !CURRENTLY_CALLING_APP_IDS.contains(i.appId))) {
			if (entry.getValue().components.stream().anyMatch(c -> c.getId().equals(componentId))) {
				CURRENTLY_CALLING_APP_IDS.remove(currentlyCallingApp);
				return entry.getKey().instanceId;
			}
		}
		CURRENTLY_CALLING_APP_IDS.remove(currentlyCallingApp);
		return null;
	}

	private static final AppManagerImpl getAppManagerImpl(ComponentManager componentManager) {
		var appManager = componentManager.getEnabledComponentsOfType(AppManager.class);
		if (appManager.size() != 1) {
			return null;
		}
		if (!(appManager.get(0) instanceof AppManagerImpl)) {
			return null;
		}
		return (AppManagerImpl) appManager.get(0);
	}

}
