package io.openems.edge.core.componentmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractWorker;

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
	private final static int INITIAL_WAIT_TIME = 30_000; // in ms

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
		boolean defaultConfigurationFailed = false;

		/*
		 * Create Controller.Api.Rest.ReadOnly
		 */
		if (existingConfigs.stream().noneMatch(c -> //
		// Check if either "Controller.Api.Rest.ReadOnly" or
		// "Controller.Api.Rest.ReadWrite" exist
		"Controller.Api.Rest.ReadOnly".equals(c.factoryPid) || "Controller.Api.Rest.ReadWrite".equals(c.factoryPid))) {
			// if not -> create configuration for "Controller.Api.Rest.ReadOnly"
			defaultConfigurationFailed = this.createConfiguration(defaultConfigurationFailed,
					"Controller.Api.Rest.ReadOnly", Arrays.asList(//
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
			defaultConfigurationFailed = this.createConfiguration(defaultConfigurationFailed,
					"Controller.Api.ModbusTcp.ReadOnly", Arrays.asList(//
							new Property("id", "ctrlApiModbusTcp0"), //
							new Property("alias", ""), //
							new Property("enabled", true), //
							new Property("port", 502), //
							new Property("component.ids", JsonUtils.buildJsonArray().add("_sum").build()), //
							new Property("maxConcurrentConnections", 5) //
					));
		}

		return defaultConfigurationFailed;
	}

	@Override
	protected void forever() {
		List<Config> existingConfigs = this.readConfigs();

		boolean defaultConfigurationFailed = this.createDefaultConfigurations(existingConfigs);

		// Set DefaultConfigurationFailed channel value
		this.parent.defaultConfigurationFailed().setNextValue(defaultConfigurationFailed);

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
	 * Creates and applies a Component configuration.
	 * 
	 * @param lastResult  the result of the last configuration
	 * @param componentId the Component-ID
	 * @param properties  the Component properties
	 * @return false on success; true on error or if lastResult was already true
	 */
	private boolean createConfiguration(boolean lastResult, String factoryPid, List<Property> properties) {
		try {
			CompletableFuture<JsonrpcResponseSuccess> response = this.parent.handleCreateComponentConfigRequest(
					null /* no user */, new CreateComponentConfigRequest(factoryPid, properties));
			response.get(60, TimeUnit.SECONDS);
			return lastResult;

		} catch (OpenemsNamedException | InterruptedException | ExecutionException | TimeoutException e) {
			this.parent.logError(this.log,
					"Unable to create Component configuration for Factory [" + factoryPid + "]: " + e.getMessage());
			e.printStackTrace();
			return true;
		}
	}

	/**
	 * Holds a configuration.
	 */
	private static class Config {
		protected static Config from(Configuration config) {
			return new Config(config.getFactoryPid());
		}

		protected final String factoryPid;

		private Config(String factoryPid) {
			this.factoryPid = factoryPid;
		}

		@Override
		public String toString() {
			return this.factoryPid;
		}
	}

	@Override
	protected int getCycleTime() {
		// initial cycle time
		return DefaultConfigurationWorker.INITIAL_WAIT_TIME;
	}

}
