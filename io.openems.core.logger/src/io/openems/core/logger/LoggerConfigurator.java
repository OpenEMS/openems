package io.openems.core.logger;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import io.openems.common.logger.ContextLogger;
import io.openems.common.utils.DictionaryUtils;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Core.Logger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.OPTIONAL, //
		property = { //
				"enabled=true" //
		})
public class LoggerConfigurator {

	private final Logger log = new ContextLogger(LoggerConfigurator.class, "LoggerConfigurator");

	@Reference
	private ConfigurationAdmin cm;

	@Activate
	private void activate(Config logConfig) {
		final Configuration config;
		try {
			config = this.cm.getConfiguration("org.ops4j.pax.logging", null);
		} catch (IOException e) {
			this.log.error("Failed to get logging configuration", e);
			return;
		}

		final var log4j = getCurrentConfiguration(config, logConfig);

		if (log4j.isEmpty()) {
			this.log.debug("Logging configuration is up to date, no changes applied.");
			return;
		}

		try {
			config.update(log4j.get());
		} catch (IOException e) {
			this.log.error("Failed to update logging configuration", e);
		}
	}

	static Optional<Dictionary<String, Object>> getCurrentConfiguration(Configuration config, Config logConfig) {
		Optional<String> logConfigPath = Optional.ofNullable(logConfig.path()).map(s -> s.isBlank() ? null : s);

		if (logConfigPath.isEmpty()) {
			return defaultConfiguration(config);
		} else {
			return fileConfiguration(config, logConfigPath.get());
		}
	}

	static Optional<Dictionary<String, Object>> defaultConfiguration(Configuration config) {
		if (DictionaryUtils.containsAnyKey(config.getProperties(), "log4j2.rootLogger.level")) {
			return Optional.empty();
		}
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

		return Optional.of(log4j);
	}

	static Optional<Dictionary<String, Object>> fileConfiguration(Configuration config, String xmlConfigFile) {
		final var file = DictionaryUtils.getAsOptionalString(config.getProperties(),
				"org.ops4j.pax.logging.log4j2.config.file");

		if (file.isPresent() && file.get().equals(xmlConfigFile)) {
			return Optional.empty();
		}
		var log4j = new Hashtable<String, Object>();
		log4j.put("org.ops4j.pax.logging.log4j2.config.file", xmlConfigFile);

		return Optional.of(log4j);
	}
}
