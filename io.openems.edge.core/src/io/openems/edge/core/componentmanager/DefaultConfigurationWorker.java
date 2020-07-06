package io.openems.edge.core.componentmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.type.TypeUtils;

/**
 * This Worker checks if certain OpenEMS-Components are configured and - if not
 * - configures them. It is used to make sure a set of standard components are
 * always activated by default on a deployed energy management system.
 */
public class DefaultConfigurationWorker extends AbstractWorker {

	/**
	 * Time to wait before doing the check. This allows the system to completely
	 * boot and read configurations.
	 */
	private static final int INITIAL_WAIT_TIME = 5_000; // in ms

	private final Logger log = LoggerFactory.getLogger(DefaultConfigurationWorker.class);
	private final ComponentManagerImpl parent;

	public DefaultConfigurationWorker(ComponentManagerImpl parent) {
		this.parent = parent;
	}

	/**
	 * Creates all default configurations.
	 * 
	 * @return true on error, false if default configuration was successfully
	 *         applied
	 */
	private boolean createDefaultConfigurations(List<Config> existingConfigs) {
		final AtomicBoolean defaultConfigurationFailed = new AtomicBoolean(false);

		/*
		 * Create Default Logging configuration
		 */
		if (existingConfigs.stream().noneMatch(c -> //
		"org.ops4j.pax.logging".equals(c.pid))) {
			// Adding Configuration manually, because this is not a OpenEMS Configuration
			try {
				Configuration config = this.parent.cm.getConfiguration("org.ops4j.pax.logging", null);
				Hashtable<String, Object> log4j = new Hashtable<>();
				log4j.put("log4j.rootLogger", "INFO, CONSOLE, osgi:*");
				log4j.put("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
				log4j.put("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
				log4j.put("log4j.appender.CONSOLE.layout.ConversionPattern",
						"%d{ISO8601} [%-8.8t] %-5p [%-30.30c] %m%n");
				log4j.put("log4j.logger.org.eclipse.osgi", "WARN");
				config.update(log4j);
			} catch (IOException e) {
				this.parent.logError(this.log, "Unable to create Default Logging configuration: " + e.getMessage());
				e.printStackTrace();
				defaultConfigurationFailed.set(true);
			}
		}

		/*
		 * Delete configuration for deprecated Controller.Api.Rest
		 */
		existingConfigs.stream().filter(c -> //
		c.componentId.isPresent() && "Controller.Api.Rest".equals(c.factoryPid)).forEach(c -> {
			this.deleteConfiguration(defaultConfigurationFailed, c.componentId.get());
		});

		/*
		 * Create Controller.Api.Rest.ReadOnly
		 */
		if (existingConfigs.stream().noneMatch(c -> //
		// Check if either "Controller.Api.Rest.ReadOnly" or
		// "Controller.Api.Rest.ReadWrite" exist
		"Controller.Api.Rest.ReadOnly".equals(c.factoryPid) || "Controller.Api.Rest.ReadWrite".equals(c.factoryPid))) {
			// if not -> create configuration for "Controller.Api.Rest.ReadOnly"
			this.createConfiguration(defaultConfigurationFailed, "Controller.Api.Rest.ReadOnly", Arrays.asList(//
					new Property("id", "ctrlApiRest0"), //
					new Property("alias", ""), //
					new Property("enabled", true), //
					new Property("port", 8084), //
					new Property("debugMode", false) //
			));
		}

		/*
		 * Create Controller.Api.Modbus.ReadOnly
		 */
		if (existingConfigs.stream().noneMatch(c -> //
		// Check if either "Controller.Api.Rest.ReadOnly" or
		// "Controller.Api.Rest.ReadWrite" exist
		"Controller.Api.ModbusTcp.ReadOnly".equals(c.factoryPid)
				|| "Controller.Api.ModbusTcp.ReadWrite".equals(c.factoryPid))) {
			// if not -> create configuration for "Controller.Api.Rest.ReadOnly"
			this.createConfiguration(defaultConfigurationFailed, "Controller.Api.ModbusTcp.ReadOnly", Arrays.asList(//
					new Property("id", "ctrlApiModbusTcp0"), //
					new Property("alias", ""), //
					new Property("enabled", true), //
					new Property("port", 502), //
					new Property("component.ids", JsonUtils.buildJsonArray().add("_sum").build()), //
					new Property("maxConcurrentConnections", 5) //
			));
		}

		/*
		 * Create Timedata.Rrd4j
		 */
		if (existingConfigs.stream().noneMatch(c -> //
		// Check if either "Timedata.Rrd4j" or
		// "Timedata.InfluxDB" exist
		"Timedata.Rrd4j".equals(c.factoryPid) || "Timedata.InfluxDB".equals(c.factoryPid))) {
			// if not -> create configuration for "Timedata.Rrd4j"
			this.createConfiguration(defaultConfigurationFailed, "Timedata.Rrd4j", Arrays.asList(//
					new Property("id", "rrd4j0"), //
					new Property("alias", ""), //
					new Property("enabled", true), //
					new Property("noOfCycles", 60) //
			));
		}

		this.migrateConfigurationsOnVersionUpgrade(existingConfigs, defaultConfigurationFailed);

		return defaultConfigurationFailed.get();
	}

	/**
	 * Apply upgrades to newer versions of OpenEMS Edge.
	 * 
	 * @param configurationFailed the result of the configuration, updated on error
	 */
	private void migrateConfigurationsOnVersionUpgrade(List<Config> existingConfigs,
			AtomicBoolean configurationFailed) {
		this.migrateConfigurationOnVersion_2020_11_5(existingConfigs, configurationFailed);
	}

	/**
	 * Migrate to OpenEMS version 2020.11.5.
	 */
	private void migrateConfigurationOnVersion_2020_11_5(List<Config> existingConfigs,
			AtomicBoolean configurationFailed) {
		/*
		 * Upgrade to generic SOCOMEC implementation.
		 */
		// Threephase
		existingConfigs.stream().filter(c -> c.componentId.isPresent() && (//
		"Meter.SOCOMEC.DirisA10".equals(c.factoryPid) || //
				"Meter.SOCOMEC.DirisA14".equals(c.factoryPid) || //
				"Meter.SOCOMEC.DirisB30".equals(c.factoryPid) || //
				"Meter.SOCOMEC.DirisE24".equals(c.factoryPid) //
		)).forEach(c -> {
			String alias = TypeUtils.getAsType(OpenemsType.STRING, c.properties.get("alias"));
			boolean enabled = TypeUtils.getAsType(OpenemsType.BOOLEAN, c.properties.get("enabled"));
			String modbusId = TypeUtils.getAsType(OpenemsType.STRING, c.properties.get("modbus.id"));
			int modbusUnitId = TypeUtils.getAsType(OpenemsType.INTEGER, c.properties.get("modbusUnitId"));
			boolean invert = TypeUtils.getAsType(OpenemsType.BOOLEAN, c.properties.get("invert"));
			String type = TypeUtils.getAsType(OpenemsType.STRING, c.properties.get("type"));

			this.deleteConfiguration(configurationFailed, c.componentId.get());

			this.createConfiguration(configurationFailed, "Meter.Socomec.Threephase", Arrays.asList(//
					new Property("id", c.componentId.get()), //
					new Property("alias", alias), //
					new Property("enabled", enabled), //
					new Property("modbus.id", modbusId), //
					new Property("modbusUnitId", modbusUnitId), //
					new Property("invert", invert), //
					new Property("type", type) //
			));
		});
		// Singlephase
		existingConfigs.stream().filter(c -> c.componentId.isPresent() && //
				"Meter.SOCOMEC.CountisE24".equals(c.factoryPid)//
		).forEach(c -> {
			String alias = TypeUtils.getAsType(OpenemsType.STRING, c.properties.get("alias"));
			boolean enabled = TypeUtils.getAsType(OpenemsType.BOOLEAN, c.properties.get("enabled"));
			String modbusId = TypeUtils.getAsType(OpenemsType.STRING, c.properties.get("modbus.id"));
			int modbusUnitId = TypeUtils.getAsType(OpenemsType.INTEGER, c.properties.get("modbusUnitId"));
			boolean invert = TypeUtils.getAsType(OpenemsType.BOOLEAN, c.properties.get("invert"));
			String type = TypeUtils.getAsType(OpenemsType.STRING, c.properties.get("type"));

			this.deleteConfiguration(configurationFailed, c.componentId.get());

			this.createConfiguration(configurationFailed, "Meter.Socomec.Singlephase", Arrays.asList(//
					new Property("id", c.componentId.get()), //
					new Property("alias", alias), //
					new Property("enabled", enabled), //
					new Property("modbus.id", modbusId), //
					new Property("modbusUnitId", modbusUnitId), //
					new Property("invert", invert), //
					new Property("type", type) //
			));
		});
	}

	@Override
	protected void forever() {
		List<Config> existingConfigs = this.readConfigs();

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
	 * @return
	 */
	private List<Config> readConfigs() {
		List<Config> result = new ArrayList<Config>();
		try {
			ConfigurationAdmin cm = this.parent.cm;
			Configuration[] configs = cm.listConfigurations(null); // NOTE: here we are not filtering for enabled=true
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
			CompletableFuture<JsonrpcResponseSuccess> response = this.parent.handleCreateComponentConfigRequest(
					null /* no user */, new CreateComponentConfigRequest(factoryPid, properties));
			response.get(60, TimeUnit.SECONDS);

		} catch (OpenemsNamedException | InterruptedException | ExecutionException | TimeoutException e) {
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
			CompletableFuture<JsonrpcResponseSuccess> response = this.parent.handleUpdateComponentConfigRequest(
					null /* no user */, new UpdateComponentConfigRequest(componentId, properties));
			response.get(60, TimeUnit.SECONDS);

		} catch (OpenemsNamedException | InterruptedException | ExecutionException | TimeoutException e) {
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
	 * @return false on success; true on error or if lastResult was already true
	 */
	protected void deleteConfiguration(AtomicBoolean defaultConfigurationFailed, String componentId) {
		try {
			CompletableFuture<JsonrpcResponseSuccess> response = this.parent.handleDeleteComponentConfigRequest(
					null /* no user */, new DeleteComponentConfigRequest(componentId));
			response.get(60, TimeUnit.SECONDS);

		} catch (OpenemsNamedException | InterruptedException | ExecutionException | TimeoutException e) {
			this.parent.logError(this.log, "Unable to delete Component [" + componentId + "]: " + e.getMessage());
			e.printStackTrace();
			defaultConfigurationFailed.set(true);
		}
	}

	/**
	 * Holds a configuration.
	 */
	protected static class Config {
		protected static Config from(Configuration config) {
			Dictionary<String, Object> properties = config.getProperties();
			Object componentIdObj = properties.get("id");
			String componentId;
			if (componentIdObj != null) {
				componentId = componentIdObj.toString();
			} else {
				componentId = null;
			}
			String pid = config.getPid();
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
