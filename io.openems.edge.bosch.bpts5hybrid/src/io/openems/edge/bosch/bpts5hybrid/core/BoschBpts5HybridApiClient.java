package io.openems.edge.bosch.bpts5hybrid.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class BoschBpts5HybridApiClient {

	private static final String GET_VALUES_URL_PART = "/cgi-bin/ipcclient.fcgi?";
	private static String BASE_URL;
	private String wui_sid;
	private byte[] requestBytes = "action=get.hyb.overview&flow=1".getBytes();
	private Integer pvLeistungWatt  = Integer.valueOf(0);
	private Integer soc = Integer.valueOf(0);
	private Integer einspeisung  = Integer.valueOf(0);
	private Integer batterieLadeStrom = Integer.valueOf(0);
	private Integer verbrauchVonPv = Integer.valueOf(0);
	private Integer verbrauchVonBatterie = Integer.valueOf(0);
	private Integer strombezugAusNetz = Integer.valueOf(0);

	public BoschBpts5HybridApiClient(String ipaddress) {
		BASE_URL = "http://"+ipaddress;
		try {
			wui_sid = getWuiSidRequest();
		} catch (OpenemsNamedException e) {
			wui_sid = "";
			e.printStackTrace();
		}
	}

	private String getWuiSidRequest() throws OpenemsNamedException {
		try {
			URL url = new URL(BASE_URL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			int status = con.getResponseCode();
			String body;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				// Read HTTP response
				StringBuilder content = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					content.append(line);
					content.append(System.lineSeparator());
				}
				body = content.toString();
			}
			if (status < 300) {
				return extractWuiSidFromBody(body);
			} else {
				throw new OpenemsException(
						"Error while reading from Bosch BPT-S 5. Response code: " + status + ". " + body);
			}
		} catch (OpenemsNamedException | IOException e) {
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
		try {
			URL url = new URL(BASE_URL+GET_VALUES_URL_PART+wui_sid);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "text/plan");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			con.setDoOutput(true);
			
			try(OutputStream os = con.getOutputStream()) {
			    os.write(requestBytes, 0, requestBytes.length);			
			}
			
			int status = con.getResponseCode();
			String body;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				// Read HTTP response
				StringBuilder content = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					content.append(line);
					content.append(System.lineSeparator());
				}
				body = content.toString();
			}
			if (status < 300) {
				extractValuesFromAnswer(body);
			} else {
				throw new OpenemsException(
						"Error while reading from Bosch BPT-S 5. Response code: " + status + ". " + body);
			}
		} catch (OpenemsNamedException | IOException e) {
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
