package io.openems.edge.core.componentmanager;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This Worker constantly validates:.
 *
 * <ul>
 * <li>that all configured OpenEMS-Components are actually activated. Otherwise
 * it sets the {@link ComponentManager.ChannelId#CONFIG_NOT_ACTIVATED} channel.
 * <li>that there is no duplicated Component-ID in the system. Otherwise it sets
 * the {@link ComponentManager.ChannelId#DUPLICATED_COMPONENT_ID} channel.
 * </ul>
 *
 * <p>
 * Next to the Warning/Fault channels details are also printed to console on
 * debugLog.
 */
public class OsgiValidateWorker extends ComponentManagerWorker {

	/*
	 * For INITIAL_CYCLES cycles the distance between two checks is
	 * INITIAL_CYCLE_TIME, afterwards the check runs every REGULAR_CYCLE_TIME
	 * milliseconds.
	 *
	 * Why? In the beginning it takes a while till all components are up and
	 * running. So it is likely, that in the beginning not all are immediately
	 * running.
	 */
	private static final int INITIAL_CYCLES = 60;
	private static final int INITIAL_CYCLE_TIME = 5_000; // in ms
	private static final int REGULAR_CYCLE_TIME = 60_000; // in ms
	private static final int RESTART_COMPONENTS_AFTER = 3;

	private final Logger log = LoggerFactory.getLogger(OsgiValidateWorker.class);

	/**
	 * Map from Component-ID to defect details.
	 */
	private final Map<String, String> defectiveComponents = new HashMap<>();

	/**
	 * Components with duplicated Component-IDs.
	 */
	private final Set<String> duplicatedComponentIds = new HashSet<>();

	/**
	 * Components waiting for restart.
	 */
	private final Map<String, Integer> restartComponents = new HashMap<>();

	public OsgiValidateWorker(ComponentManagerImpl parent) {
		super(parent);
	}

	@Override
	protected void forever() {
		this.findDuplicatedComponentIds();
		this.findDefectiveComponents();
		this.restartDefectiveComponents();
	}

	private void findDuplicatedComponentIds() {
		final var configs = this.readAllConfigurations();
		final Set<String> duplicatedComponentIds = new HashSet<>();
		updateDuplicatedComponentIds(duplicatedComponentIds, configs);
		this.parent._setDuplicatedComponentId(!this.duplicatedComponentIds.isEmpty());
		synchronized (this.duplicatedComponentIds) {
			this.duplicatedComponentIds.clear();
			this.duplicatedComponentIds.addAll(duplicatedComponentIds);
		}
	}

	private void findDefectiveComponents() {
		final var configs = this.readEnabledConfigurations();
		final Map<String, String> defectiveComponents = new HashMap<>();
		updateInactiveComponentsUsingScr(defectiveComponents, this.parent.serviceComponentRuntime);
		this.updateInactiveComponentsUsingConfigurationAdmin(defectiveComponents, this.parent.getEnabledComponents(),
				configs, this.parent.serviceComponentRuntime);
		this.parent._setConfigNotActivated(!defectiveComponents.isEmpty());
		synchronized (this.defectiveComponents) {
			this.defectiveComponents.clear();
			this.defectiveComponents.putAll(defectiveComponents);
		}
	}

	private void restartDefectiveComponents() {
		var it = this.restartComponents.entrySet().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			if (entry.getValue() >= RESTART_COMPONENTS_AFTER) {
				var componentId = entry.getKey();
				// Update Configuration to try to restart Component
				try {
					this.parent.logInfo(this.log, "Trying to restart Component [" + componentId + "]");
					var config = this.parent.getExistingConfigForId(componentId);
					var properties = config.getProperties();
					config.update(properties);

				} catch (IOException | OpenemsNamedException e) {
					this.parent.logError(this.log, "Unable to restart Component [" + componentId + "]");
					e.printStackTrace();
				}
				// Remove from list
				it.remove();
				return;
			}
		}
	}

	/**
	 * Updates the inactive Components.
	 *
	 * <p>
	 * This method uses {@link ServiceComponentRuntime} to get details about why the
	 * Component is inactive.
	 *
	 * @param defectiveComponents the map to be updated
	 * @param scr                 the {@link ServiceComponentRuntime}
	 */
	private static void updateInactiveComponentsUsingScr(Map<String, String> defectiveComponents,
			ServiceComponentRuntime scr) {
		var descriptions = scr.getComponentDescriptionDTOs();
		for (ComponentDescriptionDTO description : descriptions) {
			var configurations = scr.getComponentConfigurationDTOs(description);
			for (ComponentConfigurationDTO configuration : configurations) {
				if (!MapUtils.getAsOptionalBoolean(configuration.properties, "enabled").orElse(true)) {
					// Component is not enabled -> ignore
					continue;
				}

				final String defectDetails;
				switch (configuration.state) {
				case ComponentConfigurationDTO.ACTIVE:
				case ComponentConfigurationDTO.SATISFIED:
					continue;
				case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION: {
					defectDetails = "Missing required configuration";
					break;
				}
				case ComponentConfigurationDTO.UNSATISFIED_REFERENCE: {
					defectDetails = "Unsatisfied reference for " //
							+ Stream.of(configuration.unsatisfiedReferences) //
									.map(ref -> {
										var result = new StringBuilder().append(ref.name);
										if (ref.target != null && !ref.target.isEmpty()) {
											result.append(" (").append(ref.target).append(")");
										}
										return result.toString();
									}) //
									.collect(Collectors.joining(",")); //
					break;
				}
				case ComponentConfigurationDTO.FAILED_ACTIVATION: {
					defectDetails = "Failed activation " + configuration.failure.split(System.lineSeparator(), 2)[0];
					break;
				}
				default:
					defectDetails = "Undefined failure [" + configuration.state + "];";
				}
				var componentId = (String) configuration.properties.get("id");
				defectiveComponents.put(componentId, defectDetails);
			}
		}
	}

	/*
	 * Compare all Configuration Admin Configurations with actually existing and
	 * active OpenEMS Components.
	 *
	 * @param configs enabled {@link Configuration}s from {@link ConfigurationAdmin}
	 */
	private void updateInactiveComponentsUsingConfigurationAdmin(Map<String, String> defectiveComponents,
			List<OpenemsComponent> enabledComponents, Configuration[] configs, ServiceComponentRuntime scr) {
		Set<String> restartComponents = new HashSet<>();

		for (Configuration config : configs) {
			Dictionary<String, Object> properties;
			try {
				properties = config.getProperties();
				if (properties == null) {
					// configuration was just created and update has not been called
					continue;
				}
			} catch (IllegalStateException e) {
				// Configuration has been deleted
				continue;
			}
			var componentId = (String) properties.get("id");
			if (componentId != null) {
				if (defectiveComponents.containsKey(componentId)) {
					// already in the list
					continue;
				}
				if (!isComponentActivated(enabledComponents, componentId)) {
					var factoryPid = config.getFactoryPid();
					if (factoryPid != null && scr.getComponentDescriptionDTOs().stream()
							.anyMatch(description -> factoryPid.equals(description.name))) {
						// Bundle exists -> try to restart Component
						restartComponents.add(componentId);
					} else {
						// Bundle with this name does not exist
						defectiveComponents.putIfAbsent(componentId, "Missing Bundle");
					}
				}
			}
		}

		/*
		 * Update already known list of Components that should be restarted
		 */
		var it = this.restartComponents.entrySet().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			if (restartComponents.remove(entry.getKey())) {
				// increase count value
				entry.setValue(entry.getValue() + 1);
			} else {
				// Remove global entry that is not anymore in the new Restart-Components
				it.remove();
			}
		}
		for (String componentId : restartComponents) {
			// add remaining new Restart-Components
			this.restartComponents.put(componentId, 0);
		}
	}

	/**
	 * Read all configurations from ConfigurationAdmin - no matter if enabled or
	 * not.
	 *
	 * @return {@link Configuration}s from {@link ConfigurationAdmin}; empty array
	 *         on error
	 */
	private Configuration[] readAllConfigurations() {
		try {
			var cm = this.parent.cm;
			var configs = cm.listConfigurations(null);
			if (configs != null) {
				return configs;
			}
			return new Configuration[0];
		} catch (Exception e) {
			this.parent.logError(this.log, e.getMessage());
			e.printStackTrace();
			return new Configuration[0];
		}
	}

	/**
	 * Read all enabled configurations from ConfigurationAdmin.
	 *
	 * @return enabled {@link Configuration}s from {@link ConfigurationAdmin}; empty
	 *         array on error
	 */
	private Configuration[] readEnabledConfigurations() {
		try {
			var cm = this.parent.cm;
			var configs = cm.listConfigurations("(enabled=true)");
			if (configs != null) {
				return configs;
			}
			return new Configuration[0];
		} catch (Exception e) {
			this.parent.logError(this.log, e.getMessage());
			e.printStackTrace();
			return new Configuration[0];
		}
	}

	private static boolean isComponentActivated(List<OpenemsComponent> enabledComponents, String componentId) {
		for (OpenemsComponent component : enabledComponents) {
			if (componentId.equals(component.id())) {
				// Everything Ok
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks for duplicated Component-IDs.
	 *
	 * @param duplicatedComponentIds the Set of Component-IDs to be updated
	 * @param configs                enabled {@link Configuration}s from
	 *                               {@link ConfigurationAdmin}
	 */
	private static void updateDuplicatedComponentIds(Set<String> duplicatedComponentIds, Configuration[] configs) {
		Set<String> componentIds = new HashSet<>();
		for (Configuration config : configs) {
			var properties = config.getProperties();
			if (properties == null) {
				System.err.println(config.getPid() + ": Properties is 'null'");
				continue;
			}
			var componentId = (String) properties.get("id");
			if (componentId != null) {
				if (componentIds.contains(componentId)) {
					duplicatedComponentIds.add(componentId);
				} else {
					componentIds.add(componentId);
				}
			}
		}
	}

	private int cycleCountDown = OsgiValidateWorker.INITIAL_CYCLES;

	@Override
	protected int getCycleTime() {
		if (this.cycleCountDown > 0) {
			this.cycleCountDown--;
			return OsgiValidateWorker.INITIAL_CYCLE_TIME;
		}
		return OsgiValidateWorker.REGULAR_CYCLE_TIME;
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		// trigger immediate validation on configuration event
		this.triggerNextRun();
	}

	@Override
	public void triggerNextRun() {
		// Reset Cycle-Counter on explicit run
		this.cycleCountDown = OsgiValidateWorker.INITIAL_CYCLES;
		super.triggerNextRun();
	}

	@Override
	public String debugLog() {
		String defectiveComponents;
		synchronized (this.defectiveComponents) {
			defectiveComponents = this.defectiveComponents.entrySet().stream() //
					.map(e -> e.getKey() + "[" + e.getValue() + "]") //
					.collect(Collectors.joining(" "));
		}
		String duplicatedComponents;
		synchronized (this.duplicatedComponentIds) {
			duplicatedComponents = String.join(",", this.duplicatedComponentIds);
		}

		if (defectiveComponents.isEmpty() && duplicatedComponents.isEmpty()) {
			return null;

		}
		if (defectiveComponents.isEmpty()) {
			return "Duplicated:" + duplicatedComponents;

		} else if (duplicatedComponents.isEmpty()) {
			return "Defective:" + defectiveComponents;

		} else {
			return "Duplicated:" + duplicatedComponents + "|" + "Defective:" + defectiveComponents;
		}
	}

}
