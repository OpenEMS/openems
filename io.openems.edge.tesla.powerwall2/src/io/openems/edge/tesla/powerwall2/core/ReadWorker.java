package io.openems.edge.tesla.powerwall2.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractCycleWorker;

public class ReadWorker extends AbstractCycleWorker {

	private final String URL_SYSTEM_STATUS_SOE = "/system_status/soe";
	private final String URL_METERS_AGGREGATES = "/meters/aggregates";

	private final TeslaPowerwall2CoreImpl parent;
	private final String baseUrl;

	protected ReadWorker(TeslaPowerwall2CoreImpl parent, Inet4Address ipAddress, int port)
			throws NoSuchAlgorithmException, KeyManagementException {
		this.parent = parent;
		this.baseUrl = "https://" + ipAddress.getHostAddress() + ":" + port + "/api";

		/*
		 * Disable SSL certificate checking
		 */
		SSLContext context = SSLContext.getInstance("TLSv1.2");
		TrustManager[] trustManager = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			public void checkClientTrusted(X509Certificate[] certificate, String str) {
			}

			public void checkServerTrusted(X509Certificate[] certificate, String str) {
			}
		} };
		context.init(null, trustManager, new SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
	}

	@Override
	protected void forever() throws Throwable {
		final AtomicBoolean communicationError = new AtomicBoolean(false);

		this.parent.getBattery().ifPresent(battery -> {

			try {
				JsonObject soe = this.getResponse(URL_SYSTEM_STATUS_SOE);
				battery.getSoc().setNextValue(JsonUtils.getAsFloat(soe, "percentage"));

				JsonObject agg = this.getResponse(URL_METERS_AGGREGATES);
				JsonObject aggBattery = JsonUtils.getAsJsonObject(agg, "battery");
				float essActivePower = JsonUtils.getAsFloat(aggBattery, "instant_power");
				battery.getActivePower().setNextValue(essActivePower);
				float essReactivePower = JsonUtils.getAsFloat(aggBattery, "instant_reactive_power");
				battery.getReactivePower().setNextValue(essReactivePower);
				switch (battery.getPhase()) {
				case L1:
					battery.getActivePowerL1().setNextValue(essActivePower);
					battery.getActivePowerL2().setNextValue(0);
					battery.getActivePowerL3().setNextValue(0);
					battery.getReactivePowerL1().setNextValue(essActivePower);
					battery.getReactivePowerL2().setNextValue(0);
					battery.getReactivePowerL3().setNextValue(0);
					break;
				case L2:
					battery.getActivePowerL1().setNextValue(0);
					battery.getActivePowerL2().setNextValue(essActivePower);
					battery.getActivePowerL3().setNextValue(0);
					battery.getReactivePowerL1().setNextValue(0);
					battery.getReactivePowerL2().setNextValue(essActivePower);
					battery.getReactivePowerL3().setNextValue(0);
					break;
				case L3:
					battery.getActivePowerL1().setNextValue(0);
					battery.getActivePowerL2().setNextValue(0);
					battery.getActivePowerL3().setNextValue(essActivePower);
					battery.getReactivePowerL1().setNextValue(0);
					battery.getReactivePowerL2().setNextValue(0);
					battery.getReactivePowerL3().setNextValue(essActivePower);
					break;
				}
				battery.getActiveChargeEnergy().setNextValue(JsonUtils.getAsFloat(aggBattery, "energy_imported"));
				battery.getActiveDischargeEnergy().setNextValue(JsonUtils.getAsFloat(aggBattery, "energy_exported"));

			} catch (OpenemsNamedException e) {
				communicationError.set(true);
			}

		});

		this.parent.getSlaveCommunicationFailedChannel().setNextValue(communicationError.get());
	}

	/**
	 * Gets the JSON response of a HTTPS GET Request.
	 * 
	 * @param path the api path
	 * @return the JsonObject
	 * @throws OpenemsNamedException on error
	 */
	private JsonObject getResponse(String path) throws OpenemsNamedException {
		try {
			URL url = new URL(this.baseUrl + path);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setHostnameVerifier((hostname, session) -> {
				return true;
			});
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				String content = reader.lines().collect(Collectors.joining());
				return JsonUtils.parseToJsonObject(content);
			}
		} catch (IOException e) {
			throw new OpenemsException(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

}
