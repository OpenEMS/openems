package io.openems.backend.edge.application;

import java.io.IOException;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;

import com.google.common.base.Strings;

import io.openems.common.OpenemsConstants;

public class Utils {

	protected static void logWelcomeMessage(Logger log) {
		final var message = "OpenEMS Backend Edge Application version [" + OpenemsConstants.VERSION + "] started";
		final var line = Strings.repeat("=", message.length());
		log.info(line);
		log.info(message);
		log.info(line);
	}

	protected static void configureLogger(ConfigurationAdmin cm) throws IOException, SecurityException {
		Configuration config = cm.getConfiguration("org.ops4j.pax.logging", null);
		final var properties = config.getProperties();

		if (properties != null && !properties.isEmpty() && properties.get("log4j2.rootLogger.level") != null) {
			return; // Logger already configured
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
		config.update(log4j);
	}
}
