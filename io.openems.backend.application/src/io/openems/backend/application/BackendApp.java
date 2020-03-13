package io.openems.backend.application;

import java.io.IOException;
import java.util.Dictionary;
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
	ConfigurationAdmin cm;

	@Activate
	void activate() {
		String message = "OpenEMS Backend version [" + OpenemsConstants.VERSION + "] started";
		String line = Strings.repeat("=", message.length());
		log.info(line);
		log.info(message);
		log.info(line);

		Configuration config;
		try {
			config = cm.getConfiguration("org.ops4j.pax.logging", null);
			Dictionary<String, Object> properties = config.getProperties();
			if (properties.isEmpty()) {
				Hashtable<String, Object> log4j = new Hashtable<>();
				log4j.put("log4j.rootLogger", "INFO, CONSOLE, osgi:*");
				log4j.put("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
				log4j.put("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
				log4j.put("log4j.appender.CONSOLE.layout.ConversionPattern",
						"%d{ISO8601} [%-8.8t] %-5p [%-30.30c] %m%n");
				log4j.put("log4j.logger.org.eclipse.osgi", "WARN");
				config.update(log4j);
			}
		} catch (IOException | SecurityException e) {
			e.printStackTrace();
		}
	}

	@Deactivate
	void deactivate() {
		log.info("Deactivate BackendApp");
	}

}
