package io.openems.backend.application;

import java.io.IOException;
import java.util.Hashtable;

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

		Configuration config;
		try {
			config = this.cm.getConfiguration("org.ops4j.pax.logging", null);
			final var properties = config.getProperties();
			if (properties == null || properties.isEmpty() || properties.get("log4j2.rootLogger.level") == null) {
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
		} catch (IOException | SecurityException e) {
			e.printStackTrace();
		}
	}

	@Deactivate
	private void deactivate() {
		this.log.info("Deactivate BackendApp");
	}

}
