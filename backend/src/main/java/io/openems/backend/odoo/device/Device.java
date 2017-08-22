package io.openems.backend.odoo.device;

import org.java_websocket.WebSocket;

import com.abercap.odoo.Row;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.odoo.OdooModel;
import io.openems.backend.odoo.OdooObject;

public class Device extends OdooObject {
	private WebSocket ws = null;
	private String role = "guest";

	public Device(OdooModel<?> model, Row row) {
		super(model, row);
	}

	public Integer getId() {
		return (Integer) get(Field.ID);
	}

	public String getNameNumber() {
		return getOr(Field.NAME_NUMBER, "").toString();
	}

	public String getName() {
		return getOr(Field.NAME, "UNKNOWN").toString();
	}

	public String getComment() {
		return getOr(Field.COMMENT, "").toString();
	}

	public String getState() {
		return getOr(Field.STATE, "").toString();
	}

	public String getProductType() {
		return getOr(Field.PRODUCT_TYPE, "").toString();
	}

	public JsonObject getOpenemsConfig() {
		Object config = get(Field.OPENEMS_CONFIG);
		if (config != null) {
			return (new JsonParser()).parse(get(Field.OPENEMS_CONFIG).toString()).getAsJsonObject();
		} else {
			return new JsonObject();
		}
	}

	public void setOpenemsConfig(JsonObject j) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		put(Field.OPENEMS_CONFIG, gson.toJson(j));
	}

	public void setState(String active) {
		put(Field.STATE, active);
	}

	public void setSoc(int value) {
		put(Field.SOC, value);
	}

	public void setLastMessage() {
		put(Field.LASTMESSAGE, this.odooCompatibleNow());
	}

	public void setLastUpdate() {
		put(Field.LASTUPDATE, this.odooCompatibleNow());
	}

	public void setIpV4(String value) {
		put(Field.IPV4, value);
	}

	public void setWebSocket(WebSocket ws) {
		this.ws = ws;
	}

	public WebSocket getWebSocket() {
		return this.ws;
	}

	public boolean isWebSocketConnected() {
		return getWebSocket() != null;
	}

	public void removeWebSocket() {
		setWebSocket(null);
	}

	public JsonObject toJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("name", getName());
		j.addProperty("comment", getComment());
		j.add("config", getOpenemsConfig());
		j.addProperty("state", getState());
		j.addProperty("producttype", getProductType());
		return j;
	}

	@Override
	public String toString() {
		return this.toJsonObject().toString();
	}
}
