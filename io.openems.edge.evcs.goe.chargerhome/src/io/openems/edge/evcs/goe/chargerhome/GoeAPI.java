package io.openems.edge.evcs.goe.chargerhome;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class GoeAPI {
	
	private final String IP;
	private boolean debugMode;
	private String debugFile;
	private int executeEveryCycle;
	private int cycle;
	private JsonObject jsonStatus;
	private GoeChargerHomeImpl parent;

	public GoeAPI(String ip, boolean debugMode, String debugFile, int StatusAfterCycles, GoeChargerHomeImpl p) {
		this.IP = ip;
		this.debugMode = debugMode;
		this.debugFile = debugFile;
		this.cycle = 0;
		this.executeEveryCycle = StatusAfterCycles;
		this.jsonStatus = null;
		this.parent = p;
	}

	/**
	 * Gets the status from go-e
	 * 
	 * See https://github.com/goecharger
	 * 
	 * @return the boolean value
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getStatus() {
			
		try {
			// Execute every x-Cycle
			if (this.cycle == 0 || this.cycle % this.executeEveryCycle == 0) {
				JsonObject json = new JsonObject();
				if (!debugMode) {
					String URL = "http://" + this.IP + "/status";
					json = this.sendRequest(URL, "GET");
				}						
			
				if (debugMode) {
					JsonParser parser = new JsonParser();
					Object obj = parser.parse(new FileReader(debugFile));
					json = (JsonObject) obj;
				}										
				
				this.cycle = 1;
				this.jsonStatus = json;
				return json;		
			}
			else {
				this.cycle++;
				return this.jsonStatus;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/** Sets the activation status for go-e
	 * 
	 * See https://github.com/goecharger
	 * 
	 * @return JsonObject with new settings
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject setActive(boolean active) {
			
		try {
			if (active != this.parent.Active) {
				JsonObject json = new JsonObject();
				if (!debugMode) {
					Integer status = 0;
					if (active) {
						status = 1;
					}
					String URL = "http://" + this.IP + "/mqtt?payload=alw=" + Integer.toString(status);
					json = this.sendRequest(URL, "PUT");
					this.parent.Active = active;					
					return json;
				}		
				else {
					this.parent.Active = active;
					return this.getStatus();
				}
			}
			else {
				return this.jsonStatus;
			}			

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/** Sets the Current in Ampere for go-e
	 * 
	 * See https://github.com/goecharger
	 * 
	 * @return JsonObject with new settings
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject setCurrent(int current) {
			
		try {
			if (current != this.parent.activeCurrent) {
				JsonObject json = new JsonObject();
				if (!debugMode) {
					Integer CurrentAmpere = current / 1000;
					String URL = "http://" + this.IP + "/mqtt?payload=amp=" + Integer.toString(CurrentAmpere);
					json = this.sendRequest(URL, "PUT");
					this.parent.activeCurrent = current;
					return json;
				}		
				else {
					this.parent.activeCurrent = current;
					return this.getStatus();
				}
			}
			else {
				return this.jsonStatus;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/** Limit MaxEnergy for go-e
	 * 
	 * See https://github.com/goecharger
	 * 
	 * @return JsonObject with new settings
	 * @throws OpenemsNamedException on error
	 */
	public boolean limitMaxEnergy(boolean limit) {
			
		try {
			JsonObject json = new JsonObject();
			if (!debugMode) {
				int stp = 0;
				if (limit) {
					stp = 2;
				}				
				String URL = "http://" + this.IP + "/mqtt?payload=stp=" + Integer.toString(stp);
				json = this.sendRequest(URL, "PUT");
				if (json != null) {
					return true;
				}
				else {
					return false;
				}
			}		
			else {
				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/** Sets the MaxEnergy in 0.1 kWh for go-e
	 * 
	 * See https://github.com/goecharger
	 * 
	 * @return JsonObject with new settings
	 * @throws OpenemsNamedException on error
	 */
	public boolean setMaxEnergy(int maxEnergy) {
			
		try {
			JsonObject json = new JsonObject();
			if (!debugMode) {
				if (maxEnergy > 0) {
					this.limitMaxEnergy(true);
				}
				else {
					this.limitMaxEnergy(false);
				}				
				String URL = "http://" + this.IP + "/mqtt?payload=dwo=" + Integer.toString(maxEnergy);
				json = this.sendRequest(URL, "PUT");
				if (json != null) {
					return true;
				}
				else {
					return false;
				}
			}		
			else {
				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Sends a get or set request to the go-e API.
	 * 
	 * @param endpoint the REST Api endpoint
	 * @return a JsonObject or JsonArray
	 * @throws OpenemsNamedException on error
	 */
	private JsonObject sendRequest(String URL, String Requestmethod) throws OpenemsNamedException {
		try {
			URL url = new URL(URL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod(Requestmethod);
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
						"Error while reading from go-e API. Response code: " + status + ". " + body);
			}
		} catch (OpenemsNamedException | IOException e) {
			throw new OpenemsException(
					"Unable to read from go-e API. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
}
