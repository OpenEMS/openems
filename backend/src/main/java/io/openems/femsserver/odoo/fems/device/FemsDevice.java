package io.openems.femsserver.odoo.fems.device;

import org.java_websocket.WebSocket;

import com.abercap.odoo.Row;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.femsserver.odoo.OdooModel;
import io.openems.femsserver.odoo.OdooObject;

public class FemsDevice extends OdooObject {
	private static final String NAME = "name";
	private static final String NAME_NUMBER = "name_number";
	private static final String COMMENT = "comment";
	private static final String SOC = "soc";
	private static final String LASTMESSAGE = "lastmessage";
	private static final String LASTUPDATE = "lastupdate";
	private static final String IPV4 = "ipv4";
	private static final String OPENEMS_CONFIG = "openems_config";
	private static final String STATE = "state";
	private static final String PRODUCT_TYPE = "producttype";

	protected static String[] getFields() {
		return new String[] { NAME, NAME_NUMBER, COMMENT, SOC, LASTMESSAGE, LASTUPDATE, IPV4, OPENEMS_CONFIG, STATE,
				PRODUCT_TYPE };
	}

	private WebSocket ws = null;
	private String role = "guest";

	public FemsDevice(OdooModel<?> model, Row row) {
		super(model, row);
	}

	public String getNameNumber() {
		return get(NAME_NUMBER).toString();
	}

	public String getName() {
		try {
			return get(NAME).toString();
		} catch (Exception e) {
			return "UNKNOWN";
		}
	}

	public String getComment() {
		return get(COMMENT).toString();
	}

	public String getState() {
		return get(STATE).toString();
	}

	public String getProductType() {
		return get(PRODUCT_TYPE).toString();
	}

	public JsonObject getOpenemsConfig() {
		Object config = get(OPENEMS_CONFIG);
		if (config != null) {
			return (new JsonParser()).parse(get(OPENEMS_CONFIG).toString()).getAsJsonObject();
		} else {
			return new JsonObject();
		}
	}

	public void setOpenemsConfig(JsonObject j) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		put(OPENEMS_CONFIG, gson.toJson(j));
	}

	public void setState(String active) {
		put(STATE, active);
	}

	public void setSoc(int value) {
		put(SOC, value);
	}

	public void setLastMessage() {
		put(LASTMESSAGE, this.odooCompatibleNow());
	}

	public void setLastUpdate() {
		put(LASTUPDATE, this.odooCompatibleNow());
	}

	public void setIpV4(String value) {
		put(IPV4, value);
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

	public void setRole(String role) {
		this.role = role;
	}

	public String getRole() {
		return role;
	}

	public JsonObject toJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("name", getName());
		j.addProperty("comment", getComment());
		j.add("config", getOpenemsConfig());
		j.addProperty("role", getRole());
		j.addProperty("state", getState());
		j.addProperty("producttype", getProductType());
		return j;
	}
}
