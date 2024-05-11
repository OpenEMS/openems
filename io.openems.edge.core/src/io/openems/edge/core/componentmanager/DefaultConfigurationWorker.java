package io.openems.edge.core.componentmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;

/**
 * This Worker checks if certain OpenEMS-Components are configured and - if not
 * - configures them. It is used to make sure a set of standard components are
 * always activated by default on a deployed energy management system.
 *
 * <p>
 * Example 1: Add JSON/REST-Api Controller by default:
 *
 * <pre>
 * if (existingConfigs.stream().noneMatch(c -> //
 * // Check if either "Controller.Api.Rest.ReadOnly" or
 * // "Controller.Api.Rest.ReadWrite" exist
 * "Controller.Api.Rest.ReadOnly".equals(c.factoryPid) || "Controller.Api.Rest.ReadWrite".equals(c.factoryPid))) {
 * 	// if not -> create configuration for "Controller.Api.Rest.ReadOnly"
 * 	this.createConfiguration(defaultConfigurationFailed, "Controller.Api.Rest.ReadOnly", Arrays.asList(//
 * 			new Property("id", "ctrlApiRest0"), //
 * 			new Property("alias", ""), //
 * 			new Property("enabled", true), //
 * 			new Property("port", 8084), //
 * 			new Property("debugMode", false) //
 * 	));
 * }
 * </pre>
 *
 * <p>
 * Example 2: Add Modbus/TCP-Api Controller by default:
 *
 * <pre>
 * if (existingConfigs.stream().noneMatch(c -> //
 * // Check if either "Controller.Api.Rest.ReadOnly" or
 * // "Controller.Api.Rest.ReadWrite" exist
 * "Controller.Api.ModbusTcp.ReadOnly".equals(c.factoryPid)
 * 		|| "Controller.Api.ModbusTcp.ReadWrite".equals(c.factoryPid))) {
 * 	// if not -> create configuration for "Controller.Api.Rest.ReadOnly"
 * 	this.createConfiguration(defaultConfigurationFailed, "Controller.Api.ModbusTcp.ReadOnly", Arrays.asList(//
 * 			new Property("id", "ctrlApiModbusTcp0"), //
 * 			new Property("alias", ""), //
 * 			new Property("enabled", true), //
 * 			new Property("port", 502), //
 * 			new Property("component.ids", JsonUtils.buildJsonArray().add("_sum").build()), //
 * 			new Property("maxConcurrentConnections", 5) //
 * 	));
 * }
 * </pre>
 */
public class DefaultConfigurationWorker extends ComponentManagerWorker {

	/**
	 * Time to wait before doing the check. This allows the system to completely
	 * boot and read configurations.
	 */
	private static final int INITIAL_WAIT_TIME = 5_000; // in ms

	private final Logger log = LoggerFactory.getLogger(DefaultConfigurationWorker.class);

	public DefaultConfigurationWorker(ComponentManagerImpl parent) {
		super(parent);
	}

	/**
	 * Creates all default configurations.
	 *
	 * @param existingConfigs already existing {@link Config}s
	 * @return true on error, false if default configuration was successfully
	 *         applied
	 */
	private boolean createDefaultConfigurations(List<Config> existingConfigs) {
		final var defaultConfigurationFailed = new AtomicBoolean(false);

		/*
		 * Create Default Logging configuration
		 */
		if (existingConfigs.stream().noneMatch(c -> //
		"org.ops4j.pax.logging".equals(c.pid) && c.properties.get("log4j2.rootLogger.level") != null)) {
			// Adding Configuration manually, because this is not a OpenEMS Configuration
			try {
				var log4j = new Hashtable<String, Object>();
				log4j.put("log4j2.appender.console.type", "Console");
				log4j.put("log4j2.appender.console.name", "console");
				log4j.put("log4j2.appender.console.layout.type", "PatternLayout");
				log4j.put("log4j2.appender.console.layout.pattern", "%d{ISO8601} [%-8.8t] %-5p [%-30.30c] %m%n");

				log4j.put("log4j2.appender.paxosgi.type", "PaxOsgi");
				log4j.put("log4j2.appender.paxosgi.name", "paxosgi");

				log4j.put("log4j2.rootLogger.level", "INFO");
				log4j.put("log4j2.rootLogger.appenderRef.console.ref", "console");
				log4j.put("log4j2.rootLogger.appenderRef.paxosgi.ref", "paxosgi");
				var config = this.parent.cm.getConfiguration("org.ops4j.pax.logging", null);
				config.update(log4j);
			} catch (IOException e) {
				this.parent.logError(this.log, "Unable to create Default Logging configuration: " + e.getMessage());
				e.printStackTrace();
				defaultConfigurationFailed.set(true);
			}
		}

		return defaultConfigurationFailed.get();
	}

