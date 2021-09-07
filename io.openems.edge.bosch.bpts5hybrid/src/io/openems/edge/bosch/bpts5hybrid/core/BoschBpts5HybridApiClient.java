package io.openems.edge.bosch.bpts5hybrid.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class BoschBpts5HybridApiClient {

	private static final String POST_REQUEST_DATA = "action=get.hyb.overview&flow=1";
	private static final String REQUEST_LOG_BOOK_VIEW = "&action=get.logbookview&page=0&id=&type=BATTERY&dtype=";
	private static final String GET_VALUES_URL_PART = "/cgi-bin/ipcclient.fcgi?";
	private static String BASE_URL;
	private String wui_sid;
	private Integer pvLeistungWatt  = Integer.valueOf(0);
	private Integer soc = Integer.valueOf(0);
	private Integer einspeisung  = Integer.valueOf(0);
	private Integer batterieLadeStrom = Integer.valueOf(0);
	private Integer verbrauchVonPv = Integer.valueOf(0);
	private Integer verbrauchVonBatterie = Integer.valueOf(0);
	private Integer strombezugAusNetz = Integer.valueOf(0);
	private HttpClient httpClient;

	public BoschBpts5HybridApiClient(String ipaddress) {
		BASE_URL = "http://"+ipaddress;
		httpClient = new HttpClient();
		httpClient.setConnectTimeout(5000);
		httpClient.setFollowRedirects(true);
		try {
			httpClient.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		connect();
	}
	
	public void connect() {
		try {
			wui_sid = getWuiSidRequest();
		} catch (OpenemsNamedException e) {
			wui_sid = "";
			e.printStackTrace();
		}
	}

	private String getWuiSidRequest() throws OpenemsNamedException {
		try {
			ContentResponse response = httpClient.GET(BASE_URL);
			
			int status = response.getStatus();
			if(status < 300) {
				String body = response.getContentAsString();
				return extractWuiSidFromBody(body);
			} else {
				throw new OpenemsException(
						"Error while reading from Bosch BPT-S 5. Response code: " + status);
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new OpenemsException(
					"Unable to read from Bosch BPT-S 5. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private String extractWuiSidFromBody(String body) throws OpenemsException {
		int index = body.indexOf("WUI_SID=");
		
		if(index < 0) {
			throw new OpenemsException(
					"Error while extracting WUI_SID. Body was= " + body);
		}
		
		return body.substring(index + 9, index + 9 + 15);
	}

	public void retreiveValues() throws OpenemsException {
		Request postRequest = httpClient.POST(BASE_URL+GET_VALUES_URL_PART+wui_sid);
		postRequest.timeout(5, TimeUnit.SECONDS);
		postRequest.header(HttpHeader.CONTENT_TYPE, "text/plain");
		postRequest.content(new StringContentProvider(POST_REQUEST_DATA));
		
		ContentResponse response;
		
		try {
			response = postRequest.send();
			
			int status = response.getStatus();
			
			if (status < 300) {
				extractValuesFromAnswer(response.getContentAsString());
			} else {
				throw new OpenemsException(
						"Error while reading from Bosch BPT-S 5. Response code: " + status);
			}
		} catch (InterruptedException | TimeoutException | ExecutionException | OpenemsNamedException e) {
			throw new OpenemsException(
					"Unable to read from Bosch BPT-S 5. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
	public int retreiveBatterieStatus() throws OpenemsException {
		try {
			ContentResponse response = httpClient.GET(BASE_URL+GET_VALUES_URL_PART+wui_sid+REQUEST_LOG_BOOK_VIEW);
			
			int status = response.getStatus();
			if(status < 300) {
				String content = response.getContentAsString();
				Document document = Jsoup.parse(content);
				Element tableNode = document.select("table").get(0);
				Element firstRow = tableNode.select("tr").get(0);
				String firstRowText = firstRow.text();
				if(firstRowText.contains("Störung") && !firstRowText.contains("Keine")) {
					return 1;
				}
				else {
					return 0;
				}
			} else {
				throw new OpenemsException(
						"Error while reading from Bosch BPT-S 5. Response code: " + status);
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new OpenemsException(
					"Unable to read from Bosch BPT-S 5. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private void extractValuesFromAnswer(String body) throws OpenemsNamedException {
		if(body.contains("session invalid")) {
			getWuiSidRequest();
			return;
		}
		
		String[] values = body.split("\\|");
		
//		pvLeistungProzent = Integer.valueOf(values[1]);
		
		pvLeistungWatt = parseWattValue(values[2]);
		
		soc = Integer.valueOf(values[3]);
		
//		autarkieGrad = Float.valueOf(values[5]).floatValue();
		
//		currentOverallConsumption = parseWattValue(values[6]);
		
//		gridStatusString = values[7];
				
//		systemStatusString = values[9];
		
		batterieLadeStrom = parseWattValue(values[10]);
		
		einspeisung = parseWattValue(values[11]);
		
		verbrauchVonPv = parseWattValue(values[12]);
		
		verbrauchVonBatterie = parseWattValue(values[13]);
		
		if(values.length<15) {
			strombezugAusNetz = 0;
		}
		else {
			strombezugAusNetz = parseWattValue(values[14]);
		}
	}
	
	private Integer parseWattValue(String inputString) {
		if(inputString.trim().length() == 0 || inputString.contains("nbsp;")) {
			return Integer.valueOf(0);
		}
		
		String wattString = inputString.replace("kW", " ").replace("von"," ").trim();
		return Integer.valueOf((int) (Float.parseFloat(wattString) * 1000.0f));
	}

	public Integer getCurrentSoc() {
		return soc;
	}

	public Integer getCurrentChargePower() {
		return batterieLadeStrom;
	}

	public Integer getCurrentStromAusNetz() {
		return strombezugAusNetz;
	}

	public Integer getCurrentEinspeisung() {
		return einspeisung;
	}

	public Integer getCurrentDischargePower() {
		return verbrauchVonBatterie;
	}

	public Integer getCurrentPvProduction() {
		return pvLeistungWatt;
	}
	
	public Integer getCurrentVerbrauchVonPv() {
		return verbrauchVonPv;
	}
}
