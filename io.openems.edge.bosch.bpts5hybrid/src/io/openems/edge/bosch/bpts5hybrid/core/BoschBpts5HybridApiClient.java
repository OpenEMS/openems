package io.openems.edge.bosch.bpts5hybrid.core;

import org.jsoup.Jsoup;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

public class BoschBpts5HybridApiClient {

	protected static final String GET_VALUES_URL_PART = "/cgi-bin/ipcclient.fcgi?";

	private static final String POST_REQUEST_DATA = "action=get.hyb.overview&flow=1";
	private static final String REQUEST_LOG_BOOK_VIEW = "&action=get.logbookview&page=0&id=&type=BATTERY&dtype=";

	private String wuiSid = "";
	private Integer pvLeistungWatt = Integer.valueOf(0);
	private Integer soc = Integer.valueOf(0);
	private Integer einspeisung = Integer.valueOf(0);
	private Integer batterieLadeStrom = Integer.valueOf(0);
	private Integer verbrauchVonPv = Integer.valueOf(0);
	private Integer verbrauchVonBatterie = Integer.valueOf(0);
	private Integer strombezugAusNetz = Integer.valueOf(0);

	/**
	 * Extract WUI_SID from initial page response.
	 *
	 * @param body the HTML body
	 * @throws OpenemsException on error
	 */
	public void processConnectResponse(String body) throws OpenemsException {
		this.wuiSid = this.extractWuiSidFromBody(body);
	}

	/**
	 * Gets the URL for value retrieval (POST).
	 *
	 * @param baseUrl the base URL
	 * @return the full URL
	 */
	public String getValuesUrl(String baseUrl) {
		return baseUrl + GET_VALUES_URL_PART + this.wuiSid;
	}

	/**
	 * Gets the URL for battery status retrieval (GET).
	 *
	 * @param baseUrl the base URL
	 * @return the full URL
	 */
	public String getBatteryStatusUrl(String baseUrl) {
		return baseUrl + GET_VALUES_URL_PART + this.wuiSid + REQUEST_LOG_BOOK_VIEW;
	}

	/**
	 * Gets the POST data for value retrieval.
	 *
	 * @return the POST data string
	 */
	public static String getPostRequestData() {
		return POST_REQUEST_DATA;
	}

	/**
	 * Returns true if WUI_SID has been obtained.
	 *
	 * @return true if connected
	 */
	public boolean isConnected() {
		return this.wuiSid != null && !this.wuiSid.isEmpty();
	}

	private String extractWuiSidFromBody(String body) throws OpenemsException {
		var index = body.indexOf("WUI_SID=");

		if (index < 0) {
			throw new OpenemsException("Error while extracting WUI_SID. Body was= " + body);
		}

		return body.substring(index + 9, index + 9 + 15);
	}

	/**
	 * Process the values response body.
	 *
	 * @param body the response body
	 * @throws OpenemsNamedException on error
	 */
	public void processValuesResponse(String body) throws OpenemsNamedException {
		this.extractValuesFromAnswer(body);
	}

	/**
	 * Process the battery status response body.
	 *
	 * @param body the response body
	 * @return 0 = OK, 1 = error
	 */
	public int processBatteryStatusResponse(String body) {
		var document = Jsoup.parse(body);
		var tableNode = document.select("table").get(0);
		var firstRow = tableNode.select("tr").get(0);
		var firstRowText = firstRow.text();
		if (firstRowText.contains("Störung") && !firstRowText.contains("Keine")) {
			return 1;
		}
		return 0;
	}

	private void extractValuesFromAnswer(String body) throws OpenemsNamedException {
		if (body.contains("session invalid")) {
			this.wuiSid = ""; // force reconnect
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
