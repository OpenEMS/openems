package io.openems.femsserver.odoo.fems.device;

import com.abercap.odoo.Row;

import io.openems.femsserver.odoo.OdooModel;
import io.openems.femsserver.odoo.OdooObject;

public class FemsDevice extends OdooObject {
	public static final String NAME = "name";
	public static final String NAME_NUMBER = "name_number";
	public static final String SOC = "soc";
	public static final String LASTMESSAGE = "lastmessage";
	public static final String LASTUPDATE = "lastupdate";
	public static final String IPV4 = "ipv4";

	protected static String[] getFields() {
		return new String[] { NAME, NAME_NUMBER, SOC, LASTMESSAGE, LASTUPDATE, IPV4 };
	}
	
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
}
