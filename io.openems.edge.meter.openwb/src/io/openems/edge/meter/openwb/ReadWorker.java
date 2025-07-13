package io.openems.edge.meter.openwb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.URI;
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
import io.openems.edge.meter.api.ElectricityMeter;

public class ReadWorker extends AbstractCycleWorker {

	private static final String URL_GET_POWER = "power";
	private static final String URL_GET_POWERS = "powers";
	private static final String URL_GET_VOLTAGES = "voltages";
	private static final String URL_GET_CURRENTS = "currents";

	private final MeterOpenWb parent;
	private final String baseUrl;

	protected ReadWorker(MeterOpenWbImpl parent, Inet4Address ipAddress, int port)
			throws NoSuchAlgorithmException, KeyManagementException {
		this.parent = parent;
		this.baseUrl = "https://" + ipAddress.getHostAddress() + ":" + port + "/v1?topic=openWB/internal_chargepoint/0/get/";

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
		Integer activePower = null;
		Integer activePowerL1 = null;
		Integer activePowerL2 = null;
		Integer activePowerL3 = null;
		Integer voltageL1 = null;
		Integer voltageL2 = null;
		Integer voltageL3 = null;
		Integer currentL1 = null;
		Integer currentL2 = null;
		Integer currentL3 = null;

		final var communicationError = new AtomicBoolean(false);

			try {

				var voltageResponse = this.getResponse(URL_GET_VOLTAGES);
				var voltages = JsonUtils.getAsJsonArray(voltageResponse, "message");
				voltageL1 = JsonUtils.getAsInt(voltages.get(0)) * 1000;
				voltageL2 = JsonUtils.getAsInt(voltages.get(1)) * 1000;
				voltageL3 = JsonUtils.getAsInt(voltages.get(2)) * 1000;
				((ElectricityMeter) this.parent)._setVoltageL1(voltageL1);
				((ElectricityMeter) this.parent)._setVoltageL2(voltageL2);
				((ElectricityMeter) this.parent)._setVoltageL3(voltageL3);

				
				var powerResponse = this.getResponse(URL_GET_POWERS);
				var powers = JsonUtils.getAsJsonArray(powerResponse, "message");
				activePowerL1 = JsonUtils.getAsInt(powers.get(0));
				activePowerL2 = JsonUtils.getAsInt(powers.get(1));
				activePowerL3 = JsonUtils.getAsInt(powers.get(2));
				((ElectricityMeter) this.parent)._setActivePowerL1(activePowerL1);
				((ElectricityMeter) this.parent)._setActivePowerL2(activePowerL2);
				((ElectricityMeter) this.parent)._setActivePowerL3(activePowerL3);

				
				activePower = Math.round(JsonUtils.getAsFloat(this.getResponse(URL_GET_POWER), "message"));
				((ElectricityMeter) this.parent)._setActivePower(activePower);

				
				var currentResponse = this.getResponse(URL_GET_CURRENTS);
				var currents = JsonUtils.getAsJsonArray(currentResponse, "message");
				currentL1 = JsonUtils.getAsInt(currents.get(0)) * 1000;
				currentL2 = JsonUtils.getAsInt(currents.get(1)) * 1000;
				currentL3 = JsonUtils.getAsInt(currents.get(2)) * 1000;
				((ElectricityMeter) this.parent)._setCurrentL1(currentL1);
				((ElectricityMeter) this.parent)._setCurrentL2(currentL2);
				((ElectricityMeter) this.parent)._setCurrentL3(currentL3);

			} catch (OpenemsNamedException e) {
				communicationError.set(true);
			}


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
			var url = URI.create(this.baseUrl + path).toURL();
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
