package io.openems.edge.evcs.vw.weconnect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class VehicleId {
	
	private final Logger log = LoggerFactory.getLogger(VehicleId.class);
	
	private static final String READY_FOR_CHARGING_STRING = "readyForCharging";
	private static final String CHARGING_STRING = "charging";

	private String vin;
	private String carName;
	private int soc = 0;
	private boolean readyToCharge = false;
	private boolean charging = false;
	private int range;
	
	public VehicleId(JsonElement vehicleJson) {
		JsonObject vehicleObject = vehicleJson.getAsJsonObject();
		vin = vehicleObject.get("vin").getAsString();
		carName = vehicleObject.get("nickname").getAsString();
		
		log.error("jsonObject: "+vehicleJson.toString());
		log.error("vin: "+vin);
		log.error("carName: "+carName);
	}

	public void update(JsonObject jsonObject) {
		log.error("jsonObject: "+jsonObject.toString());
		
		JsonObject dataNode = jsonObject.get("data").getAsJsonObject();
		
		if(dataNode == null) {
			log.error("update error --> dataNode was null");
		}
		
		JsonObject batteryNode = dataNode.get("batteryStatus").getAsJsonObject();
		
		if(batteryNode == null) {
			log.error("update error --> batteryNode was null");
		}
		
		soc = batteryNode.get("currentSOC_pct").getAsInt();
		range = batteryNode.get("cruisingRangeElectric_km").getAsInt();
		log.error("soc="+soc);
		log.error("range="+range);
		
		JsonObject chargingNode = dataNode.get("chargingStatus").getAsJsonObject();

		if(chargingNode == null) {
			log.error("update error --> chargingNode was null");
		}
		readyToCharge = chargingNode.get("chargingState").getAsString().equals(READY_FOR_CHARGING_STRING);
		charging = chargingNode.get("chargingState").getAsString().equals(CHARGING_STRING);
		
		log.error("readyToCharge="+readyToCharge);
		log.error("charging="+charging);
	}

	public int getSoc() {
		return soc;
	}

	public String getVin() {
		return vin;
	}
	
	public boolean isReadyToCharge() {
		return readyToCharge;
	}
	
	public boolean isCharging() {
		return charging;
	}
}
