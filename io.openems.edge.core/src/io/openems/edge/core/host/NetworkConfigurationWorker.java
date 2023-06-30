package io.openems.edge.core.host;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractWorker;

/**
 * This Worker reads the actual network configuration and stores it in the Host
 * configuration.
 */
public class NetworkConfigurationWorker extends AbstractWorker {

	private final Logger log = LoggerFactory.getLogger(NetworkConfigurationWorker.class);

	private final HostImpl parent;

	public NetworkConfigurationWorker(HostImpl parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() {
		try {
			var actualNetworkConfiguration = JsonUtils
					.prettyToString(this.parent.operatingSystem.getNetworkConfiguration().toJson());
			var persistedNetworkConfiguration = this.parent.config.networkConfiguration();

			if (!actualNetworkConfiguration.equals(persistedNetworkConfiguration)) {
				this.persistNetworkConfiguration(actualNetworkConfiguration);
			}

		} catch (OpenemsNamedException | IOException e) {
			this.parent.logError(this.log, "Unable to persist actual network configuration: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Reconfigure Parent to persist the actual network configuration.
	 *
	 * @param networkConfiguration the actual network configuration
	 * @throws IOException on error
	 */
	private void persistNetworkConfiguration(String networkConfiguration) throws IOException {
		var factoryPid = this.parent.serviceFactoryPid();
		final var config = this.parent.cm.getConfiguration(factoryPid, null);
		var properties = config.getProperties();
		if (properties == null) {
			// No 'Host' configuration existing yet -> create new configuration
			properties = new Hashtable<>();
		} else {
			// 'Host' configuration exists -> update configuration
		}
		properties.put("networkConfiguration", networkConfiguration);
		properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_BY, "Internal NetworkConfigurationWorker");
		properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_AT,
				LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
		config.update(properties);
	}

	@Override
	protected int getCycleTime() {
		return ALWAYS_WAIT_FOR_TRIGGER_NEXT_RUN;
	}

}
