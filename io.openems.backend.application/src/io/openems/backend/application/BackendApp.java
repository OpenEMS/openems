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

@Component()
public class BackendApp {

	private final Logger log = LoggerFactory.getLogger(BackendApp.class);

	@Reference
	private ConfigurationAdmin configAdmin;

	@Activate
	void activate() {
		log.debug("Activate BackendApp");
		configureLogging();
	}

	private void configureLogging() {
		Configuration configuration;
		try {
			configuration = configAdmin.getConfiguration("org.ops4j.pax.logging", null);
			final Hashtable<String, Object> log4jProps = new Hashtable<String, Object>();
			log4jProps.put("log4j.rootLogger", "DEBUG, CONSOLE");
			log4jProps.put("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
			log4jProps.put("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
			log4jProps.put("log4j.appender.CONSOLE.layout.ConversionPattern",
					"%d{ISO8601} [%-8.8t] %-5p [%-30.30c] - %m%n");
			// set minimum log levels for some verbose packages
			log4jProps.put("log4j.logger.org.eclipse.osgi", "WARN");
			log4jProps.put("log4j.logger.org.apache.felix.configadmin", "INFO");
			log4jProps.put("log4j.logger.sun.net.www.protocol.http.HttpURLConnection", "INFO");
			configuration.update(log4jProps);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@Deactivate
	void deactivate() {
		log.debug("Deactivate BackendApp");
	}

}
