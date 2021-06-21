package io.openems.edge.predictor.solcast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class SolcastAPI {
	
	private final String Url;
	private LocalTime starttime;
	private LocalTime endtime;
	private boolean LimitedAPI;
	private boolean debugMode;
	private String debugFile;

	public SolcastAPI(String URL, String Starttime, String Endtime, boolean Limit, boolean debugMode, String debugFile) {
		this.Url = URL;
		this.starttime = LocalTime.parse(Starttime);
		this.endtime = LocalTime.parse(Endtime);
		this.LimitedAPI = Limit;
		this.debugMode = debugMode;
		this.debugFile = debugFile;
	}

	/**
	 * Gets the solar-forecast from Solcast.com
	 * 
	 * See https://solcast.com/solar-data-api/
	 * 
	 * @param index hours of prediction
	 * @return the boolean value
	 * @throws OpenemsNamedException on error
	 */
	public JsonArray getSolarForecast(int hours) throws OpenemsNamedException {
		LocalTime current = LocalTime.now();
		if (!LimitedAPI || (current.isAfter(starttime) && current.isBefore(endtime) || debugMode)) {
			
			JsonObject json = new JsonObject();
			if (!debugMode) {
				json = this.sendGetRequest(this.Url);
			}
							
			try {
				if (debugMode) {
					JsonParser parser = new JsonParser();
					Object obj = parser.parse(new FileReader(debugFile));
					json = (JsonObject) obj;
				}				
				
				JsonArray forecasts = (JsonArray) json.get("forecasts");			
				JsonArray weatherPrediction = new JsonArray();				
				
				for (Integer i = 0; i < 2*hours; i++) {	
					JsonElement dt = forecasts.get(i+1).getAsJsonObject().get("period_end");
					float solar = forecasts.get(i).getAsJsonObject().get("pv_estimate").getAsFloat();
					float solar10 = forecasts.get(i).getAsJsonObject().get("pv_estimate10").getAsFloat();
					float solar90 = forecasts.get(i).getAsJsonObject().get("pv_estimate90").getAsFloat();
					// convert kW to W				 
					JsonObject jsolar = forecasts.get(i).getAsJsonObject();
					jsolar.remove("pv_estimate");
					jsolar.addProperty("pv_estimate", (int)(solar*1000));
					jsolar.remove("pv_estimate10");
					jsolar.addProperty("pv_estimate10", (int)(solar10*1000));
					jsolar.remove("pv_estimate90");
					jsolar.addProperty("pv_estimate90", (int)(solar90*1000));
					
					JsonObject jsonObjectPrediction = new JsonObject();
					jsonObjectPrediction.add("time", dt);				
					jsonObjectPrediction.add("pv_estimate", jsolar.getAsJsonObject().get("pv_estimate"));
					jsonObjectPrediction.add("pv_estimate10", jsolar.getAsJsonObject().get("pv_estimate10"));
					jsonObjectPrediction.add("pv_estimate90", jsolar.getAsJsonObject().get("pv_estimate90"));
					weatherPrediction.add(jsonObjectPrediction);
				}
				
				return weatherPrediction;

			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		else {
			return null;
		}
	}

	/**
	 * Sends a get request to the Solcast API.
	 * 
	 * @param endpoint the REST Api endpoint
	 * @return a JsonObject or JsonArray
	 * @throws OpenemsNamedException on error
	 */
	private JsonObject sendGetRequest(String URL) throws OpenemsNamedException {
		try {
			URL url = new URL(URL);
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
				// Parse response to JSON
				return JsonUtils.parseToJsonObject(body);
			} else {
				throw new OpenemsException(
						"Error while reading from Solcast API. Response code: " + status + ". " + body);
			}
		} catch (OpenemsException | IOException e) {
			throw new OpenemsException(
					"Unable to read from Solcast API, check API-Key and Resource-ID");
		}
	}
	
}
