package io.openems.edge.core.appmanager.dependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.osgi.framework.FrameworkUtil;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppManager;
import io.openems.edge.core.appmanager.AppManagerImpl;
import io.openems.edge.core.appmanager.OpenemsAppInstance;

public class DependencyUtil {

	// instance per thread so the installation of an app is separated
	// from the validate worker
	private static final Map<Thread, DependencyUtil> THREAD_2_INSTANCE = new HashMap<>();

	// only for testing
	private static AppManagerAppHelper appHelper;

	// only create one instance per thread and remove the instance after the first
	// caller of the method finishes
	private static final <T> T using(Function<DependencyUtil, T> consumer) {
		var createdInstance = false;
		var currentThread = Thread.currentThread();
		var util = THREAD_2_INSTANCE.get(currentThread);
		if (util == null) {
			util = new DependencyUtil();
			THREAD_2_INSTANCE.put(currentThread, util);
			createdInstance = true;
		}
		var value = consumer.apply(util);
		if (createdInstance) {
			THREAD_2_INSTANCE.remove(currentThread);
		}
		return value;
	}

	/**
	 * Temporary field to avoid endless loop.
	 */
	private boolean isCurrentlyRunning = false;
	// can not use synchronized for primitive types
	private final Object getInstanceIdLock = new Object();

	private DependencyUtil() {
		// no instance needed
	}

	/**
	 * Gets the instanceId of the app which has the given Component.
	 * 
	 * @param componentManager the {@link ComponentManager}
	 * @param componentId      the ComponentId
	 * @return the Id or null if no app has this component
	 */
	public static final UUID getInstanceIdOfAppWhichHasComponent(ComponentManager componentManager,
			String componentId) {
		return using(t -> t.getInstanceIdOfAppWhichHasComponentInternal(componentManager, componentId));
	}

	/**
	 * Gets the instance of the app which has the given Component.
	 * 
	 * @param componentManager the {@link ComponentManager}
	 * @param componentId      the ComponentId
	 * @return the {@link OpenemsAppInstance} or null if no instance has this
	 *         component
	 */
	public static final OpenemsAppInstance getAppWhichHasComponent(ComponentManager componentManager,
			String componentId) {
		return using(t -> t.getAppWhichHasComponentInternal(componentManager, componentId));
	}

	/**
	 * Gets the {@link OpenemsAppInstance} of the first found instance that has the
	 * given componentId in its {@link AppConfiguration}.
	 *
	 * <p>
	 * NOTE: when calling this inside an app configuration it can lead to an endless
	 * loop
	 *
	 * @param componentManager a componentManager to get the appManager
	 * @param componentId      the component id that the app should have
	 * @return the found instanceId or null if no app has this component
	 */
	public final OpenemsAppInstance getAppWhichHasComponentInternal(//
			ComponentManager componentManager, //
			String componentId //
	) {
		synchronized (this.getInstanceIdLock) {
			if (this.isCurrentlyRunning) {
				return null;
			}
			this.isCurrentlyRunning = true;
		}
		var appManagerImpl = DependencyUtil.getAppManagerImpl(componentManager);
		if (appManagerImpl == null) {
			this.setCurrentlyRunning(false);
			return null;
		}
		var instances = new ArrayList<>(appManagerImpl.getInstantiatedApps());
		var appHelper = getAppManagerAppHelper();
		if (appHelper.getTemporaryApps() != null) {
			instances.addAll(appHelper.getTemporaryApps().currentlyCreatingApps());
		}
		for (var entry : appManagerImpl.appConfigs(instances, null)) {
			if (entry.getValue().getComponents().stream().anyMatch(c -> c.getId().equals(componentId))) {
				this.setCurrentlyRunning(false);
				return entry.getKey();
			}
		}
		this.setCurrentlyRunning(false);
		return null;
	}

	/**
	 * Gets the instanceId of the first found instance that has the given
	 * componentId in its {@link AppConfiguration}.
	 *
	 * <p>
	 * NOTE: when calling this inside an app configuration it can lead to an endless
	 * loop
	 *
	 * @param componentManager a componentManager to get the appManager
	 * @param componentId      the component id that the app should have
	 * @return the found instanceId or null if no app has this component
	 */
	public UUID getInstanceIdOfAppWhichHasComponentInternal(//
			ComponentManager componentManager, //
			String componentId //
	) {
		final var foundApp = this.getAppWhichHasComponentInternal(componentManager, componentId);
		if (foundApp != null) {
			return foundApp.instanceId;
		}
		return null;
	}

	private void setCurrentlyRunning(boolean isCurrentlyRunning) {
		synchronized (this.getInstanceIdLock) {
			this.isCurrentlyRunning = isCurrentlyRunning;
		}
	}

	private static final AppManagerImpl getAppManagerImpl(ComponentManager componentManager) {
		var appManager = componentManager.getEnabledComponentsOfType(AppManager.class);
		if (appManager.size() == 0 || !(appManager.get(0) instanceof AppManagerImpl)) {
			return null;
		}
		return (AppManagerImpl) appManager.get(0);
	}

	private static final AppManagerAppHelper getAppManagerAppHelper() {
		if (appHelper != null) {
			return appHelper;
		}
		var context = FrameworkUtil.getBundle(AppManagerAppHelper.class).getBundleContext();
		var serviceReference = context.getServiceReference(AppManagerAppHelper.class);
		var appHelper = context.getService(serviceReference);
		context.ungetService(serviceReference);
		return appHelper;
	}

}