	@Override
	protected void forever() {
		var existingConfigs = this.readConfigs();

		boolean defaultConfigurationFailed;
		try {
			defaultConfigurationFailed = this.createDefaultConfigurations(existingConfigs);
		} catch (Exception e) {
			this.parent.logError(this.log, "Unable to create default configuration: " + e.getMessage());
			e.printStackTrace();
			defaultConfigurationFailed = true;
		}

		// Set DefaultConfigurationFailed channel value
		this.parent._setDefaultConfigurationFailed(defaultConfigurationFailed);

		// Execute this worker only once
		this.deactivate();
	}

	/**
	 * Reads all currently active configurations.
	 *
	 * @return a list of currently active {@link Config}s
	 */
	private List<Config> readConfigs() {
		List<Config> result = new ArrayList<>();
		try {
			var cm = this.parent.cm;
			var configs = cm.listConfigurations(null); // NOTE: here we are not filtering for enabled=true
			if (configs != null) {
				for (Configuration config : configs) {
					result.add(Config.from(config));
				}
			}
		} catch (Exception e) {
			this.parent.logError(this.log, e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Creates a Component configuration.
	 *
	 * @param defaultConfigurationFailed the result of the last configuration,
	 *                                   updated on error
	 * @param factoryPid                 the Factory-PID
	 * @param properties                 the Component properties
	 */
	protected void createConfiguration(AtomicBoolean defaultConfigurationFailed, String factoryPid,
			List<Property> properties) {
		try {
			this.parent.logInfo(this.log,
					"Creating Component configuration [" + factoryPid + "]: " + properties.stream() //
							.map(p -> p.getName() + ":" + p.getValue().toString()) //
							.collect(Collectors.joining(", ")));
			this.parent.handleCreateComponentConfigRequest(null /* no user */,
					new CreateComponentConfigRequest(factoryPid, properties));
		} catch (OpenemsNamedException e) {
			this.parent.logError(this.log,
					"Unable to create Component configuration for Factory [" + factoryPid + "]: " + e.getMessage());
			e.printStackTrace();
			defaultConfigurationFailed.set(true);
		}
	}

	/**
	 * Updates a Component configuration.
	 *
	 * @param defaultConfigurationFailed the result of the last configuration,
	 *                                   updated on error
	 * @param componentId                the Component-ID
	 * @param properties                 the Component properties
	 */
	protected void updateConfiguration(AtomicBoolean defaultConfigurationFailed, String componentId,
			List<Property> properties) {
		try {
			this.parent.logInfo(this.log,
					"Updating Component configuration [" + componentId + "]: " + properties.stream() //
							.map(p -> p.getName() + ":" + p.getValue().toString()) //
							.collect(Collectors.joining(", ")));

			this.parent.handleUpdateComponentConfigRequest(null /* no user */,
					new UpdateComponentConfigRequest(componentId, properties));
		} catch (OpenemsNamedException e) {
			this.parent.logError(this.log,
					"Unable to update Component configuration for Component [" + componentId + "]: " + e.getMessage());
			e.printStackTrace();
			defaultConfigurationFailed.set(true);
		}
	}

	/**
	 * Deletes a Component configuration.
	 *
	 * @param defaultConfigurationFailed the result of the last configuration,
	 *                                   updated on error
	 * @param componentId                the Component-ID
	 */
	protected void deleteConfiguration(AtomicBoolean defaultConfigurationFailed, String componentId) {
		try {
			this.parent.logInfo(this.log, "Deleting Component [" + componentId + "]");

			this.parent.handleDeleteComponentConfigRequest(null /* no user */,
					new DeleteComponentConfigRequest(componentId));
		} catch (OpenemsNamedException e) {
			this.parent.logError(this.log, "Unable to delete Component [" + componentId + "]: " + e.getMessage());
			e.printStackTrace();
			defaultConfigurationFailed.set(true);
		}
	}

	/**
	 * Holds a configuration.
	 */
	protected static class Config {
		protected static Config from(Configuration config) throws OpenemsException {
			var properties = config.getProperties();
			if (properties == null) {
				throw new OpenemsException(config.getPid() + ": Properties is 'null'");
			}
			var componentIdObj = properties.get("id");
			String componentId;
			if (componentIdObj != null) {
				componentId = componentIdObj.toString();
			} else {
				componentId = null;
			}
			var pid = config.getPid();
			return new Config(config.getFactoryPid(), componentId, pid, properties);
		}

		protected final String factoryPid;
		protected final Optional<String> componentId;
		protected final String pid;
		protected final Dictionary<String, Object> properties;

		private Config(String factoryPid, String componentId, String pid, Dictionary<String, Object> properties) {
			this.factoryPid = factoryPid;
			this.componentId = Optional.ofNullable(componentId);
			this.pid = pid;
			this.properties = properties;
		}
	}

	@Override
	protected int getCycleTime() {
		// initial cycle time
		return DefaultConfigurationWorker.INITIAL_WAIT_TIME;
	}

}
