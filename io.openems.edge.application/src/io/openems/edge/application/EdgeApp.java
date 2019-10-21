package io.openems.edge.application;

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

import info.faljse.SDNotify.SDNotify;
import io.openems.common.OpenemsConstants;

@Component(immediate = true)
public class EdgeApp {

	private final Logger log = LoggerFactory.getLogger(EdgeApp.class);

	@Reference
	ConfigurationAdmin cm;

	@Activate
	void activate() {
		String message = "OpenEMS version [" + OpenemsConstants.VERSION + "] started";
		String line = Strings.repeat("=", message.length());
		this.log.info(line);
		this.log.info(message);
		this.log.info(line);

		Configuration config;
		try {
			config = cm.getConfiguration("org.ops4j.pax.logging", null);
			Hashtable<String, Object> log4j = new Hashtable<>();
			log4j.put("log4j.rootLogger", "INFO, CONSOLE, osgi:*");
			log4j.put("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
			log4j.put("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
			log4j.put("log4j.appender.CONSOLE.layout.ConversionPattern", "%d{ISO8601} [%-8.8t] %-5p [%-30.30c] %m%n");
			// set minimum log levels for some verbose packages
			log4j.put("log4j.logger.org.eclipse.osgi", "WARN");
			log4j.put("log4j.logger.org.apache.felix.configadmin", "INFO");
			log4j.put("log4j.logger.sun.net.www.protocol.http.HttpURLConnection", "INFO");
			log4j.put("log4j.logger.com.ghgande.j2mod", "INFO");
			log4j.put("log4j.logger.io.openems.edge.ess.streetscooter", "DEBUG");
			log4j.put("log4j.logger.io.openems.edge.ess.power", "INFO");
			config.update(log4j);
		} catch (IOException | SecurityException e) {
			e.printStackTrace();
		}

		// Announce Operating System that OpenEMS Edge started
		if (SDNotify.isAvailable()) {
			SDNotify.sendNotify();
		}
	}

	@Deactivate
	void deactivate() {
		this.log.debug("Deactivate EdgeApp");
	} 

}
