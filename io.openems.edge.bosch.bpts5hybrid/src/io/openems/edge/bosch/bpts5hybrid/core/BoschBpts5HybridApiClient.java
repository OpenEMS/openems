package io.openems.edge.bosch.bpts5hybrid.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.jsoup.Jsoup;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

public class BoschBpts5HybridApiClient {

	private static final String POST_REQUEST_DATA = "action=get.hyb.overview&flow=1";
	private static final String REQUEST_LOG_BOOK_VIEW = "&action=get.logbookview&page=0&id=&type=BATTERY&dtype=";
	private static final String GET_VALUES_URL_PART = "/cgi-bin/ipcclient.fcgi?";
	private static String BASE_URL;
	private String wuiSid;
	private Integer pvLeistungWatt = Integer.valueOf(0);
	private Integer soc = Integer.valueOf(0);
	private Integer einspeisung = Integer.valueOf(0);
	private Integer batterieLadeStrom = Integer.valueOf(0);
	private Integer verbrauchVonPv = Integer.valueOf(0);
	private Integer verbrauchVonBatterie = Integer.valueOf(0);
	private Integer strombezugAusNetz = Integer.valueOf(0);
	private final HttpClient httpClient;

	public BoschBpts5HybridApiClient(String ipaddress) {
		BASE_URL = "http://" + ipaddress;
		this.httpClient = new HttpClient();
		this.httpClient.setConnectTimeout(5000);
		this.httpClient.setFollowRedirects(true);
		try {
			this.httpClient.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.connect();
	}

	protected void connect() {
		try {
			this.wuiSid = this.getWuiSidRequest();
		} catch (OpenemsNamedException e) {
			this.wuiSid = "";
			e.printStackTrace();
		}
	}

	private String getWuiSidRequest() throws OpenemsNamedException {
		try {
			var response = this.httpClient.GET(BASE_URL);

			var status = response.getStatus();
			if (status < 300) {
				var body = response.getContentAsString();
				return this.extractWuiSidFromBody(body);
			}
			throw new OpenemsException("Error while reading from Bosch BPT-S 5. Response code: " + status);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new OpenemsException(
					"Unable to read from Bosch BPT-S 5. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private String extractWuiSidFromBody(String body) throws OpenemsException {
		var index = body.indexOf("WUI_SID=");

		if (index < 0) {
			throw new OpenemsException("Error while extracting WUI_SID. Body was= " + body);
		}

		return body.substring(index + 9, index + 9 + 15);
	}

	protected void retreiveValues() throws OpenemsException {
		var postRequest = this.httpClient.POST(BASE_URL + GET_VALUES_URL_PART + this.wuiSid);
		postRequest.timeout(5, TimeUnit.SECONDS);
		postRequest.header(HttpHeader.CONTENT_TYPE, "text/plain");
		postRequest.content(new StringContentProvider(POST_REQUEST_DATA));

		ContentResponse response;

		try {
			response = postRequest.send();

			var status = response.getStatus();

			if (status >= 300) {
				throw new OpenemsException("Error while reading from Bosch BPT-S 5. Response code: " + status);
			}
			this.extractValuesFromAnswer(response.getContentAsString());
		} catch (InterruptedException | TimeoutException | ExecutionException | OpenemsNamedException e) {
			throw new OpenemsException(
					"Unable to read from Bosch BPT-S 5. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	protected int retreiveBatterieStatus() throws OpenemsException {
		try {
			var response = this.httpClient.GET(BASE_URL + GET_VALUES_URL_PART + this.wuiSid + REQUEST_LOG_BOOK_VIEW);

			var status = response.getStatus();
			if (status >= 300) {
				throw new OpenemsException("Error while reading from Bosch BPT-S 5. Response code: " + status);
			}
			var content = response.getContentAsString();
			var document = Jsoup.parse(content);
			var tableNode = document.select("table").get(0);
			var firstRow = tableNode.select("tr").get(0);
			var firstRowText = firstRow.text();
			if (firstRowText.contains("Störung") && !firstRowText.contains("Keine")) {
				return 1;
			} else {
				return 0;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new OpenemsException(
					"Unable to read from Bosch BPT-S 5. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private void extractValuesFromAnswer(String body) throws OpenemsNamedException {
		if (body.contains("session invalid")) {
			this.getWuiSidRequest();
			return;
		}

		var values = body.split("\\|");

		this.pvLeistungWatt = this.parseWattValue(values[2]);

		this.soc = Integer.valueOf(values[3]);

		this.batterieLadeStrom = this.parseWattValue(values[10]);

		this.einspeisung = this.parseWattValue(values[11]);

		this.verbrauchVonPv = this.parseWattValue(values[12]);

		this.verbrauchVonBatterie = this.parseWattValue(values[13]);

		if (values.length < 15) {
			this.strombezugAusNetz = 0;
		} else {
			this.strombezugAusNetz = this.parseWattValue(values[14]);
		}
	}

	private Integer parseWattValue(String inputString) {
		if (inputString.trim().length() == 0 || inputString.contains("nbsp;")) {
			return Integer.valueOf(0);
		}

		var wattString = inputString.replace("kW", " ").replace("von", " ").trim();
		return Integer.valueOf((int) (Float.parseFloat(wattString) * 1000.0f));
	}

	public Integer getCurrentSoc() {
		return this.soc;
	}

	public Integer getCurrentChargePower() {
		return this.batterieLadeStrom;
	}

	public Integer getCurrentStromAusNetz() {
		return this.strombezugAusNetz;
	}

	public Integer getCurrentEinspeisung() {
		return this.einspeisung;
	}

	public Integer getCurrentDischargePower() {
		return this.verbrauchVonBatterie;
	}

	public Integer getCurrentPvProduction() {
		return this.pvLeistungWatt;
	}

	public Integer getCurrentVerbrauchVonPv() {
		return this.verbrauchVonPv;
	}
}
