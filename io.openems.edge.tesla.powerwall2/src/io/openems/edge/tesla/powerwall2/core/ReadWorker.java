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

	private static final String URL_SYSTEM_STATUS_SOE = "/system_status/soe";
	private static final String URL_METERS_AGGREGATES = "/meters/aggregates";

	private final TeslaPowerwall2CoreImpl parent;
	private final String baseUrl;

	protected ReadWorker(TeslaPowerwall2CoreImpl parent, Inet4Address ipAddress, int port)
			throws NoSuchAlgorithmException, KeyManagementException {
		this.parent = parent;
		this.baseUrl = "https://" + ipAddress.getHostAddress() + ":" + port + "/api";

		/*
		 * Disable SSL certificate checking
		 */
		var context = SSLContext.getInstance("TLSv1.2");
		TrustManager[] trustManager = { new X509TrustManager() {
			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certificate, String str) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certificate, String str) {
			}
		} };
		context.init(null, trustManager, new SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
	}

	@Override
	protected void forever() throws Throwable {
		final var communicationError = new AtomicBoolean(false);

		this.parent.getBattery().ifPresent(battery -> {

			try {
				var soe = this.getResponse(URL_SYSTEM_STATUS_SOE);
				battery._setSoc(Math.round(JsonUtils.getAsFloat(soe, "percentage")));

				var agg = this.getResponse(URL_METERS_AGGREGATES);
				var aggBattery = JsonUtils.getAsJsonObject(agg, "battery");
				var essActivePower = JsonUtils.getAsFloat(aggBattery, "instant_power");
				battery._setActivePower(Math.round(essActivePower));
				var essReactivePower = JsonUtils.getAsFloat(aggBattery, "instant_reactive_power");
				battery._setReactivePower(Math.round(essReactivePower));
				switch (battery.getPhase()) {
				case L1:
					battery._setActivePowerL1(Math.round(essActivePower));
					battery._setActivePowerL2(0);
					battery._setActivePowerL3(0);
					battery._setReactivePowerL1(Math.round(essActivePower));
					battery._setReactivePowerL2(0);
					battery._setReactivePowerL3(0);
					break;
				case L2:
					battery._setActivePowerL1(0);
					battery._setActivePowerL2(Math.round(essActivePower));
					battery._setActivePowerL3(0);
					battery._setReactivePowerL1(0);
					battery._setReactivePowerL2(Math.round(essActivePower));
					battery._setReactivePowerL3(0);
					break;
				case L3:
					battery._setActivePowerL1(0);
					battery._setActivePowerL2(0);
					battery._setActivePowerL3(Math.round(essActivePower));
					battery._setReactivePowerL1(0);
					battery._setReactivePowerL2(0);
					battery._setReactivePowerL3(Math.round(essActivePower));
					break;
				}
				battery._setActiveChargeEnergy(Math.round(JsonUtils.getAsFloat(aggBattery, "energy_imported")));
				battery._setActiveDischargeEnergy(Math.round(JsonUtils.getAsFloat(aggBattery, "energy_exported")));

			} catch (OpenemsNamedException e) {
				communicationError.set(true);
			}

		});

		this.parent._setSlaveCommunicationFailed(communicationError.get());
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
			var url = new URL(this.baseUrl + path);
			var connection = (HttpsURLConnection) url.openConnection();
			connection.setHostnameVerifier((hostname, session) -> true);
			try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				var content = reader.lines().collect(Collectors.joining());
				return JsonUtils.parseToJsonObject(content);
			}
		} catch (IOException e) {
			throw new OpenemsException(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

}
