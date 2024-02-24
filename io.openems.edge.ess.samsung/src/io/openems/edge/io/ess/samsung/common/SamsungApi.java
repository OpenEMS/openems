package io.openems.edge.io.ess.samsung.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

/**
 * Implements the local Samsung ESS API.
 *
 * <p>
 * See https://github.com/tbnobody/OpenDTU
 */
public class SamsungApi {

	private final String baseUrl;

    public SamsungApi(String ip) {
        this.baseUrl = "http://" + ip;
        }

	/**
	 * Sends a get request to the openDTU.
	 *
	 * @param endpoint the REST Api endpoint
	 * @return a JsonObject or JsonArray
	 * @throws OpenemsNamedException on error
	 */
	private JsonElement sendGetRequest(String endpoint) throws OpenemsNamedException {
		try {
			var url = new URL(this.baseUrl + endpoint);
			var con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			var status = con.getResponseCode();
			String body;
			try (var in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				// Read HTTP response
				var content = new StringBuilder();
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
			}
			throw new OpenemsException("Error while reading from ESS API. Response code: " + status + ". " + body);
		} catch (OpenemsNamedException | IOException e) {
			throw new OpenemsException(
					"Unable to read from ESS API. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	
	/**
	 * Fetches the ESS Real-time Status.
	 *
	 * @return A JsonObject containing necessary fields from the ESS Real-time Status
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getEssRealtimeStatus() throws OpenemsNamedException {
	    // Construct the endpoint using the serial number from Config
	    String endpoint = "/R3EMSAPP_REAL.ems?file=ESSRealtimeStatus.json";
	    
	    // Make HTTP GET request and fetch JSON data
	    JsonElement jsonResponse = this.sendGetRequest(endpoint);
	    JsonObject jsonObject = jsonResponse.getAsJsonObject();

	    // Parse only the necessary values from the JSON data
	    JsonObject essRealtimeStatus = jsonObject.getAsJsonObject("ESSRealtimeStatus");
	    double AbsPcsPw = essRealtimeStatus.get("AbsPcsPw").getAsDouble();
	    double PvPw = essRealtimeStatus.get("PvPw").getAsDouble();
	    double PcsPw = essRealtimeStatus.get("PcsPw").getAsDouble();
	    double gridPw = essRealtimeStatus.get("GridPw").getAsDouble();
	    double consPw = essRealtimeStatus.get("ConsPw").getAsDouble();
	    int btSoc = essRealtimeStatus.get("BtSoc").getAsInt();
	    int GridStusCd = essRealtimeStatus.get("GridStusCd").getAsInt();
	    int BtStusCd = essRealtimeStatus.get("BtStusCd").getAsInt();

	    	    
	    // Create a new JsonObject containing only the necessary values
	    JsonObject necessaryData = new JsonObject();
	    necessaryData.addProperty("AbsPcsPw", AbsPcsPw);
	    necessaryData.addProperty("GridStusCd", GridStusCd);
	    necessaryData.addProperty("PcsPw", PcsPw);
	    necessaryData.addProperty("GridPw", gridPw);
	    necessaryData.addProperty("PvPw", PvPw);
	    necessaryData.addProperty("ConsPw", consPw);
	    necessaryData.addProperty("BtSoc", btSoc);
	    necessaryData.addProperty("BtStusCd", BtStusCd);

	    
	    
	    
	    
	    return necessaryData;
	}
	
	/**
	 * Fetches the ESS Weather Status.
	 *
	 * @return A JsonObject containing necessary fields from the ESS Weather Status
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getEssWeatherStatus() throws OpenemsNamedException {
	    // Construct the endpoint using the serial number from Config
	    String endpoint = "/R3EMSAPP_REAL.ems?file=Weather.json";
	    
	    // Make HTTP GET request and fetch JSON data
	    JsonElement jsonResponse = this.sendGetRequest(endpoint);
	    JsonObject jsonObject = jsonResponse.getAsJsonObject();

	    // Parse the weather data
	    JsonObject weatherInfo = jsonObject.getAsJsonObject("WeatherInfo");
	    String weather = weatherInfo.get("Weather").getAsString();
	    double cloudAll = weatherInfo.get("CloudAll").getAsDouble();
	    String tempUnit = weatherInfo.get("TempUnit").getAsString();
	    double temperature = weatherInfo.get("Temperature").getAsDouble();
	    int humidity = weatherInfo.get("Humidity").getAsInt();
	    String windDirection = weatherInfo.get("WindDirection").getAsString();
	    double windSpeed = weatherInfo.get("WindSpeed").getAsDouble();

	    // Create a new JsonObject containing the parsed weather data
	    JsonObject weatherData = new JsonObject();
	    weatherData.addProperty("Weather", weather);
	    weatherData.addProperty("CloudAll", cloudAll);
	    weatherData.addProperty("TempUnit", tempUnit);
	    weatherData.addProperty("Temperature", temperature);
	    weatherData.addProperty("Humidity", humidity);
	    weatherData.addProperty("WindDirection", windDirection);
	    weatherData.addProperty("WindSpeed", windSpeed);

	    return weatherData;
	}

}
