package io.openems.backend;

import java.io.File;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.browserwebsocket.BrowserWebsocket;
import io.openems.backend.metadata.Metadata;
import io.openems.backend.openemswebsocket.OpenemsWebsocket;
import io.openems.backend.restapi.RestApi;
import io.openems.backend.timedata.Timedata;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.EnvUtils;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
		log.info("OpenEMS-Backend starting...");

		// Configure everything
		try {
			initMetadataProvider();
			initTimedataProvider();
			initOpenemsWebsocket();
			initBrowserWebsocket();
			initRestApi();

			log.info("OpenEMS Backend started.");
			log.info("================================================================================");
		} catch (OpenemsException e) {
			log.error("OpenEMS Backend failed to start: " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Configures a metadata provider. It uses either Odoo as backend or a simple Dummy provider.
	 *
	 * @throws Exception
	 */
	private static void initMetadataProvider() throws OpenemsException {
		Optional<String> metadataOpt = EnvUtils.getAsOptionalString("METADATA");
		if (metadataOpt.isPresent() && metadataOpt.get().equals("DUMMY")) {
			log.info("Start Dummy Metadata provider");
			Metadata.initializeDummy();
		} else if (metadataOpt.isPresent() && metadataOpt.get().equals("FILE")) {
			log.info("Start FILE Metadata provider");
			File file = new File(EnvUtils.getAsString("METADATA_FILE"));
			Metadata.initializeFile(file);
		} else {
			int port = EnvUtils.getAsInt("ODOO_PORT");
			String url = EnvUtils.getAsString("ODOO_URL");
			String database = EnvUtils.getAsString("ODOO_DATABASE");
			log.info("Connect to Odoo. Url [" + url + ":" + port + "] Database [" + database + "]");
			String username = EnvUtils.getAsString("ODOO_USERNAME");
			String password = EnvUtils.getAsString("ODOO_PASSWORD");
			Metadata.initializeOdoo(url, port, database, username, password);
		}
	}

	private static void initTimedataProvider() throws OpenemsException {
		Optional<String> timedataOpt = EnvUtils.getAsOptionalString("TIMEDATA");
		if (timedataOpt.isPresent() && timedataOpt.get().equals("DUMMY")) {
			log.info("Start Dummy Timedata provider");
			Timedata.initializeDummy();
		} else {
			int port = Integer.valueOf(System.getenv("INFLUX_PORT"));
			String url = EnvUtils.getAsString("INFLUX_URL");
			String database = EnvUtils.getAsString("INFLUX_DATABASE");
			log.info("Connect to InfluxDB. Url [" + url + ":" + port + "], Database [" + database + "]");
			String username = EnvUtils.getAsString("INFLUX_USERNAME");
			String password = EnvUtils.getAsString("INFLUX_PASSWORD");
			Timedata.initializeInfluxdb(database, url, port, username, password);
		}
	}

	private static void initOpenemsWebsocket() throws OpenemsException {
		int port = EnvUtils.getAsInt("OPENEMS_WEBSOCKET_PORT");
		log.info("Start OpenEMS Websocket server on port [" + port + "]");
		OpenemsWebsocket.initialize(port);
	}

	private static void initBrowserWebsocket() throws OpenemsException {
		int port = EnvUtils.getAsInt("BROWSER_WEBSOCKET_PORT");
		log.info("Start Browser Websocket server on port [" + port + "]");
		BrowserWebsocket.initialize(port);
	}

	private static void initRestApi() throws OpenemsException {
		int port = EnvUtils.getAsInt("REST_API_PORT");
		log.info("Start Rest-Api server on port [" + port + "]");
		RestApi.initialize(port);
	}
}
