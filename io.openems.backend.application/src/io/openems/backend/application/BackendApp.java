package io.openems.backend.application;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Optional;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.openems.common.OpenemsConstants;
import io.openems.common.utils.DictionaryUtils;

@Component(immediate = true)
public class BackendApp {

	private final Logger log = LoggerFactory.getLogger(BackendApp.class);

	@Reference
	private ConfigurationAdmin cm;

	@Activate
	private void activate() {
		final var message = "OpenEMS Backend version [" + OpenemsConstants.VERSION + "] started";
		final var line = Strings.repeat("=", message.length());
		this.log.info(line);
		this.log.info(message);
		this.log.info(line);

		final Configuration config;
		try {
            config = this.cm.getConfiguration("org.ops4j.pax.logging", null);
		} catch (IOException | SecurityException e) {
			this.log.error("Failed to get logging configuration for org.ops4j.pax.logging: {}", e.getMessage());
			return;
		}
		
		final var configUpdate = BackendApp.getConfigUpdate(config);
		configUpdate.ifPresent(c -> {
			try {
				config.update(c);
			} catch (IOException e) {	
				this.log.error("Failed to update logging configuration for org.ops4j.pax.logging: {}", e.getMessage());
			}
		});
	}
	
	/**
	 * Returns the configuration update for the logging configuration.
	 * If the configuration already contains a root logger level, it returns an empty Optional.
	 * 
	 * @param config the configuration to update
	 * @return an Optional containing the configuration update if needed, otherwise an empty Optional
	 */
	public static Optional<Dictionary<String, ?>> getConfigUpdate(Configuration config) {
		Objects.requireNonNull(config, "Configuration must not be null");
		
		final var properties = config.getProperties();
		if (DictionaryUtils.containsAnyKey(properties, "log4j2.rootLogger.level")) {
			return Optional.empty();
		}
		
		final var log4j = new Hashtable<String, Object>();
		log4j.put("log4j2.appender.console.type", "Console");
		log4j.put("log4j2.appender.console.name", "console");
		log4j.put("log4j2.appender.console.layout.type", "PatternLayout");
		log4j.put("log4j2.appender.console.layout.pattern", "%d{ISO8601} [%-8.8t] %-5p [%-30.30c] %m%n");

		log4j.put("log4j2.appender.paxosgi.type", "PaxOsgi");
		log4j.put("log4j2.appender.paxosgi.name", "paxosgi");

		log4j.put("log4j2.rootLogger.level", "INFO");
		log4j.put("log4j2.rootLogger.appenderRef.console.ref", "console");
		log4j.put("log4j2.rootLogger.appenderRef.paxosgi.ref", "paxosgi");
		return Optional.of(log4j);
	}

	@Deactivate
	private void deactivate() {
		this.log.info("Deactivate BackendApp");
	}

}
