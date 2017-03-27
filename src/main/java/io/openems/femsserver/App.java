package io.openems.femsserver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.femsserver.browserwebsocket.BrowserWebsocket;
import io.openems.femsserver.femswebsocket.FemsWebsocket;
import io.openems.femsserver.influx.Influxdb;
import io.openems.femsserver.odoo.Odoo;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		log.info("FEMS-Server starting...");

		// Configure everything
		Properties config = getConfig();
		initOdoo(config);
		initInfluxdb(config);
		initFemsWebsocket(config);
		initBrowserWebsocket(config);

		log.info("FEMS-Server started.");
	}

	private static Properties getConfig() throws IOException {
		log.info("Read config");
		Path configLocation = Paths.get("config.properties");
		try (InputStream stream = Files.newInputStream(configLocation)) {
			Properties config = new Properties();
			config.load(stream);
			return config;
		}
	}

	private static void initOdoo(Properties config) throws Exception {
		int port = Integer.valueOf(config.getProperty("odoo.port"));
		log.info("Connect to Odoo on port [" + port + "]");
		String url = config.getProperty("odoo.url");
		String database = config.getProperty("odoo.database");
		String username = config.getProperty("odoo.username");
		String password = config.getProperty("odoo.password");
		Odoo.initialize(url, port, database, username, password);
	}

	private static void initInfluxdb(Properties config) throws Exception {
		int port = Integer.valueOf(config.getProperty("influx.port"));
		log.info("Connect to InfluxDB on port [" + port + "]");
		String database = config.getProperty("influx.database");
		String url = config.getProperty("influx.url");
		String username = config.getProperty("influx.username");
		String password = config.getProperty("influx.password");
		Influxdb.initialize(database, url, port, username, password);
	}

	private static void initFemsWebsocket(Properties config) throws Exception {
		int port = Integer.valueOf(config.getProperty("femswebsocket.port"));
		log.info("Start FEMS Websocket server on port [" + port + "]");
		FemsWebsocket.initialize(port);
	}

	private static void initBrowserWebsocket(Properties config) throws Exception {
		int port = Integer.valueOf(config.getProperty("browserwebsocket.port"));
		log.info("Start Browser Websocket server on port [" + port + "]");
		BrowserWebsocket.initialize(port);
	}
}
