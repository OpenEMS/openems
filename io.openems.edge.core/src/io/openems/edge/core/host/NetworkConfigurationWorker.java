package io.openems.edge.core.host;

import java.io.IOException;
import java.util.Dictionary;

import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.worker.AbstractWorker;

/**
 * This Worker constantly checks if the disk is full. It may be extended in
 * future to check more Host related states.
 */
public class NetworkConfigurationWorker extends AbstractWorker {

	private final Logger log = LoggerFactory.getLogger(NetworkConfigurationWorker.class);

	private final HostImpl parent;

	public NetworkConfigurationWorker(HostImpl parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() {
		this.log.info("start network config");

		try {
			String actualNetworkConfiguration = this.parent.operatingSystem.getNetworkConfiguration().toJson()
					.toString();
			this.log.info("actualNetworkConfiguration " + actualNetworkConfiguration);

			String persistedNetworkConfiguration = this.parent.config.networkConfiguration();
			this.log.info("persistedNetworkConfiguration " + persistedNetworkConfiguration);

			if (!actualNetworkConfiguration.equals(persistedNetworkConfiguration)) {
				this.log.info("not equals");
				this.persistNetworkConfiguration(actualNetworkConfiguration);

				this.parent.logInfo(this.log, "Persisted actual network configuration");
			} else {

				this.parent.logInfo(this.log, "equals");
			}

		} catch (OpenemsNamedException | IOException e) {
			this.parent.logError(this.log, "Unable to persist actual network configuration: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Reconfigure Parent to persist the actual network configuration
	 * 
	 * @param actualNetworkConfiguration the actual network configuration
	 * @throws IOException on error
	 */
	private void persistNetworkConfiguration(String actualNetworkConfiguration) throws IOException {
		Configuration c;
		c = this.parent.cm.getConfiguration(this.parent.servicePid(), "?");
		Dictionary<String, Object> properties = c.getProperties();
		properties.put("networkConfiguration", actualNetworkConfiguration);
		c.update(properties);
	}

	@Override
	protected int getCycleTime() {
		return ALWAYS_WAIT_FOR_TRIGGER_NEXT_RUN;
	}

}
